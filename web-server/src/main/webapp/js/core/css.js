export function cssEscape(value) {
    if (window.CSS && typeof window.CSS.escape === "function") {
        return window.CSS.escape(value);
    }

    return String(value)
        .replaceAll("\\", "\\\\")
        .replaceAll('"', '\\"');
}

export function sanitizeClassPart(value) {
    return String(value || "")
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9_-]+/g, "-")
        .replace(/^-+|-+$/g, "");
}

export function getDynamicStyleClass(styleId, prefix = "dynamic-style") {
    const sanitizedStyleId = sanitizeClassPart(styleId);

    return sanitizedStyleId
        ? `${prefix}-${sanitizedStyleId}`
        : `${prefix}-default`;
}

export function sanitizeCssColor(value, fallback = "transparent") {
    const color = String(value || "").trim();

    if (!color) {
        return fallback;
    }

    if (/^#[0-9a-fA-F]{3}$/.test(color) || /^#[0-9a-fA-F]{6}$/.test(color)) {
        return color;
    }

    if (/^[a-zA-Z]+$/.test(color)) {
        return color;
    }

    if (/^rgba?\(\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*\d{1,3}(?:\s*,\s*(?:0|1|0?\.\d+))?\s*\)$/.test(color)) {
        return color;
    }

    return fallback;
}