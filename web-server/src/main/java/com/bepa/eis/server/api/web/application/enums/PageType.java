package com.bepa.eis.server.api.web.application.enums;

public enum PageType {

    PROJECT_OVERVIEW_PAGE("projectoverview","html-pages/overview.html", "Project Overview", true, "PROJECT MANAGEMENT", "Project Overview", "Review the current project status and key information."),

    MY_PROJECTS_PAGE("myprojects-old","html-pages/myprojects.html", "My Projects", true, "PROJECT MANAGEMENT", "My Projects", "Review your projects in the workspace below."),
    ALL_PROJECTS_PAGE("myprojects", "html-pages/allProjects.html", "All Projects", true, "PORTFOLIO", "Projects", "Choose a project or continue the work that needs attention."),

    /* */

    DASHBOARD_IRLS_PAGE("dashboard-irls","html-pages/dashboard-irls.html", "Dashboard IRL", true, "PROJECT MANAGEMENT", "Dashboard", "Monitor the current status and key metrics."),
    DASHBOARD_SYSTEMS_TEAMWORK_PAGE("dashboard-systems-teamwork","html-pages/dashboard-systems-teamwork.html", "Systems Teamwork", true, "PROJECT DASHBOARD", "Systems Teamwork", "Monitor the current status and key metrics."),

    STAKEHOLDER_REQUIREMENT_MAIN_PAGE("stakeholderrequirement-main","html-pages/stakeholderrequirement-main.html", "Stakeholder Requirement", true, "R-F-L-P MODEL - REQUIREMENTS", "Stakeholder Requirement", "Work with stakeholder requirements in the workspace below."),
    STAKEHOLDER_REQUIREMENT_EDIT_PAGE("stakeholderrequirement-edit","html-pages/stakeholderrequirement-edit.html", "Stakeholder Requirement Edit", true, "R-F-L-P MODEL - REQUIREMENTS", "Stakeholder Requirement Edit", "Edit the current stakeholder requirement in the workspace below."),
    STAKEHOLDER_REQUIREMENT_MAIN_HORIZONTAL_PAGE("stakeholderrequirement-main-horizontal","html-pages/stakeholderrequirement-main-horizontal.html", "Stakeholder Requirement", true, "R-F-L-P MODEL - REQUIREMENTS", "Stakeholder Requirement", "Work with stakeholder requirements in the workspace below."),
    STAKEHOLDER_REQUIREMENT_MAIN_VERTICAL_PAGE("stakeholderrequirement-main-vertical","html-pages/stakeholderrequirement-main-vertical.html", "Stakeholder Requirement", true, "R-F-L-P MODEL - REQUIREMENTS", "Stakeholder Requirement", "Work with stakeholder requirements in the workspace below."),

    SYSTEM_REQUIREMENT_MAIN_PAGE("systemrequirement-main","html-pages/systemrequirement-main.html", "System Requirement", true, "R-F-L-P MODEL - REQUIREMENTS", "System Requirement", "Work with system requirements in the workspace below."),
    SYSTEM_REQUIREMENT_EDIT_PAGE("systemrequirement-edit","html-pages/systemrequirement-edit.html", "System Requirement Edit", true, "R-F-L-P MODEL - REQUIREMENTS", "System Requirement Edit", "Edit the current system requirement in the workspace below."),
    SYSTEM_REQUIREMENT_MAIN_HORIZONTAL_PAGE("systemrequirement-main-horizontal","html-pages/systemrequirement-main-horizontal.html", "System Requirement", true, "R-F-L-P MODEL - REQUIREMENTS", "System Requirement", "Work with system requirements in the workspace below."),
    SYSTEM_REQUIREMENT_MAIN_VERTICAL_PAGE("systemrequirement-main-vertical","html-pages/systemrequirement-main-vertical.html", "System Requirement", true, "R-F-L-P MODEL - REQUIREMENTS", "System Requirement", "Work with system requirements in the workspace below."),

    FUNCTIONAL_STRUCTURE_MAIN_PAGE("functionalstructure-main","html-pages/functionalstructure-main.html", "Functional", true, "R-F-L-P MODEL - FUNCTIONS", "Functions", "Work with functional design in the workspace below."),
    FUNCTIONAL_STRUCTURE_EDIT_PAGE("functionalstructure-edit","html-pages/functionalstructure-edit.html", "Functional Structure Edit", true, "R-F-L-P MODEL - FUNCTION", "Functional Design Edit", "Edit the current functional design in the workspace below."),
    FUNCTIONAL_STRUCTURE_MAIN_HORIZONTAL_PAGE("functionalstructure-main-horizontal","html-pages/functionalstructure-main-horizontal.html", "Function Design", true, "R-F-L-P MODEL - FUNCTIONS", "Functional Design", "Work with functional design in the workspace below."),
    FUNCTIONAL_STRUCTURE_MAIN_VERTICAL_PAGE("functionalstructure-main-vertical","html-pages/functionalstructure-main-vertical.html", "Function Design", true, "R-F-L-P MODEL - FUNCTIONS", "Functional Design", "Work with functional design in the workspace below."),

    LOGICAL_STRUCTURE_MAIN_PAGE("logicalstructure-main","html-pages/logicalstructure-main.html", "Logical Design", true, "R-F-L-P MODEL - LOGICAL DESIGN", "Logical Design", "Work with logical design in the workspace below."),
    LOGICAL_STRUCTURE_EDIT_PAGE("logicalstructure-edit","html-pages/logicalstructure-edit.html", "Logical Edit", true, "R-F-L-P MODEL - LOGICAL DESIGN", "Logical Design Edit", "Edit the current logical design in the workspace below."),
    LOGICAL_STRUCTURE_MAIN_HORIZONTAL_PAGE("logicalstructure-main-horizontal","html-pages/logicalstructure-main-horizontal.html", "Logical Structure", true, "R-F-L-P MODEL - LOGICAL DESIGN", "Logical Design", "Work with logical structures in the workspace below."),
    LOGICAL_STRUCTURE_MAIN_VERTICAL_PAGE("logicalstructure-main-vertical","html-pages/logicalstructure-main-vertical.html", "Logical Structure", true, "R-F-L-P MODEL - LOGICAL DESIGN", "Logical Design", "Work with logical structures in the workspace below."),

    SYSTEMS_BREAKDOWN_MAIN_PAGE("systemsbreakdown-main","html-pages/systemsbreakdown-main.html", "Physical Structure", true, "R-F-L-P MODEL - PHYSICAL STRUCTURES", "Physical Structure", "Work with physical structure items in the workspace below."),
    SYSTEMS_BREAKDOWN_EDIT_PAGE("systemsbreakdown-edit","html-pages/systemsbreakdown-edit.html", "Physical Structure Edit", true, "R-F-L-P MODEL - PHYSICAL STRUCTURES", "Physical Structure Edit", "Edit the current physical structure item in the workspace below."),
    SYSTEMS_BREAKDOWN_MAIN_HORIZONTAL_PAGE("systemsbreakdown-main-horizontal","html-pages/systemsbreakdown-main-horizontal.html", "Physical Structure", true, "R-F-L-P MODEL - PHYSICAL STRUCTURES", "Physical Structure", "Work with physical structure items in the workspace below."),
    SYSTEMS_BREAKDOWN_MAIN_VERTICAL_PAGE("systemsbreakdown-main-vertical","html-pages/systemsbreakdown-main-vertical.html", "Physical Structure", true, "R-F-L-P MODEL - PHYSICAL STRUCTURES", "Physical Structure", "Work with physical structure items in the workspace below."),

    TRACEABILITY_MATRIX_PAGE("basistraceabilitymatrix","html-pages/basistraceabilitymatrix.html", "Traceability Matrix", true, "R-F-L-P MODEL - TRACEABILITY MATRIX", "Traceability Matrix", "Review traceability links across the model."),
    RELATION_DIAGRAM_PAGE("basisrelationdiagram","html-pages/basisrelationdiagram.html", "Relation Diagram", true, "BASIS MODEL - RELATION DIAGRAM", "Relation Diagram", "Inspect relations and dependencies in the basis model."),
    RFLP_RELATION_DIAGRAM_PAGE("rflprelationdiagram","html-pages/rflprelationdiagram.html", "RFLP Relation Diagram", true, "R-F-L-P MODEL - RELATION DIAGRAM", "Relation Diagram", "Inspect relations and dependencies in the R-F-L-P model."),

    BASELINE_MAIN_PAGE("baseline-main","html-pages/baseline-main.html", "Baselines", true, "PROJECT MANAGEMENT", "Baselines", "Review baseline data in the workspace below."),
    BASELINE_DETAIL_PAGE("baseline-detail","html-pages/baseline-detail.html", "Baseline detail", true, "PROJECT MANAGEMENT", "Baseline Detail", "Review baseline details in the workspace below."),

    INTERFACE_MANAGEMENT_PAGE("interfaces","html-pages/interfaces.html", "Interface Management", true, "R-F-L-P MODEL - INTERFACE MANAGEMENT", "Interface Management", "Manage interfaces and related relationships."),

    // Admin
    CUSTOMER_EDIT_PAGE("customer-edit","html-pages/customer-edit.html", "Company Profile", true, "COMPANY MANAGEMENT", "Company Profile", "Edit the current company profile in the workspace below."),

    PROJECT_MAIN_PAGE("project-main","html-pages/project-main.html", "Projects", true, "PROJECT MANAGEMENT", "Projects", "Review and manage projects in the workspace below."),
    PROJECT_EDIT_PAGE("project-edit","html-pages/project-edit.html", "Project", true, "PROJECT MANAGEMENT", "Project", "Edit the current project in the workspace below."),

    USER_MAIN_PAGE("user-main","html-pages/user-main.html", "User Administration", true, "USER MANAGEMENT", "User Accounts", "Use the workspace below to manage users."),
    USER_EDIT_PAGE("user-edit","html-pages/user-edit.html", "User Account", true, "USER MANAGEMENT", "User Account", "Edit the current user account in the workspace below."),

    STAKEHOLDER_MAIN_PAGE("stakeholder-main","html-pages/stakeholder-main.html", "Stakeholder", true, "STAKEHOLDER MANAGEMENT", "Stakeholder", "Work with stakeholder records in the workspace below."),
    STAKEHOLDER_EDIT_PAGE("stakeholder-edit","html-pages/stakeholder-edit.html", "Stakeholder Edit", true, "STAKEHOLDER MANAGEMENT", "Stakeholder Edit", "Edit stakeholder details in the workspace below."),
    DEPARTMENT_MAIN_PAGE("departments", "html-pages/department-main.html", "Department Administration", true, "DEPARTMENT MANAGEMENT", "Department Administration", "Use the workspace below to manage administration data."),

    LOOKUP_MAINTENANCE_PAGE("lookup-maintenance","html-pages/lookup-maintenance.html", "TRL / SRL / IRL / Classification", true, "ADMINISTRATION", "TRL / SRL / IRL / Classification Management", "Manage irl/trl/srl lookup values in the workspace below."),

    // BEPA admin
    ADMIN_DASHBOARD_PAGE("admindashboard","html-pages/admin/admindashboard.html", "Admin Dashboard | BEPA EIS", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_ALERT_PAGE("dashboard-alerts-view","html-pages/admin/views/dashboard-alerts-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_AUDIT_SECURITY_PAGE("dashboard-audit-security-view","html-pages/admin/views/dashboard-audit-security-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_CUSTOMER_CREATION_PAGE("dashboard-customer-creation-view","html-pages/admin/views/dashboard-customer-creation-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_CUSTOMERS_PAGE("dashboard-customers-view","html-pages/admin/views/dashboard-customers-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_INTEGRATIONS_PAGE("dashboard-integrations-view","html-pages/admin/views/dashboard-integrations-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_MODULES_PAGE("dashboard-modules-view","html-pages/admin/views/dashboard-modules-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_OVERVIEW_PAGE("dashboard-overview-view","html-pages/admin/views/dashboard-overview-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_PERFORMANCE_PAGE("dashboard-performance-view","html-pages/admin/views/dashboard-performance-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_SUBSCRIPTIONS_PAYMENTS_PAGE("dashboard-subscriptions-payments-view","html-pages/admin/views/dashboard-subscriptions-payments-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_SYSTEM_STATUS_PAGE("dashboard-system-status-view","html-pages/admin/views/dashboard-system-status-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_USERS_PAGE("dashboard-users-view","html-pages/admin/views/dashboard-users-view.html", "", true, "TBD", "TBD", "TBD"),
    ADMIN_DASHBOARD_MAILS_PAGE("dashboard-mail-status-view","html-pages/admin/views/dashboard-mail-status-view.html", "", true, "TBD", "TBD", "TBD"),

    ADMIN_CUSTOMER_PAGE("customer-admin","html-pages/admin/customer-administration.html", "Customer Administration | BEPA EIS", true, "BEPA ADMINISTRATION", "Customer Administration", "Use the workspace below to manage administration data."),
    ADMIN_USER_ADMINISTRATION_PAGE("user-admin","html-pages/admin/user-administration.html", "User Administration | BEPA EIS", true, "BEPA ADMINISTRATION", "User Administration", "Use the workspace below to manage administration data."),

    LOOKUP_MAIN_PAGE("lookup-main","html-pages/admin/lookup-main.html", "Lookup Administration", true, "BEPA ADMINISTRATION", "Lookup Administration", "Use the workspace below to manage administration data."),
    ADMIN_MENU_EDITOR_PAGE("menu-editor","html-pages/admin/menu-editor.html", "Menu Editor | BEPA EIS", true, "ADMINISTRATION", "Menu Editor", "Use the workspace below to manage administration data."),
    SUBSCRIPTION_EDITOR_PAGE("subscription-editor","html-pages/admin/subscription-editor.html", "Subscription Editor | BEPA EIS", true, "ADMINISTRATION", "Subscription Editor", "Use the workspace below to manage administration data."),

    INCIDENT_MAIN_PAGE("admin-incidents","html-pages/admin/incidents-main.html", "Incidents", true, "ADMINISTRATION", "Incidents", "Use the workspace below to manage administration data."),

    /* */

    NONE("invalid", "", "", true, "TBD", "TBD", "TBD") ;

    private final String pageName;
    private final String path;
    private final String title;
    private final boolean isHelpEnabled;
    private final String workspaceEyebrow;
    private final String workspaceHeading;
    private final String workspaceHelpText;

    // Constructor
    PageType(String pageName, String path, String title, boolean isHelpEnabled, String workspaceEyebrow, String workspaceHeading, String workspaceHelpText) {
        this.pageName = pageName;
        this.path = path;
        this.title = title;
        this.isHelpEnabled = isHelpEnabled;
        this.workspaceEyebrow = workspaceEyebrow;
        this.workspaceHeading = workspaceHeading;
        this.workspaceHelpText = workspaceHelpText;
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

    public String getWorkspaceEyebrow() {
        return workspaceEyebrow;
    }

    public String getWorkspaceHeading() {
        return workspaceHeading;
    }

    public String getWorkspaceHelpText() {
        return workspaceHelpText;
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
