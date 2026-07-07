import { setText } from "./dom.js";
import { getChildText } from "./xml.js";
import { applyTopbarMetadata, readTopbarMetadata } from "../components/topbar.js";

export function parseTopPanel(xmlDocument, options = {}) {
    const topPanelSelector = options.topPanelSelector || "TopPanel";
    const userTagNames = options.userTagNames || ["UserName", "Name"];

    const topPanelElement = xmlDocument?.querySelector?.(topPanelSelector)
        || xmlDocument?.getElementsByTagName?.(topPanelSelector)?.[0]
        || null;

    if (!topPanelElement) {
        return readTopbarMetadata({});
    }

    const userName = userTagNames
        .map((tagName) => getChildText(topPanelElement, tagName, ""))
        .find((value) => value);

    return {
        ...readTopbarMetadata(topPanelElement),
        customerName: getChildText(topPanelElement, "CustomerName", "—"),
        projectName: getChildText(topPanelElement, "ProjectName", "—"),
        userName: userName || "—"
    };
}

export function applyTopPanel(topPanel, elements, options = {}) {
    const metadata = readTopbarMetadata(topPanel);

    if (!elements) {
        return;
    }

    applyTopbarMetadata(document, metadata);

    setText(elements.customerName, metadata.customerName);
    setText(elements.projectName, metadata.projectName);
    setText(elements.userName, metadata.userName);
}

export function applyTopPanelFromDocument(xmlDocument, elements, options = {}) {
    const topPanel = parseTopPanel(xmlDocument, options);

    applyTopPanel(topPanel, elements, options);

    return topPanel;
}

export function setLoadStatus(loadStatusElement, statusText) {
    setText(loadStatusElement, statusText, "");
}
