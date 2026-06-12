import {
    fieldHeader,
    fieldMaxLength,
    fieldMinLength,
    fieldRequired,
    fieldVisible,
    isHiddenField
} from "./field-display.js";

import { cssEscape } from "./css.js";

export function getUiFieldValidationValue(uiField) {
    if (!uiField) {
        return "";
    }

    if (uiField.type === "checkbox") {
        return uiField.checked ? "true" : "";
    }

    if (uiField.tagName?.toLowerCase() === "select") {
        const selectedOption = uiField.selectedOptions?.[0] || null;
        return (selectedOption?.value || "").trim();
    }

    return (uiField.value || "").trim();
}

export function validateFieldElement(fieldNode, uiField) {
    const errors = [];

    if (!fieldNode || !uiField) {
        return errors;
    }

    if (!fieldVisible(fieldNode) || isHiddenField(fieldNode)) {
        return errors;
    }

    const label = fieldHeader(fieldNode, fieldNode.tagName);
    const required = fieldRequired(fieldNode);
    const minLength = fieldMinLength(fieldNode);
    const maxLength = fieldMaxLength(fieldNode);
    const value = getUiFieldValidationValue(uiField);

    if (required && !value) {
        errors.push(`${label} is required.`);
        return errors;
    }

    if (value && uiField.type !== "checkbox") {
        if (minLength !== null && value.length < minLength) {
            errors.push(`${label} must be at least ${minLength} characters.`);
        }

        if (maxLength !== null && value.length > maxLength) {
            errors.push(`${label} must be at most ${maxLength} characters.`);
        }
    }

    return errors;
}

export function clearInvalidFieldMarkers(root = document) {
    root.querySelectorAll?.(".is-invalid").forEach((element) => {
        element.classList.remove("is-invalid");
    });

    root.querySelectorAll?.(".has-validation-error").forEach((element) => {
        element.classList.remove("has-validation-error");
    });
}

export function markInvalidField(uiField) {
    if (!uiField) {
        return;
    }

    uiField.classList.add("is-invalid");

    const fieldContainer = uiField.closest(".page-field");

    if (fieldContainer) {
        fieldContainer.classList.add("has-validation-error");
    }
}

export function focusFirstInvalidField(root = document) {
    const firstInvalidField = root.querySelector?.(".is-invalid");

    if (!firstInvalidField) {
        return;
    }

    if (typeof firstInvalidField.focus === "function") {
        firstInvalidField.focus();
    }
}

export function validateFieldsFromDetailNode(detailNode, fieldsRoot) {
    const errors = [];

    if (!detailNode || !fieldsRoot) {
        return errors;
    }

    clearInvalidFieldMarkers(fieldsRoot);

    Array.from(detailNode.children || []).forEach((fieldNode) => {
        if (!fieldVisible(fieldNode) || isHiddenField(fieldNode)) {
            return;
        }

        const fieldName = fieldNode.tagName;
        const uiField = fieldsRoot.querySelector(`[data-field="${cssEscape(fieldName)}"]`);

        if (!uiField) {
            return;
        }

        const fieldErrors = validateFieldElement(fieldNode, uiField);

        if (fieldErrors.length) {
            markInvalidField(uiField);
            errors.push(...fieldErrors);
        }
    });

    return errors;
}

export function validateRequiredFieldsFromDetailNode(detailNode, fieldsRoot) {
    const errors = [];

    if (!detailNode || !fieldsRoot) {
        return errors;
    }

    clearInvalidFieldMarkers(fieldsRoot);

    Array.from(detailNode.children || []).forEach((fieldNode) => {
        if (!fieldVisible(fieldNode) || isHiddenField(fieldNode) || !fieldRequired(fieldNode)) {
            return;
        }

        const fieldName = fieldNode.tagName;
        const uiField = fieldsRoot.querySelector(`[data-field="${cssEscape(fieldName)}"]`);

        if (!uiField) {
            return;
        }

        const value = getUiFieldValidationValue(uiField);

        if (!value) {
            markInvalidField(uiField);
            errors.push(`${fieldHeader(fieldNode, fieldName)} is required.`);
        }
    });

    return errors;
}