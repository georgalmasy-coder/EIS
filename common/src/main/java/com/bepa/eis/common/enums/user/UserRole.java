package com.bepa.eis.common.enums.user;

public enum UserRole {

    BEPA_SYSTEM_ADMINISTRATOR(
            1,
            "Bepa system administrator",
            "Full access to the system.",
            false
    ),

    CUSTOMER_ADMINISTRATOR(
            2,
            "Customer Administrator",
            "Customer Administrator with access to the customer portal.",
            true
    ),

    PROJECT_MEMBER(
            3,
            "Project Member",
            "Project member with read/write access to project data",
            true
                ),

    PROJECT_VIEWER(
            4,
            "Project viewer",
            "Project member with only read access to project data",
            true
    ),

    INVASIVE_USER_ROLE(
            -1,
            "Invalid User Role",
            "No access to the system.",
            false
    );

    private final int id;
    private final String label;
    private final String description;
    private final boolean externalUserRole;

    UserRole(
            int id,
            String label,
            String description,
            boolean externalUserRole
    ) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.externalUserRole = externalUserRole;
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

    public static UserRole fromId(Integer id) {
        if (id == null) {
            return null;
        }

        for (UserRole status : values()) {
            if (status.id == id) {
                return status;
            }
        }

        return null;
    }

    public static UserRole fromIdOrDefault(
            Integer id,
            UserRole defaultStatus
    ) {
        UserRole status = fromId(id);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? INVASIVE_USER_ROLE : defaultStatus;
    }

    public static UserRole fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (UserRole status : values()) {
            if (status.name().equalsIgnoreCase(normalizedCode)) {
                return status;
            }
        }

        return null;
    }

    public static UserRole fromCodeOrDefault(
            String code,
            UserRole defaultStatus
    ) {
        UserRole status = fromCode(code);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? INVASIVE_USER_ROLE : defaultStatus;
    }
}