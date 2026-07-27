package com.bepa.eis.server.api.web.application.cache;

public class ProjectBasisInfo {

    private final Integer customerId;
    private final Integer projectId;
    private final String projectName;

    public ProjectBasisInfo(Integer customerId, Integer projectId) {
        this(customerId, projectId, null);
    }

    public ProjectBasisInfo(
            Integer customerId,
            Integer projectId,
            String projectName
    ) {
        this.customerId = customerId;
        this.projectId = projectId;
        this.projectName = projectName;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public Integer getProjectId() {
        return projectId;
    }
    public String getProjectName() {
        return projectName;
    }

}
