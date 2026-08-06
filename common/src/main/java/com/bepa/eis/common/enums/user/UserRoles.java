package com.bepa.eis.common.enums.user;

public enum UserRoles {

    BEPA_SYSTEM_ADMINISTRATOR(
            1,
            "Bepa system administrator",
            "Full access to the system.",
            true,
            true
    ),

    CUSTOMER_ADMINISTRATOR(
            2,
            "Administrator",
            "Administrator with access to the customer portal.",
            true,
            true
    ),

    PROJECT_MEMBER(
            3,
            "Project Member",
            "Project member with read/write access to project data",
            true,
            true
                ),

    PROJECT_VIEWER(
            4,
            "Project viewer",
            "Project member with only read access to project data",
            true,
            false
    ),

    INVASIVE_USER_ROLE(
            -1,
            "Invalid User Role",
            "No access to the system.",
            false,
            false
    );

    private final int id;
    private final String label;
    private final String description;
    private final boolean externalUserRole;
    private final boolean active;

    UserRoles(
            int id,
            String label,
            String description,
            boolean externalUserRole,
            boolean active
    ) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.externalUserRole = externalUserRole;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isExternalUserRole() {
        return externalUserRole;
    }

    public boolean isActive() {
        return active;
    }

    public static UserRoles fromId(Integer id) {
        if (id == null) {
            return null;
        }

        for (UserRoles status : values()) {
            if (status.id == id) {
                return status;
            }
        }

        return null;
    }

    public static UserRoles fromIdOrDefault(
            Integer id,
            UserRoles defaultStatus
    ) {
        UserRoles status = fromId(id);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? INVASIVE_USER_ROLE : defaultStatus;
    }

    public static UserRoles fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (UserRoles status : values()) {
            if (status.name().equalsIgnoreCase(normalizedCode)) {
                return status;
            }
        }

        return null;
    }

    public static UserRoles fromCodeOrDefault(
            String code,
            UserRoles defaultStatus
    ) {
        UserRoles status = fromCode(code);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? INVASIVE_USER_ROLE : defaultStatus;
    }
}