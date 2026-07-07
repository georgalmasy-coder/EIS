import { cssEscape } from "../core/css.js";
import { getDirectChild } from "../core/xml.js";

let mountedTopbarSearch = null;

const TOPBAR_FIELDS = [
    { id: "customerName", labelId: "customerNameLabel", fallbackLabel: "Customer Name" },
    { id: "projectName", labelId: "projectNameLabel", fallbackLabel: "Project Name" },
    { id: "userName", labelId: "userNameLabel", fallbackLabel: "User Name" }
];

export function mountTopbar(root = document) {
    ensureTopbarSearchMarkup(root);

    const input = findElement(root, "topbarSearchInput");
    const count = findElement(root, "topbarSearchCount");
    const previousButton = findElement(root, "topbarSearchPrevious");
    const nextButton = findElement(root, "topbarSearchNext");

    if (!input || !count || !previousButton || !nextButton) {
        return null;
    }

    if (mountedTopbarSearch) {
        return mountedTopbarSearch;
    }

    let matches = [];
    let currentIndex = -1;

    const searchableRoot = document.querySelector("main.app") || document.querySelector("main") || document.body;

    function isSearchUiNode(node) {
        return Boolean(node.parentElement?.closest(".topbar-search"));
    }

    function isExistingHighlightNode(node) {
        return Boolean(node.parentElement?.closest(".topbar-search-highlight"));
    }

    function isScriptOrStyleNode(node) {
        const parent = node.parentElement;

        if (!parent) {
            return true;
        }

        return Boolean(parent.closest("script, style, noscript"));
    }

    function isFormControlNode(node) {
        const parent = node.parentElement;

        if (!parent) {
            return true;
        }

        return Boolean(parent.closest("input, textarea, select, option, button"));
    }

    function isVisibleTextNode(node) {
        const parent = node.parentElement;

        if (
            !parent
            || isSearchUiNode(node)
            || isExistingHighlightNode(node)
            || isScriptOrStyleNode(node)
            || isFormControlNode(node)
        ) {
            return false;
        }

        const style = window.getComputedStyle(parent);

        return style.display !== "none"
            && style.visibility !== "hidden"
            && node.nodeValue.trim().length > 0;
    }

    function getSearchableTextNodes() {
        const nodes = [];
        const walker = document.createTreeWalker(
            searchableRoot,
            NodeFilter.SHOW_TEXT,
            {
                acceptNode(node) {
                    return isVisibleTextNode(node)
                        ? NodeFilter.FILTER_ACCEPT
                        : NodeFilter.FILTER_REJECT;
                }
            }
        );

        let node = walker.nextNode();

        while (node) {
            nodes.push(node);
            node = walker.nextNode();
        }

        return nodes;
    }

    function clearHighlights() {
        const highlights = Array.from(searchableRoot.querySelectorAll(".topbar-search-highlight"));

        highlights.forEach((highlight) => {
            const parent = highlight.parentNode;

            if (!parent) {
                return;
            }

            parent.replaceChild(document.createTextNode(highlight.textContent || ""), highlight);
            parent.normalize();
        });

        matches = [];
        currentIndex = -1;
    }

    function highlightMatches(query) {
        clearHighlights();

        if (!query) {
            return;
        }

        const lowerQuery = query.toLowerCase();
        const textNodes = getSearchableTextNodes();

        textNodes.forEach((node) => {
            const text = node.nodeValue;
            const lowerText = text.toLowerCase();

            if (!lowerText.includes(lowerQuery)) {
                return;
            }

            const fragment = document.createDocumentFragment();
            let cursor = 0;
            let index = lowerText.indexOf(lowerQuery);

            while (index !== -1) {
                if (index > cursor) {
                    fragment.appendChild(document.createTextNode(text.slice(cursor, index)));
                }

                const mark = document.createElement("mark");
                mark.className = "topbar-search-highlight";
                mark.textContent = text.slice(index, index + query.length);

                fragment.appendChild(mark);
                matches.push(mark);

                cursor = index + query.length;
                index = lowerText.indexOf(lowerQuery, cursor);
            }

            if (cursor < text.length) {
                fragment.appendChild(document.createTextNode(text.slice(cursor)));
            }

            node.parentNode.replaceChild(fragment, node);
        });
    }

    function updateNavigationVisibility(hasQuery) {
        const showNavigation = hasQuery && matches.length > 0;

        previousButton.hidden = !showNavigation;
        nextButton.hidden = !showNavigation;
    }

    function updateCount(hasQuery) {
        if (!hasQuery) {
            count.textContent = "";
            return;
        }

        if (!matches.length) {
            count.textContent = "0 / 0";
            return;
        }

        count.textContent = `${currentIndex + 1} / ${matches.length}`;
    }

    function setActiveMatch(index, shouldScroll = true) {
        const hasQuery = input.value.trim().length > 0;

        matches.forEach((match) => {
            match.classList.remove("topbar-search-highlight-active");
        });

        if (!matches.length) {
            currentIndex = -1;
            updateCount(hasQuery);
            return;
        }

        currentIndex = (index + matches.length) % matches.length;

        const activeMatch = matches[currentIndex];
        activeMatch.classList.add("topbar-search-highlight-active");

        updateCount(true);

        if (shouldScroll) {
            activeMatch.scrollIntoView({
                behavior: "smooth",
                block: "center",
                inline: "nearest"
            });
        }
    }

    function search() {
        const query = input.value.trim();

        if (!query) {
            clearHighlights();
            updateNavigationVisibility(false);
            updateCount(false);
            return;
        }

        highlightMatches(query);
        updateNavigationVisibility(true);
        setActiveMatch(0, false);

        if (!matches.length) {
            updateCount(true);
        }
    }

    function goToPreviousMatch() {
        setActiveMatch(currentIndex - 1, true);
        input.focus();
    }

    function goToNextMatch() {
        setActiveMatch(currentIndex + 1, true);
        input.focus();
    }

    input.addEventListener("input", search);

    input.addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
            event.preventDefault();

            if (event.shiftKey) {
                goToPreviousMatch();
            } else {
                goToNextMatch();
            }
        }

        if (event.key === "Escape") {
            input.value = "";
            search();
            input.blur();
        }
    });

    previousButton.addEventListener("click", goToPreviousMatch);
    nextButton.addEventListener("click", goToNextMatch);

    mountedTopbarSearch = {
        refresh: search,
        clear: function () {
            input.value = "";
            search();
        }
    };

    return mountedTopbarSearch;
}

export function readTopbarMetadata(source = {}) {
    if (!source) {
        return {
            customerName: "—",
            customerNameLabel: "Customer Name",
            projectName: "—",
            projectNameLabel: "Project Name",
            userName: "—",
            userNameLabel: "User Name"
        };
    }

    if (source.nodeType === Node.DOCUMENT_NODE || source.nodeType === Node.ELEMENT_NODE) {
        return readTopbarMetadataFromXml(source);
    }

    const customerName = String(source.customerName ?? "—").trim() || "—";
    const projectName = String(source.projectName ?? "—").trim() || "—";
    const userName = String(source.userName ?? "—").trim() || "—";

    return {
        customerName,
        customerNameLabel: String(source.customerNameLabel ?? "Customer Name").trim() || "Customer Name",
        projectName,
        projectNameLabel: String(source.projectNameLabel ?? "Project Name").trim() || "Project Name",
        userName,
        userNameLabel: String(source.userNameLabel ?? "User Name").trim() || "User Name"
    };
}

export function applyTopbarMetadata(root = document, topPanel = {}) {
    if (!root) {
        return;
    }

    const metadata = readTopbarMetadata(topPanel);

    TOPBAR_FIELDS.forEach((field) => {
        const valueElement = findElement(root, field.id);
        const labelElement = findElement(root, field.labelId);

        if (!valueElement && !labelElement) {
            return;
        }

        const label = resolveTopbarLabel(metadata, field.labelId, field.fallbackLabel);
        const value = resolveTopbarValue(metadata, field.id, "—");
        const line = valueElement?.closest(".meta-line") || labelElement?.closest(".meta-line");

        if (labelElement) {
            labelElement.textContent = label;
        }

        if (valueElement) {
            valueElement.textContent = value;
        }

        if (line) {
            const content = [];

            if (labelElement) {
                content.push(labelElement);
            } else if (label) {
                content.push(document.createTextNode(label));
            }

            content.push(document.createTextNode(": "));

            if (valueElement) {
                content.push(valueElement);
            }

            line.replaceChildren(...content);
        }
    });
}

function findElement(searchRoot, id) {
    if (searchRoot.getElementById) {
        return searchRoot.getElementById(id);
    }

    return searchRoot.querySelector(`#${cssEscape(id)}`);
}

function ensureTopbarSearchMarkup(root = document) {
    if (findElement(root, "topbarSearchInput")) {
        ensureTopbarSearchCount(root);
        return;
    }

    const topbar = root.querySelector(".topbar");

    if (!topbar) {
        return;
    }

    ensureTopbarStructure(topbar);

    const metaContent = topbar.querySelector(".topbar-meta-content");

    if (!metaContent) {
        return;
    }

    const logo = metaContent.querySelector(".topbar-logo");
    const search = createTopbarSearch();

    if (logo) {
        metaContent.insertBefore(search, logo);
    } else {
        metaContent.appendChild(search);
    }
}

function ensureTopbarSearchCount(root = document) {
    const search = root.querySelector(".topbar-search");

    if (!search || findElement(root, "topbarSearchCount")) {
        return;
    }

    const count = document.createElement("span");
    count.id = "topbarSearchCount";
    count.className = "topbar-search-count";
    count.setAttribute("aria-live", "polite");
    count.setAttribute("aria-label", "Search result count");

    const actions = search.querySelector(".topbar-search-actions");

    if (actions) {
        search.insertBefore(count, actions);
    } else {
        search.appendChild(count);
    }
}

function ensureTopbarStructure(topbar) {
    const metaSection = topbar.querySelector(".topbar-meta") || topbar.querySelector(".meta");

    if (!metaSection) {
        return;
    }

    metaSection.classList.add("topbar-meta");

    let metaContent = metaSection.querySelector(".topbar-meta-content");

    if (!metaContent) {
        metaContent = document.createElement("div");
        metaContent.className = "topbar-meta-content";

        const metaLines = document.createElement("div");
        metaLines.className = "topbar-meta-lines";

        const logo = topbar.querySelector(".topbar-logo");

        Array.from(metaSection.children).forEach(function (child) {
            if (child !== logo) {
                metaLines.appendChild(child);
            }
        });

        metaContent.appendChild(metaLines);

        if (logo) {
            metaContent.appendChild(logo);
        }

        metaSection.appendChild(metaContent);
    }

    let metaLines = metaContent.querySelector(".topbar-meta-lines");

    if (!metaLines) {
        metaLines = document.createElement("div");
        metaLines.className = "topbar-meta-lines";

        Array.from(metaContent.children).forEach(function (child) {
            if (!child.classList.contains("topbar-logo") && !child.classList.contains("topbar-search")) {
                metaLines.appendChild(child);
            }
        });

        metaContent.insertBefore(metaLines, metaContent.firstChild);
    }

    let logo = metaContent.querySelector(".topbar-logo");

    if (!logo) {
        const image = topbar.querySelector(".eis-logo");

        if (image) {
            logo = document.createElement("div");
            logo.className = "topbar-logo";
            image.parentElement?.insertBefore(logo, image);
            logo.appendChild(image);
            metaContent.appendChild(logo);
        }
    }
}

function readTopbarMetadataFromXml(source) {
    const topPanel = findTopPanelElement(source);

    if (!topPanel) {
        return readTopbarMetadata({});
    }

    return {
        customerNameLabel: getFieldLabel(topPanel, "CustomerName", "Customer Name"),
        customerName: getFieldValue(topPanel, "CustomerName", "—"),
        projectNameLabel: getFieldLabel(topPanel, "ProjectName", "Project Name"),
        projectName: getFieldValue(topPanel, "ProjectName", "—"),
        userNameLabel: getFieldLabel(topPanel, ["UserName", "Name"], "User Name"),
        userName: getFieldValue(topPanel, ["UserName", "Name"], "—")
    };
}

function resolveTopbarLabel(topPanel, labelId, fallbackLabel) {
    const value = String(topPanel?.[labelId] || "").trim();

    return value || fallbackLabel;
}

function resolveTopbarValue(topPanel, fieldId, fallbackValue) {
    const value = String(topPanel?.[fieldId] || "").trim();

    return value || fallbackValue;
}

function findTopPanelElement(source) {
    if (!source) {
        return null;
    }

    if (source.nodeType === Node.ELEMENT_NODE) {
        return source.tagName === "TopPanel"
            ? source
            : source.querySelector("TopPanel");
    }

    return source.querySelector?.("TopPanel")
        || source.getElementsByTagName?.("TopPanel")?.[0]
        || null;
}

function getFieldLabel(parent, tagNames, fallback = "") {
    const names = Array.isArray(tagNames) ? tagNames : [tagNames];

    for (const tagName of names) {
        const element = getDirectChild(parent, tagName) || parent?.getElementsByTagName?.(tagName)?.[0];
        const label = element?.getAttribute("header") || element?.getAttribute("label") || "";

        if (String(label || "").trim()) {
            return String(label).trim();
        }
    }

    return fallback;
}

function getFieldValue(parent, tagNames, fallback = "") {
    const names = Array.isArray(tagNames) ? tagNames : [tagNames];

    for (const tagName of names) {
        const element = getDirectChild(parent, tagName) || parent?.getElementsByTagName?.(tagName)?.[0];
        const value = element?.textContent?.trim();

        if (value) {
            return value;
        }
    }

    return fallback;
}

function createTopbarSearch() {
    const search = document.createElement("div");
    search.className = "topbar-search";
    search.setAttribute("role", "search");
    search.setAttribute("aria-label", "Search on page");

    search.innerHTML = `
        <input
            id="topbarSearchInput"
            class="topbar-search-input"
            type="search"
            placeholder="What are you looking for?"
            aria-label="Search on page"
        />

        <span
            id="topbarSearchCount"
            class="topbar-search-count"
            aria-live="polite"
            aria-label="Search result count"
        ></span>

        <div class="topbar-search-actions" aria-label="Search navigation">
            <button
                id="topbarSearchPrevious"
                class="topbar-search-button"
                type="button"
                aria-label="Previous search result"
                title="Previous"
                hidden
            >
                ↑
            </button>

            <button
                id="topbarSearchNext"
                class="topbar-search-button"
                type="button"
                aria-label="Next search result"
                title="Next"
                hidden
            >
                ↓
            </button>
        </div>

        <span class="topbar-search-icon" aria-hidden="true">⌕</span>
    `;

    return search;
}
