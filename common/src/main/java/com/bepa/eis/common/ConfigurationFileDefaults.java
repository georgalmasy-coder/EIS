package com.bepa.eis.common;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.TreeSet;

final class ConfigurationFileDefaults {
    private ConfigurationFileDefaults() {}

    /** Append only missing keys, under a lock shared by the two server processes. */
    static synchronized Properties loadAndAppend(Path file, String defaultContent) throws IOException {
        Properties defaults = new Properties();
        defaults.load(new StringReader(defaultContent));
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(channel.size()));
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) { }
            String original = new String(buffer.array(), 0, buffer.position(), StandardCharsets.ISO_8859_1);
            Properties loaded = new Properties();
            loaded.load(new StringReader(original));
            Properties missing = new Properties();
            for (String key : defaults.stringPropertyNames()) {
                // Empty values are intentional settings, not missing keys.
                if (!loaded.containsKey(key)) missing.setProperty(key, defaults.getProperty(key));
            }
            if (missing.isEmpty()) return loaded;

            String newline = original.contains("\r\n") ? "\r\n" : original.contains("\n") ? "\n" : System.lineSeparator();
            StringBuilder added = new StringBuilder(newline).append(newline)
                    .append("# Automatically added default configuration values").append(newline);
            for (String key : new TreeSet<>(missing.stringPropertyNames())) {
                // Properties.store provides correct escaping, including non-ASCII values.
                Properties entry = new Properties();
                entry.setProperty(key, missing.getProperty(key));
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                entry.store(output, null);
                String serialized = output.toString(StandardCharsets.ISO_8859_1);
                for (String line : serialized.split("\\r?\\n")) {
                    if (!line.startsWith("#")) added.append(line).append(newline);
                }
            }
            channel.position(channel.size());
            ByteBuffer additions = StandardCharsets.ISO_8859_1.encode(added.toString());
            while (additions.hasRemaining()) channel.write(additions);
            channel.force(true);
            loaded.putAll(missing);
            return loaded;
        }
    }
}
