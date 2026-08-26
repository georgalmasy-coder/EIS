const PAGE_WIDTH = 841.89;
const PAGE_HEIGHT = 595.28;

const MARGIN = 28;
const HEADER_HEIGHT = 44;
const FOOTER_HEIGHT = 28;
const SECTION_HEADER_HEIGHT = 18;
const COLUMN_HEADER_HEIGHT = 24;
const ROW_HEIGHT = 20;

const FONT_SIZE = 6.7;
const TITLE_FONT_SIZE = 12;
const SECTION_FONT_SIZE = 10;

const COLORS = {
    text: [31, 41, 55],
    muted: [100, 116, 139],
    border: [226, 232, 240],
    headerBackground: [241, 245, 249],
    sectionBackground: [248, 250, 252],
    bandBackground: [236, 244, 255]
};

const BASE_COLUMNS = [
    { key: "id", label: "SBS Code", source: "id", width: 80, maxChars: 18, fixed: true },
    { key: "name", label: "System Name", source: "name", width: 1, maxChars: 28, fixed: true },
    { key: "trl", label: "TRL", source: "trlCode", width: 34, maxChars: 6, lookup: "trl", center: true, fixed: true },
    { key: "days", label: "Days to next TRL", source: "daysNextTrl", width: 60, maxChars: 11, fixed: true },
    { key: "interfaces", label: "Interfaces", source: "interfaces", width: 42, maxChars: 6, center: true, fixed: true }
];

export function downloadDashboardIrlPdf(options) {
    const pdfBytes = createDashboardIrlPdf(options);
    const blob = new Blob([pdfBytes], {
        type: "application/pdf"
    });

    const topPanel = options?.topPanel || {};
    const fileName = buildDashboardIrlFileName(topPanel);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();

    link.remove();
    URL.revokeObjectURL(url);
}

function buildDashboardIrlFileName(topPanel) {
    const projectName = String(topPanel?.projectName || "project")
        .trim()
        .replace(/[^\p{L}\p{N}._-]+/gu, "-")
        .replace(/-+/g, "-")
        .replace(/^-|-$/g, "") || "project";

    return `dashboard-irls-${projectName}.pdf`;
}

function createDashboardIrlPdf(options) {
    const context = {
        objects: [],
        pages: [],
        currentPage: null
    };

    const generatedAt = formatGeneratedAt(new Date());
    const rows = Array.isArray(options?.rows) ? options.rows : [];
    const isFiltered = Boolean(options?.isFiltered);
    const sectionTitle = isFiltered
        ? `Filtered document (${rows.length})`
        : `Complete document (${rows.length})`;

    context.currentPage = createPage(context);

    drawHeader(
        context.currentPage,
        options,
        generatedAt
    );

    let y = getContentStartY();
    y = drawSectionTitle(
        context.currentPage,
        sectionTitle,
        y
    );

    y = drawTableHeader(
        context.currentPage,
        options?.dashboard || {},
        y
    );

    if (!rows.length) {
        y = ensureSpace(context, options, y, 28, generatedAt, sectionTitle);
        drawText(
            context.currentPage.commands,
            "No rows match the current filters.",
            MARGIN,
            y - 10,
            FONT_SIZE,
            "Helvetica",
            COLORS.muted
        );
        finalizeOpenPages(context, generatedAt, options);
        return buildPdf(context);
    }

    rows.forEach((row) => {
        y = ensureSpace(context, options, y, ROW_HEIGHT + 4, generatedAt, sectionTitle);

        if (y === getContentStartY()) {
            y = drawSectionTitle(
                context.currentPage,
                `${sectionTitle} - continued`,
                y
            );

            y = drawTableHeader(
                context.currentPage,
                options?.dashboard || {},
                y
            );
        }

        y = drawTableRow(
            context.currentPage,
            row,
            y,
            options?.dashboard?.lookup || {},
            options?.dashboard || {}
        );
    });

    finalizeOpenPages(context, generatedAt, options);
    return buildPdf(context);
}

function createPage(context) {
    const page = {
        commands: [],
        number: context.pages.length + 1,
        finalized: false,
        footerDrawn: false
    };

    context.pages.push(page);
    return page;
}

function createContinuationPage(context, options, generatedAt) {
    const page = createPage(context);
    context.currentPage = page;

    drawHeader(
        page,
        options,
        generatedAt
    );

    return page;
}

function getContentStartY() {
    return PAGE_HEIGHT - MARGIN - HEADER_HEIGHT - 10;
}

function getContentBottomY() {
    return MARGIN + FOOTER_HEIGHT + 8;
}

function ensureSpace(context, options, y, requiredHeight, generatedAt) {
    if (y >= getContentBottomY() + requiredHeight) {
        return y;
    }

    finalizePage(
        context.currentPage,
        generatedAt,
        options
    );

    createContinuationPage(
        context,
        options,
        generatedAt
    );

    return getContentStartY();
}

function drawHeader(page, options, generatedAt) {
    const topPanel = options?.topPanel || {};

    drawText(
        page.commands,
        "Dashboard IRL",
        MARGIN,
        PAGE_HEIGHT - MARGIN - 10,
        TITLE_FONT_SIZE,
        "Helvetica-Bold",
        COLORS.text
    );

    drawText(
        page.commands,
        `${topPanel.customerName || "-"} - ${topPanel.projectName || "-"}`,
        MARGIN,
        PAGE_HEIGHT - MARGIN - 26,
        FONT_SIZE,
        "Helvetica",
        COLORS.muted
    );

    drawText(
        page.commands,
        `Generated: ${generatedAt}`,
        PAGE_WIDTH - MARGIN - 116,
        PAGE_HEIGHT - MARGIN - 26,
        FONT_SIZE,
        "Helvetica",
        COLORS.muted
    );

    drawLine(
        page.commands,
        MARGIN,
        PAGE_HEIGHT - MARGIN - 36,
        PAGE_WIDTH - MARGIN,
        PAGE_HEIGHT - MARGIN - 36,
        COLORS.border
    );
}

function drawFooter(page, generatedAt, options) {
    const topPanel = options?.topPanel || {};
    const totalPages = options?.totalPages || page.number;

    drawLine(
        page.commands,
        MARGIN,
        MARGIN + 8,
        PAGE_WIDTH - MARGIN,
        MARGIN + 8,
        COLORS.border
    );

    drawText(
        page.commands,
        `Page ${page.number} of ${totalPages}`,
        MARGIN,
        MARGIN - 6,
        FONT_SIZE,
        "Helvetica",
        COLORS.muted
    );

    drawText(
        page.commands,
        topPanel.userName || "-",
        PAGE_WIDTH / 2 - 50,
        MARGIN - 6,
        FONT_SIZE,
        "Helvetica",
        COLORS.muted
    );

    drawText(
        page.commands,
        `Generated: ${generatedAt}`,
        PAGE_WIDTH - MARGIN - 105,
        MARGIN - 6,
        FONT_SIZE,
        "Helvetica",
        COLORS.muted
    );
}

function drawSectionTitle(page, title, y) {
    drawRect(
        page.commands,
        MARGIN,
        y - SECTION_HEADER_HEIGHT + 3,
        PAGE_WIDTH - MARGIN * 2,
        SECTION_HEADER_HEIGHT,
        COLORS.sectionBackground
    );

    drawText(
        page.commands,
        title,
        MARGIN + 6,
        y - 7,
        SECTION_FONT_SIZE,
        "Helvetica-Bold",
        COLORS.text
    );

    return y - (SECTION_HEADER_HEIGHT + 6);
}

function drawTableHeader(page, dashboard, y) {
    const columns = getColumns(dashboard);

    let x = MARGIN;

    columns.forEach((column) => {
        drawRect(
            page.commands,
            x,
            y - COLUMN_HEADER_HEIGHT + 4,
            column.width,
            COLUMN_HEADER_HEIGHT,
            COLORS.headerBackground
        );

        drawText(
            page.commands,
            column.label,
            column.center ? x + Math.max(4, (column.width - estimateTextWidth(column.label, FONT_SIZE)) / 2) : x + 4,
            y - 7,
            FONT_SIZE,
            "Helvetica-Bold",
            COLORS.text
        );

        if (column.sublabel) {
            drawText(
                page.commands,
                column.sublabel,
                column.center ? x + Math.max(4, (column.width - estimateTextWidth(column.sublabel, FONT_SIZE - 0.6)) / 2) : x + 4,
                y - 13,
                FONT_SIZE - 0.6,
                "Helvetica",
                COLORS.muted
            );
        }

        x += column.width;
    });

    drawLine(
        page.commands,
        MARGIN,
        y - COLUMN_HEADER_HEIGHT + 4,
        PAGE_WIDTH - MARGIN,
        y - COLUMN_HEADER_HEIGHT + 4,
        COLORS.border
    );

    return y - COLUMN_HEADER_HEIGHT;
}

function drawTableRow(page, row, y, lookup, dashboard) {
    const columns = getColumns(dashboard);
    const values = buildRowValues(row, lookup, dashboard);
    const rowTop = y;
    const rowBottom = y - ROW_HEIGHT + 4;
    let x = MARGIN;

    columns.forEach((column, index) => {
        drawRect(
            page.commands,
            x,
            rowBottom,
            column.width,
            ROW_HEIGHT,
            [255, 255, 255]
        );

        const value = values[index];
        if (column.key === "trl") {
            drawBadgeCell(
                page.commands,
                value,
                x,
                rowTop,
                column.width,
                column.center
            );
        } else if (column.key === "days") {
            drawDaysCell(
                page.commands,
                value,
                x,
                rowTop,
                column.width
            );
        } else if (column.key === "interfaces" || column.key.startsWith("irl-")) {
            drawCenteredValue(
                page.commands,
                value,
                x,
                rowTop,
                column.width
            );
        } else {
            drawPlainValue(
                page.commands,
                value,
                x,
                rowTop,
                column.width
            );
        }

        drawLine(
            page.commands,
            x + column.width,
            rowBottom,
            x + column.width,
            rowTop + 1,
            COLORS.border
        );

        x += column.width;
    });

    drawLine(
        page.commands,
        MARGIN,
        rowBottom,
        PAGE_WIDTH - MARGIN,
        rowBottom,
        COLORS.border
    );

    return y - ROW_HEIGHT;
}

function buildRowValues(row, lookup, dashboard) {
    const irlOrder = Array.from(dashboard?.lookup?.irlById?.values?.() || []);

    return [
        resolveRawValue(row?.id),
        resolveRawValue(row?.name),
        resolveLookupValue(lookup.trlById, row?.trlId),
        resolveRawValue(row?.daysNextTrl),
        resolveRawValue(String(sumCounts(row?.irlCounts))),
        ...irlOrder.map((irl) => {
            const count = row?.irlCounts?.get(irl.id) || 0;
            return resolveRawValue(count > 0 ? String(count) : "");
        })
    ];
}

function drawPlainValue(commands, value, x, y, width) {
    const text = String(value?.label || value || "--");
    const display = truncateText(text, Math.max(6, Math.floor(width / 4.4)));
    drawText(
        commands,
        display,
        x + 4,
        y - 12,
        FONT_SIZE,
        "Helvetica-Bold",
        COLORS.text
    );
}

function drawCenteredValue(commands, value, x, y, width) {
    const text = String(value?.label || value || "--");
    const display = truncateText(text, Math.max(4, Math.floor(width / 4.4)));
    const textWidth = estimateTextWidth(display, FONT_SIZE);
    const textX = x + Math.max(4, (width - textWidth) / 2);

    drawText(
        commands,
        display,
        textX,
        y - 12,
        FONT_SIZE,
        "Helvetica-Bold",
        COLORS.text
    );
}

function drawBadgeCell(commands, value, x, y, width, center) {
    const resolved = value || {};
    const text = String(resolved.label || "--");
    const color = resolved.color || "";
    const display = truncateText(text, Math.max(4, Math.floor(width / 4.4)));

    if (!color) {
        drawCenteredValue(
            commands,
            { label: display },
            x,
            y,
            width
        );
        return;
    }

    const textWidth = estimateTextWidth(display, FONT_SIZE - 0.1);
    const badgeWidth = Math.max(22, Math.min(width - 8, textWidth + 12));
    const badgeX = center ? x + Math.max(4, (width - badgeWidth) / 2) : x + 4;

    drawBadge(
        commands,
        display,
        badgeX,
        y - 14.5,
        badgeWidth,
        color || [148, 163, 184]
    );
}

function drawDaysCell(commands, value, x, y, width) {
    const text = String(value?.label || value || "").trim();

    if (!text) {
        return;
    }

    const overdue = isOverdueText(text);
    const display = truncateText(text, Math.max(6, Math.floor(width / 4.2)));
    const textWidth = estimateTextWidth(display, FONT_SIZE - 0.1);
    const badgeWidth = Math.max(28, Math.min(width - 8, textWidth + 12));
    const badgeX = x + Math.max(4, (width - badgeWidth) / 2);
    const fill = overdue ? [255, 255, 255] : [255, 255, 255];
    const stroke = overdue ? [239, 68, 68] : [255, 255, 255];
    const textColor = overdue ? [239, 68, 68] : COLORS.text;

    drawRoundedRect(
        commands,
        badgeX,
        y - 14.5,
        badgeWidth,
        10.5,
        fill,
        4
    );

    drawRoundedStrokeRect(
        commands,
        badgeX,
        y - 14.5,
        badgeWidth,
        10.5,
        stroke,
        0.8,
        4
    );

    drawText(
        commands,
        display,
        badgeX + Math.max(4, (badgeWidth - textWidth) / 2),
        y - 13,
        FONT_SIZE - 0.1,
        "Helvetica-Bold",
        textColor
    );
}

function drawBadge(commands, text, x, y, width, colorValue) {
    const color = parseColor(colorValue);
    const textWidth = estimateTextWidth(text, FONT_SIZE - 0.1);
    const textX = x + Math.max(4, (width - textWidth) / 2);

    drawRoundedRect(
        commands,
        x,
        y,
        width,
        10.5,
        [255, 255, 255],
        4
    );

    drawRoundedStrokeRect(
        commands,
        x,
        y,
        width,
        10.5,
        color,
        0.8,
        4
    );

    drawText(
        commands,
        text,
        textX,
        y + 3,
        FONT_SIZE - 0.1,
        "Helvetica-Bold",
        color
    );
}

function drawRoundedStrokeRect(commands, x, y, width, height, color, lineWidth, radius) {
    commands.push("q");
    commands.push(`${formatRgb(color)} RG`);
    commands.push(`${formatPdfNumber(lineWidth)} w`);
    pushRoundedRectPath(commands, x, y, width, height, radius);
    commands.push("S");
    commands.push("Q");
}

function drawRoundedRect(commands, x, y, width, height, color, radius) {
    commands.push("q");
    commands.push(`${formatRgb(color)} rg`);
    pushRoundedRectPath(commands, x, y, width, height, radius);
    commands.push("f");
    commands.push("Q");
}

function pushRoundedRectPath(commands, x, y, width, height, radius) {
    const r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
    const k = 0.552284749831;
    const ox = r * k;
    const oy = r * k;
    const x2 = x + width;
    const y2 = y + height;

    commands.push(`${formatPdfNumber(x + r)} ${formatPdfNumber(y)} m`);
    commands.push(`${formatPdfNumber(x2 - r)} ${formatPdfNumber(y)} l`);
    commands.push(`${formatPdfNumber(x2 - r + ox)} ${formatPdfNumber(y)} ${formatPdfNumber(x2)} ${formatPdfNumber(y + r - oy)} ${formatPdfNumber(x2)} ${formatPdfNumber(y + r)} c`);
    commands.push(`${formatPdfNumber(x2)} ${formatPdfNumber(y2 - r)} l`);
    commands.push(`${formatPdfNumber(x2)} ${formatPdfNumber(y2 - r + oy)} ${formatPdfNumber(x2 - r + ox)} ${formatPdfNumber(y2)} ${formatPdfNumber(x2 - r)} ${formatPdfNumber(y2)} c`);
    commands.push(`${formatPdfNumber(x + r)} ${formatPdfNumber(y2)} l`);
    commands.push(`${formatPdfNumber(x + r - ox)} ${formatPdfNumber(y2)} ${formatPdfNumber(x)} ${formatPdfNumber(y2 - r + oy)} ${formatPdfNumber(x)} ${formatPdfNumber(y2 - r)} c`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y + r)} l`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y + r - oy)} ${formatPdfNumber(x + r - ox)} ${formatPdfNumber(y)} ${formatPdfNumber(x + r)} ${formatPdfNumber(y)} c`);
    commands.push("h");
}

function getColumns(dashboard) {
    const irlEntries = Array.from(dashboard?.lookup?.irlById?.values?.() || []);
    const dynamicColumns = irlEntries.map((irl) => ({
        key: `irl-${irl.id}`,
        label: `IRL ${irl.code || irl.id || "-"}`,
        sublabel: `(${sumIrlTotal(dashboard, irl.id)})`,
        width: 36,
        center: true
    }));

    const columns = [...BASE_COLUMNS, ...dynamicColumns];
    const availableWidth = PAGE_WIDTH - MARGIN * 2;
    const fixedWidth = columns
        .filter((column) => column.key !== "name")
        .reduce((sum, column) => sum + column.width, 0);
    const remainingWidth = Math.max(160, availableWidth - fixedWidth);

    return columns.map((column, index) => ({
        ...column,
        width: column.key === "name" ? remainingWidth : column.width,
        maxChars: column.maxChars || Math.max(4, Math.floor((column.key === "name" ? remainingWidth : column.width) / 4.8))
    }));
}

function sumIrlTotal(dashboard, irlId) {
    let total = 0;
    for (const row of dashboard?.structures || []) {
        total += Number(row?.irlCounts?.get(irlId) || 0);
    }
    return total;
}

function sumCounts(countMap) {
    let total = 0;
    for (const count of countMap?.values?.() || []) {
        total += Number(count || 0);
    }
    return total;
}

function finalizePage(page, generatedAt, options) {
    if (!page || page.finalized) {
        return;
    }

    page.finalized = true;
}

function finalizeOpenPages(context, generatedAt, options) {
    const totalPages = context.pages.length;

    context.pages.forEach((page) => {
        if (!page.footerDrawn) {
            drawFooter(
                page,
                generatedAt,
                {
                    ...options,
                    totalPages
                }
            );
            page.footerDrawn = true;
        }

        finalizePage(
            page,
            generatedAt,
            options
        );
    });
}

function buildPdf(context) {
    const objects = [];

    const fontHelvetica = addObject(
        objects,
        "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
    );

    const fontHelveticaBold = addObject(
        objects,
        "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"
    );

    const pageObjects = [];

    context.pages.forEach((page) => {
        const content = page.commands.join("\n");
        const contentObject = addObject(
            objects,
            `<< /Length ${byteLength(content)} >>\nstream\n${content}\nendstream`
        );

        const pageObject = addObject(
            objects,
            `<< /Type /Page /Parent 0 0 R /MediaBox [0 0 ${PAGE_WIDTH} ${PAGE_HEIGHT}] /Resources << /Font << /Helvetica ${fontHelvetica} 0 R /Helvetica-Bold ${fontHelveticaBold} 0 R >> >> /Contents ${contentObject} 0 R >>`
        );

        pageObjects.push(pageObject);
    });

    const pagesObject = addObject(
        objects,
        `<< /Type /Pages /Kids [${pageObjects.map((id) => `${id} 0 R`).join(" ")}] /Count ${pageObjects.length} >>`
    );

    pageObjects.forEach((pageObjectId) => {
        objects[pageObjectId - 1].content = objects[pageObjectId - 1].content.replace(
            "/Parent 0 0 R",
            `/Parent ${pagesObject} 0 R`
        );
    });

    const catalogObject = addObject(
        objects,
        `<< /Type /Catalog /Pages ${pagesObject} 0 R >>`
    );

    return serializePdf(objects, catalogObject);
}

function addObject(objects, content) {
    objects.push({ content });
    return objects.length;
}

function serializePdf(objects, catalogObject) {
    let pdf = "%PDF-1.4\n";
    const offsets = [0];

    objects.forEach((object, index) => {
        offsets.push(byteLength(pdf));
        pdf += `${index + 1} 0 obj\n${object.content}\nendobj\n`;
    });

    const xrefOffset = byteLength(pdf);

    pdf += `xref\n0 ${objects.length + 1}\n`;
    pdf += "0000000000 65535 f \n";

    for (let index = 1; index < offsets.length; index += 1) {
        pdf += `${String(offsets[index]).padStart(10, "0")} 00000 n \n`;
    }

    pdf += `trailer\n<< /Size ${objects.length + 1} /Root ${catalogObject} 0 R >>\n`;
    pdf += `startxref\n${xrefOffset}\n%%EOF`;

    return new TextEncoder().encode(pdf);
}

function drawText(commands, text, x, y, size, font, color) {
    commands.push("BT");
    commands.push(`/${font} ${formatPdfNumber(size)} Tf`);
    commands.push(`${formatRgb(color)} rg`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} Td`);
    commands.push(`(${escapePdfText(text)}) Tj`);
    commands.push("ET");
}

function drawLine(commands, x1, y1, x2, y2, color) {
    commands.push("q");
    commands.push(`${formatRgb(color)} RG`);
    commands.push("0.6 w");
    commands.push(`${formatPdfNumber(x1)} ${formatPdfNumber(y1)} m`);
    commands.push(`${formatPdfNumber(x2)} ${formatPdfNumber(y2)} l`);
    commands.push("S");
    commands.push("Q");
}

function drawRect(commands, x, y, width, height, color) {
    commands.push("q");
    commands.push(`${formatRgb(color)} rg`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} ${formatPdfNumber(width)} ${formatPdfNumber(height)} re`);
    commands.push("f");
    commands.push("Q");
}

function parseColor(value) {
    const color = String(value || "").trim().toLowerCase();
    const named = {
        red: [220, 38, 38],
        teal: [13, 148, 136],
        orange: [234, 88, 12],
        amber: [245, 158, 11],
        blue: [37, 99, 235],
        green: [22, 163, 74],
        purple: [126, 34, 206],
        pink: [219, 39, 119],
        indigo: [79, 70, 229],
        cyan: [8, 145, 178],
        gray: [107, 114, 128],
        grey: [107, 114, 128]
    };

    if (!color) {
        return [148, 163, 184];
    }

    if (named[color]) {
        return named[color];
    }

    const hex = color.match(/^#([0-9a-f]{6})$/i);
    if (hex) {
        return [
            Number.parseInt(hex[1].slice(0, 2), 16),
            Number.parseInt(hex[1].slice(2, 4), 16),
            Number.parseInt(hex[1].slice(4, 6), 16)
        ];
    }

    return [148, 163, 184];
}

function truncateText(text, maxChars) {
    const value = String(text || "--");
    const limit = Math.max(1, Number(maxChars) || 1);

    if (value.length <= limit) {
        return value;
    }

    if (limit <= 1) {
        return value.slice(0, limit);
    }

    return `${value.slice(0, limit - 1)}…`;
}

function estimateTextWidth(text, fontSize) {
    return String(text || "").length * Number(fontSize || 1) * 0.52;
}

function resolveRawValue(value) {
    const text = String(value == null ? "" : value).trim();
    return text ? { label: text, title: text, color: "" } : { label: "--", title: "--", color: "" };
}

function resolveLookupValue(map, id) {
    const normalizedId = String(id || "").trim();

    if (!normalizedId) {
        return { label: "--", title: "--", color: "" };
    }

    const lookup = map?.get(normalizedId);

    return {
        label: lookup?.code || normalizedId,
        title: lookup?.description || lookup?.code || normalizedId,
        color: lookup?.color || ""
    };
}

function isOverdueText(value) {
    const normalized = String(value || "").trim().toLowerCase();
    return normalized.startsWith("over due") || normalized.startsWith("overdue");
}

function formatGeneratedAt(date) {
    const pad = (value) => String(value).padStart(2, "0");
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function formatPdfNumber(value) {
    const number = Number(value);
    return Number.isInteger(number) ? String(number) : String(Math.round(number * 1000) / 1000);
}

function formatRgb(rgb) {
    const [r, g, b] = rgb;
    return `${formatPdfNumber(r / 255)} ${formatPdfNumber(g / 255)} ${formatPdfNumber(b / 255)}`;
}

function escapePdfText(text) {
    return String(text || "")
        .replace(/\\/g, "\\\\")
        .replace(/\(/g, "\\(")
        .replace(/\)/g, "\\)");
}

function byteLength(text) {
    return new TextEncoder().encode(String(text || "")).length;
}
