export function buildPdfDocument(pageWidth, pageHeight, pageContents) {
    const encoder = new TextEncoder();
    const objects = [];
    const pageObjectNumbers = [];

    const catalogObjectNumber = 1;
    const pagesObjectNumber = 2;
    const helveticaObjectNumber = 3;
    const helveticaBoldObjectNumber = 4;

    objects[catalogObjectNumber] = "<< /Type /Catalog /Pages 2 0 R >>";
    objects[helveticaObjectNumber] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>";
    objects[helveticaBoldObjectNumber] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>";

    let nextObjectNumber = 5;

    pageContents.forEach((content) => {
        const pageObjectNumber = nextObjectNumber;
        const contentObjectNumber = nextObjectNumber + 1;

        nextObjectNumber += 2;
        pageObjectNumbers.push(pageObjectNumber);

        objects[pageObjectNumber] = [
            "<<",
            "/Type /Page",
            "/Parent 2 0 R",
            `/MediaBox [0 0 ${formatPdfNumber(pageWidth)} ${formatPdfNumber(pageHeight)}]`,
            "/Resources <<",
            "/Font <<",
            "/Helvetica 3 0 R",
            "/Helvetica-Bold 4 0 R",
            ">>",
            ">>",
            `/Contents ${contentObjectNumber} 0 R`,
            ">>"
        ].join(" ");

        const contentBytes = encoder.encode(content);
        objects[contentObjectNumber] = `<< /Length ${contentBytes.length} >>\nstream\n${content}\nendstream`;
    });

    objects[pagesObjectNumber] = [
        "<<",
        "/Type /Pages",
        `/Kids [${pageObjectNumbers.map((number) => `${number} 0 R`).join(" ")}]`,
        `/Count ${pageObjectNumbers.length}`,
        ">>"
    ].join(" ");

    let pdf = "%PDF-1.4\n";
    const offsets = [0];

    for (let objectNumber = 1; objectNumber < objects.length; objectNumber += 1) {
        offsets[objectNumber] = encoder.encode(pdf).length;
        pdf += `${objectNumber} 0 obj\n${objects[objectNumber]}\nendobj\n`;
    }

    const xrefOffset = encoder.encode(pdf).length;

    pdf += `xref\n0 ${objects.length}\n`;
    pdf += "0000000000 65535 f \n";

    for (let objectNumber = 1; objectNumber < objects.length; objectNumber += 1) {
        pdf += `${String(offsets[objectNumber]).padStart(10, "0")} 00000 n \n`;
    }

    pdf += `trailer\n<< /Size ${objects.length} /Root ${catalogObjectNumber} 0 R >>\n`;
    pdf += `startxref\n${xrefOffset}\n%%EOF`;

    return encoder.encode(pdf);
}

export function downloadBlob(blob, fileName) {
    const url = URL.createObjectURL(blob);

    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = fileName;
    anchor.style.display = "none";

    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();

    setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

export function drawPdfBackground(commands, pageWidth, pageHeight) {
    commands.push("q");
    commands.push("1 1 1 rg");
    commands.push(`0 0 ${formatPdfNumber(pageWidth)} ${formatPdfNumber(pageHeight)} re`);
    commands.push("f");
    commands.push("Q");
}

export function drawPdfRect(commands, x, y, width, height, fillHex, strokeHex) {
    commands.push("q");
    commands.push(`${formatRgb(hexToRgb(fillHex))} rg`);
    commands.push(`${formatRgb(hexToRgb(strokeHex))} RG`);
    commands.push("0.8 w");
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} ${formatPdfNumber(width)} ${formatPdfNumber(height)} re`);
    commands.push("B");
    commands.push("Q");
}

export function drawPdfFilledRect(commands, x, y, width, height, fillHex) {
    commands.push("q");
    commands.push(`${formatRgb(hexToRgb(fillHex))} rg`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} ${formatPdfNumber(width)} ${formatPdfNumber(height)} re`);
    commands.push("f");
    commands.push("Q");
}

export function drawPdfText(commands, text, x, y, fontSize, fontName = "Helvetica", color = [0, 0, 0]) {
    commands.push("q");
    commands.push(`${formatRgb(color)} rg`);
    commands.push("BT");
    commands.push(`/${fontName} ${formatPdfNumber(fontSize)} Tf`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} Td`);
    commands.push(`(${escapePdfText(text)}) Tj`);
    commands.push("ET");
    commands.push("Q");
}

export function drawPdfTextCentered(
    commands,
    text,
    x,
    y,
    width,
    height,
    fontSize,
    fontName = "Helvetica",
    color = [0, 0, 0],
    maxLines = 2
) {
    const lines = wrapPdfText(text, Math.max(1, Math.floor(width / (fontSize * 0.52))), maxLines);
    const lineHeight = fontSize * 1.15;
    const totalTextHeight = lines.length * lineHeight;
    const startY = y + height / 2 + totalTextHeight / 2 - fontSize;

    lines.forEach((line, index) => {
        const estimatedWidth = line.length * fontSize * 0.52;
        const textX = x + Math.max(0, (width - estimatedWidth) / 2);
        const textY = startY - index * lineHeight;

        drawPdfText(commands, line, textX, textY, fontSize, fontName, color);
    });
}

export function drawPdfMultilineText(
    commands,
    lines,
    x,
    startY,
    fontSize,
    fontName = "Helvetica",
    color = [0, 0, 0],
    lineHeight = fontSize * 1.15
) {
    lines.forEach((line, index) => {
        drawPdfText(commands, line, x, startY - index * lineHeight, fontSize, fontName, color);
    });
}

export function wrapPdfText(text, maxChars, maxLines) {
    const words = String(text || "—").trim().split(/\s+/);
    const lines = [];
    let current = "";

    for (const word of words) {
        const candidate = current ? `${current} ${word}` : word;

        if (candidate.length <= maxChars) {
            current = candidate;
            continue;
        }

        if (current) {
            lines.push(current);
        }

        current = word;

        if (lines.length >= maxLines) {
            break;
        }
    }

    if (current && lines.length < maxLines) {
        lines.push(current);
    }

    if (!lines.length) {
        lines.push("—");
    }

    return lines.slice(0, maxLines);
}

export function hexToRgb(hex) {
    const normalized = String(hex || "").replace("#", "").trim();
    const value = normalized.length === 3
        ? normalized.split("").map((char) => char + char).join("")
        : normalized.padEnd(6, "0").slice(0, 6);

    return [
        parseInt(value.slice(0, 2), 16) || 0,
        parseInt(value.slice(2, 4), 16) || 0,
        parseInt(value.slice(4, 6), 16) || 0
    ];
}

export function formatRgb(rgb) {
    return rgb.map((value) => formatPdfNumber(value / 255)).join(" ");
}

export function formatPdfNumber(value) {
    return Number(value || 0)
        .toFixed(3)
        .replace(/\.?0+$/, "");
}

export function escapePdfText(value) {
    return String(value || "")
        .replace(/\\/g, "\\\\")
        .replace(/\(/g, "\\(")
        .replace(/\)/g, "\\)")
        .replace(/[\r\n\t]+/g, " ");
}

export function formatGeneratedAt(date) {
    const pad = (value) => String(value).padStart(2, "0");

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}