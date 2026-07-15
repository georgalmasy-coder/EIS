import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { initTabs } from "../components/tabs.js";
import { applyEditDialogShellMode, closeEditDialog, getEditDialogPageContext, notifyEditDialogSaved, requestHistoricalEditDialog } from "../components/edit-dialog-page.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { createHistoryTable } from "../components/history-table.js";
import { createNotesTable } from "../components/notes-table.js";
import { createAttachmentsTable } from "../components/attachments-table.js";
import { setText } from "../core/dom.js";
import { getDirectChild, getDirectText, hasXmlParseError, serializeXml } from "../core/xml.js";
import { fieldControl, fieldEditable, fieldRequired, fieldVisible } from "../core/field-display.js";
import { escapeHtml } from "../core/html.js";
import { isTruthy } from "../core/utils.js";
import { focusFirstInvalidField, validateFieldsFromDetailNode } from "../core/validation.js";

const SAVE_URL = "/basis/stakeholder?cmd=save";
const EDIT_PAGE_URL = "/web/view?page=stakeholder-edit";
const DEFAULT_RETURN_URL = "/web/view?page=stakeholder-main";

const historyTable = createHistoryTable({
    editPageUrl: EDIT_PAGE_URL,
    defaultReturnUrl: DEFAULT_RETURN_URL
});

const notesTable = createNotesTable();
const attachmentsTable = createAttachmentsTable();

const MODES = {
    edit: "edit",
    editVersion: "edit-version",
    create: "create"
};

const PHONE_RULES = [
    { country: "Denmark", code: "+45", min: 8, max: 8, example: "12 34 56 78" },
    { country: "Sweden", code: "+46", min: 7, max: 10, example: "70 123 45 67" },
    { country: "Norway", code: "+47", min: 8, max: 8, example: "123 45 678" },
    { country: "Germany", code: "+49", min: 7, max: 13, example: "151 23456789" },
    { country: "United Kingdom", code: "+44", min: 10, max: 10, example: "7123 456789" },
    { country: "United States", code: "+1", min: 10, max: 10, example: "(123) 456-7890" },
    { country: "Canada", code: "+1", min: 10, max: 10, example: "(123) 456-7890" },
    { country: "France", code: "+33", min: 9, max: 9, example: "6 12 34 56 78" },
    { country: "Netherlands", code: "+31", min: 9, max: 9, example: "6 12345678" },
    { country: "Belgium", code: "+32", min: 8, max: 9, example: "470 12 34 56" },
    { country: "Spain", code: "+34", min: 9, max: 9, example: "612 34 56 78" },
    { country: "Italy", code: "+39", min: 8, max: 11, example: "312 345 6789" },
    { country: "Finland", code: "+358", min: 7, max: 10, example: "40 1234567" },
    { country: "Poland", code: "+48", min: 9, max: 9, example: "123 456 789" },
    { country: "Portugal", code: "+351", min: 9, max: 9, example: "912 345 678" },
    { country: "Switzerland", code: "+41", min: 9, max: 9, example: "79 123 45 67" },
    { country: "Austria", code: "+43", min: 7, max: 13, example: "664 1234567" },
    { country: "Ireland", code: "+353", min: 7, max: 9, example: "85 123 4567" },
    { country: "Iceland", code: "+354", min: 7, max: 7, example: "123 4567" },
    { country: "Faroe Islands", code: "+298", min: 6, max: 6, example: "123456" },
    { country: "Greenland", code: "+299", min: 6, max: 6, example: "123456" }
];

const DEFAULT_PHONE_RULE = PHONE_RULES[0];

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
        customerName: "-",
        projectName: "-",
        userName: "-"
    }
};

document.addEventListener("DOMContentLoaded", start);

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

    setText("customerName", "-", "-");
    setText("projectName", "-", "-");
    setText("userName", "-", "-");
    setText("loadStatus", "Loading", "-");

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
        { btnId: "tabBtn4", panelId: "tabPanel4" }
    ]);
}

function initializeRouteState() {
    const params = new URLSearchParams(window.location.search);
    const dialogContext = getEditDialogPageContext();

    state.mode = normalizeMode(params.get("mode"));
    state.id = params.get("id") || "";
    state.version = params.get("version") || "";
    state.returnUrl = params.get("returnUrl") || DEFAULT_RETURN_URL;
    state.readOnly = (state.mode === MODES.editVersion && !!state.version) || dialogContext.readOnly;
    state.modal = state.modal || dialogContext.modal;

    historyTable.setContext({
        id: state.id,
        returnUrl: buildCurrentEditReturnUrl(),
        readOnly: state.readOnly
    }, { render: false });

    notesTable.setReadOnly(state.readOnly, { render: false });
    attachmentsTable.setReadOnly(state.readOnly, { render: false });
}

function initializeEvents() {
    document.getElementById("btnCancel")?.addEventListener("click", returnToPreviousPage);
    document.getElementById("btnSave")?.addEventListener("click", saveCurrentStakeholder);

    document.addEventListener("change", (event) => {
        const target = event.target;

        if (!(target instanceof HTMLElement)) {
            return;
        }

        if (target.matches('[data-phone-country-code="true"]')) {
            const container = target.closest('[data-phone-field="true"]');
            formatCurrentPhoneValue(container);
            updatePhoneHelp(container);
        }
    });

    document.addEventListener("input", (event) => {
        const target = event.target;

        if (!(target instanceof HTMLElement)) {
            return;
        }

        if (target.matches('[data-phone-input="true"]')) {
            const container = target.closest('[data-phone-field="true"]');
            formatCurrentPhoneValue(container);
        }
    });

    historyTable.bind({
        id: state.id,
        returnUrl: buildCurrentEditReturnUrl(),
        readOnly: state.readOnly,
        onOpenHistoricalVersion: state.modal
            ? ({ id, version }) => {
                requestHistoricalEditDialog({
                    page: "stakeholder-edit",
                    id,
                    version,
                    readOnly: true,
                    title: version ? `Historical version ${version}` : "Historical version"
                });
            }
            : ({ version }) => {
                window.location.href = buildHistoricalEditUrl(version);
            }
    });

    notesTable.bind({
        readOnly: state.readOnly
    });

    attachmentsTable.bind({
        readOnly: state.readOnly
    });
}

async function loadDetail() {
    const detailUrl = buildDetailUrl();

    if (!detailUrl) {
        setText("loadStatus", "Error", "-");
        setText("dlgStatus", "Could not determine detail URL.", "");
        return;
    }

    setText("loadStatus", "Loading", "-");
    setText("dlgStatus", "Loading stakeholder details...", "");

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
            throw new Error("The stakeholder endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.lookups = parseLookups(xmlDocument);
        state.detailNode = findDetailNode(xmlDocument);
        state.topPanel = parseTopPanel(xmlDocument);

        applyTopPanel();
        renderAllFromDoc(xmlDocument);
        applyStakeholderPhoneToForm();
        applyModeUi();

        setText("loadStatus", "Loaded", "-");
        setText("dlgStatus", "Loaded.", "");
    } catch (error) {
        console.error("Failed to load stakeholder detail", error);
        setText("loadStatus", "Error", "-");
        setText("dlgStatus", `Could not load stakeholder detail. ${error.message}`, "");
    }
}

function buildDetailUrl() {
    if (state.mode === MODES.create) {
        return "/basis/stakeholder?cmd=create";
    }

    if (state.mode === MODES.editVersion) {
        if (!state.id || !state.version) {
            return "";
        }

        return `/basis/stakeholder?cmd=edit&id=${encodeURIComponent(state.id)}&version=${encodeURIComponent(state.version)}`;
    }

    if (!state.id) {
        return "";
    }

    return `/basis/stakeholder?cmd=edit&id=${encodeURIComponent(state.id)}`;
}

function buildHistoricalEditUrl(version) {
    const url = new URL(EDIT_PAGE_URL, window.location.href);

    url.searchParams.set("mode", MODES.editVersion);
    url.searchParams.set("id", state.id);
    url.searchParams.set("version", version);
    url.searchParams.set("returnUrl", state.returnUrl || DEFAULT_RETURN_URL);

    return url.toString();
}

function buildCurrentEditReturnUrl(entityId = "") {
    const id = entityId || state.id || getEntityIdFromCurrentDoc();
    const url = new URL(EDIT_PAGE_URL, window.location.href);

    url.searchParams.set("mode", MODES.edit);

    if (id) {
        url.searchParams.set("id", id);
    }

    url.searchParams.set("returnUrl", state.returnUrl || DEFAULT_RETURN_URL);

    return url.toString();
}

function applyModeUi() {
    const saveButton = document.getElementById("btnSave");
    const readOnlyBanner = document.getElementById("readOnlyBanner");

    document.body.classList.toggle("stakeholder-edit-readonly", state.readOnly);

    historyTable.setReadOnly(state.readOnly);
    notesTable.setReadOnly(state.readOnly);
    attachmentsTable.setReadOnly(state.readOnly);

    if (saveButton) {
        saveButton.hidden = state.readOnly;
        saveButton.disabled = state.readOnly;
        saveButton.title = state.readOnly ? "Save is disabled for historical versions." : "";
    }

    if (readOnlyBanner) {
        readOnlyBanner.hidden = !state.readOnly;
    }

    setText("pageModeLabel", getModeLabel(), "");
    setText("entityMeta", getEntityMetaLabel(), "");

    if (state.readOnly) {
        setFormFieldsReadOnly();
    }
}

function getModeLabel() {
    if (state.mode === MODES.editVersion) {
        return state.version ? `Historical version ${state.version}` : "Historical version";
    }

    if (state.mode === MODES.create) {
        return "Create Stakeholder";
    }

    return "Edit Stakeholder";
}

function getEntityMetaLabel() {
    if (state.mode === MODES.create) {
        return "New stakeholder";
    }

    if (state.mode === MODES.editVersion) {
        return `Entity ID: ${state.id || "-"} | Version: ${state.version || "-"}`;
    }

    return `Entity ID: ${state.id || "-"}`;
}

function normalizeMode(mode) {
    const requested = String(mode || MODES.edit).trim();

    return Object.values(MODES).includes(requested) ? requested : MODES.edit;
}

function getEntityIdFromCurrentDoc() {
    const detailNode = state.detailNode || findDetailNode(state.currentDoc);

    if (!detailNode) {
        return "";
    }

    return getFirstFieldRawValue(detailNode, ["EntityId"], "");
}

function setFormFieldsReadOnly() {
    document.querySelectorAll("#basisInfoFields input, #basisInfoFields textarea").forEach((field) => {
        if (field.type === "checkbox") {
            field.disabled = true;
        } else {
            field.readOnly = true;
        }
    });

    document.querySelectorAll("#basisInfoFields select").forEach((field) => {
        field.disabled = true;
    });
}

function parseTopPanel(xmlDocument) {
    const topPanelElement = xmlDocument.querySelector("TopPanel");

    if (!topPanelElement) {
        return {
            customerName: "-",
            projectName: "-",
            userName: "-"
        };
    }

    return {
        customerName: getChildText(topPanelElement, "CustomerName", "-"),
        projectName: getChildText(topPanelElement, "ProjectName", "-"),
        userName: getChildText(topPanelElement, ["UserName", "Name"], "-")
    };
}

function applyTopPanel() {
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
}

function renderAllFromDoc(doc) {
    renderBasisInfoFromDoc(doc);
    historyTable.loadFromDocument(doc);
    notesTable.loadFromDocument(doc);
    attachmentsTable.loadFromDocument(doc);
    applyStakeholderPhoneToForm();
}

function findDetailNode(root) {
    return root?.querySelector("stakeholderDocument > stakeholder, StakeholderDocument > Stakeholder, stakeholderInfo > stakeholder, StakeholderInfo > Stakeholder, stakeholderDocument stakeholder, StakeholderDocument Stakeholder, stakeholderInfo stakeholder, StakeholderInfo Stakeholder, stakeholder, Stakeholder")
        || null;
}

function renderBasisInfoFromDoc(doc) {
    const detailNode = findDetailNode(doc);
    const basisInfoFields = document.getElementById("basisInfoFields");

    if (!basisInfoFields) {
        return;
    }

    if (!detailNode) {
        basisInfoFields.innerHTML = '<div class="page-empty">No detail XML returned.</div>';
        return;
    }

    basisInfoFields.innerHTML = Array.from(detailNode.children || [])
        .map(renderBasisInfoFieldMarkup)
        .join("");

    if (state.readOnly) {
        setFormFieldsReadOnly();
    }
}

function renderBasisInfoFieldMarkup(field) {
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

    const readonlyAttr = editable && !state.readOnly ? "" : "readonly";
    const disabledAttr = editable && !state.readOnly ? "" : "disabled";
    const requiredStar = required ? '<span class="field-required" aria-hidden="true">*</span>' : "";
    const requiredAttr = required ? "required" : "";
    const escapedName = escapeHtml(name);
    const escapedLabel = escapeHtml(label);
    const escapedValue = escapeHtml(value);

    if (control === "hidden") {
        return `<input type="hidden" data-field="${escapedName}" id="fld-${escapedName}" value="${escapedValue}">`;
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
        const normalized = value ? value.replace(" ", "T") : "";

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="datetime-local" step="1" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "date") {
        const normalized = value ? value.substring(0, 10) : "";

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="date" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "phone") {
        return renderPhoneField(field, value, escapedName, escapedLabel, requiredStar, readonlyAttr, disabledAttr, requiredAttr);
    }

    if (control === "textarea"
        || name === "Description"
        || name === "StakeholderDescription") {
        return `
            <div class="page-field description-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <textarea id="fld-${escapedName}" data-field="${escapedName}" ${readonlyAttr} ${requiredAttr}>${escapedValue}</textarea>
            </div>
        `;
    }

    if (control === "select") {
        const options = Array.from(field.getElementsByTagName("Option"));
        const selectedValue = (field.getElementsByTagName("Value")?.[0]?.textContent || "").trim();

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
            <input id="fld-${escapedName}" data-field="${escapedName}" type="text" value="${escapedValue}" ${readonlyAttr} ${requiredAttr} />
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

    doc?.querySelectorAll("lookups > lookup").forEach((lookupNode) => {
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
            <div class="page-field stakeholder-phone-field" data-phone-field="true" data-phone-field-name="${escapedName}">
                <label for="fieldPhone">${escapedLabel}${requiredStar}</label>
                <input
                        id="fieldPhone"
                        data-field="${escapedName}"
                        data-phone-input="true"
                        type="tel"
                        inputmode="tel"
                        autocomplete="tel"
                        value="${escapeHtml(displayValue)}"
                        ${readonlyAttr}
                        ${requiredAttr}
                />
            </div>
        `;
    }

    const countryOptions = rules
        .slice()
        .sort((left, right) => String(left.country || "").localeCompare(String(right.country || "")))
        .map((rule) => {
            const selected = currentRule && rule.code === currentRule.code && rule.country === currentRule.country ? " selected" : "";
            return `<option value="${escapeHtml(rule.code)}" data-country="${escapeHtml(rule.country)}" data-min="${escapeHtml(rule.min)}" data-max="${escapeHtml(rule.max)}" data-example="${escapeHtml(rule.example)}"${selected}>${escapeHtml(`${rule.country} (${rule.code})`)}</option>`;
        }).join("");

    return `
        <div class="page-field stakeholder-phone-field" data-phone-field="true" data-phone-field-name="${escapedName}">
            <div class="stakeholder-phone-controls">
                <div class="page-field stakeholder-phone-country-code">
                    <label for="fieldPhoneCountryCode">${escapedLabel}${requiredStar}</label>
                    <select
                            id="fieldPhoneCountryCode"
                            data-phone-country-code="true"
                            aria-label="Country code"
                            title="Country code"
                            ${isReadOnly ? "disabled" : ""}
                    >
                        ${countryOptions}
                    </select>
                </div>

                <div class="page-field stakeholder-phone-input">
                    <label class="visually-hidden" for="fieldPhone">${escapedLabel}</label>
                    <input
                            id="fieldPhone"
                            data-field="${escapedName}"
                            data-phone-input="true"
                            data-phone-raw="${escapeHtml(value)}"
                            type="tel"
                            inputmode="tel"
                            autocomplete="tel"
                            value="${escapeHtml(displayValue)}"
                            ${isReadOnly ? "readonly" : ""}
                            ${requiredAttr}
                    />
                </div>
            </div>

            <p class="field-help" id="fieldPhoneHelp">Select a country code and enter the local phone number.</p>
        </div>
    `;
}

async function saveCurrentStakeholder() {
    if (state.readOnly) {
        return;
    }

    const validationErrors = validateCurrentStakeholder();

    if (validationErrors.length) {
        setText("dlgStatus", "Validation failed.");
        setText("loadStatus", "Validation error");
        window.alert(validationErrors.join("\n"));
        focusFirstInvalidField(document.getElementById("basisInfoFields") || document);
        return;
    }

    try {
        setText("dlgStatus", "Saving...");
        setText("loadStatus", "Saving");

        const payload = buildSavePayload();

        const response = await fetch(SAVE_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/xml; charset=UTF-8",
                "Accept": "application/xml,text/xml,*/*"
            },
            body: payload,
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        setText("dlgStatus", "Saved.");
        setText("loadStatus", "Saved");

        if (state.modal && notifyEditDialogSaved({
            mode: state.mode,
            id: state.id,
            version: state.version
        })) {
            return;
        }

        returnToPreviousPage();
    } catch (error) {
        console.error("Failed to save stakeholder", error);
        setText("dlgStatus", `Save failed. ${error.message}`);
        setText("loadStatus", "Error");
    }
}

function validateCurrentStakeholder() {
    const detailNode = state.detailNode || findDetailNode(state.currentDoc);
    const basisInfoFields = document.getElementById("basisInfoFields");
    const errors = validateFieldsFromDetailNode(detailNode, basisInfoFields);

    errors.push(...validatePhoneFields(detailNode, basisInfoFields));

    return errors;
}

function buildSavePayload() {
    const currentDoc = state.currentDoc;

    if (!currentDoc || !currentDoc.documentElement) {
        throw new Error("No XML document loaded.");
    }

    const updatedDoc = currentDoc.cloneNode(true);
    const root = updatedDoc.documentElement;
    const detailNode = findDetailNode(updatedDoc);

    if (!detailNode) {
        throw new Error("No detail XML returned.");
    }

    const basisInfoFields = document.getElementById("basisInfoFields");
    const fields = Array.from(basisInfoFields?.querySelectorAll("[data-field]") || []);

    fields.forEach((uiField) => {
        const name = uiField.getAttribute("data-field");

        if (!name) {
            return;
        }

        const child = ensureChild(updatedDoc, detailNode, name);
        const control = (child.getAttribute("control") || "").toLowerCase();

        if (control === "phone" || name === "ContactPhone") {
            child.textContent = getFullPhoneNumberForField(name, uiField);
            return;
        }

        if (control === "select") {
            const value = uiField.selectedOptions?.[0]?.value?.trim() || "";
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

            return;
        }

        if (uiField.type === "checkbox") {
            child.textContent = uiField.checked ? "true" : "false";
        } else {
            child.textContent = uiField.value ?? "";
        }
    });

    notesTable.writeToDocument(updatedDoc);
    attachmentsTable.writeToDocument(updatedDoc);

    return serializeXml(updatedDoc);
}

function ensureChild(doc, parent, tagName) {
    let child = getDirectChild(parent, tagName);

    if (!child) {
        child = doc.createElement(tagName);
        parent.appendChild(child);
    }

    return child;
}

function getChildText(parent, tagNames, fallback = "") {
    const names = Array.isArray(tagNames) ? tagNames : [tagNames];

    for (const tagName of names) {
        const element = parent?.getElementsByTagName?.(tagName)?.[0];
        const value = element?.textContent?.trim();

        if (value) {
            return value;
        }
    }

    return fallback;
}

function getFirstFieldRawValue(node, fieldNames, fallback = "") {
    for (const fieldName of fieldNames) {
        const field = getDirectChild(node, fieldName);

        if (!field) {
            continue;
        }

        const valueText = field.querySelector(":scope > Value")?.textContent?.trim();
        const directText = getDirectText(field).trim();

        if (valueText) {
            return valueText;
        }

        if (directText) {
            return directText;
        }
    }

    return fallback;
}

function getPhoneCountryRules() {
    const countryCodeLookup = state.lookups?.countryCode;

    if (Array.isArray(countryCodeLookup) && countryCodeLookup.length) {
        return countryCodeLookup
            .map((rule) => ({
                country: String(rule.country || rule.label || "").trim(),
                code: String(rule.code || "").trim(),
                min: Number.isFinite(rule.min) ? rule.min : DEFAULT_PHONE_RULE.min,
                max: Number.isFinite(rule.max) ? rule.max : DEFAULT_PHONE_RULE.max,
                example: String(rule.example || "").trim()
            }))
            .filter((rule) => rule.code);
    }

    return PHONE_RULES.slice();
}

function getPhoneFieldContainer(source) {
    if (!source) {
        return null;
    }

    return source.closest?.('[data-phone-field="true"]') || null;
}

function selectedPhoneRule(container) {
    const select = container?.querySelector('[data-phone-country-code="true"]');

    if (!select) {
        return getPhoneCountryRules()[0] || DEFAULT_PHONE_RULE;
    }

    const selectedOption = select.options[select.selectedIndex];

    if (!selectedOption) {
        return getPhoneCountryRules()[0] || DEFAULT_PHONE_RULE;
    }

    const code = String(selectedOption.value || "").trim();
    const country = String(selectedOption.getAttribute("data-country") || "").trim();
    const rules = getPhoneCountryRules();

    return rules.find((rule) => rule.code === code && rule.country === country)
        || rules.find((rule) => rule.code === code)
        || rules.find((rule) => rule.country === country)
        || rules[0]
        || DEFAULT_PHONE_RULE;
}

function getPhoneRuleForValue(phone, rules = getPhoneCountryRules()) {
    const normalizedPhone = normalizePhoneNumber(phone);

    if (!normalizedPhone || !rules.length) {
        return rules[0] || DEFAULT_PHONE_RULE;
    }

    const prefixMatches = rules.filter((rule) => normalizedPhone.startsWith(rule.code));

    return prefixMatches[0] || rules[0] || DEFAULT_PHONE_RULE;
}

function applyStakeholderPhoneToForm() {
    document.querySelectorAll('[data-phone-field="true"]').forEach((container) => {
        const input = container.querySelector('[data-phone-input="true"]');

        if (!input) {
            return;
        }

        const rawValue = input.getAttribute("data-phone-raw") || input.value;
        const currentRule = getPhoneRuleForValue(rawValue);

        fillPhoneCountryCodes(container, currentRule);
        input.value = formatPhoneNumberForDisplay(rawValue, currentRule);
        updatePhoneConstraints(container);
        updatePhoneHelp(container);
    });
}

function fillPhoneCountryCodes(container, selectedRule) {
    const select = container?.querySelector('[data-phone-country-code="true"]');

    if (!select) {
        return;
    }

    const rules = getPhoneCountryRules();
    const currentRule = selectedRule || selectedPhoneRule(container) || rules[0] || null;

    select.innerHTML = rules
        .slice()
        .sort((left, right) => String(left.country || "").localeCompare(String(right.country || "")))
        .map((rule) => {
            const selected = currentRule && rule.code === currentRule.code && rule.country === currentRule.country ? " selected" : "";
            return `<option value="${escapeHtml(rule.code)}" data-country="${escapeHtml(rule.country)}" data-min="${escapeHtml(rule.min)}" data-max="${escapeHtml(rule.max)}" data-example="${escapeHtml(rule.example)}"${selected}>${escapeHtml(`${rule.country} (${rule.code})`)}</option>`;
        }).join("");
}

function updatePhoneConstraints(container) {
    const input = container?.querySelector('[data-phone-input="true"]');

    if (!input) {
        return;
    }

    const rule = selectedPhoneRule(container);

    input.setAttribute("inputmode", "tel");
    input.setAttribute("autocomplete", "tel");
    input.setAttribute("type", "tel");

    if (!rule) {
        return;
    }

    input.setAttribute("placeholder", rule.example || "");
    input.setAttribute("pattern", phonePatternForRule(rule));
    input.setAttribute("title", phoneTitleForRule(rule));
    input.maxLength = Math.max((rule.max || 15) + 6, 15);
}

function updatePhoneHelp(container) {
    const help = container?.querySelector(".field-help");

    if (!help) {
        return;
    }

    const rule = selectedPhoneRule(container);

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

function formatCurrentPhoneValue(container) {
    const input = container?.querySelector('[data-phone-input="true"]');

    if (!input) {
        return;
    }

    const rule = selectedPhoneRule(container);

    if (!rule) {
        return;
    }

    input.value = formatPhoneDigits(extractLocalPhoneDigits(input.value, rule), rule);
    updatePhoneConstraints(container);
    updatePhoneHelp(container);
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
    const selectedRule = rule || DEFAULT_PHONE_RULE;
    const safeDigits = onlyDigits(digits).slice(0, selectedRule.max || 15);

    if (!safeDigits) {
        return "";
    }

    if (selectedRule.code === "+45" && safeDigits.length <= 8) {
        return safeDigits.replace(/(\d{2})(?=\d)/g, "$1 ").trim();
    }

    if (selectedRule.code === "+47" && safeDigits.length <= 8) {
        return safeDigits.replace(/^(\d{3})(\d{0,2})(\d{0,3}).*/, (_match, first, second, third) => {
            return [first, second, third].filter(Boolean).join(" ");
        });
    }

    if (selectedRule.code === "+1" && safeDigits.length <= 10) {
        if (safeDigits.length <= 3) {
            return safeDigits;
        }

        if (safeDigits.length <= 6) {
            return `(${safeDigits.slice(0, 3)}) ${safeDigits.slice(3)}`;
        }

        return `(${safeDigits.slice(0, 3)}) ${safeDigits.slice(3, 6)}-${safeDigits.slice(6)}`;
    }

    if (selectedRule.code === "+44" && safeDigits.length <= 10) {
        return safeDigits.replace(/^(\d{4})(\d{0,6}).*/, (_match, first, second) => {
            return [first, second].filter(Boolean).join(" ");
        });
    }

    if (selectedRule.code === "+33" && safeDigits.length <= 9) {
        return safeDigits.replace(/^(\d)(\d{0,2})(\d{0,2})(\d{0,2})(\d{0,2}).*/, (_match, first, second, third, fourth, fifth) => {
            return [first, second, third, fourth, fifth].filter(Boolean).join(" ");
        });
    }

    if (selectedRule.code === "+48" && safeDigits.length <= 9) {
        return safeDigits.replace(/^(\d{3})(\d{0,3})(\d{0,3}).*/, (_match, first, second, third) => {
            return [first, second, third].filter(Boolean).join(" ");
        });
    }

    if (safeDigits.length <= 6) {
        return safeDigits;
    }

    if (safeDigits.length <= 10) {
        return safeDigits.replace(/^(\d{3})(\d{0,3})(\d{0,4}).*/, (_match, first, second, third) => {
            return [first, second, third].filter(Boolean).join(" ");
        });
    }

    return safeDigits.replace(/^(\d{3})(\d{0,4})(\d{0,4})(\d{0,4}).*/, (_match, first, second, third, fourth) => {
        return [first, second, third, fourth].filter(Boolean).join(" ");
    });
}

function validatePhoneFields(detailNode, fieldsRoot) {
    const errors = [];

    if (!detailNode || !fieldsRoot) {
        return errors;
    }

    Array.from(detailNode.children || []).forEach((fieldNode) => {
        if (fieldControl(fieldNode) !== "phone") {
            return;
        }

        const fieldName = fieldNode.tagName;
        const input = fieldsRoot.querySelector(`[data-field="${fieldName}"]`);
        const container = getPhoneFieldContainer(input);

        if (!input) {
            return;
        }

        const rule = selectedPhoneRule(container);
        const digits = onlyDigits(input.value);

        if (!digits || !rule) {
            return;
        }

        if (rule.min === rule.max && digits.length !== rule.min) {
            input.classList.add("is-invalid");
            container?.classList.add("has-validation-error");
            errors.push(`${rule.country}: exactly ${rule.min} digits are required.`);
            return;
        }

        if (digits.length < rule.min || digits.length > rule.max) {
            input.classList.add("is-invalid");
            container?.classList.add("has-validation-error");
            errors.push(`${rule.country}: ${rule.min}-${rule.max} digits are required.`);
        }
    });

    return errors;
}

function getFullPhoneNumberForField(fieldName, uiField) {
    const container = getPhoneFieldContainer(uiField) || document.querySelector(`[data-phone-field-name="${fieldName}"]`);
    const rule = selectedPhoneRule(container);
    const input = container?.querySelector('[data-phone-input="true"]') || uiField;

    if (!input) {
        return "";
    }

    const digits = onlyDigits(input.value);

    if (!digits) {
        return "";
    }

    if (!rule) {
        return formatPhoneDigits(digits, rule);
    }

    return `${rule.code} ${formatPhoneDigits(digits, rule)}`.trim();
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

function returnToPreviousPage() {
    if (state.modal && closeEditDialog("cancel")) {
        return;
    }

    window.location.href = state.returnUrl || DEFAULT_RETURN_URL;
}
