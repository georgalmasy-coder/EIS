import { initMenu } from "../components/menu.js";
import { setText } from "../core/dom.js";
import { formatDateTimeForDisplay } from "../core/format.js";
import { fetchText } from "../core/http.js";

const viewHost = document.getElementById("dashboardView");
const activeViewTitle = document.getElementById("activeViewTitle");
const loadStatus = document.getElementById("loadStatus");
const lastRefresh = document.getElementById("lastRefresh");
const refreshButton = document.getElementById("refreshButton");
const tabButtons = Array.from(document.querySelectorAll("[data-dashboard-view]"));

const views = {
    "overview": {
        title: "Overview",
        htmlUrl: "/web/view?page=dashboard-overview-view",
        moduleUrl: "/js/views/overview-view.js"
    },
    "system-status": {
        title: "System Status",
        htmlUrl: "/web/view?page=dashboard-system-status-view",
        moduleUrl: "/js/views/system-status-view.js"
    },
    "customers": {
        title: "Customers",
        htmlUrl: "/web/view?page=dashboard-customers-view",
        moduleUrl: "/js/views/customers-view.js"
    },
    "users": {
        title: "Users",
        htmlUrl: "/web/view?page=dashboard-users-view",
        moduleUrl: "/js/views/users-view.js"
    },
    "customer-creation": {
        title: "Customer Creation",
        htmlUrl: "/web/view?page=dashboard-customer-creation-view",
        moduleUrl: "/js/views/customer-creation-view.js"
    },
    "subscriptions-payments": {
        title: "Subscriptions & Payments",
        htmlUrl: "/web/view?page=dashboard-subscriptions-payments-view",
        moduleUrl: "/js/views/subscriptions-payments-view.js"
    },
    "mail-status": {
        title: "Mail Status",
        htmlUrl: "/web/view?page=dashboard-mail-status-view",
        moduleUrl: "/js/views/mail-status-view.js"
    },
    "alerts": {
        title: "Alerts",
        htmlUrl: "/web/view?page=dashboard-alerts-view",
        moduleUrl: "/js/views/alerts-view.js"
    },
    "integrations": {
        title: "Integrations",
        htmlUrl: "/web/view?page=dashboard-integrations-view",
        moduleUrl: "/js/views/integrations-view.js"
    },
    "modules": {
        title: "Modules",
        htmlUrl: "/web/view?page=dashboard-modules-view",
        moduleUrl: "/js/views/modules-view.js"
    },
    "performance": {
        title: "Performance",
        htmlUrl: "/web/view?page=dashboard-performance-view",
        moduleUrl: "/js/views/performance-view.js"
    },
    "audit-security": {
        title: "Audit & Security",
        htmlUrl: "/web/view?page=dashboard-audit-security-view",
        moduleUrl: "/js/views/audit-security-view.js"
    }
};

let activeViewName = null;
let activeController = null;

function formatTime(date) {
    return formatDateTimeForDisplay(date, "da-DK", "—");
}

function setDashboardLoadStatus(value) {
    setText(loadStatus, value, "");
}

function setLastRefreshNow() {
    setText(lastRefresh, formatTime(new Date()), "");
}

function getInitialViewName() {
    const hashValue = window.location.hash.replace("#", "").trim();

    if (hashValue && views[hashValue]) {
        return hashValue;
    }

    return "overview";
}

function updateActiveTab(viewName) {
    tabButtons.forEach(function (button) {
        button.classList.toggle("is-active", button.dataset.dashboardView === viewName);
    });
}

function stopActiveController() {
    if (activeController && typeof activeController.stop === "function") {
        activeController.stop();
    }

    activeController = null;
}

async function activateView(viewName, updateHash = true) {
    const view = views[viewName];

    if (!view || activeViewName === viewName) {
        return;
    }

    stopActiveController();

    activeViewName = viewName;
    setText(activeViewTitle, view.title, "");
    updateActiveTab(viewName);
    setDashboardLoadStatus("Loading…");

    if (updateHash) {
        window.location.hash = viewName;
    }

    viewHost.innerHTML = `<div class="dashboard-loading-card">Loading ${view.title}…</div>`;

    try {
        viewHost.innerHTML = await fetchText(view.htmlUrl, {
            method: "GET",
            headers: {
                "Accept": "text/html"
            },
            credentials: "same-origin"
        });

        const module = await import(view.moduleUrl);

        if (!module.createViewController) {
            throw new Error(`View module does not export createViewController: ${view.moduleUrl}`);
        }

        activeController = module.createViewController({
            root: viewHost,
            setLoadStatus: setDashboardLoadStatus,
            setLastRefreshNow,
            refreshButton
        });

        if (activeController && typeof activeController.start === "function") {
            activeController.start();
        }

        setDashboardLoadStatus("Loaded");
    } catch (error) {
        console.error(error);
        setDashboardLoadStatus("Error");

        viewHost.innerHTML = `
            <div class="dashboard-error-card">
                Could not load dashboard view "${view.title}". Please try again.
            </div>
        `;
    }
}

tabButtons.forEach(function (button) {
    button.addEventListener("click", function () {
        activateView(button.dataset.dashboardView);
    });
});

refreshButton.addEventListener("click", function () {
    if (activeController && typeof activeController.refresh === "function") {
        activeController.refresh();
    }
});

window.addEventListener("hashchange", function () {
    activateView(getInitialViewName(), false);
});

initMenu();
activateView(getInitialViewName(), false);