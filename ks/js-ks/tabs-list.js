(function () {
    "use strict";

    const tabs = [
        { btnId: "tabBtn1", panelId: "tabPanel1" },
        { btnId: "tabBtn2", panelId: "tabPanel2" },
        { btnId: "tabBtn3", panelId: "tabPanel3" }
    ];

    function setActive(tabIndex) {
        tabs.forEach((t, i) => {
            const btn = document.getElementById(t.btnId);
            const panel = document.getElementById(t.panelId);

            if (!btn || !panel) {
                return;
            }

            const active = i === tabIndex;

            btn.classList.toggle("is-active", active);
            panel.classList.toggle("is-active", active);
            btn.setAttribute("aria-selected", active ? "true" : "false");
        });
    }

    function initTabs() {
        tabs.forEach((t, i) => {
            const btn = document.getElementById(t.btnId);

            if (!btn) {
                return;
            }

            btn.addEventListener("click", () => setActive(i));
        });
    }

    function shouldSkipCell(td) {
        return td.classList.contains("col-id")
            || td.querySelector(".data-table-cell-value");
    }

    function wrapCellContent(td) {
        if (shouldSkipCell(td)) {
            return;
        }

        const wrapper = document.createElement("span");
        wrapper.className = "data-table-cell-value";

        while (td.firstChild) {
            wrapper.appendChild(td.firstChild);
        }

        td.appendChild(wrapper);
    }

    function wrapTableCells(root) {
        const scope = root && root.querySelectorAll ? root : document;

        scope
            .querySelectorAll(".data-table tbody td")
            .forEach(wrapCellContent);
    }

    function observeTableChanges() {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType !== Node.ELEMENT_NODE) {
                        return;
                    }

                    if (node.matches && node.matches(".data-table tbody td")) {
                        wrapCellContent(node);
                        return;
                    }

                    wrapTableCells(node);
                });
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    initTabs();
    wrapTableCells(document);
    observeTableChanges();
})();