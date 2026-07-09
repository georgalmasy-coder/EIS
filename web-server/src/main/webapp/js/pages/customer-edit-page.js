import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { initTabs } from "../components/tabs.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import {
    applyEditDialogShellMode,
    closeEditDialog,
    getEditDialogPageContext
} from "../components/edit-dialog-page.js";
import { setText } from "../core/dom.js";
import {
    getDirectChild,
    getDirectText,
    hasXmlParseError
} from "../core/xml.js";
import {
    fieldEditable,
    fieldRequired,
    fieldVisible
} from "../core/field-display.js";
import { escapeHtml } from "../core/html.js";
import { isTruthy } from "../core/utils.js";

const DEFAULT_RETURN_URL = "/web/view?page=customer-admin";

const MODES = {
    edit: "edit",
    editVersion: "edit-version"
};

const state = {
    mode: MODES.edit,
    id: "",
    version: "",
    returnUrl: DEFAULT_RETURN_URL,
    modal: false,
    readOnly: false,
    currentDoc: null,
    detailNode: null,
    lookups: {},
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    }
};

document.addEventListener("DOMContentLoaded", () => {
    start();
});

function start() {
    initializeShell();
    initializeTabs();
    initializeRouteState();
    initializeEvents();
    applyModeUi();
    loadDetail();
}

function initializeShell() {
    const dialogContext = applyEditDialogShellMode(document);
    state.modal = dialogContext.modal;

    setText("customerName", "—");
    setText("projectName", "—");
    setText("userName", "—");
    setText("loadStatus", "Loading");

    if (!state.modal) {
        initMenu(document);
    }

    initHelpDialog();
}

function initializeTabs() {
    initTabs([
        { btnId: "tabBtn1", panelId: "tabPanel1" },
        { btnId: "tabBtn2", panelId: "tabPanel2" },
        { btnId: "tabBtn3", panelId: "tabPanel3" },
        { btnId: "tabBtn4", panelId: "tabPanel4" },
        { btnId: "tabBtn5", panelId: "tabPanel5" }
    ]);
}

function initializeRouteState() {
    const params = new URLSearchParams(window.location.search);
    const requestedMode = params.get("mode") || MODES.edit;
    const dialogContext = getEditDialogPageContext();

    state.mode = Object.values(MODES).includes(requestedMode) ? requestedMode : MODES.edit;
    state.id = params.get("id") || "";
    state.version = params.get("version") || "";
    state.returnUrl = params.get("returnUrl") || DEFAULT_RETURN_URL;
    state.readOnly = state.mode === MODES.editVersion && !!state.version;
    state.modal = state.modal || dialogContext.modal;
}

function initializeEvents() {
    const cancelButton = document.getElementById("btnCancel");

    if (cancelButton) {
        if (state.modal) {
            cancelButton.hidden = false;
            cancelButton.disabled = false;
            cancelButton.removeAttribute("aria-hidden");
            cancelButton.addEventListener("click", () => {
                returnToPreviousPage();
            });
        } else {
            cancelButton.hidden = true;
            cancelButton.disabled = true;
            cancelButton.setAttribute("aria-hidden", "true");
        }
    }

    document.getElementById("btnSave")?.addEventListener("click", async () => {
        await saveCurrentCustomer();
    });

    document.addEventListener("change", (event) => {
        const target = event.target;

        if (!(target instanceof HTMLElement)) {
            return;
        }

        if (target.id === "fieldPhoneCountryCode") {
            formatCurrentPhoneValue();
            updatePhoneHelp();
        }
    });

    document.addEventListener("input", (event) => {
        const target = event.target;

        if (!(target instanceof HTMLElement)) {
            return;
        }

        if (target.id === "fieldPhone") {
            formatCurrentPhoneValue();
        }
    });
}

async function loadDetail() {
    const detailUrl = buildDetailUrl();

    if (!detailUrl) {
        setText("loadStatus", "Error");
        setText("dlgStatus", "Could not determine detail URL.");
        return;
    }

    setText("loadStatus", "Loading");
    setText("dlgStatus", "Loading customer details...");

    try {
        const response = await fetch(detailUrl, {
            method: "GET",
            headers: {
                "Accept": "application/xml,text/xml,*/*"
            },
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        const xmlText = await response.text();
        const xmlDocument = new DOMParser().parseFromString(xmlText, "application/xml");

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("The customer endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.lookups = parseLookups(xmlDocument);
        state.detailNode = findDetailNode(xmlDocument);
        state.topPanel = parseTopPanel(xmlDocument);

        applyTopPanel();
        renderAllFromDoc(xmlDocument);
        applyCustomerPhoneToForm();
        applyModeUi();

        setText("loadStatus", "Loaded");
        setText("dlgStatus", "Loaded.");
    } catch (error) {
        console.error("Failed to load customer detail", error);
        setText("loadStatus", "Error");
        setText("dlgStatus", `Could not load customer detail. ${error.message}`);
    }
}

function buildDetailUrl() {
    if (state.mode === MODES.edit) {
        return "/customer?cmd=edit";
    }

    if (state.mode === MODES.editVersion) {
        if (!state.version) return "";
        return `/customer?cmd=edit&version=${encodeURIComponent(state.version)}`;
    }

    return "";
}

function applyModeUi() {
    const saveButton = document.getElementById("btnSave");
    const readOnlyBanner = document.getElementById("readOnlyBanner");

    document.body.classList.toggle("customer-edit-readonly", state.readOnly);

    if (saveButton) {
        saveButton.hidden = state.modal;
        saveButton.disabled = state.readOnly;
        saveButton.title = state.readOnly ? "Save is disabled for historical versions." : "";
    }

    if (readOnlyBanner) {
        readOnlyBanner.hidden = !state.readOnly;
    }

    setText("pageModeLabel", getModeLabel());
    setText("entityMeta", getEntityMetaLabel());

    if (state.readOnly) {
        setFormFieldsReadOnly();
    }
}

function getModeLabel() {
    if (state.mode === MODES.editVersion) {
        return state.version ? `Historical version ${state.version}` : "Historical version";
    }

    return "Edit Customer";
}

function getEntityMetaLabel() {
    if (state.mode === MODES.editVersion) {
        return `Version: ${state.version || "—"}`;
    }

    return "Current customer";
}

function setFormFieldsReadOnly() {
    document.querySelectorAll(".customer-edit-panels input, .customer-edit-panels textarea").forEach((field) => {
        if (field.type === "checkbox") {
            field.disabled = true;
        } else {
            field.readOnly = true;
        }
    });

    document.querySelectorAll(".customer-edit-panels select").forEach((field) => {
        field.disabled = true;
    });
}

function parseTopPanel(xmlDocument) {
    const topPanelElement = xmlDocument.querySelector("TopPanel");

    if (!topPanelElement) {
        return {
            customerName: "—",
            projectName: "—",
            userName: "—"
        };
    }

    return {
        customerNameLabel: getFieldLabel(topPanelElement, "CustomerName", "Customer Name"),
        customerName: getChildText(topPanelElement, "CustomerName", "—"),
        projectNameLabel: getFieldLabel(topPanelElement, "ProjectName", "Project Name"),
        projectName: getChildText(topPanelElement, "ProjectName", "—"),
        userNameLabel: getFieldLabel(topPanelElement, "Name", "User Name"),
        userName: getChildText(topPanelElement, "Name", "—")
    };
}

function applyTopPanel() {
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
}

function renderAllFromDoc(doc) {
    renderSectionFromDoc(doc, "customerBasis", "basisInfoFields");
    renderSectionFromDoc(doc, "customerSecurity", "securityFields");
    renderSectionFromDoc(doc, "customerSubscription", "subscriptionFields");
    renderSectionFromDoc(doc, "availablePlans", "availablePlansFields");
    renderSectionFromDoc(doc, "customerPaymentMethod", "paymentMethodFields");
    renderSectionFromDoc(doc, "upcomingPayments", "upcomingPaymentsFields");
    renderSectionFromDoc(doc, "completedPayments", "completedPaymentsFields");
}

function renderSectionFromDoc(doc, sectionName, containerId) {
    const container = document.getElementById(containerId);

    if (!container) {
        return;
    }

    const detailNode = findSectionNode(doc, sectionName);

    if (!detailNode) {
        container.innerHTML = '<div class="page-empty">No detail XML returned.</div>';
        return;
    }

    const children = Array.from(detailNode.children || []);

    if (!children.length) {
        container.innerHTML = '<div class="page-empty">No fields defined yet.</div>';
        return;
    }

    container.innerHTML = children
        .map(renderFieldMarkup)
        .join("");
}

function renderFieldMarkup(field) {
    const name = field.tagName;
    const label = field.getAttribute("header") || field.getAttribute("label") || name;
    const control = (field.getAttribute("control") || "").toLowerCase();
    const editable = fieldEditable(field);
    const visible = fieldVisible(field);
    const required = fieldRequired(field);
    const value = getFieldUiValue(field);

    if (!visible) {
        return "";
    }

    const forceReadOnly = state.readOnly;
    const readonlyAttr = editable && !forceReadOnly ? "" : "readonly";
    const disabledAttr = editable && !forceReadOnly ? "" : "disabled";
    const requiredStar = required ? '<span class="field-required" aria-hidden="true">*</span>' : "";
    const requiredAttr = required ? "required" : "";
    const escapedName = escapeHtml(name);
    const escapedLabel = escapeHtml(label);

    if (control === "hidden") {
        return `<input type="hidden" data-field="${escapedName}" id="fld-${escapedName}" value="${escapeHtml(value)}">`;
    }

    if (control === "checkbox") {
        const checked = isTruthy(value) ? "checked" : "";

        return `
            <div class="page-field checkbox-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="checkbox" ${checked} ${disabledAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "datetime") {
        const readonlyAttr = editable ? "" : "readonly";
        const normalized = normalizeDateTimeForInput(value);

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="datetime-local" step="1" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "phone") {
        return renderPhoneField(field, value, escapedName, escapedLabel, requiredStar, readonlyAttr, disabledAttr, requiredAttr);
    }

    if (control === "email") {
        const maxlengthAttr = field.getAttribute("maxlength") || "50";

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="email" inputmode="email" autocomplete="email" value="${escapeHtml(value)}" ${readonlyAttr} maxlength="${escapeHtml(maxlengthAttr)}" ${requiredAttr} />
            </div>
        `;
    }

    if (control === "date") {
        const normalized = normalizeDateForInput(value);

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="date" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "textarea" || name === "Description" || name === "FailureReason") {
        return `
            <div class="page-field description-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <textarea id="fld-${escapedName}" data-field="${escapedName}" ${readonlyAttr} ${requiredAttr}>${escapeHtml(value)}</textarea>
            </div>
        `;
    }

    if (control === "select") {
        const selectedValue = (field.getElementsByTagName("Value")?.[0]?.textContent || "").trim();
        const options = Array.from(field.getElementsByTagName("Option"));

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <select id="fld-${escapedName}" data-field="${escapedName}" ${disabledAttr} ${requiredAttr}>
                    ${options.map((option) => {
            const optionValue = option.getAttribute("value") || "";
            const optionLabel = (option.textContent || "").trim();
            const selected = optionValue === selectedValue ? "selected" : "";

            return `<option value="${escapeHtml(optionValue)}" ${selected}>${escapeHtml(optionLabel)}</option>`;
        }).join("")}
                </select>
            </div>
        `;
    }

    return `
        <div class="page-field">
            <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
            <input id="fld-${escapedName}" data-field="${escapedName}" type="text" value="${escapeHtml(value)}" ${readonlyAttr} ${requiredAttr} />
        </div>
    `;
}

function getFieldUiValue(field) {
    const control = (field.getAttribute("control") || "").toLowerCase();

    if (control === "select") {
        return (field.getElementsByTagName("Value")?.[0]?.textContent || "").trim();
    }

    const valueNode = getDirectChild(field, "Value");

    if (valueNode) {
        return valueNode.textContent || "";
    }

    return getDirectText(field);
}

function parseLookups(doc) {
    const lookups = {};

    if (!doc) {
        return lookups;
    }

    doc.querySelectorAll("lookups > lookup").forEach((lookupNode) => {
        const name = String(lookupNode.getAttribute("name") || "").trim();

        if (!name) {
            return;
        }

        lookups[name] = Array.from(lookupNode.querySelectorAll(":scope > option")).map((optionNode) => {
            return {
                country: String(optionNode.getAttribute("country") || optionNode.getAttribute("label") || "").trim(),
                code: String(optionNode.getAttribute("code") || "").trim(),
                label: String(optionNode.getAttribute("label") || optionNode.getAttribute("country") || optionNode.getAttribute("code") || "").trim(),
                min: Number.parseInt(optionNode.getAttribute("min") || "", 10),
                max: Number.parseInt(optionNode.getAttribute("max") || "", 10),
                example: String(optionNode.getAttribute("example") || "").trim()
            };
        }).filter((option) => option.code);
    });

    return lookups;
}

function renderPhoneField(field, value, escapedName, escapedLabel, requiredStar, readonlyAttr, disabledAttr, requiredAttr) {
    const rules = getPhoneCountryRules();
    const currentRule = getPhoneRuleForValue(value, rules);
    const displayValue = formatPhoneNumberForDisplay(value, currentRule);
    const isReadOnly = readonlyAttr === "readonly" || disabledAttr === "disabled";

    if (!rules.length) {
        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="tel" inputmode="tel" autocomplete="tel" value="${escapeHtml(displayValue)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    return `
        <div class="page-field customer-phone-field">
            <div class="customer-phone-controls">
                <div class="page-field customer-phone-country">
                    <label for="fieldPhoneCountryCode">${escapedLabel}${requiredStar}</label>
                    <select id="fieldPhoneCountryCode" aria-label="Country code" title="Country code" ${isReadOnly ? "disabled" : ""}>
                        ${rules.map((rule) => {
            const selected = rule.code === currentRule?.code && rule.country === currentRule?.country ? "selected" : "";
            const text = `${rule.country} (${rule.code})`;
            return `<option value="${escapeHtml(rule.code)}" data-country="${escapeHtml(rule.country)}" data-min="${escapeHtml(rule.min)}" data-max="${escapeHtml(rule.max)}" data-example="${escapeHtml(rule.example)}" ${selected}>${escapeHtml(text)}</option>`;
        }).join("")}
                    </select>
                </div>

                <div class="page-field customer-phone-input">
                    <label class="visually-hidden" for="fieldPhone">${escapedLabel}</label>
                    <input id="fieldPhone" data-field="${escapedName}" data-phone-raw="${escapeHtml(value)}" type="tel" inputmode="tel" autocomplete="tel" value="${escapeHtml(displayValue)}" ${isReadOnly ? "readonly" : ""} ${requiredAttr} />
                </div>
            </div>
            <p class="field-help" id="fieldPhoneHelp">Select a country code and enter the local phone number.</p>
        </div>
    `;
}

function getPhoneCountryRules() {
    const countryCodeLookup = state.lookups?.countryCode;

    if (!Array.isArray(countryCodeLookup) || !countryCodeLookup.length) {
        return [];
    }

    return countryCodeLookup;
}

function selectedPhoneRule() {
    const select = document.getElementById("fieldPhoneCountryCode");

    if (!select) {
        return getPhoneCountryRules()[0] || null;
    }

    const selectedOption = select.options[select.selectedIndex];

    if (!selectedOption) {
        return getPhoneCountryRules()[0] || null;
    }

    const code = String(selectedOption.value || "").trim();
    const country = String(selectedOption.getAttribute("data-country") || "").trim();
    const rules = getPhoneCountryRules();

    return rules.find((rule) => rule.code === code && rule.country === country)
        || rules.find((rule) => rule.code === code)
        || rules[0]
        || null;
}

function getPhoneRuleForValue(phone, rules = getPhoneCountryRules()) {
    const normalizedPhone = normalizePhoneNumber(phone);

    if (!normalizedPhone || !rules.length) {
        return rules[0] || null;
    }

    const prefixMatches = rules.filter((rule) => normalizedPhone.startsWith(rule.code));

    return prefixMatches[0] || rules[0] || null;
}

function applyCustomerPhoneToForm() {
    const select = document.getElementById("fieldPhoneCountryCode");
    const input = document.getElementById("fieldPhone");

    if (!select || !input) {
        return;
    }

    const rawValue = input.getAttribute("data-phone-raw") || input.value;
    const currentRule = getPhoneRuleForValue(rawValue);
    fillPhoneCountryCodes(currentRule);
    input.value = formatPhoneNumberForDisplay(rawValue, currentRule);
    updatePhoneConstraints();
    updatePhoneHelp();
}

function fillPhoneCountryCodes(selectedRule) {
    const select = document.getElementById("fieldPhoneCountryCode");

    if (!select) {
        return;
    }

    const rules = getPhoneCountryRules();
    const currentRule = selectedRule || selectedPhoneRule() || rules[0] || null;

    select.innerHTML = rules
        .slice()
        .sort((left, right) => String(left.country || "").localeCompare(String(right.country || "")))
        .map((rule) => {
            const selected = currentRule && rule.code === currentRule.code && rule.country === currentRule.country ? " selected" : "";
            return `<option value="${escapeHtml(rule.code)}" data-country="${escapeHtml(rule.country)}" data-min="${escapeHtml(rule.min)}" data-max="${escapeHtml(rule.max)}" data-example="${escapeHtml(rule.example)}"${selected}>${escapeHtml(`${rule.country} (${rule.code})`)}</option>`;
        }).join("");
}

function updatePhoneConstraints() {
    const input = document.getElementById("fieldPhone");

    if (!input) {
        return;
    }

    const rule = selectedPhoneRule();
    if (!rule) {
        return;
    }

    input.setAttribute("inputmode", "tel");
    input.setAttribute("autocomplete", "tel");
    input.setAttribute("type", "tel");
    input.setAttribute("placeholder", rule.example || "");
    input.setAttribute("pattern", phonePatternForRule(rule));
    input.setAttribute("title", phoneTitleForRule(rule));
    input.maxLength = Math.max((rule.max || 15) + 6, 15);
}

function updatePhoneHelp() {
    const help = document.getElementById("fieldPhoneHelp");

    if (!help) {
        return;
    }

    const rule = selectedPhoneRule();

    if (!rule) {
        help.textContent = "Select a country code and enter the local phone number.";
        return;
    }

    if (rule.min === rule.max) {
        help.textContent = `${rule.country}: exactly ${rule.min} digits. Example: ${rule.example}`;
    } else {
        help.textContent = `${rule.country}: ${rule.min}-${rule.max} digits. Example: ${rule.example}`;
    }
}

function formatCurrentPhoneValue() {
    const input = document.getElementById("fieldPhone");

    if (!input) {
        return;
    }

    const rule = selectedPhoneRule();

    if (!rule) {
        return;
    }

    input.value = formatPhoneDigits(extractLocalPhoneDigits(input.value, rule), rule);
    updatePhoneConstraints();
    updatePhoneHelp();
}

function formatPhoneNumberForDisplay(value, rule) {
    const localDigits = extractLocalPhoneDigits(value, rule);
    return formatPhoneDigits(localDigits, rule);
}

function extractLocalPhoneDigits(value, rule) {
    const normalized = normalizePhoneNumber(value);

    if (!normalized) {
        return "";
    }

    if (rule?.code && normalized.startsWith(rule.code)) {
        return onlyDigits(normalized.slice(rule.code.length));
    }

    return onlyDigits(normalized);
}

function formatPhoneDigits(digits, rule) {
    const safeDigits = onlyDigits(digits);

    if (!safeDigits) {
        return "";
    }

    if (rule?.code === "+45" && safeDigits.length <= 8) {
        return safeDigits.replace(/(\d{2})(?=\d)/g, "$1 ").trim();
    }

    return safeDigits;
}

function phonePatternForRule(_rule) {
    return "^\\d[\\d\\s().-]{3,}$";
}

function phoneTitleForRule(rule) {
    if (!rule || !rule.country) {
        return "Phone number";
    }

    return `Phone number for ${rule.country}`;
}

function normalizePhoneNumber(value) {
    return String(value == null ? "" : value)
        .trim()
        .replace(/[()\s.-]/g, "");
}

function onlyDigits(value) {
    return String(value == null ? "" : value).replace(/\D/g, "");
}

function normalizeDateForInput(value) {
    const text = String(value || "").trim();

    if (!text) {
        return "";
    }

    if (/^\d{4}-\d{2}-\d{2}/.test(text)) {
        return text.substring(0, 10);
    }

    if (/^\d{8}$/.test(text)) {
        const day = text.substring(0, 2);
        const month = text.substring(2, 4);
        const year = text.substring(4, 8);

        return `${year}-${month}-${day}`;
    }

    return text.substring(0, 10);
}

function normalizeDateTimeForInput(value) {
    const text = String(value || "").trim();

    if (!text) {
        return "";
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(text)) {
        return text;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(text)) {
        return `${text}:00`;
    }

    if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(text)) {
        return text.replace(" ", "T");
    }

    if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(text)) {
        return `${text.replace(" ", "T")}:00`;
    }

    return text.replace(" ", "T");
}

async function saveCurrentCustomer() {
    if (state.readOnly) {
        return;
    }

    setText("dlgStatus", "Save is not implemented yet.");
}

function findDetailNode(root) {
    return root?.querySelector("customerDocument > customerBasis")
        || root?.querySelector("customerDocument customerBasis")
        || root?.querySelector("customerDocument")
        || null;
}

function findSectionNode(doc, sectionName) {
    return doc?.querySelector(`customerDocument > ${sectionName}`)
        || doc?.querySelector(`customerDocument ${sectionName}`)
        || doc?.querySelector(sectionName)
        || null;
}

function returnToPreviousPage() {
    if (state.modal && closeEditDialog("cancel")) {
        return;
    }

    window.location.href = state.returnUrl || DEFAULT_RETURN_URL;
}

function getChildText(parent, tagName, fallback = "") {
    const element = parent?.getElementsByTagName(tagName)?.[0];
    const value = element?.textContent?.trim();

    return value || fallback;
}

function getFieldLabel(parent, tagName, fallback = "") {
    const element = parent?.getElementsByTagName(tagName)?.[0];
    const label = element?.getAttribute("header") || element?.getAttribute("label") || "";

    return String(label || fallback).trim() || fallback;
}
