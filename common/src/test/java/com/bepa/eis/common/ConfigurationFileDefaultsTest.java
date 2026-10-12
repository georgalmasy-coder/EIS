package com.bepa.eis.common;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.*;

public class ConfigurationFileDefaultsTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("eis-configuration-defaults-test-");
        Path file = directory.resolve("eis-global.properties");
        try {
            String original = "# Keep comments and formatting\r\ncustom.value = changed\r\nempty.value=\r\n"
                    + "continued=one\\\r\n two\r\nescaped\\ key=existing";
            Files.writeString(file, original, StandardCharsets.ISO_8859_1);
            String defaults = "custom.value=default\nempty.value=default\ncontinued=default\n"
                    + "escaped\\ key=default\ncleanup.session.job.retention.days=1\n"
                    + "cleanup.session.job.cron=0 2 * * *\nnew.value=Æøå\\\\path\n";
            Properties values = ConfigurationFileDefaults.loadAndAppend(file, defaults);
            String appended = Files.readString(file, StandardCharsets.ISO_8859_1);
            check(appended.startsWith(original), "Existing bytes preserved");
            check("changed".equals(values.getProperty("custom.value")), "Configured value preserved");
            check("".equals(values.getProperty("empty.value")), "Intentional empty value preserved");
            check("onetwo".equals(values.getProperty("continued")), "Continued property recognized");
            check("existing".equals(values.getProperty("escaped key")), "Escaped key recognized");
            check("0 2 * * *".equals(values.getProperty("cleanup.session.job.cron")), "Missing cron added");
            check(!appended.replace("\r\n", "").contains("\n"), "Original CRLF style retained");
            ConfigurationFileDefaults.loadAndAppend(file, defaults);
            check(appended.equals(Files.readString(file, StandardCharsets.ISO_8859_1)), "Repeated reload does not rewrite file");
            Properties reread = new Properties();
            try (var input = Files.newInputStream(file)) { reread.load(input); }
            check("Æøå\\path".equals(reread.getProperty("new.value")), "Non-ASCII and backslash survive disk round trip");

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                String upgraded = defaults + "another.new.value=42\n";
                Future<?> first = executor.submit(() -> append(file, upgraded));
                Future<?> second = executor.submit(() -> append(file, upgraded));
                first.get(); second.get();
                String concurrent = Files.readString(file, StandardCharsets.ISO_8859_1);
                check(concurrent.indexOf("another.new.value=42") == concurrent.lastIndexOf("another.new.value=42"), "Concurrent reload adds key once");
            } finally { executor.shutdownNow(); }
            System.out.println("Configuration defaults tests passed.");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void append(Path file, String defaults) {
        try { ConfigurationFileDefaults.loadAndAppend(file, defaults); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
