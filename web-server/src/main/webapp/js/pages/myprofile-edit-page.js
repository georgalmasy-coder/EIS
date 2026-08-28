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
import { applyTopPanel as applyPageHeader, parseTopPanel as parsePageTopPanel } from "../core/page-header.js";
import { setText } from "../core/dom.js";
import { escapeHtml } from "../core/html.js";
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
import {
    formatCurrentPhoneValue as syncIntlPhoneFieldValue,
    getFullPhoneNumber as getIntlPhoneNumber,
    initPhoneField,
    renderPhoneFieldMarkup,
    updatePhoneHelp as updateIntlPhoneHelp,
    validatePhoneNumber as validateIntlPhoneNumber
} from "../components/phone-intl-field.js";

const DETAIL_URL = "/api/myprofile";
const SAVE_URL = "/api/myprofile?cmd=save";
const DEFAULT_RETURN_URL = "/web/view?page=overview";
const EXCLUDED_FIELDS = new Set(["Active", "LockedUntil"]);
const READ_ONLY_FIELDS = new Set(["Email", "UserEmail", "Role", "UserRole"]);

const state = {
    returnUrl: DEFAULT_RETURN_URL,
    modal: false,
    currentDoc: null,
    userNode: null,
    topPanel: {
        customerName: "-",
        projectName: "-",
        userName: "-",
        workspaceEyebrow: "",
        workspaceHeading: "",
        workspaceHelpText: ""
    },
    user: {
        phone: ""
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

    mountTopbar(document);
    setText("customerName", "-", "");
    setText("projectName", "-", "");
    setText("userName", "-", "");
    setText("loadStatus", "Loading", "");

    if (!state.modal) {
        initMenu(document);
    }

    initHelpDialog();
}

function initializeTabs() {
    initTabs([
        { btnId: "tabBtnBasis", panelId: "tabPanelBasis" }
    ]);
}

function initializeRouteState() {
    const params = new URLSearchParams(window.location.search);
    const dialogContext = getEditDialogPageContext();

    state.returnUrl = params.get("returnUrl") || DEFAULT_RETURN_URL;
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
        await saveCurrentProfile();
    });

    document.addEventListener("input", (event) => {
        const target = event.target;

        if (target instanceof HTMLElement && target.id === "fieldPhone") {
            syncIntlPhoneFieldValue(target);
            updatePhoneHelp();
        }
    });
}

async function loadDetail() {
    setText("loadStatus", "Loading", "");
    setText("dlgStatus", "Loading profile...");

    try {
        const response = await fetch(`${DETAIL_URL}?cmd=edit`, {
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
            throw new Error("The profile endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.topPanel = parseTopPanel(xmlDocument);
        state.userNode = findUserNode(xmlDocument);
        state.user = parseUserNode(state.userNode);

        applyTopPanel();
        renderDetailFields();
        applyUserToForm();
        applyModeUi();

        setText("loadStatus", "Loaded", "");
        setText("dlgStatus", "Loaded.");
    } catch (error) {
        console.error("Failed to load profile detail", error);
        setText("loadStatus", "Error", "");
        setText("dlgStatus", `Could not load profile. ${error.message}`);
    }
}

function applyModeUi() {
    const saveButton = document.getElementById("btnSave");

    if (saveButton) {
        saveButton.textContent = "Save";
        saveButton.disabled = state.saving;
    }
}

function parseTopPanel(xmlDocument) {
    return parsePageTopPanel(xmlDocument);
}

function applyTopPanel() {
    applyPageHeader(state.topPanel, {
        customerName: "customerName",
        projectName: "projectName",
        userName: "userName",
        workspaceEyebrow: "pageEyebrow",
        workspaceHeading: "pageHeading",
        workspaceHelpText: "pageHelpText"
    });
}

function findUserNode(doc) {
    return doc?.querySelector("userDocument > user")
        || doc?.querySelector("userDocument user")
        || doc?.querySelector("user")
        || null;
}

function parseUserNode(node) {
    if (!node) {
        return {
            phone: ""
        };
    }

    return {
        phone: text(node, "UserPhone") || text(node, "Phone")
    };
}

function renderDetailFields() {
    const basisFields = document.getElementById("basisInfoFields");

    if (!basisFields) {
        return;
    }

    if (!state.userNode) {
        basisFields.innerHTML = '<div class="page-empty">No user XML returned.</div>';
        return;
    }

    basisFields.innerHTML = Array.from(state.userNode.children || [])
        .filter((field) => !EXCLUDED_FIELDS.has(field.tagName))
        .map(renderBasisInfoFieldMarkup)
        .join("");
}

function renderBasisInfoFieldMarkup(field) {
    const name = field.tagName;
    const label = fieldHeader(field, name);
    const control = fieldControl(field);
    const editable = fieldEditable(field) && !READ_ONLY_FIELDS.has(name);
    const visible = fieldVisible(field);
    const required = fieldRequired(field);
    const value = getFieldUiValue(field);
    const requiredStar = required ? '<span class="field-required" aria-hidden="true">*</span>' : "";
    const escapedName = escapeHtml(name);
    const escapedLabel = escapeHtml(label);

    if (!visible || control === "hidden") {
        return `<input type="hidden" data-field="${escapedName}" id="fld-${escapedName}" value="${escapeHtml(value)}">`;
    }

    if (control === "phone" || name === "Phone" || name === "UserPhone") {
        return renderPhoneFieldMarkup({
            fieldName: escapedName,
            label: escapedLabel,
            value,
            requiredStar,
            readonlyAttr: editable ? "" : "readonly",
            disabledAttr: editable ? "" : "disabled",
            requiredAttr: required ? "required" : "",
            containerClass: "page-field user-edit-phone-field phone-field"
        });
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

async function saveCurrentProfile() {
    if (state.saving) {
        return;
    }

    const validationErrors = validateCurrentProfile();

    if (validationErrors.length) {
        setText("dlgStatus", "Validation failed.");
        setText("loadStatus", "Validation error", "");
        window.alert(validationErrors.join("\n"));
        focusFirstInvalidField(document.getElementById("myProfileEditFields") || document);
        return;
    }

    state.saving = true;
    setSaveButtonDisabled(true);

    try {
        setText("dlgStatus", "Saving...");
        setText("loadStatus", "Saving", "");

        const response = await fetch(SAVE_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/xml; charset=UTF-8",
                Accept: "application/xml,text/xml,*/*"
            },
            body: buildSavePayload(),
            cache: "no-store",
            credentials: "same-origin"
        });

        const responseText = await response.text();

        if (!response.ok) {
            throw new Error(stripErrorPrefix(responseText) || `HTTP ${response.status} ${response.statusText}`);
        }

        setText("dlgStatus", "Saved.");
        setText("loadStatus", "Saved", "");

        if (state.modal && notifyEditDialogSaved({
            mode: "edit"
        })) {
            return;
        }

        await loadDetail();
        setText("dlgStatus", "Saved.");
    } catch (error) {
        console.error("Failed to save profile", error);
        const message = error.message || "Save failed.";
        setText("dlgStatus", `Save failed. ${message}`);
        setText("loadStatus", "Error", "");
        window.alert(message);
    } finally {
        state.saving = false;
        setSaveButtonDisabled(false);
    }
}

function validateCurrentProfile() {
    const errors = validateFieldsFromDetailNode(
        state.userNode,
        document.getElementById("myProfileEditFields") || document
    );

    const phoneError = validateIntlPhoneNumber(document.getElementById("fieldPhone"));
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

    if (!userNode) {
        throw new Error("No user XML returned.");
    }

    Array.from(userNode.children || []).forEach((field) => {
        if (EXCLUDED_FIELDS.has(field.tagName)) {
            field.remove();
        }
    });

    const fieldsRoot = document.getElementById("myProfileEditFields") || document;
    const fields = Array.from(fieldsRoot.querySelectorAll("[data-field]"));

    fields.forEach((uiField) => {
        const name = uiField.getAttribute("data-field");

        if (!name || EXCLUDED_FIELDS.has(name)) {
            return;
        }

        const child = ensureChild(updatedDoc, userNode, name);
        const control = fieldControl(child);

        if (control === "phone" || name === "Phone" || name === "UserPhone") {
            child.textContent = getIntlPhoneNumber(document.getElementById("fieldPhone"));
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
                option.removeAttribute("selected");

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

    return serializeXml(updatedDoc);
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

function applyUserToForm() {
    const phoneInput = document.getElementById("fieldPhone");

    if (!phoneInput) {
        return;
    }

    phoneInput.value = state.user?.phone == null ? "" : String(state.user.phone);
    initPhoneField(phoneInput, {
        onCountryChange: () => updatePhoneHelp(),
        onInput: () => updatePhoneHelp()
    });
    updatePhoneHelp();
}

function updatePhoneHelp() {
    updateIntlPhoneHelp(document.getElementById("fieldPhoneHelp"), document.getElementById("fieldPhone"));
}

function returnToPreviousPage() {
    if (state.modal && closeEditDialog("cancel")) {
        return;
    }

    window.location.href = state.returnUrl || DEFAULT_RETURN_URL;
}

function setSaveButtonDisabled(disabled) {
    const saveButton = document.getElementById("btnSave");

    if (saveButton) {
        saveButton.disabled = Boolean(disabled);
    }
}

function text(node, tagName) {
    return getChildText(node, tagName, "");
}

function stripErrorPrefix(value) {
    const textValue = String(value || "").trim();
    return textValue.replace(/^Error occurred\s*:\s*/i, "");
}
