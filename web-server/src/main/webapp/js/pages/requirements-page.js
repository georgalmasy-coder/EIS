import { initMenu, menuHasRoute } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { StakeholderRequirementController } from "./requirements-stakeholder.js";
import { SystemRequirementController } from "./requirements-system.js";

const state = {
    activeTab: "stakeholder", // "stakeholder" or "system"
    stakeholderController: null,
    systemController: null
};

document.addEventListener("DOMContentLoaded", () => {
    init();
});

function init() {
    const menuInitialization = initMenu(document);
    menuInitialization.then(updateInterfaceManagementButtonVisibility);
    // Ensure topbar is mounted
    mountTopbar(document);
    
    // Initial metadata
    applyTopbarMetadata(document, {
        helpFileName: "requirements",
        workspaceHeading: "Requirements",
        workspaceEyebrow: "R-F-L-P MODEL - REQUIREMENTS"
    });

    state.stakeholderController = new StakeholderRequirementController();
    state.systemController = new SystemRequirementController();

    state.stakeholderController.init();
    state.systemController.init();

    window.onStakeholderDataLoaded = (count) => {
        document.getElementById("stakeholderCount").textContent = count;
        // Re-apply help metadata in case it was overwritten by XML load
        applyTopbarMetadata(document, { helpFileName: "requirements" });
    };

    window.onSystemDataLoaded = (count) => {
        document.getElementById("systemCount").textContent = count;
        // Re-apply help metadata in case it was overwritten by XML load
        applyTopbarMetadata(document, { helpFileName: "requirements" });
    };

    setupTabEvents();
    
    // Load initial tab from localStorage if exists
    const savedTab = localStorage.getItem("basis.requirements.activeTab");
    if (savedTab === "system") {
        switchTab("system");
    } else {
        switchTab("stakeholder");
    }
}

function updateInterfaceManagementButtonVisibility() {
    const routesByButtonId = {
        stakeholderBtnInterfaceManagement: "/web/view?page=stk-interfaces",
        systemBtnInterfaceManagement: "/web/view?page=sys-interfaces"
    };

    Object.entries(routesByButtonId).forEach(([buttonId, route]) => {
        const button = document.getElementById(buttonId);
        if (button) {
            button.hidden = !menuHasRoute(route);
        }
    });
}

function setupTabEvents() {
    document.getElementById("tabStakeholder").addEventListener("click", () => switchTab("stakeholder"));
    document.getElementById("tabSystem").addEventListener("click", () => switchTab("system"));
}

function switchTab(tab) {
    state.activeTab = tab;
    localStorage.setItem("basis.requirements.activeTab", tab);

    const stakeholderBtn = document.getElementById("tabStakeholder");
    const systemBtn = document.getElementById("tabSystem");
    const stakeholderSection = document.getElementById("stakeholderSection");
    const systemSection = document.getElementById("systemSection");
    const stakeholderHeaderActions = document.getElementById("stakeholderHeaderActions");
    const systemHeaderActions = document.getElementById("systemHeaderActions");
    const stakeholderToolbarActions = document.getElementById("stakeholderToolbarActions");
    const systemToolbarActions = document.getElementById("systemToolbarActions");

    if (tab === "stakeholder") {
        stakeholderBtn.classList.add("is-active");
        systemBtn.classList.remove("is-active");
        stakeholderSection.classList.add("is-active");
        systemSection.classList.remove("is-active");
        stakeholderHeaderActions.classList.add("is-active");
        systemHeaderActions.classList.remove("is-active");
        stakeholderToolbarActions.classList.add("is-active");
        systemToolbarActions.classList.remove("is-active");
        
        syncCommonUI(state.stakeholderController);
    } else {
        stakeholderBtn.classList.remove("is-active");
        systemBtn.classList.add("is-active");
        stakeholderSection.classList.remove("is-active");
        systemSection.classList.add("is-active");
        stakeholderHeaderActions.classList.remove("is-active");
        systemHeaderActions.classList.add("is-active");
        stakeholderToolbarActions.classList.remove("is-active");
        systemToolbarActions.classList.add("is-active");
        
        syncCommonUI(state.systemController);
    }
}

function syncCommonUI(controller) {
    // Update counts in tabs (total count)
    document.getElementById("stakeholderCount").textContent = state.stakeholderController.getTotalCount();
    document.getElementById("systemCount").textContent = state.systemController.getTotalCount();
}

window.refreshActiveList = function(type) {
    if (type === "stakeholder") {
        state.stakeholderController.refresh();
    } else if (type === "system") {
        state.systemController.refresh();
    }
};
