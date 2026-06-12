package com.bepa.eis.server.api.web.application.enums;


public enum PageType {


    PROJECT_OVERVIEW_PAGE("projectoverview","html-pages/overview.html", "Project Overview"),
    BASIS_RELATION_DIAGRAM_PAGE("basisrelationdiagram","html-pages/basisrelationdiagram.html", "Relation Diagram"),
    BASIS_TRACEABILITY_MATRIX_PAGE("basistraceabilitymatrix","html-pages/basistraceabilitymatrix.html", "Traceability Matrix"),
    STAKEHOLDER_REQUIREMENT_PAGE("stakeholderrequirement","html-pages/stakeholderrequirement.html", "Stakeholder Requirement"),

    SYSTEM_REQUIREMENT_PAGE("systemrequirement","html-pages/systemrequirement.html", "System Requirement"),
    SYSTEM_REQUIREMENT_DIAGRAM_PAGE("systemrequirementdiagram","html-pages/systemrequirementdiagram.html", "System Requirement Hierarchy Diagram"),
    SYSTEM_REQUIREMENT_DIAGRAM_V2_PAGE("systemrequirementdiagramV2","html-pages/systemrequirementdiagramV2.html", "System Requirement Hierarchy Diagram"),


    NEW_SYSTEM_REQUIREMENT_MAIN_PAGE("systemrequirement-main","html-pages/systemrequirement-main.html", "System Requirement"),
    NEW_SYSTEM_REQUIREMENT_EDIT_PAGE("systemrequirement-edit","html-pages/systemrequirement-edit.html", "System Requirement Edit"),

    NEW_STAKEHOLDER_REQUIREMENT_MAIN_PAGE("stakeholderrequirement-main","html-pages/stakeholderrequirement-main.html", "Stakeholder Requirement"),
    NEW_STAKEHOLDER_REQUIREMENT_EDIT_PAGE("stakeholderrequirement-edit","html-pages/stakeholderrequirement-edit.html", "Stakeholder Requirement Edit"),

    SYSTEMS_BREAKDOWN_PAGE("systemsbreakdown","html-pages/systemsbreakdown.html", "Systems Breakdown"),

    NEW_SYSTEMS_BREAKDOWN_MAIN_PAGE("systemsbreakdown-main","html-pages/systemsbreakdown-main.html", "Systems Breakdown"),
    NEW_SYSTEMS_BREAKDOWN_EDIT_PAGE("systemsbreakdown-edit","html-pages/systemsbreakdown-edit.html", "Systems Breakdown Edit"),

    ADMIN_DASHBOARD_PAGE("admindashboard","html-pages/admin/admindashboard.html", "Admin Dashboard | BEPA EIS"),
    ADMIN_DASHBOARD_ALERT_PAGE("dashboard-alerts-view","html-pages/admin/views/dashboard-alerts-view.html", ""),
    ADMIN_DASHBOARD_AUDIT_SECURITY_PAGE("dashboard-audit-security-view","html-pages/admin/views/dashboard-audit-security-view.html", ""),
    ADMIN_DASHBOARD_CUSTOMER_CREATION_PAGE("dashboard-customer-creation-view","html-pages/admin/views/dashboard-customer-creation-view.html", ""),
    ADMIN_DASHBOARD_CUSTOMERS_PAGE("dashboard-customers-view","html-pages/admin/views/dashboard-customers-view.html", ""),
    ADMIN_DASHBOARD_INTEGRATIONS_PAGE("dashboard-integrations-view","html-pages/admin/views/dashboard-integrations-view.html", ""),
    ADMIN_DASHBOARD_MODULES_PAGE("dashboard-modules-view","html-pages/admin/views/dashboard-modules-view.html", ""),
    ADMIN_DASHBOARD_OVERVIEW_PAGE("dashboard-overview-view","html-pages/admin/views/dashboard-overview-view.html", ""),
    ADMIN_DASHBOARD_PERFORMANCE_PAGE("dashboard-performance-view","html-pages/admin/views/dashboard-performance-view.html", ""),
    ADMIN_DASHBOARD_SUBSCRIPTIONS_PAYMENTS_PAGE("dashboard-subscriptions-payments-view","html-pages/admin/views/dashboard-subscriptions-payments-view.html", ""),
    ADMIN_DASHBOARD_SYSTEM_STATUS_PAGE("dashboard-system-status-view","html-pages/admin/views/dashboard-system-status-view.html", ""),
    ADMIN_DASHBOARD_USERS_PAGE("dashboard-users-view","html-pages/admin/views/dashboard-users-view.html", ""),

    ADMIN_CUSTOMER_PAGE("customer-admin","html-pages/admin/customer-administration.html", "Customer Administration | BEPA EIS"),


    NONE("invalid", "", "") ;

    private final String pageName;
    private final String path;
    private final String title;

    // Constructor
    PageType(String pageName, String path, String title) {
        this.pageName = pageName;
        this.path = path;
        this.title = title;
    }

    // Getters
    public String getPageName() {
        return pageName;
    }
    public String getPath() {
        return path;
    }
    public String getTitle() {
        return title;
    }

    public static PageType mapToType(String page) {
        for (PageType pageType : PageType.values()) {
            if (pageType.pageName.equalsIgnoreCase(page)) return pageType;
        }
        return NONE;
    }
}
