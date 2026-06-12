export function byId(id) {
    return document.getElementById(id);
}

export function resolveElement(target) {
    if (!target) {
        return null;
    }

    if (typeof target === "string") {
        return document.getElementById(target);
    }

    return target;
}

export function setText(target, value, fallback = "—") {
    const element = resolveElement(target);

    if (!element) {
        return;
    }

    const normalized = String(value ?? "").trim();
    element.textContent = normalized || fallback;
}

export function setInputValue(target, value, fallback = "") {
    const element = resolveElement(target);

    if (!element) {
        return;
    }

    element.value = value ?? fallback;
}

export function show(target, displayValue = "block") {
    const element = resolveElement(target);

    if (element) {
        element.style.display = displayValue;
    }
}

export function hide(target) {
    const element = resolveElement(target);

    if (element) {
        element.style.display = "none";
    }
}

export function clear(target) {
    const element = resolveElement(target);

    if (element) {
        element.innerHTML = "";
    }
}

export function clearChildren(target) {
    const element = resolveElement(target);

    if (!element) {
        return;
    }

    while (element.firstChild) {
        element.removeChild(element.firstChild);
    }
}

export function createCell(tagName, text, className = "") {
    const cell = document.createElement(tagName);
    cell.textContent = text;

    if (className) {
        cell.className = className;
    }

    return cell;
}

export function showDialog(target) {
    const dialog = resolveElement(target);

    if (!dialog) {
        return;
    }

    if (typeof dialog.showModal === "function") {
        dialog.showModal();
    } else {
        dialog.setAttribute("open", "open");
    }
}

export function closeDialogElement(target) {
    const dialog = resolveElement(target);

    if (!dialog) {
        return;
    }

    if (dialog.open && typeof dialog.close === "function") {
        dialog.close();
    } else {
        dialog.removeAttribute("open");
    }
}

export function focusElement(target) {
    const element = resolveElement(target);

    if (!element || typeof element.focus !== "function") {
        return;
    }

    element.focus();
}

export function focusFirst(selector, root = document) {
    if (!selector) {
        return;
    }

    const element = root?.querySelector?.(selector);

    if (element && typeof element.focus === "function") {
        element.focus();
    }
}

export function addClass(target, className) {
    const element = resolveElement(target);

    if (element && className) {
        element.classList.add(className);
    }
}

export function removeClass(target, className) {
    const element = resolveElement(target);

    if (element && className) {
        element.classList.remove(className);
    }
}

export function toggleClass(target, className, force) {
    const element = resolveElement(target);

    if (element && className) {
        element.classList.toggle(className, force);
    }
}