import { setText } from "./dom.js";
import { getChildText, textOf } from "./xml.js";

export function parseTopPanel(xmlDocument, options = {}) {
    const topPanelSelector = options.topPanelSelector || "TopPanel";
    const userTagNames = options.userTagNames || ["UserName", "Name"];

    const topPanelElement = xmlDocument?.querySelector?.(topPanelSelector)
        || xmlDocument?.getElementsByTagName?.(topPanelSelector)?.[0]
        || null;

    if (!topPanelElement) {
        return {
            customerName: "—",
            projectName: "—",
            userName: "—"
        };
    }

    const userName = userTagNames
        .map((tagName) => getChildText(topPanelElement, tagName, ""))
        .find((value) => value);

    return {
        customerName: getChildText(topPanelElement, "CustomerName", "—"),
        projectName: getChildText(topPanelElement, "ProjectName", "—"),
        userName: userName || "—"
    };
}

export function applyTopPanel(topPanel, elements, options = {}) {
    const userTagNames = options.userTagNames || ["UserName", "Name"];

    if (!elements) {
        return;
    }

    if (topPanel && typeof topPanel === "object" && "customerName" in topPanel) {
        setText(elements.customerName, topPanel.customerName);
        setText(elements.projectName, topPanel.projectName);
        setText(elements.userName, topPanel.userName);
        return;
    }

    setText(elements.customerName, topPanel ? textOf(topPanel, "CustomerName") : "");
    setText(elements.projectName, topPanel ? textOf(topPanel, "ProjectName") : "");

    const userValue = topPanel
        ? userTagNames.map((tagName) => textOf(topPanel, tagName)).find((value) => value)
        : "";

    setText(elements.userName, userValue || "");
}

export function applyTopPanelFromDocument(xmlDocument, elements, options = {}) {
    const topPanel = parseTopPanel(xmlDocument, options);

    applyTopPanel(topPanel, elements, options);

    return topPanel;
}

export function setLoadStatus(loadStatusElement, statusText) {
    setText(loadStatusElement, statusText, "");
}