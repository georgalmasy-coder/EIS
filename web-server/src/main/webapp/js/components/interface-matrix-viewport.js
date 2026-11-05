export const INTERFACE_GRID = Object.freeze({rowHeight:120, columnWidth:126, headerHeight:126, frozenWidth:318});

export function interfaceViewportRange({rows, columns, top, left, height, width}) {
    const g = INTERFACE_GRID;
    const rowStart = Math.max(0, Math.min(rows - 1, Math.floor(top / g.rowHeight) - 2));
    const columnStart = Math.max(0, Math.min(columns - 1, Math.floor(left / g.columnWidth) - 2));
    return {rowStart, rowEnd:Math.min(rows, rowStart + Math.ceil(Math.max(0,height-g.headerHeight)/g.rowHeight)+5),
        columnStart, columnEnd:Math.min(columns, columnStart + Math.ceil(Math.max(0,width-g.frozenWidth)/g.columnWidth)+5)};
}

/** Recycle the visible portion of a table, keeping native scrollbars for the full matrix. */
export function createInterfaceViewport(scroller, render) {
    let frame = null, previous = '', sizes = {rows:0,columns:0}, disposed = false;
    const update = () => {
        frame = null;
        if (disposed) return;
        const range = interfaceViewportRange({...sizes,top:scroller.scrollTop,left:scroller.scrollLeft,
            height:scroller.clientHeight,width:scroller.clientWidth});
        const key = JSON.stringify(range);
        if (previous !== key) { previous = key; render(range); }
    };
    const schedule = () => { if (frame === null) frame = requestAnimationFrame(update); };
    scroller.addEventListener('scroll', schedule, {passive:true});
    const observer = new ResizeObserver(schedule); observer.observe(scroller);
    return {
        setSize(rows, columns, reset = true) {
            sizes = {rows,columns}; previous = '';
            if (reset) { scroller.scrollTop = 0; scroller.scrollLeft = 0; }
            update();
        },
        refresh() { previous="";schedule(); },
        dispose() { disposed=true; scroller.removeEventListener('scroll', schedule);observer.disconnect();if(frame!==null)cancelAnimationFrame(frame); }
    };
}
