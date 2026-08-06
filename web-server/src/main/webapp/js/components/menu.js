import {
    byId,
    clear,
    setText
} from "../core/dom.js";
import { fetchXml } from "../core/http.js";
import { directTextOf, textOf } from "../core/xml.js";

const MENU_URL = "/Menu";
const MENU_WIDTH = 280;
const MENU_HANDLE_WIDTH = 22;
const MENU_OPEN_GAP = 6;
const MENU_CLOSED_OFFSET = 23;
const MENU_TRANSITION = "transform 1200ms cubic-bezier(.22,.61,.36,1)";
const MENU_PIN_STORAGE_KEY = "eis.menu.pinned";
const RIGHT_ARROW = "\u25B8";
const DOWN_ARROW = "\u25BE";

let hybridMenuInitialized = false;

const state = {
    explicitOpen: false,
    hoverOpen: false,
    pinned: false
};

let toggleButton = null;
let sideMenu = null;
let leftHoverZone = null;
let pinButton = null;
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

function primeMenuFromStorage() {
    if (initialStateApplied) {
        return;
    }

    state.pinned = getStorageFlag(MENU_PIN_STORAGE_KEY, false);
    state.explicitOpen = state.pinned;
    initialStateApplied = true;

    if (document.body) {
        document.body.classList.add("menu-preparing");
        document.body.classList.toggle("menu-is-pinned", state.pinned);
        document.body.style.setProperty("--menu-content-offset", `${getMenuContentOffset(state.pinned)}px`);
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

function getMenuContentOffset(isVisible) {
    return isVisible
        ? MENU_WIDTH + MENU_OPEN_GAP
        : MENU_CLOSED_OFFSET;
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

            const arrow = li.querySelector(".menu-arrow");

            if (arrow) {
                arrow.textContent = RIGHT_ARROW;
            }
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
        const subItems = Array.from(mainItem.getElementsByTagName("submain-menu-item"));

        const li = document.createElement("li");
        li.className = "menu-main";

        const isMainLink = !!mainUrl && subItems.length === 0;

        if (isMainLink) {
            const link = document.createElement("a");
            link.className = "menu-main-link";
            link.href = mainUrl;
            link.textContent = mainText;
            link.setAttribute("data-url", mainUrl);

            li.appendChild(link);
            rootElement.appendChild(li);
            return;
        }

        const button = document.createElement("button");
        button.type = "button";
        button.className = "menu-main-btn";

        const arrow = document.createElement("span");
        arrow.className = "menu-arrow";
        arrow.textContent = RIGHT_ARROW;

        const label = document.createElement("span");
        label.textContent = mainText;

        button.append(arrow, label);

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
            arrow.textContent = willOpen ? DOWN_ARROW : RIGHT_ARROW;
        });

        li.append(button, ulSub);
        rootElement.appendChild(li);
    });

    setText(statusElement, "Menu indl\u00e6st.", "");
    setActiveLinks(rootElement);
}

function createPinButton(doc) {
    const button = doc.createElement("button");
    button.type = "button";
    button.className = "menu-pin-button";
    button.setAttribute("aria-label", "Fastg\u00f8r menu");
    button.setAttribute("aria-pressed", "false");
    button.title = "Fastg\u00f8r menu";
    const image = doc.createElement("img");
    image.className = "menu-pin-image";
    image.src = "/images/menu-pin.png";
    image.alt = "";
    image.setAttribute("aria-hidden", "true");
    button.appendChild(image);

    return button;
}

function applyLayoutState() {
    const isVisible = state.pinned || state.hoverOpen || state.explicitOpen;
    const body = document.body;

    if (!body || !toggleButton || !sideMenu) {
        return;
    }

    body.classList.toggle("menu-is-open", isVisible);
    body.classList.toggle("menu-is-pinned", state.pinned);
    body.style.setProperty("--menu-content-offset", `${getMenuContentOffset(isVisible)}px`);

    sideMenu.classList.toggle("is-open", isVisible);
    sideMenu.classList.toggle("is-hover-open", state.hoverOpen && !state.pinned);
    sideMenu.classList.toggle("is-pinned", state.pinned);

    toggleButton.classList.toggle("is-open", isVisible);
    toggleButton.classList.toggle("is-hover-open", state.hoverOpen && !state.pinned);
    toggleButton.classList.toggle("is-pinned", state.pinned);
    toggleButton.setAttribute("aria-expanded", isVisible ? "true" : "false");
    toggleButton.setAttribute("aria-label", isVisible ? "Luk menu" : "A\u00e5bn menu");

    if (pinButton) {
        pinButton.classList.toggle("is-active", state.pinned);
        pinButton.setAttribute("aria-pressed", state.pinned ? "true" : "false");
        pinButton.setAttribute("aria-label", state.pinned ? "Frig\u00f8r menu" : "Fastg\u00f8r menu");
        pinButton.title = state.pinned ? "Frig\u00f8r menu" : "Fastg\u00f8r menu";
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

function setPinned(nextPinned) {
    state.pinned = nextPinned;
    setStorageFlag(MENU_PIN_STORAGE_KEY, nextPinned);

    if (nextPinned) {
        state.explicitOpen = true;
    } else {
        state.explicitOpen = false;
    }

    applyLayoutState();
}

function setExplicitOpen(nextOpen) {
    state.explicitOpen = nextOpen;
    applyLayoutState();
}

function openHoverMenu() {
    if (state.pinned) {
        return;
    }

    state.hoverOpen = true;
    applyLayoutState();
}

function closeHoverMenu() {
    state.hoverOpen = false;
    applyLayoutState();
}

function toggleExplicitMenu() {
    if (state.pinned) {
        state.explicitOpen = true;
        applyLayoutState();
        return;
    }

    setExplicitOpen(!state.explicitOpen);
}

function isDesktopHoverMode() {
    return window.matchMedia("(hover: hover) and (pointer: fine)").matches;
}

function isPointerStillInsideMenuArea(event) {
    const nextElement = event.relatedTarget;

    if (!(nextElement instanceof Node)) {
        return false;
    }

    return sideMenu.contains(nextElement)
        || toggleButton.contains(nextElement)
        || leftHoverZone.contains(nextElement);
}

primeMenuFromStorage();

function initializeHybridMenu() {
    if (hybridMenuInitialized) {
        return;
    }

    primeMenuFromStorage();

    toggleButton = byId("menuToggle");
    sideMenu = byId("sideMenu");
    leftHoverZone = byId("leftHoverZone");

    if (!toggleButton || !sideMenu || !leftHoverZone) {
        return;
    }

    hybridMenuInitialized = true;

    pinButton = createPinButton(document);
    sideMenu.insertBefore(pinButton, sideMenu.firstChild);

    toggleButton.textContent = "";
    const label = document.createElement("span");
    label.className = "menu-toggle-label";
    label.textContent = "Menu";
    toggleButton.appendChild(label);

    applyLayoutState();

    const hoverMediaQuery = window.matchMedia("(hover: hover) and (pointer: fine)");

    leftHoverZone.addEventListener("pointerenter", () => {
        if (hoverMediaQuery.matches) {
            openHoverMenu();
        }
    });

    sideMenu.addEventListener("pointerenter", () => {
        if (hoverMediaQuery.matches) {
            openHoverMenu();
        }
    });

    toggleButton.addEventListener("pointerenter", () => {
        if (hoverMediaQuery.matches) {
            openHoverMenu();
        }
    });

    leftHoverZone.addEventListener("pointerleave", (event) => {
        if (!hoverMediaQuery.matches || state.pinned) {
            return;
        }

        if (isPointerStillInsideMenuArea(event)) {
            return;
        }

        closeHoverMenu();
    });

    sideMenu.addEventListener("pointerleave", (event) => {
        if (!hoverMediaQuery.matches || state.pinned) {
            return;
        }

        if (isPointerStillInsideMenuArea(event)) {
            return;
        }

        closeHoverMenu();
    });

    toggleButton.addEventListener("pointerleave", (event) => {
        if (!hoverMediaQuery.matches || state.pinned) {
            return;
        }

        if (isPointerStillInsideMenuArea(event)) {
            return;
        }

        closeHoverMenu();
    });

    toggleButton.addEventListener("click", (event) => {
        event.preventDefault();
        event.stopPropagation();
        toggleExplicitMenu();
    });

    pinButton.addEventListener("click", (event) => {
        event.preventDefault();
        event.stopPropagation();
        setPinned(!state.pinned);
    });

    document.addEventListener("click", (event) => {
        const clickedInsideMenu = sideMenu.contains(event.target);
        const clickedToggleButton = toggleButton.contains(event.target);

        if (clickedInsideMenu || clickedToggleButton) {
            return;
        }

        if (!state.pinned) {
            setExplicitOpen(false);
            closeHoverMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") {
            return;
        }

        if (!state.pinned) {
            setExplicitOpen(false);
            closeHoverMenu();
        }
    });

    window.addEventListener("storage", (event) => {
        if (event.key !== MENU_PIN_STORAGE_KEY) {
            return;
        }

        const nextPinned = event.newValue === "1";
        state.pinned = nextPinned;

        if (nextPinned) {
            state.explicitOpen = true;
        } else {
            state.explicitOpen = false;
        }

        applyLayoutState();
    });
}

export async function initMenu() {
    initializeHybridMenu();

    const statusElement = byId("menuStatus");
    const rootElement = byId("menuRoot");

    if (!statusElement || !rootElement) {
        return;
    }

    setText(statusElement, "Indl\u00e6ser menu...", "");

    try {
        const doc = await fetchXml(MENU_URL);
        buildMenu(doc, statusElement, rootElement);
    } catch (error) {
        setText(statusElement, "Kunne ikke indl\u00e6se menu.", "");
        clear(rootElement);
        console.error(error);
    }
}
