import { cssEscape } from "../core/css.js";
import { getDirectChild } from "../core/xml.js";

let mountedTopbarSearch = null;
let topbarSearchKeyGuardInstalled = false;

const TOPBAR_TITLE_PREFIX = "Projects";

export function mountTopbar(root = document) {
    ensureTopbarStructure(root);
    ensureTopbarSearchMarkup(root);
    installTopbarSearchKeyGuard();

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
            scrollMatchIntoView(activeMatch);
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
            event.stopPropagation();

            if (event.shiftKey) {
                goToPreviousMatch();
            } else {
                goToNextMatch();
            }
        }

        if (event.key === "Escape") {
            event.preventDefault();
            event.stopPropagation();
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

function scrollMatchIntoView(match) {
    if (!match || typeof match.getBoundingClientRect !== "function") {
        return;
    }

    const container = findScrollableAncestor(match);

    if (!container) {
        match.scrollIntoView({
            behavior: "smooth",
            block: "center",
            inline: "nearest"
        });
        return;
    }

    const matchRect = match.getBoundingClientRect();
    const containerRect = container.getBoundingClientRect();
    const currentTop = container.scrollTop;
    const currentLeft = container.scrollLeft;
    const targetTop = currentTop + (matchRect.top - containerRect.top) - (container.clientHeight / 2) + (matchRect.height / 2);
    const targetLeft = currentLeft + (matchRect.left - containerRect.left) - (container.clientWidth / 2) + (matchRect.width / 2);

    container.scrollTo({
        top: Math.max(0, targetTop),
        left: Math.max(0, targetLeft),
        behavior: "smooth"
    });
}

function findScrollableAncestor(node) {
    let current = node?.parentElement || null;

    while (current && current !== document.body) {
        const style = window.getComputedStyle(current);
        const overflowY = style.overflowY;
        const overflowX = style.overflowX;
        const scrollableY = (overflowY === "auto" || overflowY === "scroll" || overflowY === "overlay")
            && current.scrollHeight > current.clientHeight + 1;
        const scrollableX = (overflowX === "auto" || overflowX === "scroll" || overflowX === "overlay")
            && current.scrollWidth > current.clientWidth + 1;

        if (scrollableY || scrollableX) {
            return current;
        }

        current = current.parentElement;
    }

    return null;
}

function installTopbarSearchKeyGuard() {
    if (topbarSearchKeyGuardInstalled) {
        return;
    }

    window.addEventListener("keydown", (event) => {
        const target = event.target;
        const isTopbarSearchInput = target instanceof HTMLElement
            && target.id === "topbarSearchInput";

        if (!isTopbarSearchInput) {
            return;
        }

        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    }, true);

    topbarSearchKeyGuardInstalled = true;
}

export function readTopbarMetadata(source = {}) {
    if (!source) {
        return createEmptyTopbarMetadata();
    }

    if (source.nodeType === Node.DOCUMENT_NODE || source.nodeType === Node.ELEMENT_NODE) {
        return readTopbarMetadataFromXml(source);
    }

    return {
        customerName: String(source.customerName ?? "â€”").trim() || "â€”",
        customerNameLabel: String(source.customerNameLabel ?? "Customer Name").trim() || "Customer Name",
        projectName: String(source.projectName ?? "â€”").trim() || "â€”",
        projectNameLabel: String(source.projectNameLabel ?? "Project Name").trim() || "Project Name",
        userName: String(source.userName ?? "â€”").trim() || "â€”",
        userNameLabel: String(source.userNameLabel ?? "User Name").trim() || "User Name",
        topPanelTitle: String(source.topPanelTitle ?? "").trim(),
        helpFileName: String(source.helpFileName ?? "").trim()
    };
}

export function applyTopbarMetadata(root = document, topPanel = {}) {
    if (!root) {
        return;
    }

    const metadata = readTopbarMetadata(topPanel);

    ensureTopbarStructure(root);
    updateTopbarTitle(root, metadata);
    updateTopbarHelpButton(root, metadata);
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

    const container = topbar.querySelector(".topbar-center") || topbar;
    container.appendChild(createTopbarSearch());
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

function ensureTopbarStructure(root) {
    const topbar = root.querySelector(".topbar");

    if (!topbar) {
        return;
    }

    let shell = topbar.querySelector(".topbar-shell");

    if (!shell) {
        shell = document.createElement("section");
        shell.className = "topbar-shell";
        shell.setAttribute("aria-label", "Header");

        const left = document.createElement("div");
        left.className = "topbar-left";
        shell.appendChild(left);

        const center = document.createElement("div");
        center.className = "topbar-center";
        shell.appendChild(center);

        const right = document.createElement("div");
        right.className = "topbar-right";
        shell.appendChild(right);

        Array.from(topbar.children).forEach((child) => {
            if (!child.classList.contains("topbar-status-panel")) {
                child.remove();
            }
        });

        topbar.insertBefore(shell, topbar.firstChild);
    }

    const left = shell.querySelector(".topbar-left") || shell.appendChild(Object.assign(document.createElement("div"), { className: "topbar-left" }));
    const center = shell.querySelector(".topbar-center") || shell.appendChild(Object.assign(document.createElement("div"), { className: "topbar-center" }));
    const right = shell.querySelector(".topbar-right") || shell.appendChild(Object.assign(document.createElement("div"), { className: "topbar-right" }));

    if (!left.querySelector(".topbar-title")) {
        const title = document.createElement("div");
        title.id = "topPanelTitle";
        title.className = "topbar-title";
        title.setAttribute("data-topbar", "topPanelTitle");
        left.appendChild(title);
    }

    if (!center.querySelector(".topbar-search")) {
        center.appendChild(createTopbarSearch());
    }

    if (!right.querySelector(".topbar-help-button")) {
        right.appendChild(createTopbarHelpButton());
    }

    const statusPanel = topbar.querySelector(".topbar-status-panel");
    if (statusPanel) {
        statusPanel.classList.add("topbar-status-panel");
    }
}

function readTopbarMetadataFromXml(source) {
    const topPanel = findTopPanelElement(source);

    if (!topPanel) {
        return createEmptyTopbarMetadata();
    }

    return {
        customerNameLabel: getFieldLabel(topPanel, "CustomerName", "Customer Name"),
        customerName: getFieldValue(topPanel, "CustomerName", "â€”"),
        projectNameLabel: getFieldLabel(topPanel, "ProjectName", "Project Name"),
        projectName: getFieldValue(topPanel, "ProjectName", "â€”"),
        userNameLabel: getFieldLabel(topPanel, ["UserName", "Name"], "User Name"),
        userName: getFieldValue(topPanel, ["UserName", "Name"], "â€”"),
        topPanelTitle: getFieldValue(topPanel, "TopPanelTitle", ""),
        helpFileName: getFieldValue(topPanel, "HelpFileName", "")
    };
}

function updateTopbarTitle(root, metadata) {
    const titleElement = findElement(root, "topPanelTitle");

    if (!titleElement) {
        return;
    }

    const title = String(metadata?.topPanelTitle || "").trim();
    const prefix = document.createElement("span");
    prefix.className = "topbar-title-prefix";
    prefix.textContent = TOPBAR_TITLE_PREFIX;

    titleElement.replaceChildren(prefix);

    if (title) {
        const separator = document.createElement("span");
        separator.className = "topbar-title-separator";
        separator.textContent = " / ";

        const value = document.createElement("span");
        value.className = "topbar-title-value";
        value.textContent = title;

        titleElement.append(separator, value);
    }

    titleElement.title = title ? `${TOPBAR_TITLE_PREFIX} / ${title}` : TOPBAR_TITLE_PREFIX;
}

function updateTopbarHelpButton(root, metadata) {
    const helpButton = findElement(root, "topbarHelpButton");

    if (!helpButton) {
        return;
    }

    const helpFileName = String(metadata?.helpFileName || "").trim();

    if (!helpFileName) {
        helpButton.hidden = true;
        helpButton.removeAttribute("data-help-page");
        helpButton.removeAttribute("data-help-title");
        return;
    }

    helpButton.hidden = false;
    helpButton.setAttribute("data-help-page", helpFileName);
    helpButton.setAttribute("data-help-title", String(metadata?.topPanelTitle || "").trim() || "Help");
}

function createTopbarHelpButton() {
    const button = document.createElement("button");
    button.id = "topbarHelpButton";
    button.className = "topbar-help-button";
    button.type = "button";
    button.setAttribute("aria-label", "Open help");
    button.title = "Help";
    button.hidden = true;
    button.textContent = "?";

    return button;
}

function createTopbarSearch() {
    const search = document.createElement("div");
    search.className = "topbar-search";
    search.setAttribute("role", "search");
    search.setAttribute("aria-label", "Search on page");

    search.innerHTML = `
        <span class="topbar-search-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false" aria-hidden="true">
                <circle cx="11" cy="11" r="6.5" fill="none" stroke="currentColor" stroke-width="1.8"></circle>
                <path d="M16 16l4 4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
            </svg>
        </span>

        <input
            id="topbarSearchInput"
            class="topbar-search-input"
            type="text"
            inputmode="search"
            autocomplete="off"
            autocapitalize="off"
            spellcheck="false"
            placeholder="Search requirements, structures or commands..."
            aria-label="Search on page"
        />

        <div class="topbar-search-actions" aria-label="Search navigation">
            <span
                id="topbarSearchCount"
                class="topbar-search-count"
                aria-live="polite"
                aria-label="Search result count"
            ></span>

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
    `;

    return search;
}

function createEmptyTopbarMetadata() {
    return {
        customerName: "â€”",
        customerNameLabel: "Customer Name",
        projectName: "â€”",
        projectNameLabel: "Project Name",
        userName: "â€”",
        userNameLabel: "User Name",
        topPanelTitle: "",
        helpFileName: ""
    };
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
