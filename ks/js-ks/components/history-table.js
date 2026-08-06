import {
    applySortIndicators,
    compareSortableValues,
    nextSortState
} from "./sortable-table.js";
import { parseDateTime } from "../core/date.js";
import { escapeHtml } from "../core/html.js";
import { isTruthy } from "../core/utils.js";
import {
    getDirectText,
    textOf
} from "../core/xml.js";

const DEFAULT_CONFIG = {
    bodyId: "historyBody",
    emptyId: "historyEmpty",
    entrySelector: "entityHistory",
    historyContainerSelector: "entityHistories",
    sortKeys: ["changedDateTime", "changedByUserId", "version"],
    indicatorPrefix: "si-h-",
    defaultSortKey: "changedDateTime",
    defaultSortDir: "asc",
    editPageUrl: "",
    defaultReturnUrl: "",
    readOnlyMode: "edit-version",
    id: "",
    returnUrl: "",
    readOnly: false,
    onOpenHistoricalVersion: null,
    onAfterRender: null
};

function normalizeText(value) {
    return value == null ? "" : String(value);
}

function byId(id) {
    return id ? document.getElementById(id) : null;
}

function getConfiguredElements(config) {
    return {
        body: byId(config.bodyId),
        empty: byId(config.emptyId)
    };
}

function getFieldDisplayText(parent, fieldName) {
    const field = parent?.getElementsByTagName(fieldName)?.[0];

    if (!field) {
        return "";
    }

    const selectedOption = Array.from(field.getElementsByTagName("Option") || [])
        .find((option) => isTruthy(option.getAttribute("selected")));

    if (selectedOption?.textContent?.trim()) {
        return selectedOption.textContent.trim();
    }

    const firstOption = field.getElementsByTagName("Option")?.[0];

    if (firstOption?.textContent?.trim()) {
        return firstOption.textContent.trim();
    }

    const valueNode = field.getElementsByTagName("Value")?.[0];

    if (valueNode?.textContent?.trim()) {
        return valueNode.textContent.trim();
    }

    return getDirectText(field).trim();
}

function findHistoryContainer(doc, selector) {
    const root = doc?.documentElement || doc;

    if (!root) {
        return null;
    }

    if (root.matches?.(selector)) {
        return root;
    }

    return root.querySelector(selector)
        || root.getElementsByTagName(selector)?.[0]
        || null;
}

function parseHistoryRows(historyNode, entrySelector) {
    return Array.from(historyNode?.querySelectorAll(entrySelector) || []).map((node) => {
        const version = textOf(node, "Version").trim();
        const changedByUserId = getFieldDisplayText(node, "ChangedByUserId");
        const rawChangedDateTime = textOf(node, "ChangedDateTime");
        const changedDateTime = parseDateTime(rawChangedDateTime);
        const latest = isTruthy(textOf(node, "Latest"));

        return {
            node,
            version,
            changedByUserId,
            changedDateTime,
            rawChangedDateTime,
            latest
        };
    });
}

function getRowSortValue(row, key) {
    if (key === "changedDateTime") {
        return row.rawChangedDateTime || row.changedDateTime;
    }

    return row[key];
}

function createHistoryRowMarkup(row) {
    return `
        <tr
                data-version="${escapeHtml(row.version)}"
                data-latest="${row.latest ? "true" : "false"}"
                title="${row.latest ? "Latest version" : "Double-click to open this historical version"}">
            <td>${escapeHtml(row.changedDateTime ?? "")}</td>
            <td>${escapeHtml(row.changedByUserId ?? "")}</td>
            <td>${escapeHtml(row.version ?? "")}</td>
        </tr>
    `;
}

function setEmptyVisible(emptyElement, visible) {
    if (!emptyElement) {
        return;
    }

    emptyElement.style.display = visible ? "block" : "none";
}

function buildHistoricalVersionUrl(config, version) {
    const editPageUrl = normalizeText(config.editPageUrl).trim();
    const id = normalizeText(config.id).trim();
    const readOnlyMode = normalizeText(config.readOnlyMode).trim() || "edit-version";
    const returnUrl = normalizeText(config.returnUrl || config.defaultReturnUrl).trim();

    if (!editPageUrl || !id || !version) {
        return "";
    }

    const url = new URL(editPageUrl, window.location.href);

    url.searchParams.set("mode", readOnlyMode);
    url.searchParams.set("id", id);
    url.searchParams.set("version", version);

    if (returnUrl) {
        url.searchParams.set("returnUrl", returnUrl);
    }

    return url.toString();
}

export function createHistoryTable(config = {}) {
    const state = {
        rows: [],
        bound: false,
        sourceDoc: null,
        config: {
            ...DEFAULT_CONFIG,
            ...config
        },
        sortState: {
            key: config.defaultSortKey || DEFAULT_CONFIG.defaultSortKey,
            dir: config.defaultSortDir || DEFAULT_CONFIG.defaultSortDir
        }
    };

    function getElements() {
        return getConfiguredElements(state.config);
    }

    function notifyAfterRender() {
        if (typeof state.config.onAfterRender === "function") {
            state.config.onAfterRender(getRows());
        }
    }

    function setContext(context = {}, options = {}) {
        state.config = {
            ...state.config,
            ...context
        };

        if (options.render !== false) {
            render();
        }
    }

    function setReadOnly(readOnly, options = {}) {
        state.config.readOnly = readOnly === true;

        if (options.render !== false) {
            render();
        }
    }

    function setEntityId(id, options = {}) {
        state.config.id = normalizeText(id).trim();

        if (options.render !== false) {
            render();
        }
    }

    function setReturnUrl(returnUrl) {
        state.config.returnUrl = normalizeText(returnUrl).trim();
    }

    function getRows() {
        return state.rows;
    }

    function getRowByVersion(version) {
        const normalizedVersion = normalizeText(version).trim();

        return state.rows.find((row) => normalizeText(row.version).trim() === normalizedVersion) || null;
    }

    function loadFromDocument(doc, options = {}) {
        state.sourceDoc = doc || null;

        const historyNode = findHistoryContainer(doc, state.config.historyContainerSelector);

        state.rows = parseHistoryRows(historyNode, state.config.entrySelector);

        if (options.render !== false) {
            render();
        }
    }

    function render(historyNode = null, elements = null) {
        if (historyNode) {
            state.rows = parseHistoryRows(historyNode, state.config.entrySelector);
        }

        const renderElements = elements || getElements();

        if (!renderElements?.body || !renderElements?.empty) {
            return;
        }

        const rows = [...state.rows];
        const sortKey = state.sortState.key || state.config.defaultSortKey;
        const sortDir = state.sortState.dir || state.config.defaultSortDir;

        rows.sort((left, right) => {
            const result = compareSortableValues(
                getRowSortValue(left, sortKey),
                getRowSortValue(right, sortKey),
                { locale: "en" }
            );

            return sortDir === "asc" ? result : -result;
        });

        renderElements.body.innerHTML = rows.map(createHistoryRowMarkup).join("");

        setEmptyVisible(renderElements.empty, rows.length === 0);
        applySortIndicators(state.config.sortKeys, state.sortState, state.config.indicatorPrefix);

        notifyAfterRender();
    }

    function setSort(key, dir = "asc", options = {}) {
        state.sortState.key = key;
        state.sortState.dir = dir;

        if (options.render !== false) {
            render();
        }
    }

    function sortBy(key) {
        state.sortState = nextSortState(state.sortState, key);
        render();
    }

    function openHistoricalVersion(version) {
        const row = getRowByVersion(version);

        if (!row || row.latest) {
            return;
        }

        if (typeof state.config.onOpenHistoricalVersion === "function") {
            state.config.onOpenHistoricalVersion({
                version: row.version,
                row,
                id: state.config.id
            });
            return;
        }

        const url = buildHistoricalVersionUrl(state.config, row.version);

        if (!url) {
            window.alert("Could not determine URL for historical version.");
            return;
        }

        window.location.href = url;
    }

    function handleTableDoubleClick(event) {
        if (state.config.readOnly) {
            return;
        }

        const rowElement = event.target.closest("tr[data-version]");

        if (!rowElement) {
            return;
        }

        const latest = isTruthy(rowElement.getAttribute("data-latest"));

        if (latest) {
            return;
        }

        const version = rowElement.getAttribute("data-version") || "";

        openHistoricalVersion(version);
    }

    function handleHeaderClick(event) {
        const header = event.target.closest("[data-key]");

        if (!header) {
            return;
        }

        const key = header.getAttribute("data-key");

        if (!key || !state.config.sortKeys.includes(key)) {
            return;
        }

        sortBy(key);
    }

    function bind(customConfig = {}) {
        state.config = {
            ...state.config,
            ...customConfig
        };

        if (state.bound) {
            return;
        }

        const elements = getElements();

        elements.body?.addEventListener("dblclick", handleTableDoubleClick);

        const table = elements.body?.closest("table");
        const tableHead = table?.querySelector("thead");

        tableHead?.addEventListener("click", handleHeaderClick);

        state.bound = true;
    }

    function destroy() {
        state.bound = false;
    }

    return {
        bind,
        destroy,

        loadFromDocument,
        render,

        setContext,
        setReadOnly,
        setEntityId,
        setReturnUrl,

        setSort,
        sortBy,
        getSortState: () => ({ ...state.sortState }),

        getRows,
        getRowByVersion,
        openHistoricalVersion
    };
}
