package com.course.server.config;

import com.course.common.constants.AppConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server configuration loader from application.properties.
 */
public class ServerConfig {
    private static final Logger LOGGER = Logger.getLogger(ServerConfig.class.getName());
    private static final ServerConfig INSTANCE = new ServerConfig();

    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private int dbPoolMaxSize;
    private int dbPoolMinIdle;
    private long dbPoolIdleTimeout;
    private long dbPoolConnectionTimeout;

    private int serverPort;
    private int serverMaxClients;
    private long heartbeatTimeoutMs;
    private String nodeName = "SERVER_NODE_1";

    private ServerConfig() {
        loadProperties();
    }

    public static ServerConfig getInstance() {
        return INSTANCE;
    }

    private void loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                LOGGER.warning("application.properties not found in classpath, using fallback defaults.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load application.properties", e);
        }

        this.dbUrl = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/course_registration");
        this.dbUsername = props.getProperty("db.username", "postgres");
        this.dbPassword = props.getProperty("db.password", "123456");
        this.dbPoolMaxSize = Integer.parseInt(props.getProperty("db.pool.maximum-size", "20"));
        this.dbPoolMinIdle = Integer.parseInt(props.getProperty("db.pool.minimum-idle", "5"));
        this.dbPoolIdleTimeout = Long.parseLong(props.getProperty("db.pool.idle-timeout", "30000"));
        this.dbPoolConnectionTimeout = Long.parseLong(props.getProperty("db.pool.connection-timeout", "10000"));

        // Allow system property overrides: -Dserver.port=9991 -Dserver.node=SERVER_NODE_1
        String sysPort = System.getProperty("server.port");
        this.serverPort = sysPort != null ? Integer.parseInt(sysPort)
                : Integer.parseInt(props.getProperty("server.port", String.valueOf(AppConstants.DEFAULT_PORT)));

        String sysNode = System.getProperty("server.node");
        this.nodeName = sysNode != null ? sysNode : props.getProperty("server.node-name", "SERVER_NODE_1");

        this.serverMaxClients = Integer.parseInt(props.getProperty("server.max-clients", "100"));
        this.heartbeatTimeoutMs = Long.parseLong(props.getProperty("server.heartbeat.timeout-ms", "30000"));
    }

    /**
     * Applies dynamic arguments from CLI:
     * Supports:
     * - Positional: [port] [nodeName] (e.g. "9991" "SERVER_NODE_1")
     * - Named flags: --port=9991 --node=SERVER_NODE_1
     */
    public void applyCliArgs(String[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        int positionalIndex = 0;
        for (String arg : args) {
            if (arg == null || arg.isBlank() || arg.startsWith("--cli") || arg.startsWith("--headless")) {
                continue;
            }

            if (arg.startsWith("--port=")) {
                try {
                    this.serverPort = Integer.parseInt(arg.substring("--port=".length()).trim());
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid CLI port argument: " + arg);
                }
            } else if (arg.startsWith("--node=")) {
                this.nodeName = arg.substring("--node=".length()).trim();
            } else if (arg.startsWith("--node-name=")) {
                this.nodeName = arg.substring("--node-name=".length()).trim();
            } else if (!arg.startsWith("-")) {
                // Positional arguments
                if (positionalIndex == 0) {
                    try {
                        this.serverPort = Integer.parseInt(arg.trim());
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Invalid positional port argument: " + arg);
                    }
                    positionalIndex++;
                } else if (positionalIndex == 1) {
                    this.nodeName = arg.trim();
                    positionalIndex++;
                }
            }
        }
        LOGGER.info(String.format("Configured Node: %s on Port: %d", this.nodeName, this.serverPort));
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public int getDbPoolMaxSize() {
        return dbPoolMaxSize;
    }

    public int getDbPoolMinIdle() {
        return dbPoolMinIdle;
    }

    public long getDbPoolIdleTimeout() {
        return dbPoolIdleTimeout;
    }

    public long getDbPoolConnectionTimeout() {
        return dbPoolConnectionTimeout;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getServerMaxClients() {
        return serverMaxClients;
    }

    public long getHeartbeatTimeoutMs() {
        return heartbeatTimeoutMs;
    }

    public String getNodeName() {
        return nodeName != null ? nodeName : "SERVER_NODE_1";
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
