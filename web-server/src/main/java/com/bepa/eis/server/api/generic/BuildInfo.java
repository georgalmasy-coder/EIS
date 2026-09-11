package com.bepa.eis.server.api.generic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class BuildInfo {
    private static final String STARTUP_VERSION = Long.toString(System.currentTimeMillis());
    private static final String BUILD_NUMBER = loadBuildNumber();

    private BuildInfo() {
    }

    public static String buildNumber() {
        return BUILD_NUMBER;
    }

    private static String loadBuildNumber() {
        Properties properties = new Properties();

        try (InputStream inputStream = BuildInfo.class.getResourceAsStream("/build-info.properties")) {
            if (inputStream == null) {
                return STARTUP_VERSION;
            }
            properties.load(inputStream);

            String buildTimeStamp = properties.getProperty("build.timestamp", "dev");
            if (buildTimeStamp.isBlank() || "dev".equals(buildTimeStamp) || buildTimeStamp.startsWith("${")) {
                return STARTUP_VERSION;
            }
            return buildTimeStamp.replace("T", "").replace("Z", "").replace("-", "").replace(":", "");
        } catch (IOException exception) {
            return STARTUP_VERSION;
        }
    }
}

