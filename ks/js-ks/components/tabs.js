import { byId } from "../core/dom.js";

export function initTabs(tabs, initialIndex = 0) {
    if (!Array.isArray(tabs) || !tabs.length) {
        return;
    }

    function setActive(tabIndex) {
        tabs.forEach((tab, index) => {
            const btn = byId(tab.btnId);
            const panel = byId(tab.panelId);
            const active = index === tabIndex;

            if (!btn || !panel) {
                return;
            }

            btn.classList.toggle("is-active", active);
            panel.classList.toggle("is-active", active);
            btn.setAttribute("aria-selected", active ? "true" : "false");

            if ("hidden" in panel) {
                panel.hidden = !active;
            }
        });
    }

    tabs.forEach((tab, index) => {
        const btn = byId(tab.btnId);

        if (!btn) {
            return;
        }

        btn.addEventListener("click", () => setActive(index));
    });

    setActive(initialIndex);
}