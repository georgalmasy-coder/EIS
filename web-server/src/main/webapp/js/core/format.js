export function formatBoolean(value, trueLabel = "Yes", falseLabel = "No") {
    return value ? trueLabel : falseLabel;
}

export function formatLocalDateTime(rawValue, locale = "da-DK") {
    if (!rawValue) {
        return "—";
    }

    const trimmed = String(rawValue).trim();
    const normalized = trimmed.length === 19 ? trimmed.replace(" ", "T") : trimmed;
    const date = new Date(normalized);

    if (Number.isNaN(date.getTime())) {
        return rawValue;
    }

    return new Intl.DateTimeFormat(locale, {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(date);
}

export function formatDateTimeForDisplay(rawValue, locale = "da-DK", fallback = "—") {
    if (!rawValue) {
        return fallback;
    }

    const trimmed = String(rawValue).trim();
    const normalized = trimmed.length === 19 ? trimmed.replace(" ", "T") : trimmed;
    const date = new Date(normalized);

    if (Number.isNaN(date.getTime())) {
        return trimmed || fallback;
    }

    return new Intl.DateTimeFormat(locale, {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(date);
}

export function formatDateForDisplay(rawValue, locale = "da-DK", fallback = "—") {
    if (!rawValue) {
        return fallback;
    }

    const trimmed = String(rawValue).trim();
    const date = new Date(trimmed);

    if (Number.isNaN(date.getTime())) {
        return trimmed || fallback;
    }

    return new Intl.DateTimeFormat(locale, {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    }).format(date);
}

export function formatNumber(value, locale = "da-DK", fallback = "—") {
    const number = Number(value);

    if (!Number.isFinite(number)) {
        return fallback;
    }

    return new Intl.NumberFormat(locale).format(number);
}

export function formatInteger(value, locale = "da-DK", fallback = "—") {
    const number = Number(value);

    if (!Number.isFinite(number)) {
        return fallback;
    }

    return new Intl.NumberFormat(locale, {
        maximumFractionDigits: 0
    }).format(number);
}

export function formatFileSize(bytes, locale = "da-DK", fallback = "—") {
    const size = Number(bytes);

    if (!Number.isFinite(size) || size < 0) {
        return fallback;
    }

    if (size < 1024) {
        return `${formatInteger(size, locale, "0")} B`;
    }

    const units = ["KB", "MB", "GB", "TB"];
    let value = size / 1024;
    let unitIndex = 0;

    while (value >= 1024 && unitIndex < units.length - 1) {
        value /= 1024;
        unitIndex += 1;
    }

    return `${new Intl.NumberFormat(locale, {
        maximumFractionDigits: value >= 10 ? 1 : 2
    }).format(value)} ${units[unitIndex]}`;
}

export function formatFallback(value, fallback = "—") {
    const normalized = String(value ?? "").trim();

    return normalized || fallback;
}