package com.course.server.session;

import com.course.common.dto.ClientSessionDto;
import com.course.common.model.Role;
import com.course.common.protocol.Response;
import com.course.common.util.JsonUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates an active client session.
 */
public class ClientSession {
    private static final Logger LOGGER = Logger.getLogger(ClientSession.class.getName());

    private final String sessionId;
    private String studentId;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private String clientVersion = "1.0.0";
    private String status = "ONLINE"; // ONLINE, DISCONNECTED, KICKED, LOGOUT
    private LocalDateTime logoutTime;
    private Long logId;

    private final Socket socket;
    private final String clientIp;
    private final int clientPort;
    private final LocalDateTime loginTime;
    private volatile Instant lastHeartbeat;
    private final PrintWriter writer;

    public ClientSession(String sessionId, Socket socket, PrintWriter writer) {
        this.sessionId = sessionId;
        this.socket = socket;
        this.writer = writer;
        this.clientIp = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "unknown";
        this.clientPort = socket.getPort();
        this.loginTime = LocalDateTime.now();
        this.lastHeartbeat = Instant.now();
    }

    public synchronized boolean sendResponse(Response response) {
        if (writer == null || socket.isClosed()) {
            return false;
        }
        try {
            String json = JsonUtil.toJson(response);
            writer.println(json);
            writer.flush();
            return !writer.checkError();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send response to session " + sessionId, e);
            return false;
        }
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
    }

    public boolean isTimedOut(long timeoutMs) {
        return Duration.between(lastHeartbeat, Instant.now()).toMillis() > timeoutMs;
    }

    public boolean isOnline() {
        return "ONLINE".equalsIgnoreCase(status) && socket != null && !socket.isClosed();
    }

    public void disconnect() {
        this.status = "DISCONNECTED";
        this.logoutTime = LocalDateTime.now();
        close();
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Error closing socket for session " + sessionId, e);
        }
    }

    public ClientSessionDto toDto() {
        LocalDateTime end = logoutTime != null ? logoutTime : LocalDateTime.now();
        long durationSeconds = Duration.between(loginTime, end).getSeconds();
        if (durationSeconds < 0) {
            durationSeconds = 0;
        }
        return new ClientSessionDto(
                sessionId,
                studentId != null ? studentId : "N/A",
                username != null ? username : "anonymous",
                email != null ? email : "N/A",
                fullName != null ? fullName : "N/A",
                role != null ? role : Role.STUDENT,
                clientIp,
                clientPort,
                loginTime,
                durationSeconds,
                status,
                clientVersion
        );
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getClientIp() {
        return clientIp;
    }

    public int getClientPort() {
        return clientPort;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }
}
