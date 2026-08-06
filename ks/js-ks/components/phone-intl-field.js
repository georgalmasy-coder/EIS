import { escapeHtml } from "../core/html.js";

const PHONE_COUNTRIES = ["dk", "se", "no", "de", "us", "gb", "fr", "nl", "be", "es", "it", "fi", "pl", "pt", "ch", "at", "ie", "is"];
const PHONE_INSTANCE_KEY = "__eisIntlPhoneInstance";

export function renderPhoneFieldMarkup({
    fieldName,
    label,
    value = "",
    requiredStar = "",
    readonlyAttr = "",
    disabledAttr = "",
    requiredAttr = "",
    containerClass = "page-field phone-field",
    fieldDataAttrs = "",
    inputId = "fieldPhone",
    helpId = "fieldPhoneHelp",
    helpText = "Country code is included automatically."
} = {}) {
    const safeFieldName = escapeHtml(fieldName || "");
    const safeLabel = escapeHtml(label || "");

    return `
        <div class="${escapeHtml(containerClass)}" data-phone-field="true" data-phone-field-name="${safeFieldName}" ${fieldDataAttrs}>
            <label for="${escapeHtml(inputId)}">${safeLabel}${requiredStar}</label>
            <input
                id="${escapeHtml(inputId)}"
                data-field="${safeFieldName}"
                data-phone-input="true"
                type="tel"
                inputmode="tel"
                autocomplete="tel"
                value="${escapeHtml(value)}"
                ${readonlyAttr}
                ${disabledAttr}
                ${requiredAttr}
            />
            <p class="field-help" id="${escapeHtml(helpId)}">${escapeHtml(helpText)}</p>
        </div>
    `;
}

export function initPhoneField(inputElement, options = {}) {
    if (!inputElement || !window.intlTelInput) {
        return null;
    }

    const existing = inputElement[PHONE_INSTANCE_KEY];

    if (existing) {
        return existing;
    }

    const iti = window.intlTelInput(inputElement, {
        initialCountry: String(options.initialCountry || "dk").toLowerCase() || "dk",
        preferredCountries: Array.isArray(options.preferredCountries) && options.preferredCountries.length
            ? options.preferredCountries
            : PHONE_COUNTRIES,
        separateDialCode: true,
        loadUtils: () => import("https://cdn.jsdelivr.net/npm/intl-tel-input@25.12.2/build/js/utils.js")
    });

    inputElement[PHONE_INSTANCE_KEY] = iti;

    const existingValue = String(options.value != null ? options.value : inputElement.value || "").trim();

    if (existingValue) {
        try {
            iti.setNumber(existingValue);
        } catch (_error) {
            inputElement.value = existingValue;
        }
    }

    if (typeof options.onCountryChange === "function") {
        inputElement.addEventListener("countrychange", () => options.onCountryChange(iti));
    }

    if (typeof options.onInput === "function") {
        inputElement.addEventListener("input", () => options.onInput(iti));
    }

    if (typeof options.onReady === "function") {
        queueMicrotask(() => options.onReady(iti));
    }

    return iti;
}

export function getPhoneInput(inputOrId = "fieldPhone") {
    return resolveInput(inputOrId);
}

export function getPhoneInstance(inputOrId = "fieldPhone") {
    const input = resolveInput(inputOrId);
    return input ? input[PHONE_INSTANCE_KEY] || null : null;
}

export function setPhoneValue(inputOrId = "fieldPhone", value = "") {
    const input = resolveInput(inputOrId);
    const iti = input ? input[PHONE_INSTANCE_KEY] || null : null;

    if (!input) {
        return;
    }

    if (iti) {
        try {
            iti.setNumber(String(value || "").trim());
            return;
        } catch (_error) {
            // Fall back to raw value.
        }
    }

    input.value = String(value || "");
}

export function getPhoneValue(inputOrId = "fieldPhone") {
    const input = resolveInput(inputOrId);
    if (!input) {
        return "";
    }

    const iti = input[PHONE_INSTANCE_KEY] || null;

    if (iti?.getNumber) {
        const number = String(iti.getNumber() || "").trim();
        if (number) {
            return number;
        }
    }

    return String(input.value || "").trim();
}

export function validatePhoneNumber(inputOrId = "fieldPhone") {
    const input = resolveInput(inputOrId);

    if (!input || !String(input.value || "").trim()) {
        return null;
    }

    const iti = input[PHONE_INSTANCE_KEY] || null;

    if (iti?.isValidNumber && !iti.isValidNumber()) {
        return "Phone number is invalid.";
    }

    return null;
}

export function updatePhoneHelp(helpOrId = "fieldPhoneHelp", inputOrId = "fieldPhone") {
    const help = resolveElement(helpOrId);
    if (!help) {
        return;
    }

    const iti = getPhoneInstance(inputOrId);
    const countryData = iti?.getSelectedCountryData?.() || null;
    const countryName = String(countryData?.name || "").trim();
    const dialCode = String(countryData?.dialCode || "").trim();

    if (countryName && dialCode) {
        help.textContent = `${countryName} +${dialCode} is included automatically.`;
        return;
    }

    help.textContent = "Country code is included automatically.";
}

export function formatCurrentPhoneValue(inputOrId = "fieldPhone") {
    const input = resolveInput(inputOrId);
    const iti = input ? input[PHONE_INSTANCE_KEY] || null : null;

    if (!input || !iti) {
        return;
    }

    const currentValue = String(input.value || "").trim();

    if (!currentValue) {
        return;
    }

    try {
        iti.setNumber(currentValue);
    } catch (_error) {
        // Leave the value as-is.
    }
}

export function phonePatternForRule() {
    return "^\\d[\\d\\s().-]{3,}$";
}

export function phoneTitleForRule() {
    return "Phone number";
}

export function normalizePhoneNumber(value) {
    return String(value == null ? "" : value)
        .trim()
        .replace(/[()\s.-]/g, "");
}

export function onlyDigits(value) {
    return String(value == null ? "" : value).replace(/\D/g, "");
}

export function renderPhoneValueForDisplay(value) {
    return normalizePhoneNumber(value);
}

export function fillPhoneCountryCodes() {
    return;
}

export function selectedPhoneRule() {
    return null;
}

export function applyPhoneConstraints(inputOrId = "fieldPhone") {
    const input = resolveInput(inputOrId);

    if (!input) {
        return;
    }

    input.setAttribute("inputmode", "tel");
    input.setAttribute("autocomplete", "tel");
    input.setAttribute("type", "tel");
}

export function formatPhoneNumberForDisplay(value) {
    return renderPhoneValueForDisplay(value);
}

export function getFullPhoneNumber(inputOrId = "fieldPhone") {
    return getPhoneValue(inputOrId);
}

export function getPhoneCountryRules() {
    return [];
}

export function getPhoneRuleForValue() {
    return null;
}

function resolveInput(inputOrId) {
    if (!inputOrId) {
        return document.getElementById("fieldPhone");
    }

    if (typeof inputOrId === "string") {
        return document.getElementById(inputOrId);
    }

    return inputOrId instanceof HTMLInputElement ? inputOrId : null;
}

function resolveElement(elementOrId) {
    if (!elementOrId) {
        return document.getElementById("fieldPhoneHelp");
    }

    if (typeof elementOrId === "string") {
        return document.getElementById(elementOrId);
    }

    return elementOrId instanceof HTMLElement ? elementOrId : null;
}
