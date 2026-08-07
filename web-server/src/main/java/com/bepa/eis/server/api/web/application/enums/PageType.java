package com.bepa.eis.server.api.web.application.enums;


public enum PageType {


    PROJECT_OVERVIEW_PAGE("projectoverview","html-pages/overview.html", "Project Overview", true),
    BASIS_RELATION_DIAGRAM_PAGE("basisrelationdiagram","html-pages/basisrelationdiagram.html", "Relation Diagram", true),
    RFLP_RELATION_DIAGRAM_PAGE("rflprelationdiagram","html-pages/rflprelationdiagram.html", "RFLP Relation Diagram", true),
    BASIS_TRACEABILITY_MATRIX_PAGE("basistraceabilitymatrix","html-pages/basistraceabilitymatrix.html", "Traceability Matrix", true),
    INTERFACE_MANAGEMENT_PAGE("interfaces","html-pages/interfaces.html", "Interface Management", true),
    DASHBOARD_IRLS_PAGE("dashboard-irls","html-pages/dashboard-irls.html", "Dashboard IRL", true),
    DASHBOARD_SYSTEMS_TEAMWORK_PAGE("dashboard-systems-teamwork","html-pages/dashboard-systems-teamwork.html", "Systems Teamwork", true),

    STAKEHOLDER_MAIN_PAGE("stakeholder-main","html-pages/stakeholder-main.html", "Stakeholder", true),
    STAKEHOLDER_EDIT_PAGE("stakeholder-edit","html-pages/stakeholder-edit.html", "Stakeholder Edit", true),
    SYSTEM_REQUIREMENT_MAIN_PAGE("systemrequirement-main","html-pages/systemrequirement-main.html", "System Requirement", true),
    SYSTEM_REQUIREMENT_MAIN_LIST_PAGE("systemrequirement-main-list","html-pages/systemrequirement-main-list.html", "System Requirement", true),
    SYSTEM_REQUIREMENT_MAIN_HORIZONTAL_PAGE("systemrequirement-main-horizontal","html-pages/systemrequirement-main-horizontal.html", "System Requirement", true),
    SYSTEM_REQUIREMENT_MAIN_VERTICAL_PAGE("systemrequirement-main-vertical","html-pages/systemrequirement-main-vertical.html", "System Requirement", true),
    SYSTEM_REQUIREMENT_EDIT_PAGE("systemrequirement-edit","html-pages/systemrequirement-edit.html", "System Requirement Edit", true),
    STAKEHOLDER_REQUIREMENT_MAIN_PAGE("stakeholderrequirement-main","html-pages/stakeholderrequirement-main.html", "Stakeholder Requirement", true),
    LOGICAL_STRUCTURE_MAIN_PAGE("logicalstructure-main","html-pages/logicalstructure-main.html", "Logical Structure", true),
    FUNCTIONAL_STRUCTURE_MAIN_PAGE("functionalstructure-main","html-pages/functionalstructure-main.html", "Functional Structure", true),
    STAKEHOLDER_REQUIREMENT_EDIT_PAGE("stakeholderrequirement-edit","html-pages/stakeholderrequirement-edit.html", "Stakeholder Requirement Edit", true),
    LOGICAL_STRUCTURE_EDIT_PAGE("logicalstructure-edit","html-pages/logicalstructure-edit.html", "Logical Structure Edit", true),
    FUNCTIONAL_STRUCTURE_EDIT_PAGE("functionalstructure-edit","html-pages/functionalstructure-edit.html", "Functional Structure Edit", true),
    SYSTEMS_BREAKDOWN_MAIN_PAGE("systemsbreakdown-main","html-pages/systemsbreakdown-main.html", "Physical Structure", true),
    SYSTEMS_BREAKDOWN_MAIN_LIST_PAGE("systemsbreakdown-main-list","html-pages/systemsbreakdown-main-list.html", "Physical Structure", true),
    SYSTEMS_BREAKDOWN_MAIN_HORIZONTAL_PAGE("systemsbreakdown-main-horizontal","html-pages/systemsbreakdown-main-horizontal.html", "Physical Structure", true),
    SYSTEMS_BREAKDOWN_MAIN_VERTICAL_PAGE("systemsbreakdown-main-vertical","html-pages/systemsbreakdown-main-vertical.html", "Physical Structure", true),
    SYSTEMS_BREAKDOWN_EDIT_PAGE("systemsbreakdown-edit","html-pages/systemsbreakdown-edit.html", "Physical Structure Edit", true),
    LOOKUP_MAINTENANCE_PAGE("lookup-maintenance","html-pages/lookup-maintenance.html", "TRL / SRL / IRL / Classification", true),
    STAKEHOLDER_REQUIREMENT_MAIN_LIST_PAGE("stakeholderrequirement-main-list","html-pages/stakeholderrequirement-main-list.html", "Stakeholder Requirement", true),
    STAKEHOLDER_REQUIREMENT_MAIN_HORIZONTAL_PAGE("stakeholderrequirement-main-horizontal","html-pages/stakeholderrequirement-main-horizontal.html", "Stakeholder Requirement", true),
    STAKEHOLDER_REQUIREMENT_MAIN_VERTICAL_PAGE("stakeholderrequirement-main-vertical","html-pages/stakeholderrequirement-main-vertical.html", "Stakeholder Requirement", true),
    LOGICAL_STRUCTURE_MAIN_LIST_PAGE("logicalstructure-main-list","html-pages/logicalstructure-main-list.html", "Logical Structure", true),
    LOGICAL_STRUCTURE_MAIN_HORIZONTAL_PAGE("logicalstructure-main-horizontal","html-pages/logicalstructure-main-horizontal.html", "Logical Structure", true),
    LOGICAL_STRUCTURE_MAIN_VERTICAL_PAGE("logicalstructure-main-vertical","html-pages/logicalstructure-main-vertical.html", "Logical Structure", true),
    FUNCTIONAL_STRUCTURE_MAIN_LIST_PAGE("functionalstructure-main-list","html-pages/functionalstructure-main-list.html", "Functional Structure", true),
    FUNCTIONAL_STRUCTURE_MAIN_HORIZONTAL_PAGE("functionalstructure-main-horizontal","html-pages/functionalstructure-main-horizontal.html", "Functional Structure", true),
    FUNCTIONAL_STRUCTURE_MAIN_VERTICAL_PAGE("functionalstructure-main-vertical","html-pages/functionalstructure-main-vertical.html", "Functional Structure", true),
    LOOKUP_MAIN_PAGE("lookup-main","html-pages/admin/lookup-main.html", "Lookup Administration", true),
    INCIDENT_MAIN_PAGE("admin-incidents","html-pages/admin/incidents-main.html", "Incidents", true),
    ADMIN_MENU_EDITOR_PAGE("menu-editor","html-pages/admin/menu-editor.html", "Menu Editor | BEPA EIS", true),
    DEPARTMENT_MAIN_PAGE("departments", "html-pages/department-main.html", "Department Administration", true),

    ADMIN_DASHBOARD_PAGE("admindashboard","html-pages/admin/admindashboard.html", "Admin Dashboard | BEPA EIS", true),
    ADMIN_DASHBOARD_ALERT_PAGE("dashboard-alerts-view","html-pages/admin/views/dashboard-alerts-view.html", "", true),
    ADMIN_DASHBOARD_AUDIT_SECURITY_PAGE("dashboard-audit-security-view","html-pages/admin/views/dashboard-audit-security-view.html", "", true),
    ADMIN_DASHBOARD_CUSTOMER_CREATION_PAGE("dashboard-customer-creation-view","html-pages/admin/views/dashboard-customer-creation-view.html", "", true),
    ADMIN_DASHBOARD_CUSTOMERS_PAGE("dashboard-customers-view","html-pages/admin/views/dashboard-customers-view.html", "", true),
    ADMIN_DASHBOARD_INTEGRATIONS_PAGE("dashboard-integrations-view","html-pages/admin/views/dashboard-integrations-view.html", "", true),
    ADMIN_DASHBOARD_MODULES_PAGE("dashboard-modules-view","html-pages/admin/views/dashboard-modules-view.html", "", true),
    ADMIN_DASHBOARD_OVERVIEW_PAGE("dashboard-overview-view","html-pages/admin/views/dashboard-overview-view.html", "", true),
    ADMIN_DASHBOARD_PERFORMANCE_PAGE("dashboard-performance-view","html-pages/admin/views/dashboard-performance-view.html", "", true),
    ADMIN_DASHBOARD_SUBSCRIPTIONS_PAYMENTS_PAGE("dashboard-subscriptions-payments-view","html-pages/admin/views/dashboard-subscriptions-payments-view.html", "", true),
    ADMIN_DASHBOARD_SYSTEM_STATUS_PAGE("dashboard-system-status-view","html-pages/admin/views/dashboard-system-status-view.html", "", true),
    ADMIN_DASHBOARD_USERS_PAGE("dashboard-users-view","html-pages/admin/views/dashboard-users-view.html", "", true),
    ADMIN_DASHBOARD_MAILS_PAGE("dashboard-mail-status-view","html-pages/admin/views/dashboard-mail-status-view.html", "", true),

    ADMIN_CUSTOMER_PAGE("customer-admin","html-pages/admin/customer-administration.html", "Customer Administration | BEPA EIS", true),
    SUBSCRIPTION_EDITOR_PAGE("subscription-editor","html-pages/admin/subscription-editor.html", "Subscription Editor | BEPA EIS", true),
    ADMIN_USER_ADMINISTRATION_PAGE("user-admin","html-pages/admin/user-administration.html", "User Administration | BEPA EIS", true),

    BASELINE_MAIN_PAGE("baseline-main","html-pages/baseline-main.html", "Baselines", true),
    BASELINE_DETAIL_PAGE("baseline-detail","html-pages/baseline-detail.html", "Baseline detail", true),

    MY_PROJECTS_PAGE("myprojects","html-pages/myprojects.html", "My Projects", true),
    PROJECT_MAIN_PAGE("project-main","html-pages/project-main.html", "Projects", true),
    PROJECT_EDIT_PAGE("project-edit","html-pages/project-edit.html", "Project", true),
    CUSTOMER_EDIT_PAGE("customer-edit","html-pages/customer-edit.html", "Customer", true),
    USER_MAIN_PAGE("user-main","html-pages/user-main.html", "User Administration", true),
    USER_EDIT_PAGE("user-edit","html-pages/user-edit.html", "User Account", true),

    NONE("invalid", "", "", true) ;

    private final String pageName;
    private final String path;
    private final String title;
    private final boolean isHelpEnabled;

    // Constructor
    PageType(String pageName, String path, String title, boolean isHelpEnabled) {
        this.pageName = pageName;
        this.path = path;
        this.title = title;
        this.isHelpEnabled = isHelpEnabled;
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

    public boolean isHelpEnabled() {
        return isHelpEnabled;
    }

    public static PageType mapToType(String page) {
        for (PageType pageType : PageType.values()) {
            if (pageType.pageName.equalsIgnoreCase(page)) return pageType;
        }
        return NONE;
    }
}
