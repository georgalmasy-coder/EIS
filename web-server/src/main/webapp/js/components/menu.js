import {
    byId,
    clear,
    setText
} from "../core/dom.js";
import { fetchXml } from "../core/http.js";
import { directTextOf, textOf } from "../core/xml.js";

const MENU_URL = "/Menu";

let hybridMenuInitialized = false;

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
                arrow.textContent = "▸";
            }
        }
    });
}

function buildMenu(doc, statusElement, rootElement) {
    clear(rootElement);

    const mainItems = Array.from(doc.getElementsByTagName("main-menu-item"));

    if (!mainItems.length) {
        setText(statusElement, "No menu items returned.", "");
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
        arrow.textContent = "▸";

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
            arrow.textContent = willOpen ? "▾" : "▸";
        });

        li.append(button, ulSub);
        rootElement.appendChild(li);
    });

    setText(statusElement, "Menu loaded.", "");
    setActiveLinks(rootElement);
}

function initializeHybridMenu() {
    if (hybridMenuInitialized) {
        return;
    }

    const toggleButton = byId("menuToggle");
    const sideMenu = byId("sideMenu");
    const leftHoverZone = byId("leftHoverZone");

    if (!toggleButton || !sideMenu || !leftHoverZone) {
        return;
    }

    hybridMenuInitialized = true;

    const supportsDesktopHover = window.matchMedia("(hover: hover) and (pointer: fine)");

    function isDesktopHoverMode() {
        return supportsDesktopHover.matches;
    }

    function openTouchMenu() {
        sideMenu.classList.add("is-open");
        toggleButton.classList.add("is-open");
        toggleButton.setAttribute("aria-expanded", "true");
        toggleButton.setAttribute("aria-label", "Close menu");
        toggleButton.textContent = "‹";
    }

    function closeTouchMenu() {
        sideMenu.classList.remove("is-open");
        toggleButton.classList.remove("is-open");
        toggleButton.setAttribute("aria-expanded", "false");
        toggleButton.setAttribute("aria-label", "Open menu");
        toggleButton.textContent = "›";
    }

    function toggleTouchMenu() {
        if (sideMenu.classList.contains("is-open")) {
            closeTouchMenu();
        } else {
            openTouchMenu();
        }
    }

    function openHoverMenu() {
        sideMenu.classList.add("is-hover-open");
        toggleButton.classList.add("is-hover-open");
        toggleButton.setAttribute("aria-expanded", "true");
        toggleButton.textContent = "‹";
    }

    function closeHoverMenu() {
        sideMenu.classList.remove("is-hover-open");
        toggleButton.classList.remove("is-hover-open");

        if (!sideMenu.classList.contains("is-open")) {
            toggleButton.setAttribute("aria-expanded", "false");
            toggleButton.textContent = "›";
        }
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

    function handleHoverEnter() {
        if (!isDesktopHoverMode()) {
            return;
        }

        openHoverMenu();
    }

    function handleHoverLeave(event) {
        if (!isDesktopHoverMode()) {
            return;
        }

        if (isPointerStillInsideMenuArea(event)) {
            return;
        }

        closeHoverMenu();
    }

    leftHoverZone.addEventListener("pointerenter", handleHoverEnter);
    sideMenu.addEventListener("pointerenter", handleHoverEnter);
    toggleButton.addEventListener("pointerenter", handleHoverEnter);

    leftHoverZone.addEventListener("pointerleave", handleHoverLeave);
    sideMenu.addEventListener("pointerleave", handleHoverLeave);
    toggleButton.addEventListener("pointerleave", handleHoverLeave);

    toggleButton.addEventListener("click", (event) => {
        event.preventDefault();
        event.stopPropagation();

        if (isDesktopHoverMode()) {
            return;
        }

        toggleTouchMenu();
    });

    document.addEventListener("click", (event) => {
        if (isDesktopHoverMode()) {
            return;
        }

        const clickedInsideMenu = sideMenu.contains(event.target);
        const clickedToggleButton = toggleButton.contains(event.target);

        if (clickedInsideMenu || clickedToggleButton) {
            return;
        }

        closeTouchMenu();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") {
            return;
        }

        closeTouchMenu();
        closeHoverMenu();
    });

    supportsDesktopHover.addEventListener("change", () => {
        closeTouchMenu();
        closeHoverMenu();
    });
}

export async function initMenu() {
    initializeHybridMenu();

    const statusElement = byId("menuStatus");
    const rootElement = byId("menuRoot");

    if (!statusElement || !rootElement) {
        return;
    }

    setText(statusElement, "Loading menu…", "");

    try {
        const doc = await fetchXml(MENU_URL);
        buildMenu(doc, statusElement, rootElement);
    } catch (error) {
        setText(statusElement, "Failed to load menu.", "");
        clear(rootElement);
        console.error(error);
    }
}