import { escapeHtml } from "../core/html.js";

export const PHONE_RULES = [
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

export const DEFAULT_PHONE_RULE = PHONE_RULES[0];

export function getPhoneCountryRules(lookupRules, fallbackRules = PHONE_RULES) {
    const source = Array.isArray(lookupRules) && lookupRules.length ? lookupRules : fallbackRules;

    return source
        .map((rule) => ({
            country: String(rule.country || rule.label || "").trim(),
            code: String(rule.code || "").trim(),
            min: Number.isFinite(rule.min) ? rule.min : DEFAULT_PHONE_RULE.min,
            max: Number.isFinite(rule.max) ? rule.max : DEFAULT_PHONE_RULE.max,
            example: String(rule.example || "").trim()
        }))
        .filter((rule) => rule.code);
}

export function getPhoneRuleByCode(code, rules = PHONE_RULES) {
    const normalizedCode = String(code == null ? "" : code).trim();

    return rules.find((rule) => rule.code === normalizedCode) || null;
}

export function getPhoneRuleByCountry(country, rules = PHONE_RULES) {
    const normalizedCountry = normalizeCountryCode(country);

    return rules.find((rule) => normalizeCountryCode(rule.country) === normalizedCountry) || null;
}

export function getPhoneRuleForValue(phone, rules = PHONE_RULES, fallback = rules[0] || DEFAULT_PHONE_RULE) {
    const normalizedPhone = normalizePhoneNumber(phone);

    if (!normalizedPhone) {
        return resolveFallbackRule(fallback, rules);
    }

    const prefixMatches = rules.filter((rule) => normalizedPhone.startsWith(rule.code));

    if (prefixMatches.length) {
        return prefixMatches[0];
    }

    return resolveFallbackRule(fallback, rules);
}

export function renderPhoneFieldMarkup(options) {
    const {
        fieldName,
        label,
        value,
        requiredStar = "",
        readonlyAttr = "",
        disabledAttr = "",
        requiredAttr = "",
        rules = [],
        selectedRule = null,
        containerClass = "page-field",
        controlsClass = "",
        countryClass = "",
        inputClass = "",
        fieldDataAttrs = "",
        selectId = "fieldPhoneCountryCode",
        inputId = "fieldPhone",
        helpId = "fieldPhoneHelp",
        helpText = "Select a country code and enter the local phone number.",
        showHelp = true,
        hideLabel = false,
        isReadOnly = false
    } = options || {};

    const safeRules = Array.isArray(rules) ? rules : [];
    const currentRule = selectedRule || safeRules[0] || null;
    const displayValue = formatPhoneNumberForDisplay(value, currentRule);
    const escapedName = escapeHtml(fieldName || "");
    const escapedLabel = escapeHtml(label || "");
    const hiddenLabelClass = hideLabel ? "visually-hidden" : "";
    const selectDisabledAttr = isReadOnly ? "disabled" : disabledAttr;
    const inputReadonlyAttr = isReadOnly ? "readonly" : readonlyAttr;
    const selectOptions = safeRules
        .slice()
        .sort((left, right) => String(left.country || "").localeCompare(String(right.country || "")))
        .map((rule) => {
            const selected = currentRule && rule.code === currentRule.code && rule.country === currentRule.country ? " selected" : "";
            return `<option value="${escapeHtml(rule.code)}" data-country="${escapeHtml(rule.country)}" data-min="${escapeHtml(rule.min)}" data-max="${escapeHtml(rule.max)}" data-example="${escapeHtml(rule.example)}"${selected}>${escapeHtml(`${rule.country} (${rule.code})`)}</option>`;
        }).join("");

    if (!safeRules.length) {
        return `
            <div class="${escapeHtml(containerClass)}">
                <label for="${escapeHtml(inputId)}">${escapedLabel}${requiredStar}</label>
                <input id="${escapeHtml(inputId)}" data-field="${escapedName}" ${fieldDataAttrs} type="tel" inputmode="tel" autocomplete="tel" value="${escapeHtml(displayValue)}" ${inputReadonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    return `
        <div class="${escapeHtml(containerClass)}" ${fieldDataAttrs}>
            <div class="${escapeHtml(controlsClass)}">
                <div class="${escapeHtml(countryClass)}">
                    <label for="${escapeHtml(selectId)}">${escapedLabel}${requiredStar}</label>
                    <select id="${escapeHtml(selectId)}" aria-label="Country code" title="Country code" ${selectDisabledAttr}>
                        ${selectOptions}
                    </select>
                </div>

                <div class="${escapeHtml(inputClass)}">
                    <label class="${hiddenLabelClass}" for="${escapeHtml(inputId)}">${escapedLabel}</label>
                    <input id="${escapeHtml(inputId)}" data-field="${escapedName}" data-phone-raw="${escapeHtml(value)}" type="tel" inputmode="tel" autocomplete="tel" value="${escapeHtml(displayValue)}" ${inputReadonlyAttr} ${requiredAttr} />
                </div>
            </div>
            ${showHelp ? `<p class="field-help" id="${escapeHtml(helpId)}">${escapeHtml(helpText)}</p>` : ""}
        </div>
    `;
}

export function fillPhoneCountryCodes(selectElement, rules, selectedRule) {
    if (!selectElement) {
        return;
    }

    const safeRules = Array.isArray(rules) ? rules : [];
    const currentRule = selectedRule || safeRules[0] || null;

    selectElement.innerHTML = safeRules
        .slice()
        .sort((left, right) => String(left.country || "").localeCompare(String(right.country || "")))
        .map((rule) => {
            const selected = currentRule && rule.code === currentRule.code && rule.country === currentRule.country ? " selected" : "";
            return `<option value="${escapeHtml(rule.code)}" data-country="${escapeHtml(rule.country)}" data-min="${escapeHtml(rule.min)}" data-max="${escapeHtml(rule.max)}" data-example="${escapeHtml(rule.example)}"${selected}>${escapeHtml(`${rule.country} (${rule.code})`)}</option>`;
        }).join("");
}

export function selectedPhoneRule(selectElement, rules = PHONE_RULES, fallback = rules[0] || DEFAULT_PHONE_RULE) {
    if (!selectElement) {
        return resolveFallbackRule(fallback, rules);
    }

    const selectedOption = selectElement.options?.[selectElement.selectedIndex];

    if (!selectedOption) {
        return resolveFallbackRule(fallback, rules);
    }

    const code = String(selectedOption.value || "").trim();
    const country = String(selectedOption.getAttribute("data-country") || "").trim();

    return getPhoneRuleByCodeAndCountry(code, country, rules) || resolveFallbackRule(fallback, rules);
}

export function applyPhoneConstraints(inputElement, rule) {
    if (!inputElement) {
        return;
    }

    if (!rule) {
        inputElement.removeAttribute("placeholder");
        inputElement.removeAttribute("pattern");
        inputElement.removeAttribute("title");
        return;
    }

    inputElement.setAttribute("inputmode", "tel");
    inputElement.setAttribute("autocomplete", "tel");
    inputElement.setAttribute("type", "tel");
    inputElement.setAttribute("placeholder", rule.example || "");
    inputElement.setAttribute("pattern", phonePatternForRule(rule));
    inputElement.setAttribute("title", phoneTitleForRule(rule));
    inputElement.maxLength = Math.max((rule.max || 15) + 6, 15);
}

export function applyPhoneHelp(helpElement, rule) {
    if (!helpElement) {
        return;
    }

    if (!rule) {
        helpElement.textContent = "Select a country code and enter the local phone number.";
        return;
    }

    if (rule.min === rule.max) {
        helpElement.textContent = `${rule.country}: exactly ${rule.min} digits. Example: ${rule.example}`;
    } else {
        helpElement.textContent = `${rule.country}: ${rule.min}-${rule.max} digits. Example: ${rule.example}`;
    }
}

export function formatCurrentPhoneValue(inputElement, rule) {
    if (!inputElement || !rule) {
        return "";
    }

    const digits = extractLocalPhoneDigits(inputElement.value, rule);
    const formatted = formatPhoneDigits(digits, rule);
    inputElement.value = formatted;
    return formatted;
}

export function formatPhoneNumberForDisplay(value, rule) {
    const localDigits = extractLocalPhoneDigits(value, rule);
    return formatPhoneDigits(localDigits, rule);
}

export function extractLocalPhoneDigits(value, rule) {
    const normalized = normalizePhoneNumber(value);

    if (!normalized) {
        return "";
    }

    if (rule?.code && normalized.startsWith(rule.code)) {
        return onlyDigits(normalized.slice(rule.code.length));
    }

    return onlyDigits(normalized);
}

export function formatPhoneDigits(digits, rule) {
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

export function validatePhoneNumber(inputElement, rule) {
    if (!inputElement || !rule) {
        return null;
    }

    const digits = onlyDigits(inputElement.value);

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

export function getFullPhoneNumber(inputElement, rule) {
    if (!inputElement || !rule) {
        return "";
    }

    const digits = onlyDigits(inputElement.value);

    if (!digits) {
        return "";
    }

    return `${rule.code} ${formatPhoneDigits(digits, rule)}`.trim();
}

export function phonePatternForRule(_rule) {
    return "^\\d[\\d\\s().-]{3,}$";
}

export function phoneTitleForRule(rule) {
    if (!rule || !rule.country) {
        return "Phone number";
    }

    return `Phone number for ${rule.country}`;
}

export function normalizePhoneNumber(value) {
    return String(value == null ? "" : value)
        .trim()
        .replace(/[()\s.-]/g, "");
}

export function onlyDigits(value) {
    return String(value == null ? "" : value).replace(/\D/g, "");
}

function resolveFallbackRule(fallback, rules) {
    if (!fallback) {
        return rules[0] || DEFAULT_PHONE_RULE;
    }

    if (typeof fallback === "string") {
        return getPhoneRuleByCountry(fallback, rules)
            || getPhoneRuleByCode(fallback, rules)
            || rules[0]
            || DEFAULT_PHONE_RULE;
    }

    if (fallback.code) {
        return getPhoneRuleByCodeAndCountry(fallback.code, fallback.country, rules)
            || getPhoneRuleByCode(fallback.code, rules)
            || rules[0]
            || DEFAULT_PHONE_RULE;
    }

    return rules[0] || DEFAULT_PHONE_RULE;
}

function getPhoneRuleByCodeAndCountry(code, country, rules) {
    return rules.find((rule) => rule.code === code && rule.country === country) || null;
}

function normalizeCountryCode(value) {
    return String(value == null ? "" : value).trim().toUpperCase();
}
