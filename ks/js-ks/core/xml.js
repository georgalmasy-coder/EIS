export function parseXml(xmlText) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(xmlText, "application/xml");

    if (hasXmlParseError(doc)) {
        throw new Error("Invalid XML returned by service.");
    }

    return doc;
}

export function hasXmlParseError(xmlDocument) {
    return xmlDocument?.getElementsByTagName("parsererror")?.length > 0
        || xmlDocument?.querySelector?.("parsererror") !== null;
}

export function serializeXml(doc) {
    return new XMLSerializer().serializeToString(doc);
}

export function textOf(parent, tagName) {
    if (!parent) {
        return "";
    }

    const el = parent.getElementsByTagName(tagName)[0];
    return el ? (el.textContent || "").trim() : "";
}

export function directTextOf(parent, tagName) {
    if (!parent) {
        return "";
    }

    const el = Array.from(parent.children).find((child) => child.tagName === tagName);
    return el ? (el.textContent || "").trim() : "";
}

export function getDirectText(element) {
    return Array.from(element?.childNodes || [])
        .filter((node) => node.nodeType === Node.TEXT_NODE)
        .map((node) => node.textContent || "")
        .join("");
}

export function getDirectChild(parent, tagName) {
    if (!parent) {
        return null;
    }

    return Array.from(parent.children).find((child) => child.tagName === tagName) || null;
}

export function getDirectChildren(parent, tagName) {
    if (!parent) {
        return [];
    }

    return Array.from(parent.children).filter((child) => child.tagName === tagName);
}

export function getChildText(parentElement, selector, fallback = "") {
    const element = parentElement?.querySelector?.(selector) || parentElement?.getElementsByTagName?.(selector)?.[0];
    const value = element?.textContent?.trim();

    return value || fallback;
}

export function getAttribute(element, attributeName, fallback = "") {
    const value = element?.getAttribute?.(attributeName);

    if (value === null || value === undefined || String(value).trim() === "") {
        return fallback;
    }

    return String(value).trim();
}

export function getNumberAttribute(element, attributeName, fallback = 0) {
    const value = getAttribute(element, attributeName, "");

    if (value === "") {
        return fallback;
    }

    const parsed = Number(value);

    return Number.isFinite(parsed) ? parsed : fallback;
}

export function getBooleanAttribute(element, attributeName, fallback = false) {
    const value = getAttribute(element, attributeName, "");

    if (value === "") {
        return fallback;
    }

    return toBool(value);
}

export function toBool(value) {
    const normalized = String(value || "").trim().toLowerCase();

    return ["true", "1", "yes", "ja", "y", "on"].includes(normalized);
}

export function appendTextElement(doc, parent, tagName, value) {
    const element = doc.createElement(tagName);
    element.textContent = value ?? "";
    parent.appendChild(element);

    return element;
}

export function escapeXml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&apos;");
}