// Delegation also covers headers/cells replaced by the virtual matrix viewport.
export function installInterfaceHeaderHover(table) {
    if (!table) return;
    let marked = [];
    const clear = () => {
        marked.forEach(header => header.classList.remove("interfaces-header-hover"));
        marked = [];
    };
    table.addEventListener("mouseover", event => {
        const target = event.target.closest(".interfaces-cell, .interfaces-row-header, .interfaces-column-header");
        clear();
        if (!target) return;
        if (target.matches(".interfaces-row-header, .interfaces-column-header")) {
            marked = [target];
        } else {
            marked = Array.from(table.querySelectorAll(".interfaces-row-header, .interfaces-column-header"))
                .filter(header => header.dataset.entityId === (header.classList.contains("interfaces-row-header")
                    ? target.dataset.fromEntityId : target.dataset.toEntityId));
        }
        marked.forEach(header => header.classList.add("interfaces-header-hover"));
    });
    table.addEventListener("mouseleave", clear);
    table.closest(".interfaces-table-scroll")?.addEventListener("scroll", clear, {passive: true});
    window.addEventListener("blur", clear);
}

// Draw each part inside its own cell so backgrounds stay below the arrows and
// existing content, tooltips and mouse interactions stay above them.
export function installInterfaceFlowArrows(tableBody, {getDiagonalRect} = {}) {
    let activeCell = null;
    let arrows = [];

    function clear() {
        arrows.forEach(arrow => arrow.remove());
        arrows = [];
        activeCell = null;
    }

    tableBody.addEventListener("mouseover", event => {
        const cell = event.target.closest("td.interfaces-cell");
        if (cell === activeCell) return;
        clear();
        if (!cell || cell.classList.contains("interfaces-cell-self")) return;

        const cells = Array.from(tableBody.querySelectorAll("td.interfaces-cell"));
        const from = cell.dataset.fromEntityId;
        const to = cell.dataset.toEntityId;
        const source = cells.find(item => item.dataset.fromEntityId === from && item.dataset.toEntityId === from);
        const target = cells.find(item => item.dataset.fromEntityId === to && item.dataset.toEntityId === to);
        const a = source?.getBoundingClientRect() || getDiagonalRect?.(from);
        const b = target?.getBoundingClientRect() || getDiagonalRect?.(to);
        if (!a || !b) return;
        activeCell = cell;

        const ax = a.left + a.width / 2;
        const ay = a.top + a.height / 2;
        const bx = b.left + b.width / 2;
        const by = b.top + b.height / 2;
        const direction = Math.sign(by - ay);
        if (!direction) return;

        // Two directed, bent arrows form the loop through the reciprocal cells.
        const forwardEnd = by - direction * b.height / 2;
        const reverseEnd = ay + direction * a.height / 2;
        const paths = [
            `M ${ax + direction * a.width / 2} ${ay} H ${bx} V ${forwardEnd}`,
            `M ${bx - direction * b.width / 2} ${by} H ${ax} V ${reverseEnd}`,
            `M ${bx - 12} ${forwardEnd - direction * 14} L ${bx} ${forwardEnd} L ${bx + 12} ${forwardEnd - direction * 14}`,
            `M ${ax - 12} ${reverseEnd + direction * 14} L ${ax} ${reverseEnd} L ${ax + 12} ${reverseEnd + direction * 14}`
        ].join(" ");

        // Read geometry before inserting SVGs. Interleaving these reads with
        // DOM writes forces the browser to lay out the entire table repeatedly.
        const segments = [];
        for (const item of cells) {
            if (item.classList.contains("interfaces-cell-self")) continue;
            if (item.dataset.fromEntityId !== from && item.dataset.fromEntityId !== to
                && item.dataset.toEntityId !== from && item.dataset.toEntityId !== to) continue;
            const rect = item.getBoundingClientRect();
            const x = rect.left + rect.width / 2;
            const y = rect.top + rect.height / 2;
            const onRow = (item.dataset.fromEntityId === from || item.dataset.fromEntityId === to)
                && x >= Math.min(ax, bx) && x <= Math.max(ax, bx);
            const onColumn = (item.dataset.toEntityId === from || item.dataset.toEntityId === to)
                && y >= Math.min(ay, by) && y <= Math.max(ay, by);
            if (!onRow && !onColumn) continue;
            segments.push({item, rect});
        }

        for (const {item, rect} of segments) {
            const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
            svg.classList.add("interfaces-flow-arrow");
            svg.setAttribute("aria-hidden", "true");
            svg.setAttribute("focusable", "false");
            svg.setAttribute("viewBox", `${rect.left} ${rect.top} ${rect.width} ${rect.height}`);
            svg.setAttribute("preserveAspectRatio", "none");
            const path = document.createElementNS(svg.namespaceURI, "path");
            path.setAttribute("d", paths);
            svg.appendChild(path);
            item.prepend(svg);
            arrows.push(svg);
        }
    });
    tableBody.addEventListener("mouseleave", clear);
    window.addEventListener("resize", clear);
    tableBody.closest(".interfaces-table-scroll")?.addEventListener("scroll", clear, {passive:true});
    return clear;
}
