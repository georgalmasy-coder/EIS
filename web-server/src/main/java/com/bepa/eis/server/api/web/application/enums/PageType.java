package com.bepa.eis.server.api.web.application.enums;


public enum PageType {


    PROJECT_OVERVIEW_PAGE("projectoverview","html-pages/overview.html", "Project Overview"),
    BASIS_RELATION_DIAGRAM_PAGE("basisrelationdiagram","html-pages/basisrelationdiagram.html", "Relation Diagram"),
    BASIS_TRACEABILITY_MATRIX_PAGE("basistraceabilitymatrix","html-pages/basistraceabilitymatrix.html", "Traceability Matrix"),

    // Old versions
    STAKEHOLDER_REQUIREMENT_PAGE("stakeholderrequirement","html-pages/stakeholderrequirement.html", "Stakeholder Requirement"),
    SYSTEM_REQUIREMENT_PAGE("systemrequirement","html-pages/systemrequirement.html", "System Requirement"),
    SYSTEM_REQUIREMENT_DIAGRAM_PAGE("systemrequirementdiagram","html-pages/systemrequirementdiagram.html", "System Requirement Hierarchy Diagram"),
    SYSTEM_REQUIREMENT_DIAGRAM_V2_PAGE("systemrequirementdiagramV2","html-pages/systemrequirementdiagramV2.html", "System Requirement Hierarchy Diagram"),
    SYSTEMS_BREAKDOWN_PAGE("systemsbreakdown","html-pages/systemsbreakdown.html", "Systems Breakdown"),


    // New versions
    STAKEHOLDER_MAIN_PAGE("stakeholder-main","html-pages/stakeholder-main.html", "Stakeholder"),
    STAKEHOLDER_EDIT_PAGE("stakeholder-edit","html-pages/stakeholder-edit.html", "Stakeholder Edit"),
    SYSTEM_REQUIREMENT_MAIN_PAGE("systemrequirement-main","html-pages/systemrequirement-main.html", "System Requirement"),
    SYSTEM_REQUIREMENT_MAIN_LIST_PAGE("systemrequirement-main-list","html-pages/systemrequirement-main-list.html", "System Requirement"),
    SYSTEM_REQUIREMENT_MAIN_HORIZONTAL_PAGE("systemrequirement-main-horizontal","html-pages/systemrequirement-main-horizontal.html", "System Requirement"),
    SYSTEM_REQUIREMENT_MAIN_VERTICAL_PAGE("systemrequirement-main-vertical","html-pages/systemrequirement-main-vertical.html", "System Requirement"),
    SYSTEM_REQUIREMENT_EDIT_PAGE("systemrequirement-edit","html-pages/systemrequirement-edit.html", "System Requirement Edit"),
    STAKEHOLDER_REQUIREMENT_MAIN_PAGE("stakeholderrequirement-main","html-pages/stakeholderrequirement-main.html", "Stakeholder Requirement"),
    LOGICAL_STRUCTURE_MAIN_PAGE("logicalstructure-main","html-pages/logicalstructure-main.html", "Logical Structure"),
    FUNCTIONAL_STRUCTURE_MAIN_PAGE("functionalstructure-main","html-pages/functionalstructure-main.html", "Functional Structure"),
    STAKEHOLDER_REQUIREMENT_EDIT_PAGE("stakeholderrequirement-edit","html-pages/stakeholderrequirement-edit.html", "Stakeholder Requirement Edit"),
    LOGICAL_STRUCTURE_EDIT_PAGE("logicalstructure-edit","html-pages/logicalstructure-edit.html", "Logical Structure Edit"),
    FUNCTIONAL_STRUCTURE_EDIT_PAGE("functionalstructure-edit","html-pages/functionalstructure-edit.html", "Functional Structure Edit"),
    SYSTEMS_BREAKDOWN_MAIN_PAGE("systemsbreakdown-main","html-pages/systemsbreakdown-main.html", "Physical Structure"),
    SYSTEMS_BREAKDOWN_MAIN_LIST_PAGE("systemsbreakdown-main-list","html-pages/systemsbreakdown-main-list.html", "Physical Structure"),
    SYSTEMS_BREAKDOWN_MAIN_HORIZONTAL_PAGE("systemsbreakdown-main-horizontal","html-pages/systemsbreakdown-main-horizontal.html", "Physical Structure"),
    SYSTEMS_BREAKDOWN_MAIN_VERTICAL_PAGE("systemsbreakdown-main-vertical","html-pages/systemsbreakdown-main-vertical.html", "Physical Structure"),
    SYSTEMS_BREAKDOWN_EDIT_PAGE("systemsbreakdown-edit","html-pages/systemsbreakdown-edit.html", "Physical Structure Edit"),
    STAKEHOLDER_REQUIREMENT_MAIN_LIST_PAGE("stakeholderrequirement-main-list","html-pages/stakeholderrequirement-main-list.html", "Stakeholder Requirement"),
    STAKEHOLDER_REQUIREMENT_MAIN_HORIZONTAL_PAGE("stakeholderrequirement-main-horizontal","html-pages/stakeholderrequirement-main-horizontal.html", "Stakeholder Requirement"),
    STAKEHOLDER_REQUIREMENT_MAIN_VERTICAL_PAGE("stakeholderrequirement-main-vertical","html-pages/stakeholderrequirement-main-vertical.html", "Stakeholder Requirement"),
    LOGICAL_STRUCTURE_MAIN_LIST_PAGE("logicalstructure-main-list","html-pages/logicalstructure-main-list.html", "Logical Structure"),
    LOGICAL_STRUCTURE_MAIN_HORIZONTAL_PAGE("logicalstructure-main-horizontal","html-pages/logicalstructure-main-horizontal.html", "Logical Structure"),
    LOGICAL_STRUCTURE_MAIN_VERTICAL_PAGE("logicalstructure-main-vertical","html-pages/logicalstructure-main-vertical.html", "Logical Structure"),
    FUNCTIONAL_STRUCTURE_MAIN_LIST_PAGE("functionalstructure-main-list","html-pages/functionalstructure-main-list.html", "Functional Structure"),
    FUNCTIONAL_STRUCTURE_MAIN_HORIZONTAL_PAGE("functionalstructure-main-horizontal","html-pages/functionalstructure-main-horizontal.html", "Functional Structure"),
    FUNCTIONAL_STRUCTURE_MAIN_VERTICAL_PAGE("functionalstructure-main-vertical","html-pages/functionalstructure-main-vertical.html", "Functional Structure"),
    LOOKUP_MAIN_PAGE("lookup-main","html-pages/admin/lookup-main.html", "Lookup Administration"),
    INCIDENT_MAIN_PAGE("admin-incidents","html-pages/admin/incidents-main.html", "Incidents"),
    DEPARTMENT_MAIN_PAGE("departments", "html-pages/department-main.html", "Department Administration"),


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
    ADMIN_DASHBOARD_MAILS_PAGE("dashboard-mail-status-view","html-pages/admin/views/dashboard-mail-status-view.html", ""),

    ADMIN_CUSTOMER_PAGE("customer-admin","html-pages/admin/customer-administration.html", "Customer Administration | BEPA EIS"),
    ADMIN_USER_ADMINISTRATION_PAGE("user-admin","html-pages/admin/user-administration.html", "User Administration | BEPA EIS"),

    BASELINE_MAIN_PAGE("baseline-main","html-pages/baseline-main.html", "Baselines"),
    BASELINE_DETAIL_PAGE("baseline-detail","html-pages/baseline-detail.html", "Baseline detail"),

    MY_PROJECTS_PAGE("myprojects","html-pages/myprojects.html", "My Projects"),
    PROJECT_MAIN_PAGE("project-main","html-pages/project-main.html", "Projects"),
    PROJECT_EDIT_PAGE("project-edit","html-pages/project-edit.html", "Project"),
    CUSTOMER_EDIT_PAGE("customer-edit","html-pages/customer-edit.html", "Customer"),
    USER_MAIN_PAGE("user-main","html-pages/user-main.html", "User Administration"),
    USER_EDIT_PAGE("user-edit","html-pages/user-edit.html", "User Account"),

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
