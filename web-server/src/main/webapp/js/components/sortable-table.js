export function naturalCompare(left, right, locale = "da") {
    return String(left ?? "").localeCompare(String(right ?? ""), locale, {
        numeric: true,
        sensitivity: "base"
    });
}

export function compareSortableValues(left, right, options = {}) {
    const locale = options.locale || "da";

    const leftValue = normalizeSortableValue(left);
    const rightValue = normalizeSortableValue(right);

    if (leftValue.type === "number" && rightValue.type === "number") {
        return leftValue.value - rightValue.value;
    }

    if (leftValue.type === "date" && rightValue.type === "date") {
        return leftValue.value - rightValue.value;
    }

    return naturalCompare(leftValue.value, rightValue.value, locale);
}

export function compareByKey(left, right, key, options = {}) {
    return compareSortableValues(left?.[key], right?.[key], options);
}

export function nextSortState(currentState, key) {
    if (!currentState || currentState.key !== key) {
        return {
            key,
            dir: "asc"
        };
    }

    return {
        key,
        dir: currentState.dir === "asc" ? "desc" : "asc"
    };
}

export function applySortIndicators(keys, sortState, indicatorPrefix = "si-") {
    const safeKeys = Array.isArray(keys) ? keys : [];

    for (const key of safeKeys) {
        const indicator = document.getElementById(`${indicatorPrefix}${key}`);

        if (!indicator) {
            continue;
        }

        if (!sortState || sortState.key !== key) {
            indicator.textContent = "";
            indicator.setAttribute("aria-label", "Not sorted");
            continue;
        }

        const ascending = sortState.dir === "asc";

        indicator.textContent = ascending ? "▲" : "▼";
        indicator.setAttribute("aria-label", ascending ? "Sorted ascending" : "Sorted descending");
    }
}

export function bindSortableHeaders(container, onSort) {
    if (!container || typeof onSort !== "function") {
        return;
    }

    const headers = container.querySelectorAll("[data-key]");

    headers.forEach((header) => {
        header.classList.add("is-sortable");
        header.tabIndex = 0;
        header.setAttribute("role", "button");

        const sortHandler = () => {
            const key = header.dataset.key;

            if (key) {
                onSort(key);
            }
        };

        header.addEventListener("click", sortHandler);

        header.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                sortHandler();
            }
        });
    });
}

function normalizeSortableValue(value) {
    if (value === null || value === undefined) {
        return {
            type: "string",
            value: ""
        };
    }

    if (typeof value === "number" && Number.isFinite(value)) {
        return {
            type: "number",
            value
        };
    }

    const text = String(value).trim();

    if (!text) {
        return {
            type: "string",
            value: ""
        };
    }

    const normalizedNumber = Number(text.replace(",", "."));

    if (Number.isFinite(normalizedNumber) && /^-?\d+([,.]\d+)?$/.test(text)) {
        return {
            type: "number",
            value: normalizedNumber
        };
    }

    const isoDateMatch = /^\d{4}-\d{2}-\d{2}/.test(text);
    const localDateMatch = /^\d{2}[./-]\d{2}[./-]\d{4}/.test(text);

    if (isoDateMatch || localDateMatch) {
        const parsedDate = parseSortableDate(text);

        if (Number.isFinite(parsedDate)) {
            return {
                type: "date",
                value: parsedDate
            };
        }
    }

    return {
        type: "string",
        value: text
    };
}

function parseSortableDate(text) {
    const trimmed = String(text || "").trim();

    if (/^\d{4}-\d{2}-\d{2}/.test(trimmed)) {
        const date = new Date(trimmed);

        if (!Number.isNaN(date.getTime())) {
            return date.getTime();
        }
    }

    const localDateMatch = trimmed.match(/^(\d{2})[./-](\d{2})[./-](\d{4})(?:\s+(\d{2}):(\d{2}))?/);

    if (!localDateMatch) {
        return Number.NaN;
    }

    const day = Number(localDateMatch[1]);
    const month = Number(localDateMatch[2]) - 1;
    const year = Number(localDateMatch[3]);
    const hour = Number(localDateMatch[4] || 0);
    const minute = Number(localDateMatch[5] || 0);

    const date = new Date(year, month, day, hour, minute);

    return date.getTime();
}