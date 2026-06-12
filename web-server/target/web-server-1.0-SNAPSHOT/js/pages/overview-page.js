import { byId } from "../core/dom.js";
import { fetchXml } from "../core/http.js";
import { applyTopPanel, setLoadStatus } from "../core/page-header.js";
import { setErrorState, setLoadingState } from "../core/placeholders.js";
import { bootstrapPage } from "../core/bootstrap-page.js";
import { getDirectChild } from "../core/xml.js";
import { initMenu } from "../components/menu.js";
import {
    renderOverviewNotifications,
    setOverviewNotificationsError,
    setOverviewNotificationsLoading
} from "./overview/overview-notifications.js";
import {
    renderOverviewProjectFields
} from "./overview/overview-project-fields.js";
import {
    renderOverviewSrlChart,
    setOverviewSrlError,
    setOverviewSrlLoading
} from "./overview/overview-srl-chart.js";

const DATA_URL = "/project/overview?cmd=overview";

function getElements() {
    return {
        customerName: byId("customerName"),
        projectName: byId("projectName"),
        userName: byId("userName"),
        loadStatus: byId("loadStatus"),
        projectFields: byId("projectFields"),
        notificationsHead: byId("notificationsHead"),
        notificationsBody: byId("notificationsBody"),
        notificationsEmpty: byId("notificationsEmpty"),
        irlSlices: byId("irlSlices"),
        irlLegend: byId("irlLegend")
    };
}

function applyProjectOverviewXml(doc, elements) {
    const root = doc.getElementsByTagName("ProjectOverview")[0] || doc;
    const topPanel = getDirectChild(root, "TopPanel");
    const project = getDirectChild(root, "Project");
    const notificationsNode = getDirectChild(root, "Notifications");

    applyTopPanel(topPanel, elements, { userTagNames: ["Name", "UserName"] });
    renderOverviewProjectFields(project, elements.projectFields);
    renderOverviewNotifications(notificationsNode, elements);
    renderOverviewSrlChart(root, elements);
}

function beforeLoadOverview(elements) {
    setLoadStatus(elements.loadStatus, "Loading…");
    setOverviewNotificationsLoading(elements);
    setLoadingState(elements.projectFields, "Loading project details…");
    setOverviewSrlLoading(elements);
}

async function loadProjectOverviewXml(elements) {
    const doc = await fetchXml(DATA_URL);
    applyProjectOverviewXml(doc, elements);
}

function afterLoadOverview(elements) {
    setLoadStatus(elements.loadStatus, "Loaded");
}

function handleOverviewError(error, elements) {
    setLoadStatus(elements.loadStatus, "Error");
    setErrorState(elements.projectFields, "Failed to load project details.");
    setOverviewNotificationsError(elements);
    setOverviewSrlError(elements);
    console.error(error);
}

async function startPage() {
    await bootstrapPage({
        getElements,
        initializeMenu: true,
        menuInitializer: initMenu,
        beforeLoad: beforeLoadOverview,
        load: loadProjectOverviewXml,
        afterLoad: afterLoadOverview,
        onError: handleOverviewError
    });
}

startPage();

