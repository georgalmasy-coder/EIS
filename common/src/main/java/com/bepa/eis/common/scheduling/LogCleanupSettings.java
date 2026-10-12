package com.bepa.eis.common.scheduling;

import com.bepa.eis.common.GlobalConfiguration;
import java.nio.file.Path;
import java.util.List;

public record LogCleanupSettings(CleanupJobSettings job, List<Path> directories) {
    public static final String COMPONENT_ID = "job.cleanup.logs";
    public static final String LABEL = "Archived log cleanup job";

    public static LogCleanupSettings load() {
        CleanupJobSettings job = CleanupJobSettings.load("logs", 30);
        if (!job.enabled()) return new LogCleanupSettings(job, List.of());
        Path base = GlobalConfiguration.getEisLogsDirectory().toPath();
        Path web = Path.of(GlobalConfiguration.getString("cleanup.logs.job.web-server.directory",
                base.resolve("web-server").toString())).toAbsolutePath().normalize();
        Path integration = Path.of(GlobalConfiguration.getString("cleanup.logs.job.integration-server.directory",
                base.resolve("integration-server").toString())).toAbsolutePath().normalize();
        return new LogCleanupSettings(job, List.of(web, integration));
    }
}
