import {
    byId,
    createCell,
    setInputValue,
    setText
} from "../core/dom.js";
import { formatBoolean, formatFallback } from "../core/format.js";
import { fetchXml, postXml } from "../core/http.js";
import { applyTopPanel, setLoadStatus } from "../core/page-header.js";
import {
    hideState,
    setEmptyState,
    setErrorState,
    setLoadingState
} from "../core/placeholders.js";
import { bootstrapPage } from "../core/bootstrap-page.js";
import { escapeXml, textOf, toBool } from "../core/xml.js";
import { initMenu } from "../components/menu.js";
import {
    applySortIndicators,
    bindSortableHeaders,
    compareByKey,
    nextSortState
} from "../components/sortable-table.js";
import { initTabs } from "../components/tabs.js";

const DATA_URL = "/UserMananger";
const EDIT_URL = "/editdetail";
const SAVE_URL = "/savedetail";

let allRows = [];
let sortState = { key: null, dir: "asc" };
let currentEditId = null;
let reloadPage = async function () {};

function getElements() {
    return {
        customerName: byId("customerName"),
        projectName: byId("projectName"),
        userName: byId("userName"),
        loadStatus: byId("loadStatus"),
        activeOnly: byId("activeOnly"),
        tbody: byId("tbody"),
        emptyState: byId("emptyState"),
        editDialog: byId("editDialog"),
        dlgStatus: byId("dlgStatus"),
        dlgId: byId("dlgId"),
        dlgDescription: byId("dlgDescription"),
        dlgDate: byId("dlgDate"),
        btnSave: byId("btnSave"),
        btnCancel: byId("btnCancel")
    };
}

function setupTabs() {
    initTabs([
        { btnId: "tabBtn1", panelId: "tabPanel1" },
        { btnId: "tabBtn2", panelId: "tabPanel2" },
        { btnId: "tabBtn3", panelId: "tabPanel3" }
    ]);
}

function setSortIndicators() {
    applySortIndicators(
        ["id", "wsn", "description", "modifiedDate", "modifiedBy", "active"],
        sortState
    );
}

function compareValues(a, b, key) {
    return compareByKey(a, b, key, { locale: "en" });
}

function sortBy(key, elements) {
    sortState = nextSortState(sortState, key);

    allRows.sort((a, b) => {
        const result = compareValues(a, b, key);
        return sortState.dir === "asc" ? result : -result;
    });

    setSortIndicators();
    renderTable(elements);
}

function getVisibleRows(elements) {
    const activeOnly = !!elements.activeOnly?.checked;

    return activeOnly ? allRows.filter((row) => row.active === true) : allRows;
}

function renderTable(elements) {
    const visibleRows = getVisibleRows(elements);
    elements.tbody.innerHTML = "";

    if (!visibleRows.length) {
        setEmptyState(
            elements.emptyState,
            allRows.length
                ? "No rows match the current filter."
                : "No rows returned from the web service."
        );
        return;
    }

    hideState(elements.emptyState);

    visibleRows.forEach((row) => {
        const tr = document.createElement("tr");

        tr.appendChild(createCell("td", formatFallback(row.id), "col-id"));
        tr.appendChild(createCell("td", formatFallback(row.wsn)));
        tr.appendChild(createCell("td", formatFallback(row.description)));
        tr.appendChild(createCell("td", formatFallback(row.modifiedDate)));
        tr.appendChild(createCell("td", formatFallback(row.modifiedBy)));
        tr.appendChild(createCell("td", formatBoolean(row.active, "true", "false")));

        tr.addEventListener("click", () => openEditDialog(row.id, elements));
        elements.tbody.appendChild(tr);
    });
}

async function openEditDialog(id, elements) {
    currentEditId = id;

    setText(elements.dlgId, id, "");
    setInputValue(elements.dlgDescription, "");
    setInputValue(elements.dlgDate, "");

    setText(elements.dlgStatus, "Loading…", "");
    elements.editDialog.showModal();

    try {
        const doc = await fetchXml(`${EDIT_URL}?id=${encodeURIComponent(id)}`);
        const detail = doc.getElementsByTagName("detail")[0] || doc;

        setInputValue(elements.dlgDescription, textOf(detail, "Description"));
        setInputValue(elements.dlgDate, textOf(detail, "Date"));
        setText(elements.dlgStatus, "Loaded.", "");
    } catch (error) {
        setText(elements.dlgStatus, "Failed to load details.", "");
        console.error(error);
    }
}

async function saveAndClose(elements) {
    if (!currentEditId) {
        elements.editDialog.close();
        return;
    }

    setText(elements.dlgStatus, "Saving…", "");

    try {
        const payload =
            "<detail>" +
            "<ID>" + String(currentEditId) + "</ID>" +
            "<Description>" + escapeXml(elements.dlgDescription.value) + "</Description>" +
            "<Date>" + escapeXml(elements.dlgDate.value) + "</Date>" +
            "</detail>";

        await postXml(SAVE_URL, payload);

        setText(elements.dlgStatus, "Saved.", "");
        elements.editDialog.close();

        await reloadPage();
    } catch (error) {
        setText(elements.dlgStatus, "Save failed.", "");
        console.error(error);
    }
}

function applyUserManagerXml(doc, elements) {
    const userManager = doc.getElementsByTagName("UserManager")[0] || doc;
    const topPanel = userManager.getElementsByTagName("TopPanel")[0];

    applyTopPanel(topPanel, elements);

    const rowNodes = Array.from(doc.getElementsByTagName("row"));

    allRows = rowNodes.map((node) => ({
        id: textOf(node, "ID"),
        wsn: textOf(node, "WSN"),
        description: textOf(node, "Description"),
        modifiedDate: textOf(node, "ModifiedDate"),
        modifiedBy: textOf(node, "ModifiedBy"),
        active: toBool(textOf(node, "Active"))
    }));

    sortState = { key: null, dir: "asc" };
    setSortIndicators();
    renderTable(elements);
}

async function initializePage(elements) {
    setupTabs();
    bindEvents(elements);
    setSortIndicators();
    renderTable(elements);
}

function beforeLoadUserManager(elements) {
    setLoadStatus(elements.loadStatus, "Loading…");
    setLoadingState(elements.emptyState, "Loading XML from web service…");
}

async function loadUserManagerXml(elements) {
    const doc = await fetchXml(DATA_URL);
    applyUserManagerXml(doc, elements);
}

function afterLoadUserManager(elements) {
    setLoadStatus(elements.loadStatus, "Loaded");
}

function handleUserManagerError(error, elements) {
    setLoadStatus(elements.loadStatus, "Error");
    allRows = [];
    renderTable(elements);
    setErrorState(elements.emptyState, `Failed to load XML from ${DATA_URL}.`);
    console.error(error);
}

function bindEvents(elements) {
    elements.activeOnly?.addEventListener("change", () => renderTable(elements));

    bindSortableHeaders(document, (key) => sortBy(key, elements));

    elements.btnCancel?.addEventListener("click", () => elements.editDialog.close());
    elements.btnSave?.addEventListener("click", () => saveAndClose(elements));
}

async function startPage() {
    const page = await bootstrapPage({
        getElements,
        initialize: initializePage,
        initializeMenu: true,
        menuInitializer: initMenu,
        beforeLoad: beforeLoadUserManager,
        load: loadUserManagerXml,
        afterLoad: afterLoadUserManager,
        onError: handleUserManagerError
    });

    reloadPage = page.reload;
}

startPage();