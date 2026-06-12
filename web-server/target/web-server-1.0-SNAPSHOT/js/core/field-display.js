import {
    getDirectChild,
    getDirectText,
    textOf,
    toBool
} from "./xml.js";

export function fieldDisplayValue(node) {
    if (!node) {
        return "";
    }

    const control = fieldControl(node);

    if (control === "select") {
        const currentValue = textOf(node, "Value");
        const options = Array.from(node.getElementsByTagName("Option"));
        const selectedOption =
            options.find((option) => toBool(option.getAttribute("selected"))) ||
            options.find((option) => (option.getAttribute("value") || "") === currentValue);

        return selectedOption ? (selectedOption.textContent || "").trim() : currentValue;
    }

    if (control === "checkbox") {
        return toBool(node.textContent) ? "Yes" : "No";
    }

    return fieldValue(node);
}

export function fieldLabel(node, fallback = "") {
    if (!node) {
        return fallback;
    }

    return node.getAttribute("label") || node.getAttribute("header") || fallback || node.tagName;
}

export function fieldHeader(node, fallback = "") {
    if (!node) {
        return fallback;
    }

    return node.getAttribute("header") || node.getAttribute("label") || fallback || node.tagName;
}

export function fieldControl(node) {
    return String(node?.getAttribute?.("control") || "").toLowerCase();
}

export function fieldVisible(node) {
    return String(node?.getAttribute?.("visible") || "").toLowerCase() !== "false";
}

export function fieldEditable(node) {
    return toBool(node?.getAttribute?.("editable"));
}

export function fieldRequired(node) {
    return toBool(node?.getAttribute?.("required"));
}

export function fieldMinLength(node) {
    const raw = node?.getAttribute?.("minlength") ?? node?.getAttribute?.("minLength");

    if (raw === null || raw === undefined || raw === "") {
        return null;
    }

    const parsed = Number(raw);

    return Number.isFinite(parsed) ? parsed : null;
}

export function fieldMaxLength(node) {
    const raw = node?.getAttribute?.("maxlength") ?? node?.getAttribute?.("maxLength");

    if (raw === null || raw === undefined || raw === "") {
        return null;
    }

    const parsed = Number(raw);

    return Number.isFinite(parsed) ? parsed : null;
}

export function fieldDisplayLength(node) {
    const raw = node?.getAttribute?.("displayLength");

    if (raw === null || raw === undefined || raw === "") {
        return null;
    }

    const parsed = Number(raw);

    return Number.isFinite(parsed) ? parsed : null;
}

export function fieldSize(node) {
    const raw = node?.getAttribute?.("size");

    if (raw === null || raw === undefined || raw === "") {
        return null;
    }

    const parsed = Number(raw);

    return Number.isFinite(parsed) ? parsed : null;
}

export function fieldValue(node) {
    if (!node) {
        return "";
    }

    const control = fieldControl(node);

    if (control === "checkbox") {
        return toBool(node.textContent) ? "true" : "false";
    }

    if (control === "select") {
        const valueNode = getDirectChild(node, "Value");

        if (valueNode) {
            return (valueNode.textContent || "").trim();
        }
    }

    const valueNode = getDirectChild(node, "Value");

    if (valueNode) {
        return valueNode.textContent || "";
    }

    return getDirectText(node).trim();
}

export function isHiddenField(node) {
    return fieldControl(node) === "hidden";
}

export function isCheckboxField(node) {
    return fieldControl(node) === "checkbox";
}

export function isTextareaField(node) {
    return fieldControl(node) === "textarea";
}

export function isSelectField(node) {
    return fieldControl(node) === "select";
}