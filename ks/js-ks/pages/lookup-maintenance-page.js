import { initMenu } from "../components/menu.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { setText } from "../core/dom.js";
import { fetchXml, postXml } from "../core/http.js";
import { escapeHtml } from "../core/html.js";
import { buildColorChipStyle, sanitizeCssColor } from "../core/css.js";
import { escapeXml, getChildText, getDirectChild, getDirectChildren, hasXmlParseError } from "../core/xml.js";

const LIST_URL = "/basis/lookup-maintenance?cmd=list";
const SAVE_URL = "/basis/lookup-maintenance?cmd=save";

const TAB_CONFIG = [
    {
        key: "TRL",
        index: 0,
        tabButtonId: "trlTabButton",
        panelId: "trlPanel",
        cardsId: "trlCards",
        label: "TRL",
        sortKey: (row) => row.level
    },
    {
        key: "IRL",
        index: 1,
        tabButtonId: "irlTabButton",
        panelId: "irlPanel",
        cardsId: "irlCards",
        label: "IRL",
        sortKey: (row) => irlCodeOrder(row.code)
    },
    {
        key: "SRL",
        index: 2,
        tabButtonId: "srlTabButton",
        panelId: "srlPanel",
        cardsId: "srlCards",
        label: "SRL",
        sortKey: (row) => row.level
    },
    {
        key: "CLASSIFICATION",
        index: 3,
        tabButtonId: "classificationTabButton",
        panelId: "classificationPanel",
        cardsId: "classificationCards",
        label: "Classification",
        sortKey: (row) => row.code
    }
];

const STANDARD_COLORS = [
    { value: "silver", label: "Silver" },
    { value: "red", label: "Red" },
    { value: "green", label: "Green" },
    { value: "blue", label: "Blue" },
    { value: "yellow", label: "Yellow" },
    { value: "orange", label: "Orange" },
    { value: "purple", label: "Purple" },
    { value: "pink", label: "Pink" },
    { value: "brown", label: "Brown" },
    { value: "navy", label: "Navy" },
    { value: "teal", label: "Teal" }
];

const state = {
    currentDoc: null,
    topPanel: {
        customerName: "-",
        projectName: "-",
        userName: "-"
    },
    rows: {
        TRL: [],
        IRL: [],
        SRL: [],
        CLASSIFICATION: []
    },
    activeTab: "TRL",
    dirty: false,
    currentRow: null
};

document.addEventListener("DOMContentLoaded", () => {
    start();
});

function start() {
    initializeShell();
    initializeTabs();
    initializeEvents();
    loadData();
}

function initializeShell() {
    setText("customerName", "-", "");
    setText("projectName", "-", "");
    setText("userName", "-", "");
    setText("loadStatus", "Loading", "");

    initMenu(document);
    mountTopbar(document);
}

function initializeTabs() {
    TAB_CONFIG.forEach((tab) => {
        const button = byId(tab.tabButtonId);
        button?.addEventListener("click", () => {
            state.activeTab = tab.key;
            persistActiveTab(tab.key);
        });
    });
}

function initializeEvents() {
    const dialog = byId("lookupDialog");
    const saveButton = byId("lookupSaveBtn");
    const cancelButton = byId("lookupCancelBtn");
    const presetSelect = byId("lookupColorPresetSelect");
    const picker = byId("lookupColorInput");

    saveButton?.addEventListener("click", saveRow);
    cancelButton?.addEventListener("click", () => closeDialog());

    dialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeDialog();
    });

    dialog?.addEventListener("close", () => {
        state.dirty = false;
        state.currentRow = null;
    });

    ["lookupNameInput", "lookupDescriptionInput", "lookupExampleInput", "lookupActiveInput"].forEach((id) => {
        const element = byId(id);
        element?.addEventListener("input", () => {
            state.dirty = true;
            updatePreview();
        });
        element?.addEventListener("change", () => {
            state.dirty = true;
            updatePreview();
        });
    });

    const codeInput = byId("lookupCodeInput");
    codeInput?.addEventListener("input", () => {
        setValue("lookupBadgeTextInput", codeInput.value || "");
        state.dirty = true;
        updatePreview();
    });
    codeInput?.addEventListener("change", () => {
        setValue("lookupBadgeTextInput", codeInput.value || "");
        state.dirty = true;
        updatePreview();
    });

    presetSelect?.addEventListener("change", () => {
        syncColorControlsFromPreset();
        state.dirty = true;
        updatePreview();
    });

    picker?.addEventListener("input", () => {
        syncPresetFromPicker();
        state.dirty = true;
        updatePreview();
    });
}

async function loadData() {
    setText("loadStatus", "Loading", "");

    try {
        const xmlDocument = await fetchXml(LIST_URL, {
            cache: "no-store",
            credentials: "same-origin"
        });

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("Lookup maintenance endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.topPanel = parseTopPanel(xmlDocument);
        state.rows = {
            TRL: parseRows(xmlDocument, "trlRows", "trlRow"),
            IRL: parseRows(xmlDocument, "irlRows", "irlRow"),
            SRL: parseRows(xmlDocument, "srlRows", "srlRow"),
            CLASSIFICATION: parseClassificationRows(xmlDocument)
        };

        state.activeTab = resolveActiveTab();

        applyTopbarMetadata(document, state.currentDoc || state.topPanel);
        renderTabs();
        renderAllCards();
        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load lookup maintenance data", error);
        setText("loadStatus", "Error", "");
        renderEmptyStates(`Could not load lookup maintenance data. ${error.message}`);
    }
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

function parseRows(xmlDocument, containerName, rowName) {
    const container = getDirectChild(xmlDocument.documentElement, containerName);
    const rowNodes = getDirectChildren(container, rowName);

    return rowNodes.map((node) => ({
        lookupType: getChildText(node, "LookupType", ""),
        lookupId: parseNullableInt(getChildText(node, "LookupId", "")),
        level: parseNullableInt(getChildText(node, "LookupLevel", "")),
        code: normalizeText(getChildText(node, "LookupCode", "")),
        name: getChildText(node, "LookupName", ""),
        description: getChildText(node, "LookupDescription", ""),
        active: parseBoolean(getChildText(node, "Active", "true")),
        color: normalizeColor(getChildText(node, "Color", ""))
    })).filter((row) => row.lookupId !== null);
}

function parseClassificationRows(xmlDocument) {
    const container = getDirectChild(xmlDocument.documentElement, "classificationRows");
    const rowNodes = getDirectChildren(container, "classificationRow");

    return rowNodes.map((node) => ({
        classificationId: parseNullableInt(getChildText(node, "ClassificationId", "")),
        classId: parseNullableInt(getChildText(node, "ClassId", "")),
        code: normalizeText(getChildText(node, "Code", "")),
        description: getChildText(node, "Description", ""),
        example: getChildText(node, "Example", ""),
        active: parseBoolean(getChildText(node, "Active", "true"))
    })).filter((row) => row.classificationId !== null);
}

function renderTabs() {
    const activeIndex = TAB_CONFIG.findIndex((tab) => tab.key === state.activeTab);

    TAB_CONFIG.forEach((tab, index) => {
        const button = byId(tab.tabButtonId);
        const panel = byId(tab.panelId);

        button?.classList.toggle("is-active", index === activeIndex);
        button?.setAttribute("aria-selected", index === activeIndex ? "true" : "false");

        if (panel) {
            panel.hidden = index !== activeIndex;
            panel.classList.toggle("is-active", index === activeIndex);
        }
    });
}

function renderAllCards() {
    TAB_CONFIG.forEach((tab) => {
        const rows = getSortedRows(state.rows[tab.key] || [], tab.sortKey);
        renderCards(tab, rows);
    });
}

function renderCards(tab, rows) {
    const container = byId(tab.cardsId);

    if (!container) {
        return;
    }

    if (!rows.length) {
        container.innerHTML = `<div class="lookup-maintenance-empty">No ${escapeHtml(tab.label)} rows found.</div>`;
        return;
    }

    container.innerHTML = rows.map((row) => renderCard(tab, row)).join("");

    container.querySelectorAll("[data-row-id]").forEach((element) => {
        const rowId = element.getAttribute("data-row-id");
        const rowType = element.getAttribute("data-row-type");

        element.addEventListener("dblclick", () => openDialog(rowType, rowId));
        element.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openDialog(rowType, rowId);
            }
        });
    });
}

function renderEmptyStates(message) {
    TAB_CONFIG.forEach((tab) => {
        const container = byId(tab.cardsId);
        if (container) {
            container.innerHTML = `<div class="lookup-maintenance-empty">${escapeHtml(message)}</div>`;
        }
    });
}

function renderCard(tab, row) {
    if (tab.key === "CLASSIFICATION") {
        return renderClassificationCard(row);
    }

    const badgeText = getBadgeText(tab.key, row);
    const badgeStyle = row.color ? buildColorChipStyle(row.color, 0.14) : "";
    const badgeClass = row.color ? "lookup-maintenance-card-level has-color" : "lookup-maintenance-card-level";
    const inactiveClass = row.active ? "" : " is-inactive";
    const title = `${tab.label} ${badgeText}`;

    return `
        <article
            class="lookup-maintenance-card${inactiveClass}"
            data-row-id="${escapeHtml(String(row.lookupId ?? ""))}"
            data-row-type="${escapeHtml(tab.key)}"
            tabindex="0"
            role="button"
            aria-label="${escapeHtml(title)}"
            title="${escapeHtml(title)}"
        >
            <div class="${badgeClass}" style="${escapeHtml(badgeStyle)}">
                ${escapeHtml(badgeText)}
            </div>
            <div class="lookup-maintenance-card-body">
                <div class="lookup-maintenance-card-fields">
                    <div class="lookup-maintenance-field-box">
                        <span class="lookup-maintenance-field-label">Name</span>
                        <strong class="lookup-maintenance-field-value">${escapeHtml(row.name || "")}</strong>
                    </div>

                    <div class="lookup-maintenance-field-box">
                        <span class="lookup-maintenance-field-label">Description</span>
                        <span class="lookup-maintenance-field-value lookup-maintenance-field-description">${escapeHtml(row.description || "")}</span>
                    </div>

                    <div class="lookup-maintenance-field-box lookup-maintenance-field-box-active">
                        <span class="lookup-maintenance-card-active">
                            <span class="lookup-maintenance-card-dot${row.active ? "" : " is-inactive"}" aria-hidden="true"></span>
                            <span class="lookup-maintenance-field-value">${row.active ? "Active" : "Inactive"}</span>
                        </span>
                    </div>
                </div>
            </div>
        </article>
    `;
}

function getBadgeText(type, row) {
    if (type === "IRL") {
        return row.code || "-";
    }

    return row.level === null || row.level === undefined ? "-" : String(row.level);
}

function renderClassificationCard(row) {
    const badgeText = row.code || "-";
    const inactiveClass = row.active ? "" : " is-inactive";

    return `
        <article
            class="lookup-maintenance-card${inactiveClass}"
            data-row-id="${escapeHtml(String(row.classificationId ?? ""))}"
            data-row-type="CLASSIFICATION"
            tabindex="0"
            role="button"
            aria-label="${escapeHtml(`Classification ${badgeText}`)}"
            title="${escapeHtml(`Classification ${badgeText}`)}"
        >
            <div class="lookup-maintenance-card-level">
                ${escapeHtml(badgeText)}
            </div>
            <div class="lookup-maintenance-card-body">
                <div class="lookup-maintenance-card-fields">
                    <div class="lookup-maintenance-field-box">
                        <span class="lookup-maintenance-field-label">Description</span>
                        <strong class="lookup-maintenance-field-value">${escapeHtml(row.description || "")}</strong>
                    </div>

                    <div class="lookup-maintenance-field-box">
                        <span class="lookup-maintenance-field-label">Example</span>
                        <span class="lookup-maintenance-field-value lookup-maintenance-field-description">${escapeHtml(row.example || "")}</span>
                    </div>

                    <div class="lookup-maintenance-field-box lookup-maintenance-field-box-active">
                        <span class="lookup-maintenance-card-active">
                            <span class="lookup-maintenance-card-dot${row.active ? "" : " is-inactive"}" aria-hidden="true"></span>
                            <span class="lookup-maintenance-field-value">${row.active ? "Active" : "Inactive"}</span>
                        </span>
                    </div>
                </div>
            </div>
        </article>
    `;
}

function openDialog(type, rowId) {
    const row = findRow(type, rowId);
    const dialog = byId("lookupDialog");

    if (!row || !dialog) {
        return;
    }

    if (dialog.open && state.dirty && !window.confirm("There are unsaved changes. Continue anyway?")) {
        return;
    }

    state.currentRow = row;
    state.dirty = false;
    fillDialog(row, type);
    dialog.showModal();
}

function fillDialog(row, type) {
    setValue("lookupTypeInput", type);
    setText("lookupDialogTitle", type, "");
    setValue("lookupIdInput", getLookupRowId(row, type));

    const isClassification = type === "CLASSIFICATION";
    toggleDialogMode(isClassification);

    if (isClassification) {
        setValue("lookupBadgeTextInput", row.code || "");
        setValue("lookupCodeInput", row.code || "");
        setValue("lookupDescriptionInput", row.description || "");
        setValue("lookupExampleInput", row.example || "");
        setChecked("lookupActiveInput", row.active);
        setValue("lookupColorValueInput", "");
        updatePreview();
        state.dirty = false;
        return;
    }

    setValue("lookupBadgeTextInput", getBadgeText(type, row));
    setValue("lookupColorValueInput", row.color || "");
    setValue("lookupNameInput", row.name || "");
    setValue("lookupDescriptionInput", row.description || "");
    setChecked("lookupActiveInput", row.active);
    syncColorControlsFromValue(row.color || "");
    updatePreview();
    state.dirty = false;
}

async function saveRow() {
    const type = getValue("lookupTypeInput");
    const rowId = parseNullableInt(getValue("lookupIdInput"));
    const current = state.currentRow;

    if (!type || rowId === null || !current) {
        window.alert("No lookup row is selected.");
        return;
    }

    const payload = type === "CLASSIFICATION"
        ? buildClassificationSavePayload({
            lookupType: type,
            classificationId: rowId,
            classId: current.classId,
            code: getValue("lookupCodeInput"),
            description: getValue("lookupDescriptionInput"),
            example: getValue("lookupExampleInput"),
            active: getChecked("lookupActiveInput")
        })
        : buildLookupSavePayload({
            lookupType: type,
            lookupId: rowId,
            lookupLevel: current.level,
            lookupCode: current.code,
            lookupName: getValue("lookupNameInput"),
            lookupDescription: getValue("lookupDescriptionInput"),
            active: getChecked("lookupActiveInput"),
            color: normalizeColor(getCurrentColorValue())
        });

    try {
        await postXml(SAVE_URL, payload, {
            cache: "no-store",
            credentials: "same-origin"
        });

        closeDialog();
        await loadData();
    } catch (error) {
        console.error("Failed to save lookup row", error);
        window.alert(error.message || "Failed to save lookup row.");
    }
}

function buildLookupSavePayload(row) {
    return `
<lookupRow>
  <LookupType>${escapeXml(row.lookupType)}</LookupType>
  <LookupId>${escapeXml(String(row.lookupId ?? ""))}</LookupId>
  <LookupLevel>${escapeXml(String(row.lookupLevel ?? ""))}</LookupLevel>
  <LookupCode>${escapeXml(row.lookupCode ?? "")}</LookupCode>
  <LookupName>${escapeXml(row.lookupName ?? "")}</LookupName>
  <LookupDescription>${escapeXml(row.lookupDescription ?? "")}</LookupDescription>
  <Active>${row.active ? "true" : "false"}</Active>
  <Color>${escapeXml(row.color ?? "")}</Color>
</lookupRow>`.trim();
}

function buildClassificationSavePayload(row) {
    return `
<lookupRow>
  <LookupType>${escapeXml(row.lookupType)}</LookupType>
  <LookupId>${escapeXml(String(row.classificationId ?? ""))}</LookupId>
  <ClassId>${escapeXml(String(row.classId ?? ""))}</ClassId>
  <LookupCode>${escapeXml(row.code ?? "")}</LookupCode>
  <LookupDescription>${escapeXml(row.description ?? "")}</LookupDescription>
  <LookupExample>${escapeXml(row.example ?? "")}</LookupExample>
  <Active>${row.active ? "true" : "false"}</Active>
</lookupRow>`.trim();
}

function closeDialog() {
    const dialog = byId("lookupDialog");

    if (dialog?.open) {
        dialog.close();
    }

    state.dirty = false;
    state.currentRow = null;
}

function updatePreview() {
    const badge = getValue("lookupBadgeTextInput");
    const color = getCurrentColorValue();
    const chip = byId("lookupPreviewChip");

    if (!chip) {
        return;
    }

    const normalized = normalizeColor(color);
    const chipStyle = normalized ? buildColorChipStyle(normalized, 0.16) : "";

    chip.classList.toggle("is-empty", !normalized);
    chip.classList.toggle("lookup-maintenance-preview-chip-dialog", true);
    chip.style.cssText = chipStyle;
    chip.textContent = badge || "Value";
}

function getLookupRowId(row, type) {
    if (type === "CLASSIFICATION") {
        return row.classificationId === null || row.classificationId === undefined ? "" : String(row.classificationId);
    }

    return row.lookupId === null || row.lookupId === undefined ? "" : String(row.lookupId);
}

function syncColorControlsFromValue(value) {
    const presetSelect = byId("lookupColorPresetSelect");
    const picker = byId("lookupColorInput");
    const hiddenValue = byId("lookupColorValueInput");
    const normalized = normalizeColor(value);

    if (!presetSelect || !picker || !hiddenValue) {
        return;
    }

    if (!normalized) {
        presetSelect.value = "";
        picker.value = "#0000FF";
        picker.disabled = true;
        hiddenValue.value = "";
        return;
    }

    const preset = STANDARD_COLORS.find((option) => option.value.toLowerCase() === normalized.toLowerCase());

    if (preset) {
        presetSelect.value = preset.value;
        picker.value = namedColorToHex(preset.value);
        picker.disabled = false;
        hiddenValue.value = normalized;
        return;
    }

    presetSelect.value = "__custom__";
    picker.value = isHexColor(normalized) ? normalized : "#0000FF";
    picker.disabled = false;
    hiddenValue.value = normalized;
}

function syncColorControlsFromPreset() {
    const presetSelect = byId("lookupColorPresetSelect");
    const picker = byId("lookupColorInput");
    const hiddenValue = byId("lookupColorValueInput");

    if (!presetSelect || !picker || !hiddenValue) {
        return;
    }

    const value = presetSelect.value;

    if (!value) {
        picker.value = "#0000FF";
        picker.disabled = true;
        hiddenValue.value = "";
        return;
    }

    picker.disabled = false;

    if (value === "__custom__") {
        if (!picker.value) {
            picker.value = "#0000FF";
        }
        hiddenValue.value = picker.value || hiddenValue.value || "";
        return;
    }

    picker.value = namedColorToHex(value);
    hiddenValue.value = value;
}

function syncPresetFromPicker() {
    const presetSelect = byId("lookupColorPresetSelect");
    const picker = byId("lookupColorInput");
    const hiddenValue = byId("lookupColorValueInput");

    if (!presetSelect || !picker || !hiddenValue) {
        return;
    }

    const value = normalizeColor(picker.value);
    const preset = STANDARD_COLORS.find((option) => option.value.toLowerCase() === value.toLowerCase());

    presetSelect.value = preset ? preset.value : "__custom__";
    hiddenValue.value = value;
}

function findRow(type, rowId) {
    const rows = state.rows[type] || [];
    const normalizedId = parseNullableInt(String(rowId ?? ""));
    return rows.find((row) => getRowId(row, type) === normalizedId) || null;
}

function renderCardsForActiveTab() {
    renderTabs();
    renderAllCards();
}

function resolveActiveTab() {
    const stored = loadStoredActiveTab();
    return TAB_CONFIG.some((tab) => tab.key === stored) ? stored : "TRL";
}

function persistActiveTab(tab) {
    try {
        window.sessionStorage.setItem("lookup.maintenance.activeTab", tab);
    } catch (_error) {
        // Ignore storage failures.
    }

    renderCardsForActiveTab();
}

function loadStoredActiveTab() {
    try {
        return window.sessionStorage.getItem("lookup.maintenance.activeTab") || "TRL";
    } catch (_error) {
        return "TRL";
    }
}

function parseNullableInt(value) {
    const text = String(value ?? "").trim();
    if (!text) {
        return null;
    }

    const parsed = Number.parseInt(text, 10);
    return Number.isFinite(parsed) ? parsed : null;
}

function parseBoolean(value) {
    return ["true", "1", "yes", "ja", "y", "on"].includes(String(value ?? "").trim().toLowerCase());
}

function normalizeText(value) {
    return String(value ?? "").trim();
}

function normalizeColor(value) {
    return sanitizeCssColor(value, "");
}

function isHexColor(value) {
    return /^#[0-9a-fA-F]{6}$/.test(String(value || "").trim()) || /^#[0-9a-fA-F]{3}$/.test(String(value || "").trim());
}

function getSortedRows(rows, sortKey) {
    return [...rows].sort((left, right) => {
        const leftOrder = sortKey(left);
        const rightOrder = sortKey(right);

        if (leftOrder !== rightOrder) {
            return leftOrder - rightOrder;
        }

        return String(left.name || "").localeCompare(String(right.name || ""), "da", {
            numeric: true,
            sensitivity: "base"
        });
    });
}

function getRowId(row, type) {
    if (type === "CLASSIFICATION") {
        return row.classificationId;
    }

    return row.lookupId;
}

function toggleDialogMode(isClassification) {
    const codeField = byId("lookupCodeField");
    const nameField = byId("lookupNameField");
    const exampleField = byId("lookupExampleField");
    const colorField = byId("lookupColorField");
    const codeInput = byId("lookupCodeInput");
    const nameInput = byId("lookupNameInput");
    const descriptionInput = byId("lookupDescriptionInput");
    const exampleInput = byId("lookupExampleInput");
    const codeLabel = byId("lookupCodeLabel");
    const previewLabel = byId("lookupPreviewLabel");

    if (codeField) {
        codeField.hidden = !isClassification;
    }

    if (nameField) {
        nameField.hidden = isClassification;
    }

    if (exampleField) {
        exampleField.hidden = !isClassification;
    }

    if (colorField) {
        colorField.hidden = isClassification;
    }

    if (codeLabel) {
        codeLabel.textContent = isClassification ? "Code" : "Code / level";
    }

    if (previewLabel) {
        previewLabel.textContent = isClassification ? "Code" : "Code / level";
    }

    if (codeInput) {
        codeInput.required = isClassification;
    }

    if (nameInput) {
        nameInput.required = !isClassification;
    }

    if (descriptionInput) {
        descriptionInput.required = true;
        descriptionInput.maxLength = isClassification ? 255 : 4000;
    }

    if (exampleInput) {
        exampleInput.required = false;
    }
}

function irlCodeOrder(code) {
    const normalized = String(code || "").trim();

    switch (normalized) {
        case "-": return 0;
        case "0": return 1;
        case "1": return 2;
        case "2": return 3;
        case "3": return 4;
        case "4": return 5;
        case "5": return 6;
        case "6": return 7;
        case "7": return 8;
        case "8": return 9;
        case "9": return 10;
        default: return 999;
    }
}

function getValue(id) {
    return byId(id)?.value?.trim() || "";
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
        element.checked = !!value;
    }
}

function getChecked(id) {
    return !!byId(id)?.checked;
}

function getCurrentColorValue() {
    return byId("lookupColorValueInput")?.value?.trim() || "";
}

function namedColorToHex(value) {
    switch (String(value || "").trim().toLowerCase()) {
        case "silver": return "#C0C0C0";
        case "red": return "#FF0000";
        case "green": return "#008000";
        case "blue": return "#0000FF";
        case "yellow": return "#FFFF00";
        case "orange": return "#FFA500";
        case "purple": return "#800080";
        case "pink": return "#FFC0CB";
        case "brown": return "#A52A2A";
        case "navy": return "#000080";
        case "teal": return "#008080";
        default: return value || "#0000FF";
    }
}

function byId(id) {
    return document.getElementById(id);
}
