export function nowIsoLocal() {
    const d = new Date();
    const pad = (n) => String(n).padStart(2, "0");

    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

export function parseDateTime(value) {
    if (!value) {
        return "";
    }

    const normalized = String(value).trim();
    const match = normalized.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})(?::(\d{2}))?/);

    if (match) {
        return `${match[1]} ${match[2]}${match[3] ? `:${match[3]}` : ""}`;
    }

    return normalized;
}

export function toDateInputValue(value) {
    const normalized = String(value || "").trim();
    const match = normalized.match(/^(\d{4}-\d{2}-\d{2})/);

    return match ? match[1] : normalized;
}

export function toDateTimeInputValue(value) {
    const normalized = String(value || "").trim();
    const match = normalized.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})(?::(\d{2}))?/);

    if (match) {
        return `${match[1]}T${match[2]}:${match[3] || "00"}`;
    }

    return normalized;
}

export function toDateTimeLocalValue(value) {
    if (!value) {
        return "";
    }

    const trimmed = String(value).trim();

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(trimmed)) {
        return trimmed;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(trimmed)) {
        return trimmed.slice(0, 16);
    }

    const match = trimmed.match(/^(\d{2})\/(\d{2})-(\d{4})\s+(\d{2}):(\d{2})(?::(\d{2}))?$/);

    if (!match) {
        return "";
    }

    const day = match[1];
    const month = match[2];
    const year = match[3];
    const hour = match[4];
    const minute = match[5];

    return `${year}-${month}-${day}T${hour}:${minute}`;
}

export function toTimeInputValue(value) {
    if (!value) {
        return "";
    }

    const trimmed = String(value).trim();

    if (/^\d{2}:\d{2}$/.test(trimmed)) {
        return trimmed;
    }

    const match = trimmed.match(/^(\d{2}):(\d{2})(?::\d{2})?$/);

    if (!match) {
        return "";
    }

    return `${match[1]}:${match[2]}`;
}

export function formatHistoryDateTime(value) {
    if (!value) {
        return "";
    }

    const normalized = String(value).trim();
    const match = normalized.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})(?::(\d{2}))?/);

    if (match) {
        return `${match[1]} ${match[2]}${match[3] ? `:${match[3]}` : ""}`;
    }

    return normalized.replace("T", " ");
}