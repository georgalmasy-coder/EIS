import { parseXml } from "./xml.js";

async function requestText(url, options = {}) {
    const response = await fetch(url, options);
    const text = await response.text();

    if (!response.ok) {
        throw buildHttpError(url, response, text);
    }

    return text;
}

function buildHttpError(url, response, text) {
    const trimmed = String(text || "").trim();

    if (!trimmed) {
        return new Error(`HTTP ${response.status} from ${url}`);
    }

    const xmlMessage = extractXmlErrorMessage(trimmed);

    if (xmlMessage) {
        return new Error(xmlMessage);
    }

    return new Error(trimmed || `HTTP ${response.status} from ${url}`);
}

function extractXmlErrorMessage(text) {
    try {
        const errorDoc = parseXml(text);

        return errorDoc.getElementsByTagName("message")[0]?.textContent?.trim()
            || errorDoc.getElementsByTagName("error")[0]?.textContent?.trim()
            || "";
    } catch (_error) {
        return "";
    }
}

export async function fetchText(url, options = {}) {
    return await requestText(url, options);
}

export async function fetchJson(url, options = {}) {
    const mergedOptions = {
        ...options,
        headers: {
            "Accept": "application/json",
            ...(options.headers || {})
        }
    };

    const text = await requestText(url, mergedOptions);
    const trimmed = String(text || "").trim();

    if (!trimmed) {
        return null;
    }

    try {
        return JSON.parse(trimmed);
    } catch (_error) {
        throw new Error(`Invalid JSON returned from ${url}`);
    }
}

export async function fetchXml(url, options = {}) {
    const mergedOptions = {
        ...options,
        headers: {
            "Accept": "application/xml, text/xml",
            ...(options.headers || {})
        }
    };

    const xmlText = await requestText(url, mergedOptions);

    return parseXml(xmlText);
}

export async function postXml(url, xmlPayload, options = {}) {
    const mergedOptions = {
        ...options,
        method: "POST",
        headers: {
            "Content-Type": "application/xml; charset=UTF-8",
            "Accept": "application/xml, text/xml",
            ...(options.headers || {})
        },
        body: xmlPayload
    };

    return await requestText(url, mergedOptions);
}

export async function postForm(url, formPayload, options = {}) {
    const mergedOptions = {
        ...options,
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            ...(options.headers || {})
        },
        body: formPayload
    };

    return await requestText(url, mergedOptions);
}

export async function postJson(url, payload, options = {}) {
    const mergedOptions = {
        ...options,
        method: "POST",
        headers: {
            "Content-Type": "application/json; charset=UTF-8",
            "Accept": "application/json",
            ...(options.headers || {})
        },
        body: JSON.stringify(payload ?? {})
    };

    return await fetchJson(url, mergedOptions);
}