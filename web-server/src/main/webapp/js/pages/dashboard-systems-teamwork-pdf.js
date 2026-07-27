const PAGE_WIDTH = 841.89;
const PAGE_HEIGHT = 595.28;

const MARGIN = 28;
const HEADER_HEIGHT = 44;
const FOOTER_HEIGHT = 28;
const SECTION_HEADER_HEIGHT = 18;
const GROUP_HEADER_HEIGHT = 18;
const COLUMN_HEADER_HEIGHT = 18;
const ROW_HEIGHT = 28;

const FONT_SIZE = 6.7;
const TITLE_FONT_SIZE = 12;
const SECTION_FONT_SIZE = 10;
const LINE_HEIGHT = 8.5;

const COLORS = {
    text: [31, 41, 55],
    muted: [100, 116, 139],
    border: [226, 232, 240],
    headerBackground: [241, 245, 249],
    sectionBackground: [248, 250, 252],
    bandBackground: [236, 244, 255]
};

const COLUMN_DEFINITIONS = [
    { key: "fromSbsCode", label: "SBS Code", source: "fromSbsCode", width: 60, maxChars: 16 },
    { key: "fromTrlId", label: "TRL", source: "fromTrlId", width: 42, maxChars: 8, lookup: "trl", center: true },
    { key: "fromSystemName", label: "System Name", source: "fromSystemName", width: 190, maxChars: 30 },
    { key: "fromSystemOwnerId", label: "System Owner", source: "fromSystemOwnerId", width: 88, maxChars: 18, lookup: "user" },
    { key: "fromSystemDepartmentId", label: "Department", source: "fromSystemDepartmentId", width: 80, maxChars: 18, lookup: "department" },
    { key: "interfaceClass", label: "Class", sublabel: "From -> To", dual: true, type: "classification", width: 120, maxChars: 18 },
    { key: "interfaceIrl", label: "IRL", sublabel: "From -> To", dual: true, type: "irl", width: 92, maxChars: 15 },
    { key: "toSbsCode", label: "SBS Code", source: "toSbsCode", width: 60, maxChars: 16 },
    { key: "toTrlId", label: "TRL", source: "toTrlId", width: 42, maxChars: 8, lookup: "trl", center: true },
    { key: "toSystemName", label: "System Name", source: "toSystemName", width: 190, maxChars: 30 },
    { key: "toSystemOwnerId", label: "System Owner", source: "toSystemOwnerId", width: 88, maxChars: 18, lookup: "user" },
    { key: "toSystemDepartmentId", label: "Department", source: "toSystemDepartmentId", width: 80, maxChars: 18, lookup: "department" }
];

export function downloadDashboardSystemsTeamworkPdf(options) {
    const pdfBytes = createDashboardSystemsTeamworkPdf(options);
    const blob = new Blob([pdfBytes], {
        type: "application/pdf"
    });

    const topPanel = options?.topPanel || {};
    const fileName = buildDashboardSystemsTeamworkFileName(topPanel);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = `${fileName}.pdf`;
    document.body.appendChild(link);
    link.click();

    link.remove();
    URL.revokeObjectURL(url);
}

function buildDashboardSystemsTeamworkFileName(topPanel) {
    const projectName = String(topPanel?.projectName || "project")
        .trim()
        .replace(/[^\p{L}\p{N}._-]+/gu, "-")
        .replace(/-+/g, "-")
        .replace(/^-|-$/g, "") || "project";

    return `systems-teamwork-${projectName}.pdf`;
}

function createDashboardSystemsTeamworkPdf(options) {
    const context = {
        objects: [],
        pages: [],
        currentPage: null
    };

    const generatedAt = formatGeneratedAt(new Date());
    const rows = Array.isArray(options?.records) ? options.records : [];
    const sectionTitle = options?.isFiltered
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
        y
    );

    if (!rows.length) {
        y = ensureSpace(context, options, y, 30, generatedAt);
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
        y = ensureSpace(context, options, y, ROW_HEIGHT + 6, generatedAt);

        if (y === getContentStartY()) {
            y = drawSectionTitle(
                context.currentPage,
                options?.isFiltered ? "Filtered document - continued" : "Complete document - continued",
                y
            );

            y = drawTableHeader(
                context.currentPage,
                y
            );
        }

        y = drawTableRow(
            context.currentPage,
            row,
            y,
            options?.dashboard?.lookup || {}
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
    const dashboard = options?.dashboard || {};

    drawText(
        page.commands,
        dashboard.title || "Systems Teamwork",
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

function drawTableHeader(page, y) {
    const columns = getColumns();
    const groupWidths = [
        sum(columns, 0, 5),
        sum(columns, 5, 7),
        sum(columns, 7, 12)
    ];
    const groupLabels = ["From", "Interface", "To"];

    let x = MARGIN;

    drawRect(
        page.commands,
        MARGIN,
        y - GROUP_HEADER_HEIGHT + 4,
        PAGE_WIDTH - MARGIN * 2,
        GROUP_HEADER_HEIGHT,
        COLORS.bandBackground
    );

    groupLabels.forEach((label, index) => {
        drawRect(
            page.commands,
            x,
            y - GROUP_HEADER_HEIGHT + 4,
            groupWidths[index],
            GROUP_HEADER_HEIGHT,
            COLORS.bandBackground
        );
        drawText(
            page.commands,
            label,
            x + 4,
            y - 7,
            FONT_SIZE,
            "Helvetica-Bold",
            COLORS.text
        );
        x += groupWidths[index];
    });

    let columnY = y - GROUP_HEADER_HEIGHT;
    x = MARGIN;

    columns.forEach((column) => {
        drawRect(
            page.commands,
            x,
            columnY - COLUMN_HEADER_HEIGHT + 4,
            column.width,
            COLUMN_HEADER_HEIGHT,
            COLORS.headerBackground
        );

        drawText(
            page.commands,
            column.label,
            x + 4,
            columnY - 7,
            FONT_SIZE,
            "Helvetica-Bold",
            COLORS.text
        );

        if (column.sublabel) {
            drawText(
                page.commands,
                column.sublabel,
                x + 4,
                columnY - 13,
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
        columnY - COLUMN_HEADER_HEIGHT + 4,
        PAGE_WIDTH - MARGIN,
        columnY - COLUMN_HEADER_HEIGHT + 4,
        COLORS.border
    );

    return y - (GROUP_HEADER_HEIGHT + COLUMN_HEADER_HEIGHT);
}

function drawTableRow(page, row, y, lookup) {
    const values = buildRowValues(row, lookup);
    const columns = getColumns();
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

        if (column.dual) {
            drawDualCell(
                page.commands,
                values[index],
                x,
                rowTop,
                column.width,
                column.maxChars
            );
        } else {
            drawSingleCell(
                page.commands,
                values[index],
                x,
                rowTop,
                column.width,
                column.maxChars,
                column.center
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

function buildRowValues(row, lookup) {
    return [
        resolveRawValue(row.fromSbsCode),
        resolveLookupValue(lookup.trlById, row.fromTrlId),
        resolveRawValue(row.fromSystemName),
        resolveLookupValue(lookup.userById, row.fromSystemOwnerId),
        resolveLookupValue(lookup.departmentById, row.fromSystemDepartmentId),
        {
            top: resolveLookupList(lookup.classificationById, row.fromClassificationIds),
            bottom: resolveLookupList(lookup.classificationById, row.toClassificationIds)
        },
        {
            top: resolveLookupValue(lookup.irlById, row.fromIrlId),
            bottom: resolveLookupValue(lookup.irlById, row.toIrlId)
        },
        resolveRawValue(row.toSbsCode),
        resolveLookupValue(lookup.trlById, row.toTrlId),
        resolveRawValue(row.toSystemName),
        resolveLookupValue(lookup.userById, row.toSystemOwnerId),
        resolveLookupValue(lookup.departmentById, row.toSystemDepartmentId)
    ];
}

function drawSingleCell(commands, value, x, y, width, maxChars, center) {
    const text = String(value?.label || value || "--");
    const color = value?.color || "";
    const display = truncateText(text, maxChars);
    const textWidth = estimateTextWidth(display, FONT_SIZE);
    const isBadge = Boolean(color);
    const contentWidth = isBadge ? Math.min(width - 8, textWidth + 12) : textWidth;
    const textX = center ? x + Math.max(4, (width - Math.min(contentWidth, width - 8)) / 2) : x + 4;

    if (isBadge) {
        const badgeWidth = Math.max(22, Math.min(width - 8, textWidth + 12));
        const badgeX = center ? x + Math.max(4, (width - badgeWidth) / 2) : x + 4;
        drawBadge(
            commands,
            display,
            badgeX,
            y - 17,
            badgeWidth,
            color
        );
    } else {
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
}

function drawDualCell(commands, pair, x, y, width, maxChars) {
    const top = pair?.top || {};
    const bottom = pair?.bottom || {};

    drawDualLine(
        commands,
        "->",
        top,
        x,
        y - 8,
        width,
        maxChars
    );

    drawDualLine(
        commands,
        "<-",
        bottom,
        x,
        y - 17,
        width,
        maxChars
    );
}

function drawDualLine(commands, arrow, resolved, x, y, width, maxChars) {
    const arrowWidth = 10;
    const display = truncateText(String(resolved?.label || "--"), maxChars);
    const textWidth = estimateTextWidth(display, FONT_SIZE);
    const hasColor = Boolean(resolved?.color);
    const badgeWidth = Math.min(width - arrowWidth - 8, Math.max(26, textWidth + 12));
    const textX = x + arrowWidth + 4;

    drawText(
        commands,
        arrow,
        x + 1,
        y,
        FONT_SIZE,
        "Helvetica-Bold",
        COLORS.text
    );

    if (hasColor) {
        drawBadge(
            commands,
            display,
            textX,
            y - 5.5,
            badgeWidth,
            resolved.color
        );
        return;
    }

    drawText(
        commands,
        display,
        textX,
        y,
        FONT_SIZE,
        "Helvetica-Bold",
        COLORS.text
    );
}

function drawBadge(commands, text, x, y, width, colorValue) {
    const color = parseColor(colorValue);
    const badgeHeight = 11;
    const borderWidth = 0.8;
    const textSize = FONT_SIZE - 0.1;
    const textWidth = estimateTextWidth(text, textSize);
    const textX = x + Math.max(4, (width - textWidth) / 2);

    drawRoundedRect(
        commands,
        x,
        y,
        width,
        badgeHeight,
        [255, 255, 255],
        4
    );

    drawRoundedStrokeRect(
        commands,
        x,
        y,
        width,
        badgeHeight,
        color,
        borderWidth,
        4
    );

    drawText(
        commands,
        text,
        textX,
        y + 3,
        textSize,
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

function resolveLookupList(map, rawValue) {
    const ids = String(rawValue || "")
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean);

    if (!ids.length) {
        return { label: "--", title: "--", color: "" };
    }

    const items = ids.map((id) => resolveLookupValue(map, id));

    return {
        label: items.map((item) => item.label).join(", "),
        title: items.map((item) => item.title).join(", "),
        color: items.length === 1 ? items[0].color : ""
    };
}

function getColumns() {
    const totalWidth = COLUMN_DEFINITIONS.reduce((sumWidth, column) => sumWidth + column.width, 0);
    const scale = (PAGE_WIDTH - MARGIN * 2) / totalWidth;
    const scaledWidths = COLUMN_DEFINITIONS.map((column) => Math.max(28, Math.round(column.width * scale)));
    const widthDifference = Math.round((PAGE_WIDTH - MARGIN * 2) - scaledWidths.reduce((sumWidth, width) => sumWidth + width, 0));

    if (scaledWidths.length) {
        scaledWidths[scaledWidths.length - 1] += widthDifference;
    }

    return COLUMN_DEFINITIONS.map((column, index) => {
        const width = scaledWidths[index];
        return {
            ...column,
            width,
            maxChars: column.maxChars || Math.max(8, Math.floor(width / 4.6))
        };
    });
}

function sum(columns, startIndex, endIndex) {
    return columns.slice(startIndex, endIndex).reduce((total, column) => total + column.width, 0);
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

    return serializePdf(
        objects,
        catalogObject
    );
}

function addObject(objects, content) {
    objects.push({
        content
    });

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

    for (let index = 1; index < offsets.length; index++) {
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
        blue: [37, 99, 235],
        green: [22, 163, 74],
        purple: [126, 34, 206],
        gray: [107, 114, 128],
        grey: [107, 114, 128],
        yellow: [202, 138, 4]
    };

    if (!color) {
        return [37, 99, 235];
    }

    if (named[color]) {
        return named[color];
    }

    if (color.startsWith("#")) {
        const hex = color.slice(1);

        if (hex.length === 3) {
            const r = parseInt(hex[0] + hex[0], 16);
            const g = parseInt(hex[1] + hex[1], 16);
            const b = parseInt(hex[2] + hex[2], 16);
            if ([r, g, b].every((part) => Number.isFinite(part))) {
                return [r, g, b];
            }
        } else if (hex.length === 6) {
            const r = parseInt(hex.slice(0, 2), 16);
            const g = parseInt(hex.slice(2, 4), 16);
            const b = parseInt(hex.slice(4, 6), 16);
            if ([r, g, b].every((part) => Number.isFinite(part))) {
                return [r, g, b];
            }
        }
    }

    return [37, 99, 235];
}

function estimateTextWidth(text, size) {
    return String(text || "").length * size * 0.52;
}

function truncateText(value, maxChars) {
    const text = String(value == null ? "" : value);

    if (text.length <= maxChars) {
        return text;
    }

    return `${text.slice(0, Math.max(0, maxChars - 1))}...`;
}

function formatGeneratedAt(date) {
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = String(date.getFullYear());
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    const seconds = String(date.getSeconds()).padStart(2, "0");

    return `${day}/${month}/${year} ${hours}:${minutes}:${seconds}`;
}

function safeFileName(value) {
    return String(value || "systems-teamwork")
        .trim()
        .replace(/[\\/:*?"<>|]+/g, "-")
        .replace(/\s+/g, "-")
        .toLowerCase();
}

function escapePdfText(value) {
    return String(value == null ? "" : value)
        .replaceAll("\\", "\\\\")
        .replaceAll("(", "\\(")
        .replaceAll(")", "\\)");
}

function formatRgb(color) {
    return color
        .map((value) => formatPdfNumber(value / 255))
        .join(" ");
}

function formatPdfNumber(value) {
    return Number(value || 0)
        .toFixed(3)
        .replace(/\.?0+$/, "");
}

function byteLength(value) {
    return new TextEncoder().encode(value).length;
}

