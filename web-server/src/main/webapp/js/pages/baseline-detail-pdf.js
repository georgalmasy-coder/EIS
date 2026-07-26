const PAGE_WIDTH = 595.28;
const PAGE_HEIGHT = 841.89;

const MARGIN = 28;
const HEADER_HEIGHT = 44;
const FOOTER_HEIGHT = 28;

const FONT_SIZE = 7;
const TITLE_FONT_SIZE = 12;
const SECTION_FONT_SIZE = 10;
const LINE_HEIGHT = 10;
const ROW_HEIGHT = 15;

const COLORS = {
    text: [31, 41, 55],
    muted: [100, 116, 139],
    border: [226, 232, 240],
    headerBackground: [241, 245, 249],
    sectionBackground: [248, 250, 252]
};

export function downloadBaselineDetailPdf(options) {
    const pdfBytes = createBaselineDetailPdf(options);
    const blob = new Blob([pdfBytes], {
        type: "application/pdf"
    });

    const baseline = options?.baseline;
    const tagName = safeFileName(baseline?.tagName || "baseline-detail");

    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = `${tagName}.pdf`;
    document.body.appendChild(link);
    link.click();

    link.remove();
    URL.revokeObjectURL(url);
}

function createBaselineDetailPdf(options) {
    const context = {
        objects: [],
        pages: [],
        currentPage: null
    };

    const generatedAt = formatGeneratedAt(new Date());

    context.currentPage = createPage(context);

    drawHeader(
        context.currentPage,
        options,
        generatedAt
    );

    let y = getContentStartY();

    y = drawBaselineDetails(
        context.currentPage,
        options,
        y
    );

    y -= 8;

    y = drawChangeSection(
        context,
        options,
        y,
        "Stakeholder requirements",
        options?.stakeholderRequirements || [],
        generatedAt
    );

    y = drawChangeSection(
        context,
        options,
        y,
        "System requirements",
        options?.systemRequirements || [],
        generatedAt
    );

    y = drawOptionalChangeSection(
        context,
        options,
        y,
        "Functional structures",
        options?.functionalStructures,
        generatedAt
    );

    y = drawOptionalChangeSection(
        context,
        options,
        y,
        "Logical structures",
        options?.logicalStructures,
        generatedAt
    );

    y = drawOptionalChangeSection(
        context,
        options,
        y,
        "Physical structures",
        options?.physicalStructures ?? options?.systemsBreakdown,
        generatedAt
    );

    finalizeOpenPages(
        context,
        generatedAt,
        options
    );

    return buildPdf(context);
}

function createPage(context) {
    const page = {
        commands: [],
        number: context.pages.length + 1,
        finalized: false
    };

    context.pages.push(page);

    return page;
}

function createContinuationPage(
    context,
    options,
    generatedAt
) {
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

function ensureSpace(
    context,
    options,
    y,
    requiredHeight,
    generatedAt
) {
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

function drawHeader(
    page,
    options,
    generatedAt
) {
    const topPanel = options?.topPanel || {};
    const baseline = options?.baseline || {};

    drawText(
        page.commands,
        `Baseline detail - ${baseline.tagName || "—"}`,
        MARGIN,
        PAGE_HEIGHT - MARGIN - 10,
        TITLE_FONT_SIZE,
        "Helvetica-Bold",
        COLORS.text
    );

    drawText(
        page.commands,
        `${topPanel.customerName || "—"} · ${topPanel.projectName || "—"}`,
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

function drawFooter(
    page,
    generatedAt,
    options
) {
    const topPanel = options?.topPanel || {};

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
        `Page ${page.number}`,
        MARGIN,
        MARGIN - 6,
        FONT_SIZE,
        "Helvetica",
        COLORS.muted
    );

    drawText(
        page.commands,
        topPanel.userName || "—",
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

function drawBaselineDetails(
    page,
    options,
    y
) {
    const baseline = options?.baseline || {};
    const formatDateTime = options?.formatDateTime || identity;

    y = drawSectionTitle(
        page,
        "Baseline information",
        y
    );

    const details = [
        ["Tag Name", baseline.tagName || "—"],
        ["Created By", baseline.changedBy || "—"],
        ["Created At", formatDateTime(baseline.changedDateTime) || "—"],
        ["Previous Baseline", formatDateTime(baseline.previousBaselineDateTime) || "\u2014"],
        ["Description", baseline.description || "—"]
    ];

    for (const [label, value] of details) {
        y = drawKeyValue(
            page,
            label,
            value,
            y
        );
    }

    return y;
}

function drawChangeSection(
    context,
    options,
    y,
    title,
    rows,
    generatedAt
) {
    const formatDateTime = options?.formatDateTime || identity;

    y = ensureSpace(
        context,
        options,
        y,
        70,
        generatedAt
    );

    y = drawSectionTitle(
        context.currentPage,
        `${title} (${rows.length})`,
        y
    );

    y = drawTableHeader(
        context.currentPage,
        y
    );

    if (!rows.length) {
        y = ensureSpace(
            context,
            options,
            y,
            ROW_HEIGHT + 8,
            generatedAt
        );

        drawText(
            context.currentPage.commands,
            "No changes.",
            MARGIN,
            y - 10,
            FONT_SIZE,
            "Helvetica",
            COLORS.muted
        );

        return y - ROW_HEIGHT - 8;
    }

    rows.forEach((row) => {
        const nextY = ensureSpace(
            context,
            options,
            y,
            ROW_HEIGHT + 4,
            generatedAt
        );

        if (nextY !== y) {
            y = drawSectionTitle(
                context.currentPage,
                `${title} - continued`,
                nextY
            );

            y = drawTableHeader(
                context.currentPage,
                y
            );
        } else {
            y = nextY;
        }

        y = drawTableRow(
            context.currentPage,
            row,
            y,
            formatDateTime
        );
    });

    return y - 8;
}

function drawOptionalChangeSection(
    context,
    options,
    y,
    title,
    rows,
    generatedAt
) {
    if (rows == null) {
        return y;
    }

    return drawChangeSection(
        context,
        options,
        y,
        title,
        rows,
        generatedAt
    );
}

function drawSectionTitle(
    page,
    title,
    y
) {
    drawRect(
        page.commands,
        MARGIN,
        y - 15,
        PAGE_WIDTH - MARGIN * 2,
        18,
        COLORS.sectionBackground
    );

    drawText(
        page.commands,
        title,
        MARGIN + 6,
        y - 10,
        SECTION_FONT_SIZE,
        "Helvetica-Bold",
        COLORS.text
    );

    return y - 26;
}

function drawKeyValue(
    page,
    label,
    value,
    y
) {
    drawText(
        page.commands,
        `${label}:`,
        MARGIN + 6,
        y,
        FONT_SIZE,
        "Helvetica-Bold",
        COLORS.text
    );

    const lines = wrapText(
        String(value || "—"),
        70
    );

    lines.forEach((line, index) => {
        drawText(
            page.commands,
            line,
            MARGIN + 105,
            y - index * LINE_HEIGHT,
            FONT_SIZE,
            "Helvetica",
            COLORS.text
        );
    });

    return y - Math.max(1, lines.length) * LINE_HEIGHT - 3;
}

function drawTableHeader(
    page,
    y
) {
    const columns = getColumns();

    drawRect(
        page.commands,
        MARGIN,
        y - ROW_HEIGHT + 4,
        PAGE_WIDTH - MARGIN * 2,
        ROW_HEIGHT,
        COLORS.headerBackground
    );

    let x = MARGIN + 4;

    columns.forEach((column) => {
        drawText(
            page.commands,
            column.label,
            x,
            y - 7,
            FONT_SIZE,
            "Helvetica-Bold",
            COLORS.text
        );

        x += column.width;
    });

    return y - ROW_HEIGHT;
}

function drawTableRow(
    page,
    row,
    y,
    formatDateTime
) {
    const columns = getColumns();
    const values = [
        row.activity || "",
        row.id || "",
        row.name || "",
        row.lastModifiedBy || "",
        formatDateTime(row.lastModified) || ""
    ];

    let x = MARGIN + 4;

    columns.forEach((column, index) => {
        drawText(
            page.commands,
            truncateText(values[index], column.maxChars),
            x,
            y - 7,
            FONT_SIZE,
            "Helvetica",
            COLORS.text
        );

        x += column.width;
    });

    drawLine(
        page.commands,
        MARGIN,
        y - ROW_HEIGHT + 2,
        PAGE_WIDTH - MARGIN,
        y - ROW_HEIGHT + 2,
        COLORS.border
    );

    return y - ROW_HEIGHT;
}

function getColumns() {
    return [
        {
            label: "Activity",
            width: 42,
            maxChars: 10
        },
        {
            label: "Id",
            width: 42,
            maxChars: 12
        },
        {
            label: "Name",
            width: 205,
            maxChars: 48
        },
        {
            label: "Last modified by",
            width: 150,
            maxChars: 30
        },
        {
            label: "Last modified",
            width: 94,
            maxChars: 19
        }
    ];
}

function finalizePage(
    page,
    generatedAt,
    options
) {
    if (!page || page.finalized) {
        return;
    }

    drawFooter(
        page,
        generatedAt,
        options
    );

    page.finalized = true;
}

function finalizeOpenPages(
    context,
    generatedAt,
    options
) {
    context.pages.forEach((page) => {
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

function addObject(
    objects,
    content
) {
    objects.push({
        content
    });

    return objects.length;
}

function serializePdf(
    objects,
    catalogObject
) {
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

function drawText(
    commands,
    text,
    x,
    y,
    size,
    font,
    color
) {
    commands.push("BT");
    commands.push(`/${font} ${formatPdfNumber(size)} Tf`);
    commands.push(`${formatRgb(color)} rg`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} Td`);
    commands.push(`(${escapePdfText(text)}) Tj`);
    commands.push("ET");
}

function drawLine(
    commands,
    x1,
    y1,
    x2,
    y2,
    color
) {
    commands.push("q");
    commands.push(`${formatRgb(color)} RG`);
    commands.push("0.6 w");
    commands.push(`${formatPdfNumber(x1)} ${formatPdfNumber(y1)} m`);
    commands.push(`${formatPdfNumber(x2)} ${formatPdfNumber(y2)} l`);
    commands.push("S");
    commands.push("Q");
}

function drawRect(
    commands,
    x,
    y,
    width,
    height,
    color
) {
    commands.push("q");
    commands.push(`${formatRgb(color)} rg`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} ${formatPdfNumber(width)} ${formatPdfNumber(height)} re`);
    commands.push("f");
    commands.push("Q");
}

function wrapText(
    value,
    maxChars
) {
    const words = String(value || "").split(/\s+/);
    const lines = [];
    let line = "";

    words.forEach((word) => {
        const next = line ? `${line} ${word}` : word;

        if (next.length > maxChars && line) {
            lines.push(line);
            line = word;
        } else {
            line = next;
        }
    });

    if (line) {
        lines.push(line);
    }

    return lines.length ? lines : ["—"];
}

function truncateText(
    value,
    maxChars
) {
    const text = String(value == null ? "" : value);

    if (text.length <= maxChars) {
        return text;
    }

    return `${text.slice(0, Math.max(0, maxChars - 1))}…`;
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
    return String(value || "baseline-detail")
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

function identity(value) {
    return value == null ? "" : String(value);
}
