import { initMenu } from "../components/menu.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { setText } from "../core/dom.js";
import { fetchXml, postXml } from "../core/http.js";
import { escapeHtml } from "../core/html.js";
import { buildColorChipStyle, sanitizeCssColor } from "../core/css.js";
import { escapeXml, getChildText, getDirectChild, getDirectChildren, hasXmlParseError } from "../core/xml.js";

const LIST_URL = "/basis/lookup?cmd=list";
const EDIT_URL = "/basis/lookup?cmd=edit&id=";
const CREATE_URL = "/basis/lookup?cmd=create&lookupTypeId=";
const SAVE_URL = "/basis/lookup?cmd=save";
const STORAGE_KEY = "lookup.main.selectedLookupTypeId";

const STANDARD_COLORS = [
    { value: "#000000", label: "Black" },
    { value: "#FFFFFF", label: "White" },
    { value: "#808080", label: "Gray" },
    { value: "#C0C0C0", label: "Silver" },
    { value: "#FF0000", label: "Red" },
    { value: "#008000", label: "Green" },
    { value: "#0000FF", label: "Blue" },
    { value: "#FFFF00", label: "Yellow" },
    { value: "#FFA500", label: "Orange" },
    { value: "#800080", label: "Purple" },
    { value: "#FFC0CB", label: "Pink" },
    { value: "#A52A2A", label: "Brown" },
    { value: "#000080", label: "Navy" },
    { value: "#008080", label: "Teal" },
    { value: "#FFD700", label: "Gold" }
];

const state = {
    currentDoc: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    },
    lookupTypes: [],
    lookups: [],
    selectedTypeId: loadStoredLookupTypeId(),
    currentLookup: null,
    dirty: false
};

document.addEventListener("DOMContentLoaded", () => {
    start();
});

function start() {
    initializeShell();
    initializeEvents();
    loadLookupData();
}

function initializeShell() {
    setText("customerName", "—", "");
    setText("projectName", "—", "");
    setText("userName", "—", "");
    setText("loadStatus", "Loading", "");

    initMenu(document);
    mountTopbar(document);
}

function initializeEvents() {
    const typeSelect = byId("lookupTypeSelect");
    const addButton = byId("btnAddLookup");
    const saveButton = byId("lookupSaveBtn");
    const cancelButton = byId("lookupCancelBtn");
    const dialog = byId("lookupDialog");
    const presetSelect = byId("lookupColorPresetSelect");
    const picker = byId("lookupColorInput");

    typeSelect?.addEventListener("change", async () => {
        const nextTypeId = typeSelect.value.trim();

        if (dialog?.open && state.dirty && !window.confirm("Der er ændringer i dialogen, som ikke er gemt. Vil du skifte type alligevel?")) {
            typeSelect.value = String(state.selectedTypeId || "");
            return;
        }

        state.selectedTypeId = nextTypeId ? Number(nextTypeId) : null;
        persistSelectedLookupTypeId(state.selectedTypeId);
        await loadLookupData(state.selectedTypeId);
    });

    addButton?.addEventListener("click", () => {
        if (!state.selectedTypeId) {
            window.alert("Vælg en lookup type først.");
            return;
        }

        openLookupDialog("create");
    });

    saveButton?.addEventListener("click", saveLookup);

    cancelButton?.addEventListener("click", () => {
        closeLookupDialog("cancel");
    });

    dialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeLookupDialog("cancel");
    });

    dialog?.addEventListener("close", () => {
        state.dirty = false;
        state.currentLookup = null;
        setText("lookupDialogStatus", "Idle", "");
        setText("lookupDialogModeLabel", "Idle", "");
    });

    const codeInput = byId("lookupCodeInput");
    const descriptionInput = byId("lookupDescriptionInput");
    const displayOrderInput = byId("lookupDisplayOrderInput");
    const activeInput = byId("lookupActiveInput");

    codeInput?.addEventListener("input", () => {
        markDirty();
        updateLookupPreview();
    });
    codeInput?.addEventListener("change", () => {
        markDirty();
        updateLookupPreview();
    });

    [descriptionInput, displayOrderInput, activeInput].forEach((element) => {
        element?.addEventListener("input", () => markDirty());
        element?.addEventListener("change", () => markDirty());
    });

    presetSelect?.addEventListener("change", () => {
        syncColorControlsFromPreset();
        markDirty();
        updateLookupPreview();
    });

    picker?.addEventListener("input", () => {
        syncPresetFromPicker();
        markDirty();
        updateLookupPreview();
    });
}

async function loadLookupData(typeId = state.selectedTypeId) {
    showEmptyState("Loading lookups...");
    setText("loadStatus", "Loading", "");

    try {
        const url = new URL(LIST_URL, window.location.origin);

        if (typeId) {
            url.searchParams.set("lookupTypeId", String(typeId));
        }

        const xmlDocument = await fetchXml(url.toString(), {
            cache: "no-store",
            credentials: "same-origin"
        });

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("Lookup endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.topPanel = parseTopPanel(xmlDocument);
        state.lookupTypes = parseLookupTypes(xmlDocument);
        state.lookups = parseLookups(xmlDocument);

        const serverSelectedTypeId = parseNullableInt(getChildText(xmlDocument.documentElement, "selectedLookupTypeId", ""));
        const storedTypeIsValid = state.lookupTypes.some((lookupType) => lookupType.lookupTypeId === state.selectedTypeId);
        const resolvedTypeId = serverSelectedTypeId !== null
            ? serverSelectedTypeId
            : (storedTypeIsValid ? state.selectedTypeId : firstLookupTypeId());
        state.selectedTypeId = resolvedTypeId;
        persistSelectedLookupTypeId(state.selectedTypeId);

        applyTopPanel();
        renderLookupTypeSelect();
        renderTable();

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load lookups", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load lookups. ${error.message}`);
    }
}

function applyTopPanel() {
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
}

function parseTopPanel(xmlDocument) {
    const topPanel = getDirectChild(xmlDocument.documentElement, "TopPanel");

    if (!topPanel) {
        return {
            customerName: "—",
            projectName: "—",
            userName: "—"
        };
    }

    return {
        customerName: getChildText(topPanel, "CustomerName", "—"),
        projectName: getChildText(topPanel, "ProjectName", "—"),
        userName: getChildText(topPanel, "Name", getChildText(topPanel, "UserName", "—"))
    };
}

function parseLookupTypes(xmlDocument) {
    const typesElement = getDirectChild(xmlDocument.documentElement, "lookupTypes");
    const typeNodes = typesElement
        ? getDirectChildren(typesElement, "lookupType")
        : Array.from(xmlDocument.getElementsByTagName("lookupType"));

    return typeNodes.map((node) => ({
        lookupTypeId: parseNullableInt(getChildText(node, "LookupTypeId", getChildText(node, "LookupTypeID", ""))),
        lookupTypeDesc: getChildText(node, "LookupTypeDesc", getChildText(node, "LookupTypeDescription", "")),
        color: normalizeColor(node.getAttribute("color") || node.getAttribute("Color") || "")
    })).filter((item) => item.lookupTypeId !== null);
}

function parseLookups(xmlDocument) {
    const lookupsElement = getDirectChild(xmlDocument.documentElement, "lookups");

    return getDirectChildren(lookupsElement, "lookup").map((node, index) => ({
        node,
        index,
        lookupId: parseNullableInt(getChildText(node, "LookupId", "")),
        lookupTypeId: parseNullableInt(getChildText(node, "LookupType", "")),
        lookupCode: getChildText(node, "LookupCode", ""),
        lookupDescription: getChildText(node, "LookupDescription", ""),
        color: normalizeColor(getChildText(node, "Color", "")),
        displayOrder: parseNullableInt(getChildText(node, "DisplayOrder", "")),
        active: parseBoolean(getChildText(node, "Active", "true"))
    })).filter((row) => row.lookupId !== null);
}

function renderLookupTypeSelect() {
    const typeSelect = byId("lookupTypeSelect");
    const dialogTypeSelect = byId("lookupTypeLabel");

    if (!typeSelect || !dialogTypeSelect) {
        return;
    }

    const optionsHtml = state.lookupTypes.map((lookupType) => {
        const selected = lookupType.lookupTypeId === state.selectedTypeId ? " selected" : "";
        const label = lookupType.lookupTypeDesc || String(lookupType.lookupTypeId);
        const color = lookupType.color ? ` style="color: ${escapeHtml(lookupType.color)};"` : "";
        return `<option value="${escapeHtml(String(lookupType.lookupTypeId))}"${selected}${color}>${escapeHtml(label)}</option>`;
    }).join("");

    const placeholder = '<option value="">Select lookup type</option>';
    typeSelect.innerHTML = placeholder + optionsHtml;
    dialogTypeSelect.innerHTML = optionsHtml || placeholder;

    if (state.selectedTypeId !== null && state.selectedTypeId !== undefined) {
        typeSelect.value = String(state.selectedTypeId);
        dialogTypeSelect.value = String(state.selectedTypeId);
    } else {
        typeSelect.value = "";
    }
}

function renderTable() {
    const tbody = byId("tbody");
    const emptyState = byId("listEmptyState");

    if (!tbody) {
        return;
    }

    const rows = getSortedLookups(state.lookups.filter((lookup) => lookup.lookupTypeId === state.selectedTypeId));
    tbody.innerHTML = "";

    if (!rows.length) {
        showEmptyState(state.lookupTypes.length ? "No lookup rows for the selected type." : "No lookup types found.");
        return;
    }

    hideEmptyState();

    tbody.innerHTML = rows.map((lookup) => renderRow(lookup)).join("");

    tbody.querySelectorAll("tr[data-lookup-id]").forEach((row) => {
        const lookupId = row.getAttribute("data-lookup-id");

        row.addEventListener("dblclick", () => openLookupDialog("edit", lookupId));
        row.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openLookupDialog("edit", lookupId);
            }
        });

        row.tabIndex = 0;
    });

    if (emptyState) {
        emptyState.classList.remove("is-visible");
    }
}

function renderRow(lookup) {
    return `
        <tr data-lookup-id="${escapeHtml(String(lookup.lookupId || ""))}">
            <td title="${escapeHtml(lookup.lookupCode || "")}">${renderLookupCodeCell(lookup.lookupCode, lookup.color)}</td>
            <td title="${escapeHtml(lookup.lookupDescription || "")}">${escapeHtml(lookup.lookupDescription || "")}</td>
            <td title="${escapeHtml(lookup.color || "")}">${renderColorCell(lookup.color)}</td>
            <td title="${escapeHtml(displayOrderValue(lookup.displayOrder))}">${escapeHtml(displayOrderValue(lookup.displayOrder))}</td>
            <td class="lookup-main-active-cell">${renderActiveIcon(lookup.active)}</td>
        </tr>
    `;
}

function renderLookupCodeCell(code, color) {
    const value = String(code || "");
    const normalizedColor = sanitizeCssColor(color, "");

    if (!normalizedColor) {
        return escapeHtml(value);
    }

    const colorStyle = buildColorChipStyle(normalizedColor, 0.12);

    return `
        <span class="lookup-main-code-chip" style="${escapeHtml(colorStyle)}">
            ${escapeHtml(value)}
        </span>
    `;
}

function renderColorCell(color) {
    const normalized = normalizeColor(color);

    if (!normalized) {
        return "";
    }

    return `
        <span class="lookup-main-color-cell">
            <span class="lookup-main-color-swatch" style="background-color: ${escapeHtml(normalized)};"></span>
            <span class="lookup-main-color-value">${escapeHtml(normalized)}</span>
        </span>
    `;
}

function renderActiveIcon(value) {
    return value
        ? '<span class="lookup-main-active-state" title="Active" aria-label="Active"><span class="lookup-main-active-dot is-active" aria-hidden="true"></span></span>'
        : '<span class="lookup-main-active-state" title="Inactive" aria-label="Inactive"><span class="lookup-main-active-dot is-inactive" aria-hidden="true"></span></span>';
}

function getSortedLookups(lookups) {
    return [...lookups].sort((left, right) => {
        const leftOrder = left.displayOrder === null ? Number.MAX_SAFE_INTEGER : Number(left.displayOrder);
        const rightOrder = right.displayOrder === null ? Number.MAX_SAFE_INTEGER : Number(right.displayOrder);

        if (leftOrder !== rightOrder) {
            return leftOrder - rightOrder;
        }

        return String(left.lookupCode || "").localeCompare(String(right.lookupCode || ""), "da", {
            numeric: true,
            sensitivity: "base"
        });
    });
}

function openLookupDialog(mode, lookupId = null) {
    const dialog = byId("lookupDialog");

    if (!dialog) {
        return;
    }

    if (dialog.open && state.dirty && !window.confirm("Der er ændringer, som ikke er gemt. Vil du fortsætte?")) {
        return;
    }

    state.dirty = false;
    state.currentLookup = null;
    setText("lookupDialogStatus", "Loading...", "");
    setText("lookupDialogModeLabel", mode === "edit" ? "Editing lookup row" : "Creating lookup row", "");
    dialog.showModal();

    loadLookupDialog(mode, lookupId).catch((error) => {
        console.error("Failed to open lookup dialog", error);
        setText("lookupDialogStatus", "Failed to load.", "");
        window.alert(error.message || "Failed to load lookup row.");
        closeLookupDialog("error");
    });
}

async function loadLookupDialog(mode, lookupId) {
    const url = new URL(mode === "edit" ? `${EDIT_URL}${encodeURIComponent(String(lookupId || ""))}` : `${CREATE_URL}${encodeURIComponent(String(state.selectedTypeId || ""))}`, window.location.origin);
    const xmlDocument = await fetchXml(url.toString(), {
        cache: "no-store",
        credentials: "same-origin"
    });

    if (hasXmlParseError(xmlDocument)) {
        throw new Error("Lookup detail endpoint returned invalid XML.");
    }

    const lookupNode = getDirectChild(xmlDocument.documentElement, "lookup");
    const lookup = lookupNode ? parseLookupNode(lookupNode) : buildEmptyLookup();

    state.currentLookup = lookup;
    fillLookupDialog(lookup, mode);
    setText("lookupDialogStatus", mode === "edit" ? "Editing" : "Creating", "");
}

function parseLookupNode(node) {
    return {
        lookupId: parseNullableInt(getChildText(node, "LookupId", "")),
        lookupTypeId: parseNullableInt(getChildText(node, "LookupType", "")),
        lookupCode: getChildText(node, "LookupCode", ""),
        lookupDescription: getChildText(node, "LookupDescription", ""),
        color: normalizeColor(getChildText(node, "Color", "")),
        displayOrder: parseNullableInt(getChildText(node, "DisplayOrder", "")),
        active: parseBoolean(getChildText(node, "Active", "true"))
    };
}

function buildEmptyLookup() {
    return {
        lookupId: null,
        lookupTypeId: state.selectedTypeId,
        lookupCode: "",
        lookupDescription: "",
        color: "",
        displayOrder: null,
        active: true
    };
}

function fillLookupDialog(lookup, mode) {
    setValue("lookupId", lookup.lookupId === null ? "" : String(lookup.lookupId));
    setValue("lookupTypeId", lookup.lookupTypeId === null ? "" : String(lookup.lookupTypeId));
    setValue("lookupCodeInput", lookup.lookupCode || "");
    setValue("lookupDescriptionInput", lookup.lookupDescription || "");
    setValue("lookupDisplayOrderInput", lookup.displayOrder === null ? "" : String(lookup.displayOrder));
    setChecked("lookupActiveInput", lookup.active);

    const typeLabel = byId("lookupTypeLabel");
    if (typeLabel) {
        typeLabel.value = String(lookup.lookupTypeId || state.selectedTypeId || "");
    }

    syncColorControlsFromValue(lookup.color || "");
    setText("lookupDialogTitle", mode === "edit" ? "Edit Lookup" : "Create Lookup", "");
    setText("lookupDialogModeLabel", mode === "edit" ? "Editing lookup row" : "Creating lookup row", "");
    updateLookupPreview();
    state.dirty = false;
}

function syncColorControlsFromValue(value) {
    const presetSelect = byId("lookupColorPresetSelect");
    const picker = byId("lookupColorInput");

    if (!presetSelect || !picker) {
        return;
    }

    const normalized = normalizeColor(value);

    if (!normalized) {
        presetSelect.value = "";
        picker.value = "#0000FF";
        picker.disabled = true;
        return;
    }

    const preset = STANDARD_COLORS.find((option) => option.value.toLowerCase() === normalized.toLowerCase());

    if (preset) {
        presetSelect.value = preset.value;
        picker.value = preset.value;
        picker.disabled = false;
        return;
    }

    presetSelect.value = "__custom__";
    picker.value = isHexColor(normalized) ? normalized : "#0000FF";
    picker.disabled = false;
}

function syncColorControlsFromPreset() {
    const presetSelect = byId("lookupColorPresetSelect");
    const picker = byId("lookupColorInput");

    if (!presetSelect || !picker) {
        return;
    }

    const value = presetSelect.value;

    if (!value) {
        picker.value = "#0000FF";
        picker.disabled = true;
        return;
    }

    picker.disabled = false;

    if (value === "__custom__") {
        if (!picker.value) {
            picker.value = "#0000FF";
        }
        return;
    }

    picker.value = value;
}

function syncPresetFromPicker() {
    const presetSelect = byId("lookupColorPresetSelect");
    const picker = byId("lookupColorInput");

    if (!presetSelect || !picker) {
        return;
    }

    const match = STANDARD_COLORS.find((option) => option.value.toLowerCase() === picker.value.toLowerCase());

    presetSelect.value = match ? match.value : "__custom__";
}

function updateLookupPreview() {
    const previewChip = byId("lookupPreviewChip");
    const previewMeta = byId("lookupPreviewMeta");

    if (!previewChip || !previewMeta) {
        return;
    }

    const code = getValue("lookupCodeInput").trim();
    const color = sanitizeCssColor(getDialogColorValue(), "");
    const previewText = code || "Lookup code";

    previewChip.textContent = previewText;
    previewChip.classList.toggle("is-empty", !code);

    if (color) {
        previewChip.setAttribute("style", buildColorChipStyle(color, 0.12));
        previewMeta.textContent = color;
    } else {
        previewChip.removeAttribute("style");
        previewMeta.textContent = "No color";
    }
}

async function saveLookup() {
    const lookupId = getValue("lookupId");
    const lookupTypeId = getValue("lookupTypeId") || String(state.selectedTypeId || "");
    const lookupCode = getValue("lookupCodeInput").trim();
    const lookupDescription = getValue("lookupDescriptionInput").trim();
    const displayOrder = getValue("lookupDisplayOrderInput").trim();
    const active = byId("lookupActiveInput")?.checked ? "true" : "false";
    const color = getDialogColorValue();

    if (!lookupTypeId) {
        window.alert("Lookup type is missing.");
        return;
    }

    if (!lookupCode) {
        window.alert("LookupCode is required.");
        return;
    }

    const payload = `
        <lookupSave>
            <lookup>
                <LookupId>${escapeXml(lookupId)}</LookupId>
                <LookupType>${escapeXml(lookupTypeId)}</LookupType>
                <LookupCode>${escapeXml(lookupCode)}</LookupCode>
                <LookupDescription>${escapeXml(lookupDescription)}</LookupDescription>
                <Color>${escapeXml(color)}</Color>
                <DisplayOrder>${escapeXml(displayOrder)}</DisplayOrder>
                <Active>${escapeXml(active)}</Active>
            </lookup>
        </lookupSave>
    `.trim();

    setText("lookupDialogStatus", "Saving...", "");

    try {
        await postXml(SAVE_URL, payload, {
            credentials: "same-origin"
        });

        state.dirty = false;
        closeLookupDialog("saved");
        await loadLookupData(Number(lookupTypeId));
    } catch (error) {
        console.error("Failed to save lookup row", error);
        setText("lookupDialogStatus", "Save failed.", "");
        window.alert(error.message || "Failed to save lookup row.");
    }
}

function getDialogColorValue() {
    const presetSelect = byId("lookupColorPresetSelect");
    const picker = byId("lookupColorInput");

    if (!presetSelect || !picker) {
        return "";
    }

    if (!presetSelect.value) {
        return "";
    }

    if (presetSelect.value === "__custom__") {
        return picker.value || "";
    }

    return presetSelect.value;
}

function closeLookupDialog(_reason) {
    const dialog = byId("lookupDialog");

    if (!dialog) {
        return;
    }

    if (_reason !== "saved" && state.dirty && !window.confirm("Der er ændringer, som ikke er gemt. Vil du lukke dialogen?")) {
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

function firstLookupTypeId() {
    return state.lookupTypes.length ? state.lookupTypes[0].lookupTypeId : null;
}

function loadStoredLookupTypeId() {
    const raw = localStorage.getItem(STORAGE_KEY);
    const value = Number(raw);

    return Number.isFinite(value) && value > 0 ? value : null;
}

function persistSelectedLookupTypeId(value) {
    if (value === null || value === undefined || value === "") {
        localStorage.removeItem(STORAGE_KEY);
        return;
    }

    localStorage.setItem(STORAGE_KEY, String(value));
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

function displayOrderValue(value) {
    return value === null || value === undefined ? "" : String(value);
}

function normalizeColor(value) {
    const normalized = String(value || "").trim();
    return isHexColor(normalized) || /^[a-zA-Z]+$/.test(normalized) ? normalized : "";
}

function isHexColor(value) {
    return /^#[0-9a-fA-F]{3}$/.test(String(value || "").trim()) || /^#[0-9a-fA-F]{6}$/.test(String(value || "").trim());
}
