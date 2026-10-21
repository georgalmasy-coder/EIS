import {buildPdf} from "./dashboard-irls-pdf.js?v=20261021-pdf-encoding";
import {srlPosition} from "./dashboard-srls-model.js?v=20261020-tooltips";

const WIDTH = 841.89, HEIGHT = 595.28, MARGIN = 28;
const CODE_X = 32, NAME_X = 194, COUNT_X = 398, CHART_LEFT = 464, CHART_RIGHT = WIDTH - 48;
const TEXT = [31, 41, 55], MUTED = [100, 116, 139], BORDER = [215, 223, 216];
const BAND = [244, 247, 243], RED = [220, 38, 38], GREEN = [0, 138, 101];
const number = value => String(Math.round(value * 1000) / 1000);
const rgb = color => color.map(value => number(value / 255)).join(' ');

// WinAnsi octal escapes keep PDF streams ASCII while preserving Danish letters.
function pdfText(value) {
    return [...String(value ?? '')].map(char => {
        const code = char.codePointAt(0);
        if (code > 255) return char === '\u2013' || char === '\u2014' ? '-' : '?';
        if (code < 32 || code > 126) return `\\${code.toString(8).padStart(3, '0')}`;
        return /[\\()]/.test(char) ? `\\${char}` : char;
    }).join('');
}

function text(commands, value, x, y, bold = false, size = 8, color = TEXT) {
    commands.push(`BT /${bold ? 'Helvetica-Bold' : 'Helvetica'} ${size} Tf ${rgb(color)} rg ${number(x)} ${number(y)} Td (${pdfText(value)}) Tj ET`);
}
function line(commands, x1, y, x2, color = TEXT) {
    commands.push(`q ${rgb(color)} RG 0.7 w ${number(x1)} ${number(y)} m ${number(x2)} ${number(y)} l S Q`);
}
function rect(commands, x, y, width, height, color) {
    commands.push(`q ${rgb(color)} rg ${number(x)} ${number(y)} ${number(width)} ${number(height)} re f Q`);
}
function dot(commands, x, y, radius, color) {
    const k = radius * 0.5522847498;
    commands.push(`q ${rgb(color)} rg`, `${number(x + radius)} ${number(y)} m`,
        `${number(x + radius)} ${number(y + k)} ${number(x + k)} ${number(y + radius)} ${number(x)} ${number(y + radius)} c`,
        `${number(x - k)} ${number(y + radius)} ${number(x - radius)} ${number(y + k)} ${number(x - radius)} ${number(y)} c`,
        `${number(x - radius)} ${number(y - k)} ${number(x - k)} ${number(y - radius)} ${number(x)} ${number(y - radius)} c`,
        `${number(x + k)} ${number(y - radius)} ${number(x + radius)} ${number(y - k)} ${number(x + radius)} ${number(y)} c f Q`);
}
const chartX = (value, levels) => CHART_LEFT + srlPosition(value, levels) / 100 * (CHART_RIGHT - CHART_LEFT);

// Conservative character widths avoid clipping long names or deeply nested codes.
function wrap(value, width) {
    const limit = Math.max(8, Math.floor(width / 5));
    const source = String(value ?? '').replace(/\s+/g, ' ').trim();
    if (!source) return [''];
    const lines = [];
    let remaining = source;
    while (remaining.length > limit) {
        const space = remaining.lastIndexOf(' ', limit);
        const split = space > limit / 2 ? space : limit;
        lines.push(remaining.slice(0, split));
        remaining = remaining.slice(split).trimStart();
    }
    if (remaining) lines.push(remaining);
    return lines;
}

export function createDashboardSrlPdf({dashboard, topPanel = {}, generatedAt = new Date()}) {
    const pages = [];
    let commands, y;
    const timestamp = generatedAt.toLocaleString('da-DK');
    const newPage = () => {
        commands = [];
        pages.push({commands});
        text(commands, 'Monitor progress and control SRLs', MARGIN, HEIGHT - 38, true, 14);
        text(commands, `${topPanel.customerName || ''} / ${topPanel.projectName || ''}`, MARGIN, HEIGHT - 54);
        text(commands, 'Project PRL', MARGIN, HEIGHT - 91, true, 11);
        if (dashboard.summary && dashboard.levels.length) {
            line(commands, CHART_LEFT, HEIGHT - 82, CHART_RIGHT);
            for (const [key, label] of [['lowest', 'Low'], ['average', 'Current Level'], ['highest', 'High']]) {
                const x = chartX(Math.round(dashboard.summary[key]), dashboard.levels);
                dot(commands, x, HEIGHT - 82, 4.5, key === 'average' ? GREEN : TEXT);
                text(commands, label, x - label.length * 1.7, HEIGHT - 98, false, 7, MUTED);
            }
        }
        y = HEIGHT - 121;
        rect(commands, MARGIN, y - 22, WIDTH - MARGIN * 2, 22, BAND);
        text(commands, 'SBS Code', CODE_X, y - 14, true);
        text(commands, 'System Name', NAME_X, y - 14, true);
        text(commands, '# Subsystems', COUNT_X - 20, y - 14, true, 7);
        for (const level of dashboard.levels) {
            text(commands, `SRL ${level}`, chartX(level, dashboard.levels) - 9, y - 14, true, 7);
        }
        y -= 22;
    };
    newPage();
    let rowIndex = 0;
    const drawRow = (system, level, count = null) => {
        const bold = system.codeLevel === 1;
        const dots = (String(system.code).match(/\./g) || []).length;
        const indent = bold ? 0 : Math.min(90, dots * 10);
        const codes = wrap(system.code, NAME_X - CODE_X - indent - 8);
        const names = wrap(system.name, COUNT_X - NAME_X - 26);
        const height = Math.max(23, Math.max(codes.length, names.length) * 11 + 10);
        // A long field may span pages, preserving every line of its content.
        let offset = 0;
        const lineCount = Math.max(codes.length, names.length);
        do {
            if (y - Math.min(height, 23) < 48) newPage();
            const availableLines = Math.max(1, Math.floor((y - 48 - 10) / 11));
            const countLines = Math.min(lineCount - offset, availableLines);
            const partHeight = Math.max(23, countLines * 11 + 10);
            if (rowIndex % 2 === 0 || bold) rect(commands, MARGIN, y - partHeight, WIDTH - MARGIN * 2, partHeight, BAND);
            for (let index = 0; index < countLines; index++) {
                text(commands, codes[offset + index] || '', CODE_X + indent, y - 14 - index * 11, bold);
                text(commands, names[offset + index] || '', NAME_X, y - 14 - index * 11, bold);
            }
            if (offset === 0) {
                if (count !== null) text(commands, count, COUNT_X, y - 14, bold);
                if (level !== null && dashboard.levels.length) {
                    const end = chartX(level, dashboard.levels);
                    line(commands, CHART_LEFT, y - 12, end);
                    dot(commands, end, y - 12, 2.5, RED);
                } else text(commands, 'Not calculated', CHART_LEFT, y - 14, false, 7, MUTED);
            }
            line(commands, MARGIN, y - partHeight, WIDTH - MARGIN, BORDER);
            y -= partHeight;
            offset += countLines;
            if (offset < lineCount) newPage();
        } while (offset < lineCount);
        rowIndex++;
    };
    for (const group of dashboard.groups) {
        drawRow(group.system, group.summary ? Math.round(group.summary.average) : null, String(group.children.length));
        for (const system of group.children) drawRow(system, system.level);
    }
    if (!dashboard.rows.length) text(commands, 'No active systems in this project.', MARGIN, y - 18);
    pages.forEach((page, index) => {
        line(page.commands, MARGIN, 36, WIDTH - MARGIN, BORDER);
        text(page.commands, `Generated ${timestamp}`, MARGIN, 24, false, 7, MUTED);
        text(page.commands, `Page ${index + 1} / ${pages.length}`, WIDTH - 84, 24, false, 7, MUTED);
    });
    return buildPdf({pages}, 'WinAnsiEncoding');
}

export function downloadDashboardSrlPdf(options) {
    const blob = new Blob([createDashboardSrlPdf(options)], {type: 'application/pdf'});
    const project = String(options.topPanel?.projectName || 'project').replace(/[^\p{L}\p{N}._-]+/gu, '-');
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `dashboard-srls-${project}.pdf`;
    document.body.append(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}
