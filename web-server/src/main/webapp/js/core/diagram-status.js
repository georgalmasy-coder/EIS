import { userPreferences } from "./user-preferences.js";
import { escapeHtml } from "./html.js";

export const DIAGRAM_STATUS_PALETTES = [
    [
        "#fde68a", "#86efac", "#93c5fd", "#fca5a5", "#c4b5fd", "#67e8f9", "#fdba74",
        "#bef264", "#f9a8d4", "#d9f99d", "#a5f3fc", "#fcd34d", "#bbf7d0", "#ddd6fe"
    ],
    [
        "#fcd34d", "#7dd3fc", "#a7f3d0", "#f0abfc", "#fb7185", "#c084fc", "#5eead4",
        "#fbbf24", "#a3e635", "#e879f9", "#bfdbfe", "#fecaca", "#ccfbf1", "#e9d5ff"
    ]
];

export function getStatusFieldOptions(items) {
    const byName = new Map();

    Array.from(items || []).forEach((item) => {
        Array.from(item?.fields || []).forEach((field) => {
            if (!field || byName.has(field.name) || field.control !== "select") {
                return;
            }

            byName.set(field.name, {
                key: field.name,
                label: field.label || field.name,
                displayOrder: Number.isFinite(field.displayOrder) ? field.displayOrder : field.originalIndex,
                originalIndex: Number.isFinite(field.originalIndex) ? field.originalIndex : byName.size
            });
        });
    });

    return Array.from(byName.values()).sort((a, b) => {
        if (a.displayOrder !== b.displayOrder) {
            return a.displayOrder - b.displayOrder;
        }

        return a.originalIndex - b.originalIndex;
    });
}

export function loadSelectedStatusFields(storageKey, options) {
    const validKeys = new Set((options || []).map((option) => option.key));
    const fallback = (options || []).slice(0, 2).map((option) => option.key);

    try {
        const parsed = JSON.parse(userPreferences.getItem(storageKey) || "null");

        if (Array.isArray(parsed)) {
            const selected = parsed.filter((key) => validKeys.has(key)).slice(0, 2);
            return selected.length ? selected : fallback;
        }
    } catch (error) {
        console.warn("Could not load diagram status preferences.", error);
    }

    return fallback;
}

export function persistSelectedStatusFields(storageKey, selectedKeys) {
    userPreferences.setItem(storageKey, JSON.stringify(Array.from(selectedKeys || []).slice(0, 2)));
}

export function getDiagramStatusLines(entity, selectedKeys) {
    return Array.from(selectedKeys || []).slice(0, 2).map((key, index) => {
        const field = Array.from(entity?.fields || []).find((item) => item.name === key);
        const value = normalizeDiagramStatusValue(field?.value);

        return {
            key,
            value,
            color: getDiagramStatusColor(value, index)
        };
    });
}

function normalizeDiagramStatusValue(value) {
    const text = String(value || "").trim();

    return !text || text === "--" || text === "\u2014" ? "" : text;
}

export function renderDiagramStatusBars(prefix, entity, selectedKeys) {
    const lines = getDiagramStatusLines(entity, selectedKeys);

    if (!lines.length) {
        return "";
    }

    return `
        <div class="${prefix}-diagram-status-bars">
            ${lines.map((line) => `
                <div class="${prefix}-diagram-status-bar" style="background:${line.color};">
                    <span class="${prefix}-diagram-status-value">${escapeHtml(line.value)}</span>
                </div>
            `).join("")}
        </div>
    `;
}

export function ensureDiagramStatusControl({
    controlId,
    buttonId,
    popoverId,
    listId,
    closeButtonId,
    prefix,
    insertBeforeId = "",
    containerSelector = "",
    onToggle,
    onClose,
    onChange
}) {
    if (document.getElementById(controlId)) {
        return;
    }

    const insertBefore = insertBeforeId ? document.getElementById(insertBeforeId) : null;
    const container = insertBefore?.parentElement || document.querySelector(containerSelector);

    if (!container) {
        return;
    }

    const control = document.createElement("div");
    control.id = controlId;
    control.className = `${prefix}-columns-control`;
    control.hidden = true;
    control.innerHTML = `
        <button
                id="${buttonId}"
                class="${prefix}-secondary-button ${prefix}-columns-button"
                type="button"
                aria-haspopup="dialog"
                aria-controls="${popoverId}"
                aria-expanded="false"
                title="Choose status fields"
        >
            Status
        </button>

        <div id="${popoverId}" class="${prefix}-columns-popover" hidden role="dialog" aria-label="Choose status fields">
            <div class="${prefix}-columns-popover-head">
                <span>Status</span>
                <button
                        id="${closeButtonId}"
                        class="${prefix}-columns-close"
                        type="button"
                        aria-label="Close status chooser"
                        title="Close"
                >
                    &times;
                </button>
            </div>
            <div class="${prefix}-status-limit-popup" data-status-limit-popup hidden role="status" aria-live="polite">
                You can select up to 2 columns.
            </div>
            <div id="${listId}" class="${prefix}-columns-list"></div>
        </div>
    `;

    container.insertBefore(control, insertBefore || null);
    document.getElementById(buttonId)?.addEventListener("click", onToggle);
    document.getElementById(closeButtonId)?.addEventListener("click", onClose);
    document.getElementById(listId)?.addEventListener("click", (event) => {
        const option = event.target?.closest?.(`.${prefix}-column-option`);
        const input = event.target?.closest?.("input[type='checkbox'][data-status-key]")
            || option?.querySelector?.("input[type='checkbox'][data-status-key]");

        if (!input || input.checked) {
            return;
        }

        const checkedCount = document.getElementById(listId)
            ?.querySelectorAll?.("input[type='checkbox'][data-status-key]:checked")
            ?.length || 0;

        if (checkedCount >= 2) {
            event.preventDefault();
            showDiagramStatusLimitPopup(popoverId);
        }
    });
    document.getElementById(listId)?.addEventListener("change", onChange);
}

export function renderDiagramStatusMenu({
    buttonId,
    listId,
    options,
    selectedKeys,
    menuOpen,
    prefix
}) {
    const list = document.getElementById(listId);
    const button = document.getElementById(buttonId);

    if (!list || !button) {
        return;
    }

    const selected = new Set(selectedKeys || []);
    const selectedCount = selected.size;
    list.innerHTML = Array.from(options || []).map((option) => {
        const checked = selected.has(option.key);

        return `
            <label class="${prefix}-column-option">
                <input type="checkbox" data-status-key="${escapeHtml(option.key)}" ${checked ? "checked" : ""}>
                <span>${escapeHtml(option.label)}</span>
            </label>
        `;
    }).join("");

    button.classList.toggle("is-partial", selectedCount > 0 && selectedCount < Math.min(2, (options || []).length));
    button.setAttribute("aria-expanded", menuOpen ? "true" : "false");
    button.setAttribute("aria-label", selectedCount ? `Status, ${selectedCount} selected` : "Status");
    button.title = selectedCount ? `${selectedCount} selected status field${selectedCount === 1 ? "" : "s"}` : "Choose status fields";
}

export function showDiagramStatusLimitPopup(popoverId) {
    const popup = document.getElementById(popoverId)?.querySelector?.("[data-status-limit-popup]");

    if (!popup) {
        return;
    }

    window.clearTimeout(Number(popup.dataset.hideTimer || 0));
    popup.hidden = false;
    popup.classList.add("is-visible");
    popup.dataset.hideTimer = String(window.setTimeout(() => {
        popup.classList.remove("is-visible");
        popup.hidden = true;
        popup.dataset.hideTimer = "";
    }, 2200));
}

export function getDiagramStatusColor(value, lineIndex) {
    const palette = DIAGRAM_STATUS_PALETTES[Math.abs(Number(lineIndex) || 0) % DIAGRAM_STATUS_PALETTES.length];
    const index = hashStatusValue(value) % palette.length;

    return palette[index];
}

function hashStatusValue(value) {
    const text = String(value || "").trim().toLowerCase();
    let hash = 0;

    for (let index = 0; index < text.length; index += 1) {
        hash = ((hash * 31) + text.charCodeAt(index)) >>> 0;
    }

    return hash;
}
