package com.course.server.session;

import com.course.common.dto.ClientSessionDto;
import com.course.common.protocol.Action;
import com.course.common.protocol.Request;
import com.course.common.util.JsonUtil;
import com.course.server.network.TcpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClientDisconnectRetentionIntegrationTest {

    private int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Test
    @DisplayName("End-to-end TCP: when client terminates connection, server retains session on list as DISCONNECTED")
    void testClientSocketDisconnectRetainsSession() throws Exception {
        int testPort = findFreePort();
        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.clearAll();

        TcpServer server = new TcpServer(testPort, 50, 10000);
        server.start();

        try {
            // 1. Client connects via TCP Socket
            Socket clientSocket = new Socket("127.0.0.1", testPort);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            // Send a ping to establish handshake
            Request ping = Request.of(Action.PING, "PING");
            writer.println(JsonUtil.toJson(ping));
            String pong = reader.readLine();
            assertNotNull(pong, "Server must respond to ping");

            // Verify session is registered and online
            Thread.sleep(300);
            assertEquals(1, sessionManager.getOnlineCount(), "Online count should be 1");
            List<ClientSessionDto> initialDtos = sessionManager.getAllSessionDtos();
            assertEquals(1, initialDtos.size(), "Total sessions should be 1");
            assertEquals("ONLINE", initialDtos.get(0).getStatus(), "Status should be ONLINE");
            assertEquals("1.0.0", initialDtos.get(0).getClientVersion(), "Client version should be 1.0.0");
            String targetSessionId = initialDtos.get(0).getSessionId();

            // 2. Client exits / closes socket
            clientSocket.close();

            // Wait for Server's ClientHandler to process EOF and handle disconnect
            Thread.sleep(600);

            // 3. Verification: Session is NOT deleted and STILL RETAINED on the list
            List<ClientSessionDto> afterExitDtos = sessionManager.getAllSessionDtos();
            assertEquals(1, afterExitDtos.size(), "Session must NOT be removed from list upon client exit");

            ClientSessionDto retainedDto = afterExitDtos.get(0);
            assertEquals(targetSessionId, retainedDto.getSessionId());
            assertEquals("DISCONNECTED", retainedDto.getStatus(), "Status must be updated to DISCONNECTED");
            assertEquals("1.0.0", retainedDto.getClientVersion(), "Client version must be retained");
            assertEquals(0, sessionManager.getOnlineCount(), "Online count must be 0 after exit");

        } finally {
            server.stop();
            sessionManager.clearAll();
        }
    }
}
