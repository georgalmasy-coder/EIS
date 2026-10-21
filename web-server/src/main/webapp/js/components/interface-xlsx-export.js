// Export the rendered matrix so the workbook follows the current filters.
export function snapshotInterfaceMatrix(table, title, project) {
    const occupied = [];
    const cells = [];
    const widths = [];
    const heights = [];
    Array.from(table.rows).forEach((row, rowIndex) => {
        occupied[rowIndex] ||= [];
        heights.push(row.getBoundingClientRect().height * 0.75);
        let column = 0;
        for (const cell of row.cells) {
            while (occupied[rowIndex][column]) column++;
            const rowSpan = cell.rowSpan || 1;
            const colSpan = cell.colSpan || 1;
            for (let r = rowIndex; r < rowIndex + rowSpan; r++) {
                occupied[r] ||= [];
                for (let c = column; c < column + colSpan; c++) occupied[r][c] = true;
            }
            const style = getComputedStyle(cell);
            const header = cell.querySelector('.interfaces-header-content');
            const lines = cell.querySelectorAll('.interfaces-cell-line');
            let text = header
                ? Array.from(header.children, node => node.textContent.trim()).join('\n')
                : lines.length
                    ? Array.from(lines, node => Array.from(node.children, child => child.textContent.trim()).join(' ')).join('\n')
                    : cell.textContent.trim();
            if (cell.querySelector('.interfaces-n2-mark')) text = 'N²';
            if (cell.querySelector('.interfaces-cell-overdue-badge')) text += '\nOverdue';
            const statusIcon = cell.querySelector('.traceability-status-icon');
            if (statusIcon) text = statusIcon.dataset.exportSymbol;
            const foreground = getComputedStyle(statusIcon || cell.querySelector('.interfaces-header-id, .interfaces-cell-line-value') || cell);
            cells.push({row: rowIndex, column, rowSpan, colSpan, text,
                background: excelColor(style.backgroundColor, 'FFFFFF'),
                foreground: excelColor(foreground.color, '111827'),
                centered: Boolean(statusIcon),
                bold: cell.tagName === 'TH' || cell.classList.contains('interfaces-cell-self'),
                rotation: cell.matches('.interfaces-axis-cell, .traceability-axis-cell') ? 90 : 0});
            if (rowIndex === 0) widths[column] = cell.getBoundingClientRect().width;
            column += colSpan;
        }
    });
    return {title, project, widths, heights, cells};
}

function excelColor(value, fallback) {
    const channels = value.match(/[\d.]+/g)?.map(Number);
    if (!channels || channels.length < 3 || channels[3] === 0) return fallback;
    return channels.slice(0, 3).map(channel => Math.round(channel).toString(16).padStart(2, '0')).join('').toUpperCase();
}

export async function downloadInterfaceXlsx({table, endpoint, title, project}) {
    const response = await fetch(`${endpoint}?cmd=export`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json', 'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'},
        credentials: 'same-origin',
        body: JSON.stringify(snapshotInterfaceMatrix(table, title, project))
    });
    if (!response.ok || !response.headers.get('Content-Type')?.includes('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')) {
        throw new Error('Could not export the matrix to Excel.');
    }
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement('a');
    link.href = url;
    link.download = `${title}${project ? ` - ${project}` : ''}.xlsx`.replace(/[\\/:*?"<>|]/g, '-');
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}
