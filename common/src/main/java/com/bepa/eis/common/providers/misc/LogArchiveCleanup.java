package com.bepa.eis.common.providers.misc;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LogArchiveCleanup {
    private LogArchiveCleanup() {}

    /** Only direct, regular .gz files in the explicitly configured directories are eligible. */
    public static long deleteExpired(List<Path> directories, int retentionDays, Instant now) throws IOException {
        if (retentionDays < 1 || retentionDays > 36500) throw new IllegalArgumentException("Invalid retention days");
        Instant cutoff = now.minus(Duration.ofDays(retentionDays));
        Set<Path> visited = new HashSet<>();
        long deleted = 0;
        IOException failure = null;
        for (Path configured : directories) {
            Path directory = configured.toAbsolutePath().normalize();
            if (!visited.add(directory)) continue;
            try {
                // Refuse a root folder or directory link; the job never traverses subdirectories.
                if (directory.getParent() == null || Files.isSymbolicLink(directory)) {
                    throw new IOException("Invalid log archive directory: " + directory);
                }
                if (Files.notExists(directory)) continue;
                Path resolved = directory.toRealPath();
                try (DirectoryStream<Path> entries = Files.newDirectoryStream(resolved)) {
                    for (Path entry : entries) {
                        if (!entry.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".gz")) continue;
                        try {
                            BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                            if (!attributes.isRegularFile() || attributes.isSymbolicLink()) continue;
                            if (attributes.lastModifiedTime().toInstant().isBefore(cutoff)) {
                                // The enumerated absolute target must stay in the verified log directory.
                                Path target = entry.toAbsolutePath().normalize();
                                if (!target.getParent().equals(resolved)) throw new IOException("Log file outside configured directory");
                                Files.delete(target);
                                deleted++;
                            }
                        } catch (NoSuchFileException ignored) {
                            // A concurrent rotation/cleanup already removed this archive.
                        } catch (IOException e) {
                            if (failure == null) failure = new IOException("Some log archives could not be deleted");
                            failure.addSuppressed(e);
                        }
                    }
                }
            } catch (IOException e) {
                if (failure == null) failure = new IOException("Log archive cleanup failed");
                failure.addSuppressed(e);
            }
        }
        if (failure != null) throw failure;
        return deleted;
    }
}
