import {
    fieldDisplayLength,
    fieldEditable,
    fieldLabel,
    fieldRequired,
    fieldVisible,
    fieldControl,
    fieldMaxLength,
    fieldMinLength
} from "../../core/field-display.js";
import {
    toDateInputValue,
    toDateTimeLocalValue,
    toTimeInputValue
} from "../../core/date.js";
import { getDirectChild } from "../../core/xml.js";
import { isTruthy } from "../../core/utils.js";

function isVisibleField(node) {
    return fieldVisible(node) && fieldControl(node) !== "hidden";
}

function getDisplayLength(node, fallback) {
    const parsed = fieldDisplayLength(node);

    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function applyControlWidth(control, node) {
    const controlTag = control.tagName;
    const controlType = fieldControl(node);

    if (controlType === "checkbox" || controlType === "radio") {
        control.style.width = "auto";
        control.style.maxWidth = "none";
        return;
    }

    if (controlTag === "TEXTAREA") {
        const cols = Number(node.getAttribute("cols"));
        const width = Number.isFinite(cols) && cols > 0 ? cols : getDisplayLength(node, 35);
        control.style.width = width + "ch";
        control.style.maxWidth = "100%";
        return;
    }

    const width = getDisplayLength(node, controlTag === "SELECT" ? 15 : 20);
    control.style.width = width + "ch";
    control.style.maxWidth = "100%";
}

function applyCommonFieldAttributes(control, node) {
    const editable = fieldEditable(node);
    const required = fieldRequired(node);
    const maxLength = fieldMaxLength(node);
    const minLength = fieldMinLength(node);

    control.name = node.tagName;
    control.id = "projectField_" + node.tagName;

    if (!editable) {
        control.disabled = true;
    }

    if (required) {
        control.required = true;
    }

    if (maxLength !== null && "maxLength" in control) {
        control.maxLength = maxLength;
    }

    if (minLength !== null && "minLength" in control) {
        control.minLength = minLength;
    }

    applyControlWidth(control, node);

    if (control.tagName === "INPUT" || control.tagName === "SELECT" || control.tagName === "TEXTAREA") {
        control.classList.add("dlg-input");
    }
}

function createProjectFieldControl(node) {
    const controlType = fieldControl(node) || "text";
    const rawValue = (node.textContent || "").trim();

    let control;

    switch (controlType) {
        case "textarea": {
            control = document.createElement("textarea");
            control.value = rawValue;
            control.rows = Number(node.getAttribute("rows") || "4");

            if (node.getAttribute("cols")) {
                control.cols = Number(node.getAttribute("cols"));
            }

            applyCommonFieldAttributes(control, node);
            return control;
        }

        case "select": {
            control = document.createElement("select");
            applyCommonFieldAttributes(control, node);

            const currentValue = getDirectChild(node, "Value")?.textContent?.trim() || "";
            const options = Array.from(node.getElementsByTagName("Option"));

            options.forEach((optionNode) => {
                const option = document.createElement("option");
                option.value = optionNode.getAttribute("value") || "";
                option.textContent = (optionNode.textContent || "").trim();

                if (isTruthy(optionNode.getAttribute("selected")) || option.value === currentValue) {
                    option.selected = true;
                }

                control.appendChild(option);
            });

            return control;
        }

        case "checkbox": {
            control = document.createElement("input");
            control.type = "checkbox";
            control.checked = isTruthy(rawValue);

            applyCommonFieldAttributes(control, node);
            control.classList.remove("dlg-input");
            control.classList.add("project-checkbox");
            return control;
        }

        case "number": {
            control = document.createElement("input");
            control.type = "number";
            control.value = rawValue;

            applyCommonFieldAttributes(control, node);
            return control;
        }

        case "decimal": {
            control = document.createElement("input");
            control.type = "number";
            control.step = "0.01";
            control.value = rawValue;

            applyCommonFieldAttributes(control, node);
            return control;
        }

        case "date": {
            control = document.createElement("input");
            control.type = "date";
            control.value = toDateInputValue(rawValue);

            applyCommonFieldAttributes(control, node);

            if (!control.value && rawValue) {
                control.type = "text";
                control.value = rawValue;
            }

            return control;
        }

        case "datetime": {
            control = document.createElement("input");
            control.type = "datetime-local";
            control.step = "1";
            control.value = toDateTimeLocalValue(rawValue);

            applyCommonFieldAttributes(control, node);

            if (!control.value && rawValue) {
                control.type = "text";
                control.value = rawValue;
            }

            return control;
        }

        case "time": {
            control = document.createElement("input");
            control.type = "time";
            control.value = toTimeInputValue(rawValue);

            applyCommonFieldAttributes(control, node);

            if (!control.value && rawValue) {
                control.type = "text";
                control.value = rawValue;
            }

            return control;
        }

        case "radio": {
            control = document.createElement("input");
            control.type = "radio";
            control.checked = isTruthy(rawValue);

            applyCommonFieldAttributes(control, node);
            control.classList.remove("dlg-input");
            control.classList.add("project-checkbox");
            return control;
        }

        case "text":
        default: {
            control = document.createElement("input");
            control.type = "text";
            control.value = rawValue;

            applyCommonFieldAttributes(control, node);
            return control;
        }
    }
}

export function renderOverviewProjectFields(projectNode, projectFieldsElement) {
    if (!projectFieldsElement) {
        return;
    }

    projectFieldsElement.innerHTML = "";

    if (!projectNode) {
        projectFieldsElement.innerHTML = '<div class="empty">No project data found.</div>';
        return;
    }

    const visibleFields = Array.from(projectNode.children).filter(isVisibleField);

    if (!visibleFields.length) {
        projectFieldsElement.innerHTML = '<div class="empty">No visible project fields found.</div>';
        return;
    }

    visibleFields.forEach((field) => {
        const label = document.createElement("label");
        label.className = "detail-label";
        label.setAttribute("for", "projectField_" + field.tagName);
        label.textContent = fieldLabel(field);

        const valueWrap = document.createElement("div");
        valueWrap.className = "detail-value";

        const control = createProjectFieldControl(field);
        valueWrap.appendChild(control);

        projectFieldsElement.append(label, valueWrap);
    });
}
