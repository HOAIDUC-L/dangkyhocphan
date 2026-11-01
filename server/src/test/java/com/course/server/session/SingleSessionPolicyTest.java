package com.course.server.session;

import com.course.common.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class SingleSessionPolicyTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = SessionManager.getInstance();
        sessionManager.clearAll();
    }

    @Test
    @DisplayName("Single-session policy: 1 student = 1 active socket. Reject concurrent second login")
    void testSingleSessionEnforcement() {
        Socket mockSocket1 = new Socket();
        Socket mockSocket2 = new Socket();

        ClientSession session1 = new ClientSession("SESS-1", mockSocket1, new PrintWriter(new StringWriter()));
        ClientSession session2 = new ClientSession("SESS-2", mockSocket2, new PrintWriter(new StringWriter()));

        sessionManager.addSession(session1);
        sessionManager.addSession(session2);

        // First login succeeds
        boolean firstBound = sessionManager.bindUser("SV001", "sv01", "sv01@ptit.edu.vn", "Nguyen Van A", Role.STUDENT, "SESS-1");
        assertTrue(firstBound, "First login for SV001 must succeed");
        assertTrue(sessionManager.isStudentOnline("SV001"));

        // Second login from another socket/session fails
        boolean secondBound = sessionManager.bindUser("SV001", "sv01", "sv01@ptit.edu.vn", "Nguyen Van A", Role.STUDENT, "SESS-2");
        assertFalse(secondBound, "Second login attempt for SV001 must be rejected by single-session policy");

        // Logout first session
        sessionManager.unbindUser("SESS-1");
        assertFalse(sessionManager.isStudentOnline("SV001"));

        // Now second session can log in
        boolean retryBound = sessionManager.bindUser("SV001", "sv01", "sv01@ptit.edu.vn", "Nguyen Van A", Role.STUDENT, "SESS-2");
        assertTrue(retryBound, "After previous logout, second session can now log in");
    }

    @Test
    @DisplayName("When client exits/disconnects, session is retained in list as DISCONNECTED rather than being deleted")
    void testClientExitRetainsSessionInList() {
        Socket mockSocket1 = new Socket();
        ClientSession session1 = new ClientSession("SESS-10", mockSocket1, new PrintWriter(new StringWriter()));
        session1.setClientVersion("1.0.0");
        sessionManager.addSession(session1);

        sessionManager.bindUser("SV002", "sv02", "sv02@ptit.edu.vn", "Tran Thi B", Role.STUDENT, "SESS-10");
        assertTrue(sessionManager.isStudentOnline("SV002"));
        assertEquals("ONLINE", session1.getStatus());

        // Client exits / disconnects
        sessionManager.removeSession("SESS-10");

        // 1. Session is still retained in list, not deleted
        assertNotNull(sessionManager.getSession("SESS-10"), "Session must not be removed from session map");
        java.util.List<com.course.common.dto.ClientSessionDto> dtos = sessionManager.getAllSessionDtos();
        com.course.common.dto.ClientSessionDto dto = dtos.stream()
                .filter(d -> "SESS-10".equals(d.getSessionId()))
                .findFirst()
                .orElse(null);
        assertNotNull(dto, "Session must still appear in getAllSessionDtos list");
        assertEquals("DISCONNECTED", dto.getStatus(), "Status in DTO must be DISCONNECTED");
        assertEquals("1.0.0", dto.getClientVersion(), "Client version must be preserved");
        assertEquals("SV002", dto.getStudentId(), "Student ID must be preserved");

        // 2. Student is no longer online
        assertFalse(sessionManager.isStudentOnline("SV002"), "Student SV002 should not be online after disconnect");

        // 3. Student can log in again on a new session without single-session block
        Socket mockSocket2 = new Socket();
        ClientSession session2 = new ClientSession("SESS-11", mockSocket2, new PrintWriter(new StringWriter()));
        session2.setClientVersion("1.0.0");
        sessionManager.addSession(session2);

        boolean reloginBound = sessionManager.bindUser("SV002", "sv02", "sv02@ptit.edu.vn", "Tran Thi B", Role.STUDENT, "SESS-11");
        assertTrue(reloginBound, "Student must be able to log in again on a new session");
        assertTrue(sessionManager.isStudentOnline("SV002"));
        assertEquals(1, sessionManager.getOnlineCount(), "Only 1 session should be online");

        // Both sessions (previous disconnected session and new active session) are retained
        assertEquals(2, sessionManager.getAllSessions().size(), "Both previous and active sessions must be retained in the list");
    }
}
