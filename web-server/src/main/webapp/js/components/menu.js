import {
    byId,
    clear,
    setText
} from "../core/dom.js";
import { fetchXml } from "../core/http.js";
import { directTextOf, getDirectChild, textOf } from "../core/xml.js";

const MENU_URL = "/Menu";
const PROJECT_OVERVIEW_URL = "/web/view?page=projectoverview";
const MENU_SELECT_PROJECT_CMD = "selectproject";
const MENU_WIDTH = 272;
const MENU_COLLAPSED_WIDTH = 72;
const MENU_TRANSITION = "transform 1200ms cubic-bezier(.22,.61,.36,1)";
const MENU_COLLAPSED_STORAGE_KEY = "eis.menu.collapsed";
const MENU_SELECTED_PROJECT_STORAGE_KEY = "eis.menu.projectId";
const COLLAPSE_ICON_SVG = `
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path d="M14.7 6.3a1 1 0 0 1 0 1.4L10.41 12l4.3 4.3a1 1 0 1 1-1.42 1.4l-5-5a1 1 0 0 1 0-1.4l5-5a1 1 0 0 1 1.4 0Z"></path>
    </svg>
`;
const EXPAND_ICON_SVG = `
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path d="M9.3 17.7a1 1 0 0 1 0-1.4L13.59 12 9.3 7.7a1 1 0 1 1 1.4-1.4l5 5a1 1 0 0 1 0 1.4l-5 5a1 1 0 0 1-1.4 0Z"></path>
    </svg>
`;

let hybridMenuInitialized = false;

const state = {
    collapsed: false,
    projects: [],
    topPanel: {
        customerId: "",
        projectId: "",
        customerName: "-",
        projectName: "-",
        userName: "-"
    }
};

let sideMenu = null;
let projectPicker = null;
let projectPickerButton = null;
let projectPickerPanel = null;
let projectPickerList = null;
let projectPickerValue = null;
let projectPickerBadge = null;
let projectPickerChevron = null;
let collapseButton = null;
let menuBrand = null;
let menuBrandLogo = null;
let menuFooter = null;
let initialStateApplied = false;
let menuPreparingReleaseScheduled = false;

function getStorageFlag(key, fallback = false) {
    try {
        const value = window.localStorage.getItem(key);

        if (value === null) {
            return fallback;
        }

        return value === "1";
    } catch {
        return fallback;
    }
}

function setStorageFlag(key, value) {
    try {
        window.localStorage.setItem(key, value ? "1" : "0");
    } catch {
        // Ignore storage failures. The menu still works without persistence.
    }
}

function getStoredProjectId() {
    try {
        const value = window.localStorage.getItem(MENU_SELECTED_PROJECT_STORAGE_KEY);

        return value ? value.trim() : "";
    } catch {
        return "";
    }
}

export function setStoredProjectId(projectId) {
    try {
        if (projectId) {
            window.localStorage.setItem(MENU_SELECTED_PROJECT_STORAGE_KEY, String(projectId));
        } else {
            window.localStorage.removeItem(MENU_SELECTED_PROJECT_STORAGE_KEY);
        }
    } catch {
        // Ignore storage failures.
    }
}

function normalizeId(value) {
    return String(value ?? "").trim();
}

function getCurrentProjectId() {
    return normalizeId(state.topPanel?.projectId);
}

function primeMenuFromStorage() {
    if (initialStateApplied) {
        return;
    }

    state.collapsed = getStorageFlag(MENU_COLLAPSED_STORAGE_KEY, false);
    initialStateApplied = true;

    if (document.body) {
        document.body.classList.add("menu-preparing");
        document.body.classList.add("menu-is-open");
        document.body.classList.toggle("menu-is-collapsed", state.collapsed);
        document.body.style.setProperty("--menu-content-offset", `${getMenuContentOffset()}px`);
    }
}

function applyMenuTransitionStyles() {
    const app = document.querySelector(".app");

    if (app) {
        app.style.setProperty("transition", "padding-left 1200ms cubic-bezier(.22,.61,.36,1)", "important");
    }

    if (sideMenu) {
        sideMenu.style.setProperty("transition", MENU_TRANSITION, "important");
    }
}

function getMenuContentOffset() {
    return state.collapsed
        ? MENU_COLLAPSED_WIDTH
        : MENU_WIDTH;
}

function readMenuTopPanel(doc) {
    const topPanel = getDirectChild(doc?.documentElement || doc, "TopPanel")
        || doc?.querySelector?.("TopPanel")
        || doc?.getElementsByTagName?.("TopPanel")?.[0]
        || null;

    if (!topPanel) {
        return {
            customerId: "",
            projectId: "",
            customerName: "-",
            projectName: "-",
            userName: "-"
        };
    }

    return {
        customerId: normalizeId(textOf(topPanel, "CustomerId")),
        projectId: normalizeId(textOf(topPanel, "ProjectId")),
        customerName: normalizeText(textOf(topPanel, "CustomerName"), "-"),
        projectName: normalizeText(textOf(topPanel, "ProjectName"), "-"),
        userName: readFirstText(topPanel, ["Name", "UserName"], "-")
    };
}

function readProjectsFromXml(doc) {
    const projectsRoot = getDirectChild(doc?.documentElement || doc, "projects")
        || doc?.querySelector?.("projects")
        || doc?.getElementsByTagName?.("projects")?.[0]
        || null;

    if (!projectsRoot) {
        return [];
    }

    return Array.from(projectsRoot.children || [])
        .filter((child) => child.tagName === "project")
        .map((projectNode) => ({
            projectId: normalizeId(textOf(projectNode, "projectId")),
            projectName: normalizeText(textOf(projectNode, "projectName"), "Untitled")
        }))
        .filter((project) => project.projectId);
}

function normalizeText(value, fallback = "-") {
    const normalized = String(value ?? "").trim();

    return normalized || fallback;
}

function readFirstText(parent, tagNames, fallback = "") {
    const names = Array.isArray(tagNames) ? tagNames : [tagNames];

    for (const tagName of names) {
        const value = normalizeText(textOf(parent, tagName), "");

        if (value) {
            return value;
        }
    }

    return fallback;
}

function getActiveRouteKey(path) {
    if (!path) {
        return "path:/";
    }

    try {
        const url = new URL(path, window.location.origin);
        const page = url.searchParams.get("page");

        if (page) {
            return `page:${page.trim().toLowerCase()}`;
        }

        return `path:${url.pathname || "/"}`;
    } catch {
        return `path:${path}`;
    }
}

function setActiveLinks(root) {
    const currentKey = getActiveRouteKey(window.location.href);

    root.querySelectorAll("a[data-url]").forEach((link) => {
        const linkKey = getActiveRouteKey(link.getAttribute("data-url"));
        link.classList.toggle("is-active", linkKey === currentKey);
    });
}

function closeAllExcept(root, exceptLi) {
    root.querySelectorAll(".menu-main.is-open").forEach((li) => {
        if (li !== exceptLi) {
            li.classList.remove("is-open");
        }
    });
}

function buildMenu(doc, statusElement, rootElement) {
    clear(rootElement);

    const mainItems = Array.from(doc.getElementsByTagName("main-menu-item"));

    if (!mainItems.length) {
        setText(statusElement, "Ingen menupunkter blev returneret.", "");
        return;
    }

    mainItems.forEach((mainItem) => {
        const mainText = directTextOf(mainItem, "display") || "Untitled";
        const mainUrl = directTextOf(mainItem, "url") || "";
        const mainType = getMenuItemTypeId(mainItem);
        const mainIconSvg = directTextOf(mainItem, "iconSvg") || "";
        const subItems = Array.from(mainItem.getElementsByTagName("submain-menu-item"));

        const li = document.createElement("li");
        li.className = mainType === 1 ? "menu-main menu-header" : "menu-main";

        if (mainType === 1) {
            const header = document.createElement("div");
            header.className = "menu-header-label";

            if (mainIconSvg) {
                header.appendChild(createIconSpan(mainIconSvg));
            }

            const label = document.createElement("span");
            label.textContent = mainText;
            header.appendChild(label);

            li.appendChild(header);

            if (subItems.length) {
                const ulSub = document.createElement("ul");
                ulSub.className = "menu-sub menu-sub--section";

                subItems.forEach((subItem) => {
                    ulSub.appendChild(createSubMenuItem(doc, subItem));
                });

                li.appendChild(ulSub);
            }

            rootElement.appendChild(li);
            return;
        }

        const isMainLink = !!mainUrl && subItems.length === 0;

        if (isMainLink) {
            const link = document.createElement("a");
            link.className = "menu-main-link";
            link.href = mainUrl;
            link.setAttribute("data-url", mainUrl);
            if (mainIconSvg) {
                link.appendChild(createIconSpan(mainIconSvg));
            }

            const label = document.createElement("span");
            label.textContent = mainText;
            link.appendChild(label);

            li.appendChild(link);
            rootElement.appendChild(li);
            return;
        }

        const button = document.createElement("button");
        button.type = "button";
        button.className = "menu-main-btn";

        if (mainIconSvg) {
            button.appendChild(createIconSpan(mainIconSvg));
        }

        const label = document.createElement("span");
        label.textContent = mainText;

        button.append(label);

        const ulSub = document.createElement("ul");
        ulSub.className = "menu-sub";

        subItems.forEach((subItem) => {
            const subText = textOf(subItem, "display") || "Untitled";
            const subUrl = textOf(subItem, "url") || "/";

            const subLi = document.createElement("li");
            const link = document.createElement("a");

            link.href = subUrl;
            link.textContent = subText;
            link.setAttribute("data-url", subUrl);

            subLi.appendChild(link);
            ulSub.appendChild(subLi);
        });

        button.addEventListener("click", () => {
            const willOpen = !li.classList.contains("is-open");

            closeAllExcept(rootElement, li);
            li.classList.toggle("is-open", willOpen);
        });

        li.append(button, ulSub);
        rootElement.appendChild(li);
    });

    setText(statusElement, "Menu indl\u00e6st.", "");
    setActiveLinks(rootElement);
    refreshSidebarChrome();
}

function createSubMenuItem(doc, subItem) {
    const subText = textOf(subItem, "display") || "Untitled";
    const subUrl = textOf(subItem, "url") || "/";
    const subIconSvg = textOf(subItem, "iconSvg") || "";

    const subLi = document.createElement("li");
    const link = document.createElement("a");

    link.href = subUrl;
    link.setAttribute("data-url", subUrl);

    if (subIconSvg) {
        link.appendChild(createIconSpan(subIconSvg));
    }

    const label = document.createElement("span");
    label.textContent = subText;
    link.appendChild(label);

    subLi.appendChild(link);

    return subLi;
}

function createIconSpan(svgCode) {
    const span = document.createElement("span");
    span.className = "menu-item-icon";
    span.innerHTML = svgCode;
    return span;
}

function getMenuItemTypeId(menuItem) {
    const value = directTextOf(menuItem, "menuItemType") || "";
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) ? parsed : 2;
}

function createCollapseButton(doc) {
    const button = doc.createElement("button");
    button.type = "button";
    button.className = "menu-collapse-button";
    button.setAttribute("aria-label", "Collapse menu");
    button.setAttribute("aria-pressed", "false");
    button.title = "Collapse menu";

    const icon = doc.createElement("span");
    icon.className = "menu-collapse-icon";
    icon.setAttribute("aria-hidden", "true");
    icon.innerHTML = COLLAPSE_ICON_SVG;

    const label = doc.createElement("span");
    label.className = "menu-collapse-label";
    label.textContent = "Collapse";

    button.append(icon, label);

    return button;
}

function createSidebarBrand(doc) {
    const brand = doc.createElement("div");
    brand.className = "menu-brand";

    menuBrandLogo = doc.createElement("img");
    menuBrandLogo.className = "menu-brand-logo";
    menuBrandLogo.alt = "EIS Engineering In Systems";
    menuBrandLogo.src = "/svg/eis-logo-light.svg";
    brand.appendChild(menuBrandLogo);

    return brand;
}

function createSidebarProjectPicker(doc) {
    const picker = doc.createElement("div");
    picker.className = "menu-project-picker";

    picker.innerHTML = `
        <button type="button" class="menu-project-picker-button" aria-haspopup="listbox" aria-expanded="false">
            <span class="menu-project-picker-badge" aria-hidden="true">D4</span>
            <span class="menu-project-picker-copy">
                <span class="menu-project-picker-label">Active project</span>
                <span class="menu-project-picker-value" id="menuProjectValue">-</span>
            </span>
            <span class="menu-project-picker-chevron" aria-hidden="true">\u25BE</span>
        </button>
        <div class="menu-project-picker-panel" hidden>
            <div class="menu-project-picker-list" role="listbox" aria-label="Projects"></div>
        </div>
    `;

    projectPickerButton = picker.querySelector(".menu-project-picker-button");
    projectPickerPanel = picker.querySelector(".menu-project-picker-panel");
    projectPickerList = picker.querySelector(".menu-project-picker-list");
    projectPickerValue = picker.querySelector(".menu-project-picker-value");
    projectPickerBadge = picker.querySelector(".menu-project-picker-badge");
    projectPickerChevron = picker.querySelector(".menu-project-picker-chevron");

    projectPickerButton?.addEventListener("click", (event) => {
        event.preventDefault();
        event.stopPropagation();

        if (state.collapsed) {
            return;
        }

        toggleProjectPickerPanel();
    });

    document.addEventListener("click", (event) => {
        if (!projectPickerPanel || projectPickerPanel.hidden) {
            return;
        }

        if (projectPicker.contains(event.target)) {
            return;
        }

        closeProjectPickerPanel();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeProjectPickerPanel();
        }
    });

    return picker;
}

function createSidebarFooter(doc) {
    const footer = doc.createElement("div");
    footer.className = "menu-footer";

    footer.innerHTML = `
        <div class="menu-footer-collapse"></div>
        <div class="menu-footer-card" aria-label="User and customer information">
            <div class="menu-footer-avatar" aria-hidden="true">U</div>
            <div class="menu-footer-copy">
                <strong id="menuFooterUserName" class="menu-footer-value menu-footer-user" aria-label="User name">-</strong>
                <strong id="menuFooterCustomerName" class="menu-footer-value menu-footer-customer" aria-label="Customer name">-</strong>
            </div>
        </div>
    `;

    const collapse = footer.querySelector(".menu-footer-collapse");
    collapse?.appendChild(createCollapseButton(doc));

    return footer;
}

function renderProjectPicker() {
    if (!projectPicker || !projectPickerList || !projectPickerValue) {
        return;
    }

    const currentProjectId = getCurrentProjectId();
    const storedProjectId = getStoredProjectId();
    const selectedProjectId = currentProjectId || storedProjectId;
    const selectedProject = state.projects.find((project) => project.projectId === selectedProjectId)
        || state.projects.find((project) => project.projectId === currentProjectId)
        || state.projects[0]
        || null;

    projectPickerValue.textContent = selectedProject?.projectName || state.topPanel.projectName || "-";

    if (projectPickerBadge) {
        projectPickerBadge.textContent = "D4";
    }

    projectPickerList.replaceChildren();

    state.projects.forEach((project) => {
        const item = document.createElement("button");
        item.type = "button";
        item.className = "menu-project-picker-item";
        item.setAttribute("role", "option");
        item.setAttribute("data-project-id", project.projectId);
        item.setAttribute("aria-selected", project.projectId === selectedProjectId ? "true" : "false");
        item.textContent = project.projectName;

        if (project.projectId === selectedProjectId) {
            item.classList.add("is-selected");
        }

        item.addEventListener("click", async (event) => {
            event.preventDefault();
            event.stopPropagation();

            closeProjectPickerPanel();
            try {
                await applyProjectSelection(project.projectId, true);
            } catch (error) {
                console.error(error);
            }
        });

        projectPickerList.appendChild(item);
    });

    const hasProjects = state.projects.length > 0;
    projectPicker.classList.toggle("has-projects", hasProjects);
    projectPicker.classList.toggle("is-empty", !hasProjects);
    projectPickerButton.disabled = !hasProjects;
    projectPickerButton.setAttribute("aria-expanded", projectPickerPanel.hidden ? "false" : "true");
}

function openProjectPickerPanel() {
    if (!projectPickerPanel || !projectPickerButton || state.collapsed || !state.projects.length) {
        return;
    }

    projectPickerPanel.hidden = false;
    projectPicker.classList.add("is-open");
    projectPickerButton.setAttribute("aria-expanded", "true");
}

function closeProjectPickerPanel() {
    if (!projectPickerPanel || !projectPickerButton) {
        return;
    }

    projectPickerPanel.hidden = true;
    projectPicker.classList.remove("is-open");
    projectPickerButton.setAttribute("aria-expanded", "false");
}

function toggleProjectPickerPanel() {
    if (!projectPickerPanel || projectPickerPanel.hidden) {
        openProjectPickerPanel();
    } else {
        closeProjectPickerPanel();
    }
}

async function applyProjectSelection(projectId, redirectToOverview = false, persistProjectId = true) {
    const nextProjectId = normalizeId(projectId);

    if (!nextProjectId) {
        return;
    }

    if (getCurrentProjectId() === nextProjectId) {
        if (persistProjectId) {
            setStoredProjectId(nextProjectId);
        }
        return;
    }

    const body = new URLSearchParams();
    body.set("cmd", MENU_SELECT_PROJECT_CMD);
    body.set("projectId", nextProjectId);

    const response = await fetch(MENU_URL, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "Accept": "application/xml, text/xml"
        },
        credentials: "same-origin",
        body
    });

    if (!response.ok) {
        throw new Error(`HTTP ${response.status} from ${MENU_URL}`);
    }

    const xmlText = await response.text();
    const redirectUrl = readRedirectUrlFromXml(xmlText) || PROJECT_OVERVIEW_URL;

    if (persistProjectId) {
        setStoredProjectId(nextProjectId);
    }

    if (redirectToOverview) {
        const separator = redirectUrl.includes("?") ? "&" : "?";
        window.location.replace(`${redirectUrl}${separator}_ts=${Date.now()}`);
    }
}

function readRedirectUrlFromXml(xmlText) {
    if (!xmlText) {
        return "";
    }

    const parser = new DOMParser();
    const doc = parser.parseFromString(xmlText, "application/xml");

    if (doc.getElementsByTagName("parsererror").length) {
        return "";
    }

    const element = doc.querySelector("redirectUrl");

    return element?.textContent?.trim() || "";
}

function refreshSidebarChrome() {
    const userName = state.topPanel?.userName?.trim() || byId("userName")?.textContent?.trim() || "-";
    const customerName = state.topPanel?.customerName?.trim() || byId("customerName")?.textContent?.trim() || "-";
    const menuFooterUserName = byId("menuFooterUserName");
    const menuFooterCustomerName = byId("menuFooterCustomerName");
    const avatar = sideMenu?.querySelector(".menu-footer-avatar");

    if (menuFooterUserName) {
        menuFooterUserName.textContent = userName;
    }

    if (menuFooterCustomerName) {
        menuFooterCustomerName.textContent = customerName;
    }

    if (avatar) {
        const initials = getInitials(userName);
        avatar.textContent = initials || "U";
    }

    if (projectPickerValue) {
        renderProjectPicker();
    }
}

function updateSidebarBrandLogo() {
    if (!menuBrandLogo) {
        return;
    }

    menuBrandLogo.src = state.collapsed
        ? "/svg/eis-app-mark.svg"
        : "/svg/eis-logo-light.svg";
}

function getInitials(value) {
    const tokens = String(value || "").trim().split(/\s+/).filter(Boolean);

    if (!tokens.length) {
        return "";
    }

    return tokens.slice(0, 2).map((token) => token.charAt(0).toUpperCase()).join("");
}

function applyLayoutState() {
    const body = document.body;

    if (!body || !sideMenu) {
        return;
    }

    body.classList.add("menu-is-open");
    body.classList.toggle("menu-is-collapsed", state.collapsed);
    body.style.setProperty("--menu-content-offset", `${getMenuContentOffset()}px`);

    sideMenu.classList.toggle("is-collapsed", state.collapsed);

    if (collapseButton) {
        collapseButton.classList.toggle("is-active", state.collapsed);
        collapseButton.setAttribute("aria-pressed", state.collapsed ? "true" : "false");
        collapseButton.setAttribute("aria-label", state.collapsed ? "Expand menu" : "Collapse menu");
        collapseButton.title = state.collapsed ? "Expand menu" : "Collapse menu";

        const label = collapseButton.querySelector(".menu-collapse-label");
        const icon = collapseButton.querySelector(".menu-collapse-icon");

        if (label) {
            label.textContent = state.collapsed ? "Expand" : "Collapse";
        }

        if (icon) {
            icon.innerHTML = state.collapsed
                ? EXPAND_ICON_SVG
                : COLLAPSE_ICON_SVG;
        }
    }

    if (projectPicker) {
        projectPicker.classList.toggle("is-collapsed", state.collapsed);
    }

    updateSidebarBrandLogo();

    if (state.collapsed) {
        closeProjectPickerPanel();
    }

    if (initialStateApplied && body.classList.contains("menu-preparing") && !menuPreparingReleaseScheduled) {
        menuPreparingReleaseScheduled = true;
        window.requestAnimationFrame(() => {
            window.requestAnimationFrame(() => {
                document.body?.classList.remove("menu-preparing");
                applyMenuTransitionStyles();
            });
        });
    }
}

function setCollapsed(nextCollapsed) {
    state.collapsed = nextCollapsed;
    setStorageFlag(MENU_COLLAPSED_STORAGE_KEY, nextCollapsed);
    applyLayoutState();
}

function toggleCollapsedMenu() {
    setCollapsed(!state.collapsed);
}

primeMenuFromStorage();

function initializeSidebar() {
    if (hybridMenuInitialized) {
        return;
    }

    primeMenuFromStorage();

    sideMenu = byId("sideMenu");

    if (!sideMenu) {
        return;
    }

    hybridMenuInitialized = true;

    menuBrand = createSidebarBrand(document);
    sideMenu.insertBefore(menuBrand, sideMenu.firstChild);

    projectPicker = createSidebarProjectPicker(document);
    sideMenu.insertBefore(projectPicker, menuBrand.nextSibling);

    menuFooter = createSidebarFooter(document);
    sideMenu.appendChild(menuFooter);

    collapseButton = menuFooter.querySelector(".menu-collapse-button");

    const title = sideMenu.querySelector("h2");
    if (title) {
        title.className = "menu-brand-heading";
        title.textContent = "";
        title.remove();
    }

    applyLayoutState();
    refreshSidebarChrome();

    collapseButton.addEventListener("click", (event) => {
        event.preventDefault();
        event.stopPropagation();
        toggleCollapsedMenu();
    });
}

export async function initMenu() {
    initializeSidebar();

    const statusElement = byId("menuStatus");
    const rootElement = byId("menuRoot");

    if (!statusElement || !rootElement) {
        return;
    }

    setText(statusElement, "Indl\u00e6ser menu...", "");

    try {
        const doc = await fetchXml(MENU_URL);
        state.topPanel = readMenuTopPanel(doc);
        state.projects = readProjectsFromXml(doc);
        buildMenu(doc, statusElement, rootElement);
    } catch (error) {
        setText(statusElement, "Kunne ikke indl\u00e6se menu.", "");
        clear(rootElement);
        console.error(error);
    }
}
