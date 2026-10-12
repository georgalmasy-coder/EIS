package com.bepa.eis.common.providers.misc;

import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.util.List;

public class LogArchiveCleanupTest {
    public static void main(String[] args) throws Exception {
        Path parent = Path.of("common", "target").toAbsolutePath().normalize();
        Files.createDirectories(parent);
        Path root = Files.createTempDirectory(parent, "log-cleanup-test-");
        Instant now = Instant.parse("2026-10-09T02:00:00Z");
        try {
            Path web = Files.createDirectory(root.resolve("web-server"));
            Path integration = Files.createDirectory(root.resolve("integration-server"));
            Path oldWeb = file(web.resolve("old.log.gz"), now.minus(Duration.ofDays(31)));
            Path oldIntegration = file(integration.resolve("old.log.GZ"), now.minus(Duration.ofDays(31)));
            Path boundary = file(web.resolve("boundary.log.gz"), now.minus(Duration.ofDays(30)));
            Path recent = file(web.resolve("recent.log.gz"), now.minus(Duration.ofDays(29)));
            Path active = file(web.resolve("active.log"), now.minus(Duration.ofDays(90)));
            Path unrelated = file(web.resolve("other.zip"), now.minus(Duration.ofDays(90)));
            Path nested = Files.createDirectory(web.resolve("nested"));
            Path nestedLog = file(nested.resolve("old.log.gz"), now.minus(Duration.ofDays(90)));
            Path gzipDirectory = Files.createDirectory(web.resolve("directory.gz"));
            Files.setLastModifiedTime(gzipDirectory, FileTime.from(now.minus(Duration.ofDays(90))));
            check(LogArchiveCleanup.deleteExpired(List.of(web, integration, web), 30, now) == 2, "Both servers and duplicate paths");
            check(!Files.exists(oldWeb) && !Files.exists(oldIntegration), "Expired gzip files deleted");
            for (Path preserved : List.of(boundary, recent, active, unrelated, nestedLog, gzipDirectory)) {
                check(Files.exists(preserved), "Preserve " + preserved.getFileName());
            }
            check(LogArchiveCleanup.deleteExpired(List.of(web, integration), 30, now) == 0, "Second run is harmless");
            check(LogArchiveCleanup.deleteExpired(List.of(root.resolve("missing")), 30, now) == 0, "No archive directory yet");
            for (int days : new int[]{0, -1, 36501}) {
                try { LogArchiveCleanup.deleteExpired(List.of(web), days, now); throw new AssertionError("Invalid retention accepted"); }
                catch (IllegalArgumentException expected) { }
            }
            try { LogArchiveCleanup.deleteExpired(List.of(root.getRoot()), 30, now); throw new AssertionError("Filesystem root accepted"); }
            catch (java.io.IOException expected) { }
            try { LogArchiveCleanup.deleteExpired(List.of(active), 30, now); throw new AssertionError("Invalid directory error hidden"); }
            catch (java.io.IOException expected) { }
            System.out.println("Log archive cleanup tests passed.");
        } finally {
            // Only remove the isolated test tree, after checking its absolute workspace location.
            if (!root.toAbsolutePath().normalize().getParent().equals(parent)) throw new AssertionError("Invalid test cleanup path");
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static Path file(Path path, Instant modified) throws Exception {
        Files.writeString(path, "test archive placeholder");
        Files.setLastModifiedTime(path, FileTime.from(modified));
        return path;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
