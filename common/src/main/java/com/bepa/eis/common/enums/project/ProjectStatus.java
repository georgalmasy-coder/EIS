package com.bepa.eis.common.enums.project;

public enum ProjectStatus {

    CREATED(
            1,
            "Created",
            "Project has been created, but work has not yet started."
    ),

    PLANNED(
            2,
            "Planned",
            "Project is planned and ready to start."
    ),

    IN_PROGRESS(
            3,
            "In progress",
            "Project work is currently in progress."
    ),

    ON_HOLD(
            4,
            "On hold",
            "Project has temporarily been put on hold."
    ),

    AT_RISK(
            5,
            "At risk",
            "Project is active, but one or more risks may affect delivery."
    ),

    COMPLETED(
            6,
            "Completed",
            "Project has been completed."
    ),

    CANCELLED(
            7,
            "Cancelled",
            "Project has been cancelled."
    ),

    ARCHIVED(
            8,
            "Archived",
            "Project is archived and no longer active."
    );

    private final int id;
    private final String label;
    private final String description;

    ProjectStatus(
            int id,
            String label,
            String description
    ) {
        this.id = id;
        this.label = label;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActiveStatus() {
        return this == CREATED
                || this == PLANNED
                || this == IN_PROGRESS
                || this == ON_HOLD
                || this == AT_RISK;
    }

    public boolean isWorkInProgress() {
        return this == IN_PROGRESS
                || this == AT_RISK;
    }

    public boolean isTerminalStatus() {
        return this == COMPLETED
                || this == CANCELLED
                || this == ARCHIVED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isArchived() {
        return this == ARCHIVED;
    }

    public static ProjectStatus fromId(Integer id) {
        if (id == null) {
            return null;
        }

        for (ProjectStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }

        return null;
    }

    public static ProjectStatus fromIdOrDefault(
            Integer id,
            ProjectStatus defaultStatus
    ) {
        ProjectStatus status = fromId(id);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? CREATED : defaultStatus;
    }

    public static ProjectStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (ProjectStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalizedCode)) {
                return status;
            }
        }

        return null;
    }

    public static ProjectStatus fromCodeOrDefault(
            String code,
            ProjectStatus defaultStatus
    ) {
        ProjectStatus status = fromCode(code);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? CREATED : defaultStatus;
    }
}