package com.course.server.network;

import com.course.common.dto.CourseUpdatedPushDto;
import com.course.common.util.JsonUtil;
import com.course.server.config.ServerConfig;
import com.course.server.service.BroadcastService;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background daemon thread listening for PostgreSQL LISTEN/NOTIFY events on 'course_channel'.
 * Provides cross-server real-time synchronization between cluster nodes (e.g. Node 1 and Node 2).
 * Uses a dedicated, standalone JDBC connection outside the HikariCP pool.
 */
public class PostgresNotificationListener {
    private static final Logger LOGGER = Logger.getLogger(PostgresNotificationListener.class.getName());
    public static final String CHANNEL_NAME = "course_channel";

    private static final PostgresNotificationListener INSTANCE = new PostgresNotificationListener();

    private Thread listenerThread;
    private Connection dbConnection;
    private volatile boolean running = false;
    private volatile boolean listening = false;

    private PostgresNotificationListener() {
    }

    public static PostgresNotificationListener getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        this.running = true;
        this.listenerThread = new Thread(this::listenLoop, "Postgres-NotificationListenerThread");
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        this.running = false;
        this.listening = false;

        if (listenerThread != null) {
            listenerThread.interrupt();
        }

        closeConnection();
        LOGGER.info("PostgresNotificationListener stopped.");
    }

    public boolean isListening() {
        return listening;
    }

    private void listenLoop() {
        ServerConfig config = ServerConfig.getInstance();
        LOGGER.info(String.format("[%s] Starting PostgresNotificationListener on channel '%s'...",
                config.getNodeName(), CHANNEL_NAME));

        while (running) {
            try {
                if (dbConnection == null || dbConnection.isClosed()) {
                    connectAndListen();
                }

                if (dbConnection != null && !dbConnection.isClosed()) {
                    PGConnection pgConn = dbConnection.unwrap(PGConnection.class);
                    // getNotifications(500) blocks up to 500ms or until notifications arrive
                    PGNotification[] notifications = pgConn.getNotifications(500);

                    if (notifications != null && notifications.length > 0) {
                        for (PGNotification notification : notifications) {
                            if (CHANNEL_NAME.equalsIgnoreCase(notification.getName())) {
                                handleNotificationPayload(notification.getParameter());
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                this.listening = false;
                if (running) {
                    LOGGER.log(Level.WARNING, String.format("[%s] PostgreSQL notification connection lost: %s. Reconnecting in 3s...",
                            config.getNodeName(), e.getMessage()));
                    closeConnection();
                    sleepQuietly(3000);
                }
            } catch (Exception e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "Unexpected error in PostgresNotificationListener", e);
                    sleepQuietly(2000);
                }
            }
        }
        closeConnection();
    }

    private void connectAndListen() throws SQLException {
        ServerConfig config = ServerConfig.getInstance();
        try {
            // Establish dedicated standalone connection outside connection pool
            this.dbConnection = DriverManager.getConnection(
                    config.getDbUrl(),
                    config.getDbUsername(),
                    config.getDbPassword()
            );

            try (Statement stmt = dbConnection.createStatement()) {
                stmt.execute("LISTEN " + CHANNEL_NAME);
            }
            this.listening = true;
            LOGGER.info(String.format("[%s] Successfully listening on PostgreSQL channel '%s'. Real-time cluster sync ACTIVE.",
                    config.getNodeName(), CHANNEL_NAME));
        } catch (SQLException e) {
            this.listening = false;
            throw e;
        }
    }

    private void handleNotificationPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }

        try {
            CourseUpdatedPushDto dto = JsonUtil.fromJson(payload, CourseUpdatedPushDto.class);
            if (dto != null && dto.getCourseId() != null) {
                LOGGER.info(String.format("[%s] Received PG NOTIFY '%s': Course %s -> %d slots. Broadcasting to local clients.",
                        ServerConfig.getInstance().getNodeName(), CHANNEL_NAME, dto.getCourseId(), dto.getRemainingSlots()));
                BroadcastService.getInstance().notifyCourseUpdatedLocal(dto.getCourseId(), dto.getRemainingSlots());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse PG notification payload: " + payload, e);
        }
    }

    private void closeConnection() {
        this.listening = false;
        if (dbConnection != null) {
            try {
                if (!dbConnection.isClosed()) {
                    dbConnection.close();
                }
            } catch (SQLException ignored) {
            }
            dbConnection = null;
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
