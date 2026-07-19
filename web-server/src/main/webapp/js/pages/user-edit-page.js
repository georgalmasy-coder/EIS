import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { initTabs } from "../components/tabs.js";
import { mountTopbar } from "../components/topbar.js";
import {
    applyEditDialogShellMode,
    closeEditDialog,
    getEditDialogPageContext,
    notifyEditDialogSaved
} from "../components/edit-dialog-page.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { setText } from "../core/dom.js";
import { escapeHtml } from "../core/html.js";
import { toDateTimeLocalValue } from "../core/date.js";
import {
    fieldControl,
    fieldEditable,
    fieldHeader,
    fieldRequired,
    fieldVisible
} from "../core/field-display.js";
import {
    getChildText,
    getDirectChild,
    getDirectText,
    hasXmlParseError,
    serializeXml
} from "../core/xml.js";
import {
    focusFirstInvalidField,
    validateFieldsFromDetailNode
} from "../core/validation.js";
import { isTruthy } from "../core/utils.js";

const DETAIL_URL = "/api/user-main";
const SAVE_URL = "/api/user-main?cmd=save";
const RETURN_URL = "/web/view?page=user-main";

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
    mode: "edit",
    id: "",
    returnUrl: RETURN_URL,
    modal: false,
    currentDoc: null,
    lookups: {},
    userNode: null,
    projectAccessNode: null,
    securityAndPasswordNode: null,
    topPanel: {
        customerName: "-",
        projectName: "-",
        userName: "-"
    },
    user: {
        phone: "",
        userRole: "",
        departmentId: ""
    },
    saving: false
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

    setText("customerName", "-", "");
    setText("projectName", "-", "");
    setText("userName", "-", "");
    setText("loadStatus", "Loading", "");

    if (!state.modal) {
        initMenu(document);
        mountTopbar(document);
    }

    initHelpDialog();
}

function initializeTabs() {
    initTabs([
        { btnId: "tabBtnBasis", panelId: "tabPanelBasis" },
        { btnId: "tabBtnProjects", panelId: "tabPanelProjects" },
        { btnId: "tabBtnSecurityAndPassword", panelId: "tabPanelSecurityAndPassword" }
    ]);

    const securityTabButton = document.getElementById("tabBtnSecurityAndPassword");
    const securityTabPanel = document.getElementById("tabPanelSecurityAndPassword");

    if (securityTabButton) {
        securityTabButton.hidden = true;
        securityTabButton.setAttribute("aria-hidden", "true");
    }

    if (securityTabPanel) {
        securityTabPanel.hidden = true;
        securityTabPanel.setAttribute("aria-hidden", "true");
    }
}

function initializeRouteState() {
    const params = new URLSearchParams(window.location.search);
    const requestedMode = String(params.get("mode") || "edit").toLowerCase();
    const dialogContext = getEditDialogPageContext();

    state.mode = requestedMode === "create" ? "create" : "edit";
    state.id = params.get("id") || "";
    state.returnUrl = params.get("returnUrl") || RETURN_URL;
    state.modal = state.modal || dialogContext.modal;
}

function initializeEvents() {
    document.getElementById("btnCancel")?.addEventListener("click", () => {
        returnToPreviousPage();
    });

    document.getElementById("btnSave")?.addEventListener("click", async () => {
        await saveCurrentUser();
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

    window.addEventListener("resize", () => {
        syncProjectPanelHeight();
    });
}

async function loadDetail() {
    const detailUrl = buildDetailUrl();

    if (!detailUrl) {
        setText("loadStatus", "Error", "");
        setText("dlgStatus", "Could not determine detail URL.");
        return;
    }

    setText("loadStatus", "Loading", "");
    setText("dlgStatus", "Loading user details...");

    try {
        const response = await fetch(detailUrl, {
            method: "GET",
            headers: {
                Accept: "application/xml,text/xml,*/*"
            },
            cache: "no-store",
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        const xmlText = await response.text();
        const xmlDocument = new DOMParser().parseFromString(xmlText, "application/xml");

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("The user endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.lookups = parseLookups(xmlDocument);
        state.topPanel = parseTopPanel(xmlDocument);
        state.userNode = findUserNode(xmlDocument);
        state.projectAccessNode = findProjectAccessNode(xmlDocument);
        state.securityAndPasswordNode = findSecurityAndPasswordNode(xmlDocument);
        state.user = parseUserNode(state.userNode);

        applyTopPanel();
        renderDetailFields();
        applyUserToForm();
        syncProjectPanelHeight();
        applyModeUi();

        setText("loadStatus", "Loaded", "");
        setText("dlgStatus", "Loaded.");
    } catch (error) {
        console.error("Failed to load user detail", error);
        setText("loadStatus", "Error", "");
        setText("dlgStatus", `Could not load user details. ${error.message}`);
    }
}

function buildDetailUrl() {
    if (state.mode === "create") {
        return `${DETAIL_URL}?cmd=create`;
    }

    if (!state.id) {
        return "";
    }

    return `${DETAIL_URL}?cmd=edit&id=${encodeURIComponent(state.id)}`;
}

function applyModeUi() {
    const saveButton = document.getElementById("btnSave");

    setText("pageModeLabel", state.mode === "create" ? "Create user" : "Edit user");
    setText("entityMeta", state.mode === "create" ? "New user" : `User ID: ${state.id || "-"}`);
    setText("pageTitle", "User Account");

    if (saveButton) {
        saveButton.textContent = state.mode === "create" ? "Create" : "Save";
    }
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
        userName: getChildText(topPanelElement, "UserName", getChildText(topPanelElement, "Name", "-"))
    };
}

function applyTopPanel() {
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
}

function findUserNode(doc) {
    return doc?.querySelector("userDocument > user")
        || doc?.querySelector("userDocument user")
        || doc?.querySelector("user")
        || null;
}

function findSecurityAndPasswordNode(doc) {
    return doc?.querySelector("securityAndPassword")
        || doc?.querySelector("userDocument > securityAndPassword")
        || doc?.querySelector("userDocument securityAndPassword")
        || null;
}

function findProjectAccessNode(doc) {
    return doc?.querySelector("userProjects")
        || doc?.querySelector("userDocument > userProjects")
        || doc?.querySelector("userDocument userProjects")
        || null;
}

function parseUserNode(node) {
    if (!node) {
        return {
            phone: "",
            userRole: "",
            departmentId: ""
        };
    }

    return {
        userId: text(node, "UserId"),
        initials: text(node, "Initials"),
        name: text(node, "Name"),
        email: text(node, "UserEmail") || text(node, "Email"),
        phone: text(node, "UserPhone") || text(node, "Phone"),
        departmentId: text(node, "DepartmentId"),
        customerNames: text(node, "CustomerNames"),
        active: boolText(node, "Active"),
        userRole: text(node, "UserRole"),
        lockedUntil: text(node, "LockedUntil"),
        departmentDescription: text(node, "DepartmentDescription"),
        userMfaPolicy: text(node, "UserMfaPolicy"),
        mfaEnabled: boolText(node, "MfaEnabled"),
        mfaVerified: boolText(node, "MfaVerified"),
        mfaResetRequired: boolText(node, "MfaResetRequired"),
        mfaResetAt: text(node, "MfaResetAt"),
        mfaResetByUserId: text(node, "MfaResetByUserId"),
        lastLoginAt: text(node, "LastLoginAt"),
        passwordSet: boolText(node, "PasswordSet")
    };
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
            const country = String(optionNode.getAttribute("country") || optionNode.getAttribute("label") || "").trim();
            const code = String(optionNode.getAttribute("code") || "").trim();

            return {
                country,
                code,
                label: String(optionNode.getAttribute("label") || country || code).trim(),
                min: parseInt(optionNode.getAttribute("min") || "", 10),
                max: parseInt(optionNode.getAttribute("max") || "", 10),
                example: String(optionNode.getAttribute("example") || "").trim()
            };
        }).filter((option) => option.code);
    });

    return lookups;
}

function renderDetailFields() {
    const basisFields = document.getElementById("basisInfoFields");
    const projectAccessFields = document.getElementById("projectAccessFields");
    const securityAndPasswordFields = document.getElementById("securityAndPasswordFields");

    if (!basisFields || !projectAccessFields || !securityAndPasswordFields) {
        return;
    }

    if (!state.userNode) {
        basisFields.innerHTML = '<div class="page-empty">No user XML returned.</div>';
    } else {
        basisFields.innerHTML = Array.from(state.userNode.children || [])
            .map(renderBasisInfoFieldMarkup)
            .join("");
    }

    if (!state.projectAccessNode) {
        projectAccessFields.innerHTML = '<div class="page-empty">No project access XML returned.</div>';
    } else {
        projectAccessFields.innerHTML = renderProjectAccessMarkup(state.projectAccessNode);
    }

    if (!state.securityAndPasswordNode) {
        securityAndPasswordFields.innerHTML = '<div class="page-empty">No security and password XML returned.</div>';
        return;
    }

    securityAndPasswordFields.innerHTML = Array.from(state.securityAndPasswordNode.children || [])
        .map(renderBasisInfoFieldMarkup)
        .join("");
}

function syncProjectPanelHeight() {
    const basisFields = document.getElementById("basisInfoFields");
    const projectAccessFields = document.getElementById("projectAccessFields");

    if (!basisFields || !projectAccessFields) {
        return;
    }

    const basisHeight = Math.ceil(basisFields.getBoundingClientRect().height);

    if (Number.isFinite(basisHeight) && basisHeight > 0) {
        projectAccessFields.style.setProperty("--user-edit-project-list-height", `${basisHeight}px`);
    }
}

function renderBasisInfoFieldMarkup(field) {
    const name = field.tagName;
    const label = fieldHeader(field, name);
    const control = fieldControl(field);
    const editable = fieldEditable(field);
    const visible = fieldVisible(field);
    const required = fieldRequired(field);
    const value = getFieldUiValue(field);
    const requiredStar = required ? '<span class="field-required" aria-hidden="true">*</span>' : "";
    const escapedName = escapeHtml(name);
    const escapedLabel = escapeHtml(label);

    if (!visible || control === "hidden") {
        return `<input type="hidden" data-field="${escapedName}" id="fld-${escapedName}" value="${escapeHtml(value)}">`;
    }

    if (control === "phone" || name === "Phone") {
        const disabledAttr = editable ? "" : "disabled";
        const readonlyAttr = editable ? "" : "readonly";
        const phoneLabel = `${escapedLabel}${requiredStar}`;

        return `
            <div class="page-field user-edit-phone-field">
                <div class="user-edit-phone-controls">
                    <div class="page-field user-edit-phone-country-code">
                        <label for="fieldPhoneCountryCode">${phoneLabel}</label>
                        <select id="fieldPhoneCountryCode" aria-label="Country code" title="Country code" ${disabledAttr}></select>
                    </div>

                    <div class="page-field user-edit-phone-input">
                        <label class="visually-hidden" for="fieldPhone">${escapedLabel}</label>
                        <input id="fieldPhone" data-field="${escapedName}" type="tel" inputmode="tel" autocomplete="tel" aria-label="${escapedLabel}" value="${escapeHtml(value)}" ${readonlyAttr} ${required ? "required" : ""} />
                    </div>
                </div>
                <p class="field-help" id="fieldPhoneHelp">Select a country code and enter the local phone number.</p>
            </div>
        `;
    }

    if (control === "email") {
        const readonlyAttr = editable ? "" : "readonly";

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="text" inputmode="email" autocomplete="email" value="${escapeHtml(value)}" ${readonlyAttr} ${required ? "required" : ""} />
            </div>
        `;
    }

    if (control === "checkbox") {
        const checked = isTruthy(value) ? "checked" : "";
        const disabledAttr = editable ? "" : "disabled";

        return `
            <div class="page-field checkbox-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="checkbox" ${checked} ${disabledAttr} ${required ? "required" : ""} />
            </div>
        `;
    }

    if (control === "datetime") {
        const readonlyAttr = editable ? "" : "readonly";
        const normalized = toDateTimeLocalValue(value);

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="datetime-local" step="1" value="${escapeHtml(normalized)}" ${readonlyAttr} ${required ? "required" : ""} />
            </div>
        `;
    }

    if (control === "select") {
        const selectedValue = (getDirectChild(field, "Value")?.textContent || "").trim();
        const options = Array.from(field.children || []).filter((child) => child.tagName === "Option");
        const disabledAttr = editable ? "" : "disabled";

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <select id="fld-${escapedName}" data-field="${escapedName}" ${disabledAttr} ${required ? "required" : ""}>
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

    const readonlyAttr = editable ? "" : "readonly";
    return `
        <div class="page-field">
            <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
            <input id="fld-${escapedName}" data-field="${escapedName}" type="text" value="${escapeHtml(value)}" ${readonlyAttr} ${required ? "required" : ""} />
        </div>
    `;
}

function getFieldUiValue(field) {
    const control = fieldControl(field);

    if (control === "select") {
        return (getDirectChild(field, "Value")?.textContent || "").trim();
    }

        const valueNode = getDirectChild(field, "Value");

    if (valueNode) {
        return valueNode.textContent || "";
    }

    return getDirectText(field) || "";
}

function renderProjectAccessMarkup(projectAccessNode) {
    const rows = Array.from(projectAccessNode.children || [])
        .filter((child) => child.tagName === "project")
        .map(renderProjectAccessRowMarkup)
        .join("");

    if (!rows) {
        return '<div class="page-empty">No active projects found for this customer.</div>';
    }

    return `
        <div class="user-edit-projects-table-wrap">
            <table class="user-edit-projects-table">
                <thead>
                    <tr>
                        <th scope="col">Project name</th>
                        <th scope="col" class="user-edit-projects-check-col">Access</th>
                    </tr>
                </thead>
                <tbody>
                    ${rows}
                </tbody>
            </table>
        </div>
    `;
}

function renderProjectAccessRowMarkup(projectNode) {
    const projectId = text(projectNode, "ProjectId");
    const projectName = text(projectNode, "ProjectName") || `Project ${projectId || "-"}`;
    const selected = boolText(projectNode, "Selected") ? "checked" : "";
    const safeId = escapeHtml(projectId);
    const checkboxId = `projectAccess-${safeId}`;

    return `
        <tr>
            <td class="user-edit-project-name-cell">
                <label for="${checkboxId}">${escapeHtml(projectName)}</label>
            </td>
            <td class="user-edit-project-check-cell">
                <input
                    id="${checkboxId}"
                    class="user-edit-project-access-checkbox"
                    type="checkbox"
                    data-project-id="${safeId}"
                    ${selected}
                />
            </td>
        </tr>
    `;
}

async function saveCurrentUser() {
    if (state.saving) {
        return;
    }

    const validationErrors = validateCurrentUser();

    if (validationErrors.length) {
        setText("dlgStatus", "Validation failed.");
        setText("loadStatus", "Validation error");
        window.alert(validationErrors.join("\n"));
        focusFirstInvalidField(document.getElementById("userEditFields") || document);
        return;
    }

    state.saving = true;
    setSaveButtonDisabled(true);

    try {
        setText("dlgStatus", "Saving...");
        setText("loadStatus", "Saving");

        const payload = buildSavePayload();
        const response = await fetch(SAVE_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/xml; charset=UTF-8",
                Accept: "application/xml,text/xml,*/*"
            },
            body: payload,
            cache: "no-store",
            credentials: "same-origin"
        });

        const responseText = await response.text();

        if (!response.ok) {
            throw new Error(stripErrorPrefix(responseText) || `HTTP ${response.status} ${response.statusText}`);
        }

        setText("dlgStatus", "Saved.");
        setText("loadStatus", "Saved");

        if (state.modal && notifyEditDialogSaved({
            mode: state.mode,
            id: state.id
        })) {
            return;
        }

        returnToPreviousPage();
    } catch (error) {
        console.error("Failed to save user", error);
        const message = error.message || "Save failed.";
        setText("dlgStatus", `Save failed. ${message}`);
        setText("loadStatus", "Error");
        window.alert(message);
    } finally {
        state.saving = false;
        setSaveButtonDisabled(false);
    }
}

function validateCurrentUser() {
    const errors = [];
    const fieldRoot = document.getElementById("userEditFields") || document;
    const userErrors = validateFieldsFromDetailNode(state.userNode, fieldRoot);
    const securityErrors = validateFieldsFromDetailNode(state.securityAndPasswordNode, fieldRoot);

    errors.push(...userErrors, ...securityErrors);

    const phoneError = validatePhoneNumber();
    if (phoneError) {
        errors.push(phoneError);
    }

    return errors;
}

function buildSavePayload() {
    const currentDoc = state.currentDoc;

    if (!currentDoc || !currentDoc.documentElement) {
        throw new Error("No XML document loaded.");
    }

    const updatedDoc = currentDoc.cloneNode(true);
    const userNode = findUserNode(updatedDoc);
    const projectAccessNode = findProjectAccessNode(updatedDoc);
    const securityAndPasswordNode = findSecurityAndPasswordNode(updatedDoc);

    if (!userNode) {
        throw new Error("No user XML returned.");
    }

    const fieldsRoot = document.getElementById("userEditFields") || document;
    const fields = Array.from(fieldsRoot.querySelectorAll("[data-field]"));

    fields.forEach((uiField) => {
        const name = uiField.getAttribute("data-field");

        if (!name) {
            return;
        }

        const targetParent = uiField.closest("#securityAndPasswordFields")
            ? securityAndPasswordNode
            : userNode;

        const child = ensureChild(updatedDoc, targetParent || userNode, name);
        const control = fieldControl(child);

        if (control === "phone" || name === "Phone") {
            child.textContent = getFullPhoneNumber();
            return;
        }

        if (control === "select") {
            const value = uiField.selectedOptions?.[0]?.value?.trim() || "";
            let valueNode = getDirectChild(child, "Value");

            if (!valueNode) {
                valueNode = updatedDoc.createElement("Value");
                child.insertBefore(valueNode, child.firstChild);
            }

            valueNode.textContent = value;

            Array.from(child.children || []).filter((option) => option.tagName === "Option").forEach((option) => {
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
            return;
        }

        child.textContent = uiField.value ?? "";
    });

    syncProjectAccessSelections(updatedDoc, projectAccessNode);

    return serializeXml(updatedDoc);
}

function syncProjectAccessSelections(doc, projectAccessNode) {
    if (!doc || !projectAccessNode) {
        return;
    }

    const projectFieldsRoot = document.getElementById("projectAccessFields") || document;
    const projectCheckboxes = Array.from(projectFieldsRoot.querySelectorAll("input[type='checkbox'][data-project-id]"));

    projectCheckboxes.forEach((checkbox) => {
        const projectId = String(checkbox.getAttribute("data-project-id") || "").trim();

        if (!projectId) {
            return;
        }

        const projectNode = findProjectAccessProjectNode(projectAccessNode, projectId);

        if (!projectNode) {
            return;
        }

        const selectedNode = ensureChild(doc, projectNode, "Selected");
        selectedNode.textContent = checkbox.checked ? "true" : "false";
    });
}

function findProjectAccessProjectNode(projectAccessNode, projectId) {
    if (!projectAccessNode || !projectId) {
        return null;
    }

    return Array.from(projectAccessNode.children || [])
        .find((child) => child.tagName === "project" && text(child, "ProjectId") === projectId)
        || null;
}

function ensureChild(doc, parent, name) {
    const existing = getDirectChild(parent, name);

    if (existing) {
        return existing;
    }

    const element = doc.createElement(name);
    parent.appendChild(element);
    return element;
}

function returnToPreviousPage() {
    if (state.modal && closeEditDialog("cancel")) {
        return;
    }

    window.location.href = state.returnUrl || RETURN_URL;
}

function setSaveButtonDisabled(disabled) {
    const saveButton = document.getElementById("btnSave");

    if (saveButton) {
        saveButton.disabled = Boolean(disabled);
    }
}

function applyUserToForm() {
    const user = state.user || {};
    const rule = getPhoneRuleForValue(user.phone);

    setValue("fieldPhone", formatPhoneNumberForDisplay(user.phone, rule));
    fillPhoneCountryCodes(rule);
    updatePhoneConstraints();
    updatePhoneHelp();
}

function setValue(id, itemValue) {
    const element = document.getElementById(id);

    if (!element) {
        return;
    }

    element.value = itemValue == null ? "" : String(itemValue);
}

function fillPhoneCountryCodes(selectedRule) {
    const select = document.getElementById("fieldPhoneCountryCode");

    if (!select) {
        return;
    }

    const currentRule = selectedRule || DEFAULT_PHONE_RULE;
    const phoneRules = getPhoneCountryRules();

    select.innerHTML = phoneRules
        .slice()
        .sort((left, right) => String(left.country || "").localeCompare(String(right.country || "")))
        .map((rule) => {
            const selected = rule.code === currentRule.code && rule.country === currentRule.country ? " selected" : "";
            return `<option value="${escapeHtml(rule.code)}" data-country="${escapeHtml(rule.country)}" data-min="${escapeHtml(rule.min)}" data-max="${escapeHtml(rule.max)}" data-example="${escapeHtml(rule.example)}"${selected}>${escapeHtml(`${rule.country} (${rule.code})`)}</option>`;
        }).join("");
}

function selectedPhoneRule() {
    const select = document.getElementById("fieldPhoneCountryCode");

    if (!select) {
        return DEFAULT_PHONE_RULE;
    }

    const selectedOption = select.options[select.selectedIndex];

    if (!selectedOption) {
        return DEFAULT_PHONE_RULE;
    }

    const code = String(selectedOption.value || "").trim();
    const country = String(selectedOption.getAttribute("data-country") || "").trim();
    const rules = getPhoneCountryRules();

    return rules.find((rule) => rule.code === code && rule.country === country)
        || rules.find((rule) => rule.code === code)
        || DEFAULT_PHONE_RULE;
}

function getPhoneRuleForValue(phone) {
    const normalizedPhone = normalizePhoneNumber(phone);
    const rules = getPhoneCountryRules();

    if (!normalizedPhone) {
        return DEFAULT_PHONE_RULE;
    }

    const prefixMatches = rules.filter((rule) => normalizedPhone.startsWith(rule.code));

    if (prefixMatches.length === 1) {
        return prefixMatches[0];
    }

    return prefixMatches[0] || DEFAULT_PHONE_RULE;
}

function getPhoneCountryRules() {
    const countryCodeLookup = state.lookups?.countryCode;

    if (Array.isArray(countryCodeLookup) && countryCodeLookup.length) {
        return countryCodeLookup.map((rule) => ({
            country: String(rule.country || rule.label || "").trim(),
            code: String(rule.code || "").trim(),
            min: Number.isFinite(rule.min) ? rule.min : DEFAULT_PHONE_RULE.min,
            max: Number.isFinite(rule.max) ? rule.max : DEFAULT_PHONE_RULE.max,
            example: String(rule.example || "").trim()
        })).filter((rule) => rule.code);
    }

    return PHONE_RULES.slice();
}

function onlyDigits(value) {
    return String(value == null ? "" : value).replace(/\D/g, "");
}

function extractLocalPhoneDigits(value, rule) {
    const normalized = normalizePhoneNumber(value);
    const selectedRule = rule || DEFAULT_PHONE_RULE;

    if (!normalized) {
        return "";
    }

    if (selectedRule.code && normalized.startsWith(selectedRule.code)) {
        return onlyDigits(normalized.slice(selectedRule.code.length));
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

function updatePhoneConstraints() {
    const input = document.getElementById("fieldPhone");

    if (!input) {
        return;
    }

    const rule = selectedPhoneRule();
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

    if (rule.min === rule.max) {
        help.textContent = `${rule.country}: exactly ${rule.min} digits. Example: ${rule.example}`;
    } else {
        help.textContent = `${rule.country}: ${rule.min}-${rule.max} digits. Example: ${rule.example}`;
    }
}

function formatPhoneNumberForDisplay(value, rule) {
    const selectedRule = rule || DEFAULT_PHONE_RULE;
    const localDigits = extractLocalPhoneDigits(value, selectedRule);
    return formatPhoneDigits(localDigits, selectedRule);
}

function formatCurrentPhoneValue() {
    const input = document.getElementById("fieldPhone");

    if (!input) {
        return;
    }

    const rule = selectedPhoneRule();
    input.value = formatPhoneDigits(extractLocalPhoneDigits(input.value, rule), rule);
    updatePhoneConstraints();
    updatePhoneHelp();
}

function validatePhoneNumber() {
    const input = document.getElementById("fieldPhone");

    if (!input) {
        return null;
    }

    const rule = selectedPhoneRule();
    const digits = onlyDigits(input.value);

    if (!digits) {
        return null;
    }

    if (rule.min === rule.max && digits.length !== rule.min) {
        return `Phone number for ${rule.country} must contain exactly ${rule.min} digits.`;
    }

    if (digits.length < rule.min || digits.length > rule.max) {
        return `Phone number for ${rule.country} must contain between ${rule.min} and ${rule.max} digits.`;
    }

    return null;
}

function getFullPhoneNumber() {
    const rule = selectedPhoneRule();
    const input = document.getElementById("fieldPhone");

    if (!input) {
        return "";
    }

    const digits = onlyDigits(input.value);

    if (!digits) {
        return "";
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

function boolText(node, tagName) {
    return text(node, tagName).toLowerCase() === "true";
}

function text(node, tagName) {
    const element = node ? node.querySelector(tagName) : null;
    return element ? element.textContent || "" : "";
}

function stripErrorPrefix(value) {
    const textValue = String(value || "").trim();
    return textValue.replace(/^Error occurred\s*:\s*/i, "");
}
