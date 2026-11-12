import {escapeHtml} from "../core/html.js";

// One instance per table keeps the two requirements tabs independent.
export function createHierarchyTable(codeKeys, {showAllRootToggles = false} = {}) {
    const collapsed = new Set();
    return {
        setAll(items, expanded) {
            collapsed.clear();
            if (!expanded) {
                for (const item of items) {
                    if (item.id && !item.id.includes(".")) collapsed.add(item.id);
                }
            }
        },
        apply(tbody, items, columns, grouped) {
            const table = tbody.closest("table");
            table.classList.add("hierarchy-table");
            const byIndex = new Map(items.map((item) => [String(item.index), item]));
            const codeIndex = columns.findIndex((column) => codeKeys.includes(column.key));
            const rootsWithChildren = new Set(items.filter((item) => item.id.includes("."))
                .map((item) => item.id.split(".")[0]));
            const rows = Array.from(tbody.querySelectorAll("tr[data-requirement-index]"));
            const refresh = () => {
                for (const row of rows) {
                    const item = byIndex.get(row.dataset.requirementIndex);
                    if (!item) continue;
                    const root = item.id.split(".")[0];
                    row.hidden = !grouped && item.id !== root && collapsed.has(root);
                    const button = row.querySelector(".hierarchy-toggle");
                    if (button) {
                        const expanded = !collapsed.has(root);
                        button.setAttribute("aria-expanded", String(expanded));
                        button.setAttribute("aria-label", `${expanded ? "Collapse" : "Expand"} ${item.name || item.id}`);
                    }
                }
            };
            for (const row of rows) {
                const item = byIndex.get(row.dataset.requirementIndex);
                if (!item) continue;
                const isRoot = Boolean(item.id) && !item.id.includes(".");
                row.classList.toggle("hierarchy-root-row", isRoot);
                columns.forEach((column, index) => {
                    if (["FunctionalName", "LogicalName", "RequirementName", "StakeholderRequirementName",
                        "SystemRequirementName", "ReqName", "Name", "Title"].includes(column.key)) {
                        row.cells[index]?.classList.add("hierarchy-name-cell");
                    }
                });
                const cell = row.cells[codeIndex];
                if (!cell) continue;
                const label = cell.querySelector(".data-table-cell-value")?.textContent || item.id;
                const canToggle = isRoot && !grouped && (showAllRootToggles || rootsWithChildren.has(item.id));
                cell.innerHTML = `<div class="hierarchy-code-cell" style="--hierarchy-depth:${item.id.split(".").length - 1}">
                    ${canToggle ? '<button type="button" class="hierarchy-toggle"><svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m9 6 6 6-6 6"/></svg></button>' : '<span class="hierarchy-toggle-spacer" aria-hidden="true"></span>'}
                    <span class="data-table-cell-value">${escapeHtml(label)}</span></div>`;
                const button = cell.querySelector("button");
                if (button) {
                    button.addEventListener("dblclick", (event) => event.stopPropagation());
                    button.addEventListener("click", (event) => {
                        event.stopPropagation();
                        if (collapsed.has(item.id)) collapsed.delete(item.id);
                        else collapsed.add(item.id);
                        refresh();
                    });
                }
            }
            refresh();
        }
    };
}
