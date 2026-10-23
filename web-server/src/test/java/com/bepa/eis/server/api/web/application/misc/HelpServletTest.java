package com.bepa.eis.server.api.web.application.misc;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Standalone servlet regression test; no database or running server required. */
public final class HelpServletTest {
    public static void main(String[] args) throws Exception {
        Fixture servlet = new Fixture();
        servlet.resources.put("/WEB-INF/help/en/page.md", "# English");
        servlet.resources.put("/WEB-INF/help/dk/page.md", "# Dansk hjælp: æøå");

        Result english = servlet.request("page");
        check(english.text().equals("# English"), "English is the default");
        check("en".equals(english.headers.get("Content-Language")), "English header");

        servlet.preference = "dk";
        Result danish = servlet.request("page");
        check(danish.text().equals("# Dansk hjælp: æøå"), "Danish preference and UTF-8");
        check("da".equals(danish.headers.get("Content-Language")), "Danish language tag");
        check(danish.headers.get("Cache-Control").contains("no-store"), "No cached language responses");

        servlet.developmentMode = false;
        servlet.preferenceReads = 0;
        check(servlet.request("page").text().equals("# English"), "Production always uses English");
        check(servlet.preferenceReads == 0, "Production does not read development preferences");

        servlet.developmentMode = true;
        servlet.resources.remove("/WEB-INF/help/dk/page.md");
        Result fallback = servlet.request("page");
        check(fallback.text().equals("# English") && "en".equals(fallback.headers.get("Content-Language")), "English fallback");
        check(servlet.request("missing").status == 404, "Missing help is 404");
        for (String invalid : new String[]{"../page", "en/page", "", "page.md"}) {
            check(servlet.request(invalid).status == 400, "Unsafe page rejected: " + invalid);
        }
        for (String invalid : new String[]{"../dk", "da", "unsupported", ""}) {
            servlet.preference = invalid;
            check(servlet.request("page").text().equals("# English"), "Invalid persisted value defaults to English");
            check(!HelpLanguage.isSupported(invalid), "Invalid preference rejected on write");
        }
        check(HelpLanguage.isSupported("en") && HelpLanguage.isSupported("dk"), "Supported preference values");
        check(HelpLanguage.fromPreference(null) == HelpLanguage.ENGLISH, "Missing preference default");

        Path help = Path.of("web-server/src/main/webapp/WEB-INF/help");
        try (var files = Files.list(help.resolve("en"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".md")).toList()) {
                Path translation = help.resolve("dk").resolve(file.getFileName());
                check(Files.isRegularFile(translation), "Danish counterpart: " + file.getFileName());
                String translated = Files.readString(translation);
                check(translated.startsWith("# ") && !translated.equals(Files.readString(file)), "Translated content: " + file.getFileName());
            }
        }
        System.out.println("HelpServletTest passed: defaults, production gate, language routing, UTF-8, fallback, invalid paths, translation coverage.");
    }

    private static final class Fixture extends HelpServlet {
        final Map<String, String> resources = new HashMap<>();
        boolean developmentMode = true;
        String preference;
        int preferenceReads;

        @Override protected boolean isLanguageSelectionEnabled() { return developmentMode; }
        @Override protected HelpLanguage readHelpLanguage(HttpServletRequest request, HttpServletResponse response) {
            preferenceReads++;
            return HelpLanguage.fromPreference(preference);
        }
        @Override public ServletContext getServletContext() {
            return proxy(ServletContext.class, (method, args) -> {
                if (method.equals("getResourceAsStream")) {
                    String text = resources.get(args[0]);
                    return text == null ? null : new java.io.ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });
        }
        Result request(String page) throws Exception {
            Result result = new Result();
            HttpServletRequest request = proxy(HttpServletRequest.class, (method, args) -> method.equals("getParameter") ? page : null);
            HttpServletResponse response = proxy(HttpServletResponse.class, (method, args) -> {
                switch (method) {
                    case "sendError" -> result.status = (int) args[0];
                    case "setHeader" -> result.headers.put((String) args[0], (String) args[1]);
                    case "getOutputStream" -> { return result.output; }
                }
                return null;
            });
            doGet(request, response);
            return result;
        }
    }
    private static final class Result {
        int status = 200;
        final Map<String, String> headers = new HashMap<>();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final ServletOutputStream output = new ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener listener) { }
            @Override public void write(int value) throws IOException { bytes.write(value); }
        };
        String text() { return bytes.toString(StandardCharsets.UTF_8); }
    }
    private interface Invocation { Object call(String method, Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                (proxy, method, args) -> invocation.call(method.getName(), args)));
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
