import { createGenericPage } from "../generel.js";
import { initMenu } from "../components/menu.js";
import { mountTopbar } from "../components/topbar.js";
import { initTabs } from "../components/tabs.js";
import { createHistoryTable } from "../components/history-table.js";
import { createNotesTable } from "../components/notes-table.js";
import { createAttachmentsTable } from "../components/attachments-table.js";
import { createExportDialog } from "../components/export-dialog.js";
import { createImportDialog } from "../components/import-dialog.js";
import {
    getDirectChild,
    serializeXml
} from "../core/xml.js";

const SYSTEMS_BREAKDOWN_PAGE_URL = "/web/view?page=systemsbreakdown";

const historyTable = createHistoryTable({
    editPageUrl: SYSTEMS_BREAKDOWN_PAGE_URL,
    defaultReturnUrl: SYSTEMS_BREAKDOWN_PAGE_URL,
    onAfterRender: initializeResizableTablesAfterHistoryRender
});

const notesTable = createNotesTable({
    onAfterRender: initializeResizableTablesAfterNotesRender
});

const attachmentsTable = createAttachmentsTable({
    onAfterRender: initializeResizableTablesAfterAttachmentRender
});

function findDetailNode(root) {
    return root.querySelector("systemBreakdownDocument > systembreakdown")
        || root.querySelector("systemBreakdownDocument systembreakdown")
        || root.querySelector("systembreakdown");
}

function ensureChild(doc, parent, tagName) {
    let child = getDirectChild(parent, tagName);

    if (!child) {
        child = doc.createElement(tagName);
        parent.appendChild(child);
    }

    return child;
}

function resolveSelectValue(uiField, xmlFieldNode) {
    if (!uiField) {
        return xmlFieldNode?.getElementsByTagName("Value")?.[0]?.textContent?.trim() || "";
    }

    const options = Array.from(uiField.options || []);
    const selectedOption = options.find((option) => option.selected && (option.value || "").trim() !== "");

    if (selectedOption) {
        return (selectedOption.value || "").trim();
    }

    const valueFromSelectedIndex =
        uiField.selectedIndex != null && uiField.selectedIndex >= 0
            ? (uiField.options?.[uiField.selectedIndex]?.value || "").trim()
            : "";

    if (valueFromSelectedIndex) {
        return valueFromSelectedIndex;
    }

    const firstNonEmpty = options.find((option) => (option.value || "").trim() !== "");

    if (firstNonEmpty) {
        return (firstNonEmpty.value || "").trim();
    }

    return xmlFieldNode?.getElementsByTagName("Value")?.[0]?.textContent?.trim() || "";
}

function updateDetailFieldsFromUi(updatedDoc, detailNode, elements) {
    if (!detailNode || !elements?.basisInfoFields) {
        return;
    }

    const fields = Array.from(elements.basisInfoFields.querySelectorAll("[data-field]"));

    fields.forEach((uiField) => {
        const name = uiField.getAttribute("data-field");

        if (!name) {
            return;
        }

        const child = ensureChild(updatedDoc, detailNode, name);
        const control = (child.getAttribute("control") || "").toLowerCase();

        if (control === "select") {
            updateSelectField(updatedDoc, child, resolveSelectValue(uiField, child));
            return;
        }

        if (uiField.type === "checkbox") {
            child.textContent = uiField.checked ? "true" : "false";
            return;
        }

        child.textContent = uiField.value ?? "";
    });
}

function updateSelectField(updatedDoc, child, value) {
    let valueNode = child.getElementsByTagName("Value")?.[0] || null;

    if (!valueNode) {
        valueNode = updatedDoc.createElement("Value");
        child.insertBefore(valueNode, child.firstChild);
    }

    valueNode.textContent = value;

    Array.from(child.getElementsByTagName("Option")).forEach((option) => {
        if (option.getAttribute("selected") != null) {
            option.removeAttribute("selected");
        }

        if ((option.getAttribute("value") || "").trim() === value) {
            option.setAttribute("selected", "true");
        }
    });
}

function buildSystemBreakdownSavePayload(elements, context) {
    const currentDoc = context?.currentDoc;
    const detailEntityTag = context?.detailEntityTag || "systembreakdown";

    if (!currentDoc || !currentDoc.documentElement) {
        throw new Error("No XML document loaded.");
    }

    const updatedDoc = currentDoc.cloneNode(true);
    const root = updatedDoc.documentElement;

    const detailContainer = getDirectChild(root, "systemBreakdownDocument");
    const detailNode = detailContainer
        ? getDirectChild(detailContainer, detailEntityTag)
        : getDirectChild(root, detailEntityTag);

    updateDetailFieldsFromUi(updatedDoc, detailNode, elements);

    notesTable.writeToDocument(updatedDoc);
    attachmentsTable.writeToDocument(updatedDoc);

    return serializeXml(updatedDoc);
}

function getSystemBreakdownEntityIdFromDoc(doc) {
    const detailNode = findDetailNode(doc);

    if (!detailNode) {
        return "";
    }

    return getFirstFieldRawValue(detailNode, [
        "EntityId",
        "SystemBreakdownId",
        "SystemBreakdownPK",
        "SystemBreakdownEntityId"
    ]);
}

function getFirstFieldRawValue(node, fieldNames, fallback = "") {
    for (const fieldName of fieldNames) {
        const field = getDirectChild(node, fieldName);

        if (!field) {
            continue;
        }

        const valueText = field.querySelector(":scope > Value")?.textContent?.trim();
        const directText = getDirectTextOnly(field).trim();

        if (valueText) {
            return valueText;
        }

        if (directText) {
            return directText;
        }
    }

    return fallback;
}

function getDirectTextOnly(node) {
    if (!node) {
        return "";
    }

    return Array.from(node.childNodes || [])
        .filter((child) => child.nodeType === Node.TEXT_NODE)
        .map((child) => child.textContent || "")
        .join("");
}

function ensureTableColGroup(table) {
    if (!table) {
        return;
    }

    const headerCells = Array.from(table.querySelectorAll("thead th"));

    if (!headerCells.length) {
        return;
    }

    let colGroup = table.querySelector(":scope > colgroup");

    if (!colGroup) {
        colGroup = document.createElement("colgroup");
        table.insertBefore(colGroup, table.firstChild);
    }

    while (colGroup.children.length < headerCells.length) {
        colGroup.appendChild(document.createElement("col"));
    }

    while (colGroup.children.length > headerCells.length) {
        colGroup.removeChild(colGroup.lastElementChild);
    }
}

function initializeResizableTablesAfterHistoryRender() {
    ensureTableColGroup(document.querySelector(".history-table"));
}

function initializeResizableTablesAfterNotesRender() {
    ensureTableColGroup(document.querySelector(".notes-table"));
}

function initializeResizableTablesAfterAttachmentRender() {
    ensureTableColGroup(document.querySelector(".attachments-table"));
}

function start() {
    mountTopbar();

    const exportDialog = createExportDialog({
        dialogId: "exportDialog",
        openButtonId: "btnExport",
        exportUrl: "/project/systembreakdown",
        baseFileName: "systemsbreakdown"
    });

    let page = null;

    const importDialog = createImportDialog({
        dialogId: "importDialog",
        openButtonId: "btnImport",
        importUrl: "/project/systembreakdown?cmd=import",
        onImportComplete: async () => {
            await page?.start?.();
        }
    });

    page = createGenericPage({
        listUrl: "/project/systembreakdown?cmd=list",
        detailUrl: "/project/systembreakdown?cmd=edit&id=",
        saveUrl: "/project/systembreakdown?cmd=save",
        rootListTag: "SystemBreakdownList",
        listEntityTag: "systembreakdowns",
        detailEntityTag: "systembreakdown",
        findDetailNode,
        afterListLoad: (_doc, _elements, context) => {
            const rowCount = context?.rowCount || 0;

            exportDialog.setVisible(rowCount > 0);
            importDialog.setVisible(true);
        },
        afterDetailLoad: (doc) => {
            const entityId = getSystemBreakdownEntityIdFromDoc(doc);

            historyTable.setContext({
                id: entityId,
                returnUrl: SYSTEMS_BREAKDOWN_PAGE_URL
            }, {
                render: false
            });

            historyTable.loadFromDocument(doc);
            notesTable.loadFromDocument(doc);
            attachmentsTable.loadFromDocument(doc);
        },
        buildSavePayload: buildSystemBreakdownSavePayload
    });

    initTabs([
        { btnId: "tabBtn1", panelId: "tabPanel1" },
        { btnId: "tabBtn2", panelId: "tabPanel2" },
        { btnId: "tabBtn3", panelId: "tabPanel3" },
        { btnId: "tabBtn4", panelId: "tabPanel4" },
        { btnId: "tabBtn5", panelId: "tabPanel5" },
        { btnId: "tabBtn6", panelId: "tabPanel6" },
        { btnId: "tabBtn7", panelId: "tabPanel7" },
        { btnId: "tabBtn8", panelId: "tabPanel8" },
        { btnId: "tabBtn9", panelId: "tabPanel9" }
    ]);

    initMenu(document);

    historyTable.bind();
    notesTable.bind();
    attachmentsTable.bind();

    exportDialog.bind();
    importDialog.bind();

    page.start();
}

start();