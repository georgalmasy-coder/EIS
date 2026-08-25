import { cssEscape } from "../core/css.js";
import { getDirectChild } from "../core/xml.js";
import { initHelpDialog, openHelpDialogForPage } from "./help-dialog.js";

let mountedTopbarSearch = null;

const TOPBAR_FIELDS = [
    { id: "customerName", labelId: "customerNameLabel", fallbackLabel: "Customer Name" },
    { id: "projectName", labelId: "projectNameLabel", fallbackLabel: "Project Name" },
    { id: "userName", labelId: "userNameLabel", fallbackLabel: "User Name" }
];

export function mountTopbar(root = document) {
    ensureTopbarSearchMarkup(root);
    ensureHelpDialogMarkup(root);
    initHelpDialog();
    bindTopbarHelpButton(root);

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

if (typeof document !== "undefined") {
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => mountTopbar(document));
    } else {
        mountTopbar(document);
    }
}

export function readTopbarMetadata(source = {}) {
    if (!source) {
        return {
            customerName: "—",
            customerNameLabel: "Customer Name",
            projectName: "—",
            projectNameLabel: "Project Name",
            userName: "—",
            userNameLabel: "User Name",
            helpFileName: "",
            workspaceEyebrow: "",
            workspaceHeading: "",
            workspaceHelpText: ""
        };
    }

    if (source.nodeType === Node.DOCUMENT_NODE || source.nodeType === Node.ELEMENT_NODE) {
        return readTopbarMetadataFromXml(source);
    }

    const customerName = String(source.customerName ?? "—").trim() || "—";
    const projectName = String(source.projectName ?? "—").trim() || "—";
    const userName = String(source.userName ?? "—").trim() || "—";
    const helpFileName = String(source.helpFileName ?? "").trim();
    const workspaceEyebrow = String(source.workspaceEyebrow ?? "").trim();
    const workspaceHeading = String(source.workspaceHeading ?? "").trim();
    const workspaceHelpText = String(source.workspaceHelpText ?? "").trim();

    return {
        customerName,
        customerNameLabel: String(source.customerNameLabel ?? "Customer Name").trim() || "Customer Name",
        projectName,
        projectNameLabel: String(source.projectNameLabel ?? "Project Name").trim() || "Project Name",
        userName,
        userNameLabel: String(source.userNameLabel ?? "User Name").trim() || "User Name",
        helpFileName,
        workspaceEyebrow,
        workspaceHeading,
        workspaceHelpText
    };
}

export function applyTopbarMetadata(root = document, topPanel = {}) {
    if (!root) {
        return;
    }

    mountTopbar(root);

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

    updateTopbarHelpButton(root, metadata);
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
        helpButton.removeAttribute("data-help-bound-page");

        return;
    }

    helpButton.hidden = false;
    helpButton.setAttribute("data-help-page", helpFileName);
    helpButton.setAttribute("data-help-title", "Help");
    helpButton.setAttribute("title", "Open help");

    bindTopbarHelpButton(root);
}

function bindTopbarHelpButton(root = document) {
    const helpButton = findElement(root, "topbarHelpButton");

    if (!helpButton || helpButton.dataset.helpBound === "true") {
        return;
    }

    helpButton.dataset.helpBound = "true";
    helpButton.addEventListener("click", handleTopbarHelpClick);
}

async function handleTopbarHelpClick(event) {
    event.preventDefault();
    event.stopPropagation();

    const helpButton = event.currentTarget;
    const topbar = helpButton?.closest?.(".topbar");
    const page = helpButton?.getAttribute("data-help-page")
        || topbar?.getAttribute("data-help-page")
        || "";
    const title = helpButton?.getAttribute("data-help-title")
        || topbar?.getAttribute("data-help-title")
        || "Help";

    await openHelpDialogForPage(page, title);
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

    const content = topbar.querySelector(".topbar-content") || topbar;

    if (!content) {
        return;
    }

    const search = createTopbarSearch();
    const actions = content.querySelector(".topbar-actions");
    const helpButton = content.querySelector(".topbar-help-button");

    if (actions) {
        content.insertBefore(search, actions);
    } else if (helpButton) {
        content.insertBefore(search, helpButton);
    } else {
        content.appendChild(search);
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
    let content = topbar.querySelector(".topbar-content");

    if (!content) {
        content = document.createElement("div");
        content.className = "topbar-content";

        while (topbar.firstChild) {
            content.appendChild(topbar.firstChild);
        }

        topbar.appendChild(content);
    }

    if (!content.querySelector(".topbar-project")) {
        const project = document.createElement("div");
        project.className = "topbar-project";
        project.setAttribute("aria-label", "Current project");
        project.innerHTML = `
            <span class="topbar-project-prefix">Project</span>
            <span class="topbar-project-separator" aria-hidden="true">/</span>
            <strong id="projectName" data-topbar="projectName">—</strong>
        `;
        content.insertBefore(project, content.firstChild);
    }

    if (!content.querySelector(".topbar-spinner")) {
        const spinner = document.createElement("div");
        spinner.className = "topbar-spinner";
        spinner.id = "topbarGlobalSpinner";
        spinner.innerHTML = "<div></div>".repeat(12);
        content.appendChild(spinner);

        // Inject hidden elements for legacy scripts
        const hidden = document.createElement("div");
        hidden.style.display = "none";
        hidden.innerHTML = `
            <span id="customerName"></span>
            <span id="userName"></span>
            <span id="loadStatus"></span>
        `;
        content.appendChild(hidden);
    }

    if (!content.querySelector(".topbar-actions")) {
        const actions = document.createElement("div");
        actions.className = "topbar-actions";
        content.appendChild(actions);
    }

    const actions = content.querySelector(".topbar-actions");

    if (actions && !actions.querySelector(".topbar-help-button")) {
        actions.appendChild(createTopbarHelpButton());
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
        userName: getFieldValue(topPanel, ["UserName", "Name"], "—"),
        helpFileName: getFieldValue(topPanel, "HelpFileName", ""),
        workspaceEyebrow: getFieldValue(topPanel, "WorkspaceEyebrow", ""),
        workspaceHeading: getFieldValue(topPanel, "WorkspaceHeading", ""),
        workspaceHelpText: getFieldValue(topPanel, "WorkspaceHelpText", "")
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

        <svg class="topbar-search-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <circle cx="11" cy="11" r="6.5"></circle>
            <path d="M16.2 16.2L21 21"></path>
        </svg>
    `;

    return search;
}

function createTopbarHelpButton() {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "topbar-help-button";
    button.id = "topbarHelpButton";
    button.hidden = true;
    button.setAttribute("aria-label", "Open help");
    button.setAttribute("data-help-title", "Help");
    button.setAttribute("title", "Open help");
    button.innerHTML = `
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 18.2A8.2 8.2 0 1 1 12 3.8a8.2 8.2 0 0 1 0 16.4z"></path>
            <path d="M12 16.75a1.15 1.15 0 1 0 0 2.3 1.15 1.15 0 0 0 0-2.3z"></path>
            <path d="M12.05 5.25c-2.05 0-3.55 1.25-3.8 3.15a.95.95 0 0 0 1.88.25c.13-.96.84-1.55 1.87-1.55 1.13 0 1.9.67 1.9 1.65 0 .82-.42 1.24-1.32 1.88-1.12.8-1.78 1.48-1.78 3.02v.45a.95.95 0 1 0 1.9 0v-.45c0-.68.22-.94 1-1.5 1.04-.74 2.1-1.54 2.1-3.4 0-2.03-1.56-3.5-3.75-3.5z"></path>
        </svg>
    `;
    return button;
}

function ensureHelpDialogMarkup(root = document) {
    if (findElement(root, "helpDialog")) {
        return;
    }

    const body = root.body || document.body;

    if (!body) {
        return;
    }

    const dialog = document.createElement("dialog");
    dialog.id = "helpDialog";
    dialog.className = "help-dialog";
    dialog.setAttribute("aria-labelledby", "helpDialogTitle");
    dialog.innerHTML = `
        <form method="dialog" class="help-dialog-form">
            <div class="help-dialog-head">
                <h3 id="helpDialogTitle">Help</h3>
                <button id="helpDialogCloseButton" class="help-dialog-close-button" type="button" aria-label="Close help">×</button>
            </div>

            <div id="helpDialogContent" class="help-dialog-content">Loading help…</div>

            <div class="help-dialog-footer">
                <button id="helpDialogOkButton" class="primary" type="button">Close</button>
            </div>
        </form>
    `;

    body.appendChild(dialog);
}
