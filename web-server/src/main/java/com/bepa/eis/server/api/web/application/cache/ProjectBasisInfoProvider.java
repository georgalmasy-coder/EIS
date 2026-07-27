package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.server.dataprovider.project.ProjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectBasisInfoProvider {

    private static final Logger log = LoggerFactory.getLogger(ProjectBasisInfoProvider.class);

    private final ProjectProvider projectProvider;

    public ProjectBasisInfoProvider() {
        this(new ProjectProvider(null));
    }

    public ProjectBasisInfoProvider(ProjectProvider projectProvider) {
        this.projectProvider = projectProvider == null
                ? new ProjectProvider(null)
                : projectProvider;
    }

    public ProjectBasisInfo getProjectBasisInfo(
            Integer customerId,
            Integer projectId
    ) {
        String projectName = resolveProjectName(projectId);

        return new ProjectBasisInfo(
                customerId,
                projectId,
                projectName
        );
    }

    private String resolveProjectName(Integer projectId) {
        if (projectId == null) {
            return "";
        }

        try {
            ProjectRecord projectRecord = projectProvider.getLatestProjectByProjectId(projectId);

            if (projectRecord == null || projectRecord.getProjectName() == null) {
                return "";
            }

            return projectRecord.getProjectName().trim();
        } catch (Exception e) {
            log.error("Error loading project name. projectId={}", projectId, e);
            return "";
        }
    }
}
