export function isBlank(value) {
    return String(value ?? "").trim() === "";
}

export function isPresent(value) {
    return !isBlank(value);
}

export function isTruthy(value) {
    const normalized = String(value || "").trim().toLowerCase();

    return ["true", "1", "yes", "ja", "y", "on"].includes(normalized);
}

export function isFalsy(value) {
    const normalized = String(value || "").trim().toLowerCase();

    return ["false", "0", "no", "nej", "n", "off"].includes(normalized);
}

export function normalizeString(value, fallback = "") {
    const normalized = String(value ?? "").trim();

    return normalized || fallback;
}

export function coalesce(...values) {
    for (const value of values) {
        if (!isBlank(value)) {
            return value;
        }
    }

    return "";
}

export function toNumber(value, fallback = 0) {
    const parsed = Number(value);

    return Number.isFinite(parsed) ? parsed : fallback;
}

export function clampNumber(value, min, max) {
    const parsed = Number(value);

    if (!Number.isFinite(parsed)) {
        return min;
    }

    return Math.min(Math.max(parsed, min), max);
}