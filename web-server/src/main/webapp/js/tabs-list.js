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

    function ensureTableFooter(table) {
        const frame = table?.closest?.(".table-frame.has-table-footer");

        if (!frame) {
            return null;
        }

        let footer = frame.querySelector(".data-table-footer");

        if (footer) {
            return footer;
        }

        footer = document.createElement("div");
        footer.className = "data-table-footer";
        footer.innerHTML = `
            <span class="data-table-footer-count" aria-live="polite"></span>
        `;

        const scroll = frame.querySelector(".table-scroll");

        if (scroll?.nextSibling) {
            frame.insertBefore(footer, scroll.nextSibling);
        } else {
            frame.appendChild(footer);
        }

        return footer;
    }

    function countVisibleRows(table) {
        const tbody = table?.tBodies?.[0] || table?.querySelector?.("tbody");

        if (!tbody) {
            return 0;
        }

        return Array.from(tbody.querySelectorAll("tr"))
            .filter((row) => !row.hidden && window.getComputedStyle(row).display !== "none")
            .length;
    }

    function getFooterCount(table, key, fallback) {
        const value = Number.parseInt(table?.dataset?.[key] || "", 10);
        return Number.isFinite(value) ? value : fallback;
    }

    function syncTableFooter(table) {
        const footer = ensureTableFooter(table);

        if (!footer) {
            return;
        }

        const countElement = footer.querySelector(".data-table-footer-count");

        if (!countElement) {
            return;
        }

        const visibleCount = getFooterCount(table, "filteredRowCount", countVisibleRows(table));
        const totalCount = getFooterCount(table, "totalRowCount", visibleCount);

        countElement.textContent = `${visibleCount} of ${totalCount}`;
    }

    function syncDataTableFooters(root = document) {
        const scope = root && root.querySelectorAll ? root : document;

        scope.querySelectorAll(".data-table").forEach(syncTableFooter);
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

                    const table = node.matches?.(".data-table")
                        ? node
                        : node.closest?.(".data-table");

                    if (table) {
                        syncTableFooter(table);
                    } else {
                        syncDataTableFooters(node);
                    }
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
    syncDataTableFooters(document);
    observeTableChanges();

    window.syncDataTableFooters = syncDataTableFooters;
})();
