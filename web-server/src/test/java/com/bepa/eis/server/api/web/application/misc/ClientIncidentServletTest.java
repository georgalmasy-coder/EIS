package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.ServiceType;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Standalone regression suite; uses no database and writes no incidents. */
public class ClientIncidentServletTest {
    public static void main(String[] args) throws Exception {
        check(ServiceType.valueOf(4) == ServiceType.CLIENT_JAVASCRIPT, "Client service can be read back");
        Fixture valid = new Fixture();
        valid.post("""
                {"page":"systemsbreakdown-edit","message":"Pointer capture failed",
                "file":"/js/components/dialog-geometry.js","line":69,"column":24,
                "stack":"NotFoundError: Pointer capture failed\\n    at start (dialog-geometry.js:69:24)",
                "gesture":"resize","build":"build-123","userId":999,"customerId":999}
                """);
        check(valid.status == 204 && valid.writes == 1, "Valid report accepted");
        check(valid.savedSession == valid.user, "Identity comes from session");
        check(valid.module.equals("frontend.systemsbreakdown-edit"), "Page module");
        check(valid.trace.contains("gesture: resize") && valid.trace.contains("build: build-123"), "Diagnostic context");
        check(valid.trace.endsWith("NotFoundError: Pointer capture failed\n    at start (dialog-geometry.js:69:24)"), "Original JavaScript stack retained");
        check(!valid.trace.contains("ClientIncidentServlet.java"), "No fabricated Java stack");

        for (String invalid : new String[]{"{", "null", "[]", "{}", "{\"page\":\"unknown\",\"message\":\"Oops\"}"}) {
            Fixture f = new Fixture(); f.post(invalid);
            check(f.status == 400 && f.writes == 0, "Invalid payload rejected");
        }
        Fixture large = new Fixture(); large.post("x".repeat(48_001));
        check(large.status == 413 && large.writes == 0, "Chunked/unknown-length payload bounded");
        Fixture unauthenticated = new Fixture(); unauthenticated.user = null;
        unauthenticated.post("{}");
        check(unauthenticated.status == 401 && unauthenticated.writes == 0, "No writes without session");
        Fixture rate = new Fixture();
        for (int i = 0; i < 21; i++) rate.post("{\"page\":\"systemsbreakdown-main\",\"message\":\"Oops\"}");
        check(rate.status == 429 && rate.writes == 20, "Reports limited across same session");
        System.out.println("ClientIncidentServletTest passed: JS traces, session identity, validation, size and rate limits. No database writes.");
    }

    static final class Fixture extends ClientIncidentServlet {
        WebSession user = new WebSession(), savedSession;
        int status, writes;
        String module, trace;
        final Map<String, Object> attributes = new HashMap<>();
        final HttpSession session = proxy(HttpSession.class, (method, args) -> switch (method) {
            case "getAttribute" -> attributes.get(args[0]);
            case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
            default -> null;
        });

        @Override public String getSessionIdFromRequest(HttpServletRequest request) { return "test-session"; }
        @Override public WebSession getSession(HttpServletRequest request) { return user; }
        @Override protected IncidentProvider createIncidentProvider(WebSession webSession) {
            savedSession = webSession;
            return new IncidentProvider(webSession) {
                @Override public void createJavaScriptIncident(String module, String message, String info, String trace) {
                    Fixture.this.module = module; Fixture.this.trace = trace; writes++;
                }
            };
        }

        void post(String body) throws Exception {
            BufferedReader reader = new BufferedReader(new StringReader(body));
            HttpServletRequest request = proxy(HttpServletRequest.class, (method, args) -> switch (method) {
                case "getSession" -> session;
                case "getContentLengthLong" -> -1L;
                case "getReader" -> reader;
                default -> null;
            });
            HttpServletResponse response = proxy(HttpServletResponse.class, (method, args) -> {
                if (method.equals("setStatus")) status = (Integer) args[0];
                return null;
            });
            doPost(request, response);
        }
    }

    interface Call { Object invoke(String method, Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> call.invoke(method.getName(), args)));
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
