// Variable-height table window. Prefix sums keep scroll lookup logarithmic even
// when classification text wraps differently after resizing a column.
export function createTeamworkViewport({scroller, body, records, createRow, afterRender, columns}) {
    const estimate = 64, overscan = 6;
    const heights = new Float64Array(records.length).fill(estimate);
    const tree = new Float64Array(records.length + 1);
    for (let i = 1; i < tree.length; i++) tree[i] = (i & -i) * estimate;
    const sum = end => { let total = 0; for (let i = end; i > 0; i -= i & -i) total += tree[i]; return total; };
    const setHeight = (index, height) => {
        const delta = height - heights[index]; heights[index] = height;
        for (let i = index + 1; i < tree.length; i += i & -i) tree[i] += delta;
    };
    const indexAt = offset => {
        let low = 0, high = records.length;
        while (low < high) { const mid = (low + high) >>> 1; if (sum(mid + 1) <= offset) low = mid + 1; else high = mid; }
        return Math.min(low, Math.max(0, records.length - 1));
    };
    const spacer = () => {
        const row = document.createElement('tr'); row.className = 'teamwork-viewport-spacer';
        row.setAttribute('aria-hidden', 'true');
        const cell = document.createElement('td'); cell.colSpan = columns; row.append(cell); return row;
    };
    const top = spacer(), bottom = spacer();
    const sizeSpacer = (row, height) => { row.firstChild.style.height = `${Math.max(0, height)}px`; };
    let first = -1, last = -1, frame = null, disposed = false;
    const render = (force = false) => {
        if (disposed) return;
        const offset = Math.max(0, scroller.scrollTop);
        const anchor = indexAt(offset), within = offset - sum(anchor);
        const start = Math.max(0, anchor - overscan);
        const end = Math.min(records.length, indexAt(offset + scroller.clientHeight) + overscan + 1);
        if (!force && start === first && end === last) return;
        first = start; last = end;
        const fragment = document.createDocumentFragment(); fragment.append(top);
        const rows = [];
        for (let i = start; i < end; i++) {
            const row = createRow(records[i], i);
            row.classList.toggle('is-even', i % 2 === 1);
            row.setAttribute('aria-rowindex', String(i + 1));
            rows.push(row); fragment.append(row);
        }
        fragment.append(bottom);
        sizeSpacer(top, sum(start)); sizeSpacer(bottom, sum(records.length) - sum(end));
        body.replaceChildren(fragment); afterRender();
        rows.forEach((row, i) => { const height = row.getBoundingClientRect().height; if (height > 0) setHeight(start + i, height); });
        sizeSpacer(top, sum(start)); sizeSpacer(bottom, sum(records.length) - sum(end));
        // Native anchoring is disabled; preserve the visible row ourselves.
        scroller.scrollTop = sum(anchor) + within;
        schedule();
    };
    const schedule = () => {
        if (disposed || frame !== null) return;
        frame = requestAnimationFrame(() => { frame = null; render(); });
    };
    const observer = new ResizeObserver(() => render(true));
    observer.observe(scroller);
    scroller.addEventListener('scroll', schedule, {passive: true});
    scroller.style.overflowAnchor = 'none';
    body.closest('table').setAttribute('aria-rowcount', String(records.length));
    render(true);
    return {
        remeasure() {
            const anchor = indexAt(scroller.scrollTop);
            const within = scroller.scrollTop - sum(anchor);
            heights.fill(estimate);
            for (let i = 1; i < tree.length; i++) tree[i] = (i & -i) * estimate;
            // Update extent before moving to the same row in the new estimates.
            sizeSpacer(top, sum(Math.max(0, first)));
            sizeSpacer(bottom, sum(records.length) - sum(Math.max(0, last)));
            scroller.scrollTop = sum(anchor) + within;
            render(true);
        },
        dispose() { disposed = true; observer.disconnect(); scroller.removeEventListener('scroll', schedule); if (frame !== null) cancelAnimationFrame(frame); }
    };
}
