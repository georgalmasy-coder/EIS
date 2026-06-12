import { hide, show } from "./dom.js";

function resetDisplay(element) {
    if (!element) {
        return;
    }

    element.hidden = false;
    element.style.removeProperty("display");
}

function applyPlaceholderState(element, message, options = {}) {
    if (!element) {
        return;
    }

    const {
        asHtml = false,
        className = "",
        replaceClassName = false,
        visibleClass = ""
    } = options;

    resetDisplay(element);

    if (replaceClassName && className) {
        element.className = className;
    } else if (className) {
        element.classList.add(className);
    }

    if (visibleClass) {
        element.classList.add(visibleClass);
    }

    if (asHtml) {
        element.innerHTML = message;
        return;
    }

    element.textContent = message;
}

export function setLoadingState(element, message, options = {}) {
    applyPlaceholderState(element, message, options);
}

export function setErrorState(element, message, options = {}) {
    applyPlaceholderState(element, message, options);
}

export function setEmptyState(element, message, options = {}) {
    applyPlaceholderState(element, message, options);
}

export function clearState(element) {
    if (!element) {
        return;
    }

    element.textContent = "";
    element.innerHTML = "";
}

export function hideState(element) {
    if (!element) {
        return;
    }

    element.hidden = true;
    hide(element);
}

export function showState(element) {
    if (!element) {
        return;
    }

    element.hidden = false;
    show(element);
}