package com.bepa.eis.common.providers.security;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.common.providers.UserProvider;
import java.util.*;

public class SessionManagerTest {
    public static void main(String[] args) {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            String id = SessionIdGenerator.create();
            check(SessionIdGenerator.isValid(id), "43-character opaque id");
            check(Base64.getUrlDecoder().decode(id).length == 32, "256 random bits");
            check(generated.add(id), "Independent ids");
        }
        check(!SessionIdGenerator.isValid("user@example.com"), "Legacy email sessions rejected");
        MemorySessions sessions = new MemorySessions();
        List<WebSession> audited = new ArrayList<>();
        SessionManager manager = new SessionManager(sessions, new UserProvider(null) {
            @Override public String getEmailByUserId(Integer userId) { return "same@example.com"; }
        }) {
            @Override protected void audit(WebSession ws, String type, String description, String status) {
                audited.add(ws);
            }
        };
        String first = SessionIdGenerator.create();
        String second = SessionIdGenerator.create();
        manager.login(first, 7, null, null, null);
        sessions.rows.get(first).setProjectId(10);
        manager.login(second, 7, null, null, null);
        sessions.rows.get(second).setProjectId(20);
        check(sessions.rows.size() == 2, "Second login preserves first session");
        check(sessions.rows.get(first).getProjectId() == 10, "Project context remains independent");
        check(sessions.rows.get(first).getUserId().equals(sessions.rows.get(second).getUserId()), "Shared user identity");
        manager.logout(first);
        check(sessions.rows.size() == 1 && sessions.rows.containsKey(second), "Logout ends only one login");
        check(audited.stream().allMatch(ws -> ws.getUserId() == 7 && ws.getEmail().equals("same@example.com")), "Audit preserves user identity");
        manager.expire(second);
        check(sessions.rows.isEmpty(), "Expiry removes its session");
        try {
            manager.login("same@example.com", 7, null, null, null);
            throw new AssertionError("Email accepted as session id");
        } catch (IllegalArgumentException expected) { }
        System.out.println("Session isolation tests passed.");
    }

    private static class MemorySessions extends SessionProvider {
        final Map<String, WebSession> rows = new HashMap<>();
        MemorySessions() { super(null); }
        @Override public boolean createSession(WebSession ws) {
            if (rows.putIfAbsent(ws.getSessionId(), ws) != null) throw new AssertionError("Duplicate session");
            return true;
        }
        @Override public WebSession getBySessionId(String id) { return rows.get(id); }
        @Override public boolean endSession(String id, String reason) { return rows.containsKey(id); }
        @Override public boolean deleteBySessionId(String id) { return rows.remove(id) != null; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
