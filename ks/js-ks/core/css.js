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

export function cssColorToRgba(value, alpha = 1, fallback = "") {
    const color = String(value || "").trim();

    if (!color) {
        return fallback;
    }

    const normalizedAlpha = Math.max(0, Math.min(1, Number(alpha)));

    const hex3 = color.match(/^#([0-9a-fA-F]{3})$/);
    if (hex3) {
        const [r, g, b] = hex3[1].split("").map((part) => Number.parseInt(part + part, 16));
        return `rgba(${r}, ${g}, ${b}, ${normalizedAlpha})`;
    }

    const hex6 = color.match(/^#([0-9a-fA-F]{6})$/);
    if (hex6) {
        const hex = hex6[1];
        const r = Number.parseInt(hex.slice(0, 2), 16);
        const g = Number.parseInt(hex.slice(2, 4), 16);
        const b = Number.parseInt(hex.slice(4, 6), 16);
        return `rgba(${r}, ${g}, ${b}, ${normalizedAlpha})`;
    }

    const rgb = color.match(/^rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,\s*(0|1|0?\.\d+))?\s*\)$/);
    if (rgb) {
        const r = Number(rgb[1]);
        const g = Number(rgb[2]);
        const b = Number(rgb[3]);
        return `rgba(${r}, ${g}, ${b}, ${normalizedAlpha})`;
    }

    return fallback || color;
}

export function buildColorChipStyle(value, alpha = 0.18) {
    const color = sanitizeCssColor(value, "");

    if (!color) {
        return "";
    }

    const backgroundColor = cssColorToRgba(color, alpha, color);

    return `background-color: ${backgroundColor}; border-color: ${color};`;
}
