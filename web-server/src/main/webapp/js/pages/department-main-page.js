import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { setText } from "../core/dom.js";
import { fetchXml, postXml } from "../core/http.js";
import { escapeHtml } from "../core/html.js";
import { escapeXml, getChildText, getDirectChild, getDirectChildren, hasXmlParseError } from "../core/xml.js";

const LIST_URL = "/api/admin/departments?cmd=list";
const EDIT_URL = "/api/admin/departments?cmd=edit&id=";
const CREATE_URL = "/api/admin/departments?cmd=create";
const SAVE_URL = "/api/admin/departments?cmd=save";

const state = {
    currentDoc: null,
    topPanel: {
        customerName: "-",
        projectName: "-",
        userName: "-"
    },
    departments: [],
    currentDepartment: null,
    dirty: false
};

document.addEventListener("DOMContentLoaded", () => {
    start();
});

function start() {
    initializeShell();
    initializeEvents();
    loadDepartmentData();
}

function initializeShell() {
    setText("customerName", "-", "");
    setText("projectName", "-", "");
    setText("userName", "-", "");
    setText("loadStatus", "Loading", "");

    initMenu(document);
    initHelpDialog();
    mountTopbar(document);
}

function initializeEvents() {
    const addButton = byId("btnAddDepartment");
    const saveButton = byId("departmentSaveBtn");
    const cancelButton = byId("departmentCancelBtn");
    const dialog = byId("departmentDialog");

    addButton?.addEventListener("click", () => {
        openDepartmentDialog("create");
    });

    saveButton?.addEventListener("click", saveDepartment);

    cancelButton?.addEventListener("click", () => {
        closeDepartmentDialog("cancel");
    });

    dialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeDepartmentDialog("cancel");
    });

    dialog?.addEventListener("close", () => {
        state.dirty = false;
        state.currentDepartment = null;
        setText("departmentDialogStatus", "Idle", "");
        setText("departmentDialogModeLabel", "Idle", "");
    });

    [byId("departmentNameInput"), byId("departmentDescriptionInput"), byId("departmentActiveInput")].forEach((element) => {
        element?.addEventListener("input", () => markDirty());
        element?.addEventListener("change", () => markDirty());
    });
}

async function loadDepartmentData() {
    showEmptyState("Loading departments...");
    setText("loadStatus", "Loading", "");

    try {
        const xmlDocument = await fetchXml(LIST_URL, {
            cache: "no-store",
            credentials: "same-origin"
        });

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("Department endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.topPanel = parseTopPanel(xmlDocument);
        state.departments = parseDepartments(xmlDocument);

        applyTopPanel();
        renderTable();

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load departments", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load departments. ${error.message}`);
    }
}

function applyTopPanel() {
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
}

function parseTopPanel(xmlDocument) {
    const topPanel = getDirectChild(xmlDocument.documentElement, "TopPanel");

    if (!topPanel) {
        return {
            customerName: "-",
            projectName: "-",
            userName: "-"
        };
    }

    return {
        customerName: getChildText(topPanel, "CustomerName", "-"),
        projectName: getChildText(topPanel, "ProjectName", "-"),
        userName: getChildText(topPanel, "Name", getChildText(topPanel, "UserName", "-"))
    };
}

function parseDepartments(xmlDocument) {
    const departmentsElement = getDirectChild(xmlDocument.documentElement, "departments");
    const departmentNodes = departmentsElement
        ? getDirectChildren(departmentsElement, "department")
        : Array.from(xmlDocument.getElementsByTagName("department"));

    return departmentNodes.map((node) => ({
        departmentId: parseNullableInt(getChildText(node, "DepartmentId", "")),
        departmentName: getChildText(node, "DepartmentName", ""),
        departmentDescription: getChildText(node, "DepartmentDescription", ""),
        active: parseBoolean(getChildText(node, "Active", "true"))
    })).filter((item) => item.departmentId !== null);
}

function renderTable() {
    const tbody = byId("tbody");
    const emptyState = byId("listEmptyState");

    if (!tbody) {
        return;
    }

    const rows = [...state.departments].sort((left, right) => String(left.departmentName || "").localeCompare(String(right.departmentName || ""), "da", {
        numeric: true,
        sensitivity: "base"
    }));

    tbody.innerHTML = "";

    if (!rows.length) {
        showEmptyState("No departments found for the current customer.");
        return;
    }

    hideEmptyState();

    tbody.innerHTML = rows.map((department) => renderRow(department)).join("");

    tbody.querySelectorAll("tr[data-department-id]").forEach((row) => {
        const departmentId = row.getAttribute("data-department-id");

        row.addEventListener("dblclick", () => openDepartmentDialog("edit", departmentId));
        row.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openDepartmentDialog("edit", departmentId);
            }
        });

        row.tabIndex = 0;
    });

    if (emptyState) {
        emptyState.classList.remove("is-visible");
    }
}

function renderRow(department) {
    return `
        <tr data-department-id="${escapeHtml(String(department.departmentId || ""))}">
            <td title="${escapeHtml(department.departmentName || "")}">${escapeHtml(department.departmentName || "")}</td>
            <td title="${escapeHtml(department.departmentDescription || "")}">${escapeHtml(department.departmentDescription || "")}</td>
            <td class="department-main-active-cell">${renderActiveIcon(department.active)}</td>
        </tr>
    `;
}

function renderActiveIcon(value) {
    return value
        ? '<span class="department-main-active-state" title="Active" aria-label="Active"><span class="department-main-active-dot is-active" aria-hidden="true"></span></span>'
        : '<span class="department-main-active-state" title="Inactive" aria-label="Inactive"><span class="department-main-active-dot is-inactive" aria-hidden="true"></span></span>';
}

function openDepartmentDialog(mode, departmentId = null) {
    const dialog = byId("departmentDialog");

    if (!dialog) {
        return;
    }

    if (dialog.open && state.dirty && !window.confirm("There are unsaved changes. Continue?")) {
        return;
    }

    state.dirty = false;
    state.currentDepartment = null;
    setText("departmentDialogStatus", "Loading...", "");
    setText("departmentDialogModeLabel", mode === "edit" ? "Editing department" : "Creating department", "");
    dialog.showModal();

    loadDepartmentDialog(mode, departmentId).catch((error) => {
        console.error("Failed to open department dialog", error);
        setText("departmentDialogStatus", "Failed to load.", "");
        window.alert(error.message || "Failed to load department.");
        closeDepartmentDialog("error");
    });
}

async function loadDepartmentDialog(mode, departmentId) {
    const url = new URL(mode === "edit" ? `${EDIT_URL}${encodeURIComponent(String(departmentId || ""))}` : CREATE_URL, window.location.origin);
    const xmlDocument = await fetchXml(url.toString(), {
        cache: "no-store",
        credentials: "same-origin"
    });

    if (hasXmlParseError(xmlDocument)) {
        throw new Error("Department detail endpoint returned invalid XML.");
    }

    const departmentNode = getDirectChild(xmlDocument.documentElement, "department");
    const department = departmentNode ? parseDepartmentNode(departmentNode) : buildEmptyDepartment();

    state.currentDepartment = department;
    fillDepartmentDialog(department, mode);
    setText("departmentDialogStatus", mode === "edit" ? "Editing" : "Creating", "");
}

function parseDepartmentNode(node) {
    return {
        departmentId: parseNullableInt(getChildText(node, "DepartmentId", "")),
        departmentName: getChildText(node, "DepartmentName", ""),
        departmentDescription: getChildText(node, "DepartmentDescription", ""),
        active: parseBoolean(getChildText(node, "Active", "true"))
    };
}

function buildEmptyDepartment() {
    return {
        departmentId: null,
        departmentName: "",
        departmentDescription: "",
        active: true
    };
}

function fillDepartmentDialog(department, mode) {
    setValue("departmentId", department.departmentId === null ? "" : String(department.departmentId));
    setValue("departmentNameInput", department.departmentName || "");
    setValue("departmentDescriptionInput", department.departmentDescription || "");
    setChecked("departmentActiveInput", department.active);
    setText("departmentDialogTitle", mode === "edit" ? "Edit Department" : "Create Department", "");
    setText("departmentDialogModeLabel", mode === "edit" ? "Editing department" : "Creating department", "");
    state.dirty = false;
}

async function saveDepartment() {
    const departmentId = getValue("departmentId");
    const departmentName = getValue("departmentNameInput").trim();
    const departmentDescription = getValue("departmentDescriptionInput").trim();
    const active = byId("departmentActiveInput")?.checked ? "true" : "false";

    if (!departmentName) {
        window.alert("Department name is required.");
        return;
    }

    const payload = `
        <departmentSave>
            <department>
                <DepartmentId>${escapeXml(departmentId)}</DepartmentId>
                <DepartmentName>${escapeXml(departmentName)}</DepartmentName>
                <DepartmentDescription>${escapeXml(departmentDescription)}</DepartmentDescription>
                <Active>${escapeXml(active)}</Active>
            </department>
        </departmentSave>
    `.trim();

    setText("departmentDialogStatus", "Saving...", "");

    try {
        await postXml(SAVE_URL, payload, {
            credentials: "same-origin"
        });

        state.dirty = false;
        closeDepartmentDialog("saved");
        await loadDepartmentData();
    } catch (error) {
        console.error("Failed to save department", error);
        setText("departmentDialogStatus", "Save failed.", "");
        window.alert(error.message || "Failed to save department.");
    }
}

function closeDepartmentDialog(_reason) {
    const dialog = byId("departmentDialog");

    if (!dialog) {
        return;
    }

    if (_reason !== "saved" && state.dirty && !window.confirm("There are unsaved changes. Close the dialog?")) {
        return;
    }

    dialog.close();
    state.dirty = false;
}

function markDirty() {
    state.dirty = true;
}

function showEmptyState(message) {
    const emptyState = byId("listEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.textContent = message;
    emptyState.classList.add("is-visible");
    emptyState.hidden = false;
}

function hideEmptyState() {
    const emptyState = byId("listEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.classList.remove("is-visible");
    emptyState.hidden = true;
}

function getValue(id) {
    return byId(id)?.value ?? "";
}

function setValue(id, value) {
    const element = byId(id);
    if (element) {
        element.value = value ?? "";
    }
}

function setChecked(id, value) {
    const element = byId(id);
    if (element) {
        element.checked = Boolean(value);
    }
}

function byId(id) {
    return document.getElementById(id);
}

function parseNullableInt(value) {
    const normalized = String(value || "").trim();

    if (!normalized) {
        return null;
    }

    const parsed = Number.parseInt(normalized, 10);
    return Number.isFinite(parsed) ? parsed : null;
}

function parseBoolean(value) {
    const normalized = String(value || "").trim().toLowerCase();
    return ["true", "1", "yes", "ja", "y", "on"].includes(normalized);
}
