package com.bepa.eis.server.api.generic;

import com.bepa.eis.server.dataprovider.fields.AbstractField;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

abstract public class GenericExporters {

    public static final String NEW_LINE = "\n";

    abstract public String[] getHeaders();

    abstract public String getFileName();

    abstract public String getWorksheetName();

    abstract public void buildWorksheetRow(Row sheetRow, Object rowData);

    abstract public String getXmlRootNodeName();

    abstract public void buildXmlRow(StringBuilder xml, Object rowData);

    abstract public void buildCsvRow(StringBuilder csv, Object rowData);

    abstract public float[] getPdfColWidth();

    abstract public String getPdfTitle();

    abstract public String[] getPdfRowValues(Object rowData);

    public boolean isPdfLandscape() {
        return true;
    }

    public float getPdfMargin() {
        return 36f;
    }

    public float getPdfMinRowHeight() {
        return 18f;
    }

    public int getPdfFontSize() {
        return 8;
    }

    public String getPdfGeneratedAtPattern() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    public String getXlsxContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    public String getXlsxFileName() {
        return getFileName() + ".xlsx";
    }

    public String getCsvContentType() {
        return "text/csv; charset=UTF-8";
    }

    public String getCsvFileName() {
        return getFileName() + ".csv";
    }

    public String getPdfContentType() {
        return "application/pdf";
    }

    public String getPdfFileName() {
        return getFileName() + ".pdf";
    }

    public String getXmlContentType() {
        return "application/xml; charset=UTF-8";
    }

    public String getXmlFileName() {
        return getFileName() + ".xml";
    }

    public String toCsv(List list) {
        StringBuilder csv = new StringBuilder();
        csv.append(buildCsvHeader());

        for (Object row : list) {
            buildCsvRow(csv, row);
        }

        return csv.toString();
    }

    private String buildCsvHeader() {
        StringBuilder csvHeader = new StringBuilder();

        for (String header : getHeaders()) {
            if (!csvHeader.isEmpty()) {
                csvHeader.append(",");
            }
            csvHeader.append(csv(header));
        }

        csvHeader.append(NEW_LINE);
        return csvHeader.toString();
    }

    public String toXml(List list) {
        StringBuilder xml = new StringBuilder();
        xml.append(getXmlHeader());
        xml.append("<").append(getXmlRootNodeName()).append(">").append(NEW_LINE);

        for (Object row : list) {
            buildXmlRow(xml, row);
        }

        xml.append("</").append(getXmlRootNodeName()).append(">").append(NEW_LINE);
        return xml.toString();
    }

    private String getXmlHeader() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NEW_LINE;
    }

    public byte[] toXlsx(List list) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(getWorksheetName());

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < getHeaders().length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(getHeaders()[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Object row : list) {
                Row sheetRow = sheet.createRow(rowIndex++);
                buildWorksheetRow(sheetRow, row);
            }

            for (int i = 0; i < getHeaders().length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] toPdf(List list) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            final PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            final PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            final PDType1Font textFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            final PDRectangle pageSize = getPdfPageSize();
            final float pageWidth = pageSize.getWidth();
            final float pageHeight = pageSize.getHeight();

            final float margin = getPdfMargin();
            final float topY = pageHeight - margin;
            final float footerY = margin - 12f;
            final float bottomY = margin + 24f;
            final float minRowHeight = getPdfMinRowHeight();
            final float startX = margin;

            final String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern(getPdfGeneratedAtPattern()));

            int pageNumber = 1;
            int rowIndex = 0;

            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.OVERWRITE,
                    true,
                    true
            );

            float y = topY;

            y = drawTitle(content, titleFont, startX, y, getPdfTitle());
            y = drawMetaLine(content, textFont, startX, y, "Exported rows: " + list.size());
            y -= 8f;

            y = drawTableHeader(getHeaders(), content, headerFont, startX, y, getPdfColWidth(), minRowHeight);
            y -= minRowHeight;

            for (Object row : list) {
                float rowHeight = estimatePdfRowHeight(row, getPdfColWidth(), textFont, getPdfFontSize(), minRowHeight);

                if (y < bottomY + rowHeight) {
                    drawPdfFooter(content, textFont, margin, footerY, pageNumber, generatedAt, pageWidth - margin);
                    content.close();

                    pageNumber++;

                    page = new PDPage(pageSize);
                    document.addPage(page);
                    content = new PDPageContentStream(
                            document,
                            page,
                            PDPageContentStream.AppendMode.OVERWRITE,
                            true,
                            true
                    );

                    y = topY;
                    y = drawTableHeader(getHeaders(), content, headerFont, startX, y, getPdfColWidth(), minRowHeight);
                    y -= minRowHeight;
                }

                drawPdfTableRow(content, textFont, startX, y, getPdfColWidth(), rowHeight, row, rowIndex);
                y -= rowHeight;
                rowIndex++;
            }

            drawPdfFooter(content, textFont, margin, footerY, pageNumber, generatedAt, pageWidth - margin);

            content.close();
            document.save(out);
            return out.toByteArray();
        }
    }

    private PDRectangle getPdfPageSize() {
        if (isPdfLandscape()) {
            return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        }

        return PDRectangle.A4;
    }

    public float estimatePdfRowHeight(
            Object rowData,
            float[] widths,
            PDType1Font font,
            int fontSize,
            float minRowHeight
    ) throws IOException {
        String[] values = getPdfRowValues(rowData);
        float maxHeight = minRowHeight;

        for (int i = 0; i < values.length && i < widths.length; i++) {
            List<String> wrapped = wrapText(values[i], font, fontSize, widths[i] - 8f);
            float height = Math.max(minRowHeight, wrapped.size() * (fontSize + 2f) + 6f);
            maxHeight = Math.max(maxHeight, height);
        }

        return maxHeight;
    }

    public void drawPdfTableRow(
            PDPageContentStream content,
            PDType1Font font,
            float x,
            float y,
            float[] widths,
            float rowHeight,
            Object rowData,
            int rowIndex
    ) throws IOException {
        String[] values = getPdfRowValues(rowData);
        boolean zebra = rowIndex % 2 == 1;

        float currentX = x;
        for (int i = 0; i < values.length && i < widths.length; i++) {
            drawCellBackground(content, currentX, y - rowHeight + 3, widths[i], rowHeight, false, zebra);
            drawWrappedCellText(
                    content,
                    font,
                    currentX + 4f,
                    y - 12f,
                    values[i],
                    getPdfFontSize(),
                    widths[i] - 8f
            );
            currentX += widths[i];
        }

        drawTableBorders(content, x, y - rowHeight + 3, widths, rowHeight);
    }

    public void drawWrappedCellText(
            PDPageContentStream content,
            PDType1Font font,
            float x,
            float y,
            String text,
            int size,
            float maxWidth
    ) throws IOException {
        List<String> lines = wrapText(text, font, size, maxWidth);

        float lineY = y;
        float lineStep = size + 2f;

        for (String line : lines) {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, lineY);
            content.showText(cleanPdfText(line));
            content.endText();

            lineY -= lineStep;
        }
    }

    public List<String> wrapText(String text, PDType1Font font, int fontSize, float maxWidth) throws IOException {
        String value = cleanPdfText(nvl(text));

        if (value.isBlank()) {
            return List.of("");
        }

        List<String> lines = new ArrayList<>();
        String[] paragraphs = value.split("\\R");

        for (String paragraph : paragraphs) {
            String trimmed = cleanPdfText(paragraph.trim());

            if (trimmed.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();

            for (String rawWord : trimmed.split("\\s+")) {
                String word = cleanPdfText(rawWord);
                String candidate = line.isEmpty() ? word : line + " " + word;
                float width = safeStringWidth(font, candidate, fontSize);

                if (width <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }

                    if (safeStringWidth(font, word, fontSize) <= maxWidth) {
                        line.append(word);
                    } else {
                        lines.addAll(splitLongWord(word, font, fontSize, maxWidth));
                    }
                }
            }

            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
        }

        return lines.isEmpty() ? List.of("") : lines;
    }

    private List<String> splitLongWord(String word, PDType1Font font, int fontSize, float maxWidth) throws IOException {
        String cleanedWord = cleanPdfText(word);

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < cleanedWord.length(); i++) {
            String candidate = current + String.valueOf(cleanedWord.charAt(i));
            float width = safeStringWidth(font, candidate, fontSize);

            if (width <= maxWidth || current.isEmpty()) {
                current.append(cleanedWord.charAt(i));
            } else {
                result.add(current.toString());
                current.setLength(0);
                current.append(cleanedWord.charAt(i));
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    private static float safeStringWidth(PDType1Font font, String text, int fontSize) throws IOException {
        String safe = cleanPdfText(text);

        try {
            return font.getStringWidth(safe) / 1000f * fontSize;
        } catch (IllegalArgumentException e) {
            String fallback = fallbackPdfText(safe);
            return font.getStringWidth(fallback) / 1000f * fontSize;
        }
    }

    public void drawPdfFooter(
            PDPageContentStream content,
            PDType1Font font,
            float leftX,
            float y,
            int pageNumber,
            String generatedAt,
            float rightX
    ) throws IOException {
        content.setStrokingColor(190f / 255f, 190f / 255f, 190f / 255f);
        drawHorizontalLine(content, leftX, y + 10f, rightX);

        content.beginText();
        content.setFont(font, 8);
        content.newLineAtOffset(leftX, y);
        content.showText(cleanPdfText("Generated: " + generatedAt));
        content.endText();

        String pageText = cleanPdfText("Page " + pageNumber);
        float pageTextWidth = safeStringWidth(font, pageText, 8);

        content.beginText();
        content.setFont(font, 8);
        content.newLineAtOffset(rightX - pageTextWidth, y);
        content.showText(pageText);
        content.endText();

        content.setStrokingColor(0f, 0f, 0f);
    }

    public static float drawTitle(PDPageContentStream content, PDType1Font font, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, 16);
        content.newLineAtOffset(x, y);
        content.showText(cleanPdfText(text));
        content.endText();
        return y - 22f;
    }

    public static float drawMetaLine(PDPageContentStream content, PDType1Font font, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, 10);
        content.newLineAtOffset(x, y);
        content.showText(cleanPdfText(text));
        content.endText();
        return y - 14f;
    }

    public static float drawTableHeader(
            String[] headers,
            PDPageContentStream content,
            PDType1Font font,
            float x,
            float y,
            float[] widths,
            float rowHeight
    ) throws IOException {
        float currentX = x;

        for (int i = 0; i < headers.length && i < widths.length; i++) {
            drawCellBackground(content, currentX, y - rowHeight + 3, widths[i], rowHeight, true, false);
            drawCellText(content, font, currentX + 4, y - 12, headers[i], 8);
            currentX += widths[i];
        }

        drawTableBorders(content, x, y - rowHeight + 3, widths, rowHeight);
        return y - rowHeight;
    }

    public static void drawTableBorders(PDPageContentStream content, float x, float y, float[] widths, float rowHeight) throws IOException {
        float totalWidth = sum(widths);

        content.setStrokingColor(140f / 255f, 140f / 255f, 140f / 255f);
        content.setLineWidth(0.5f);

        drawHorizontalLine(content, x, y, x + totalWidth);
        drawHorizontalLine(content, x, y + rowHeight, x + totalWidth);

        float lineX = x;
        for (float width : widths) {
            drawVerticalLine(content, lineX, y, y + rowHeight);
            lineX += width;
        }
        drawVerticalLine(content, x + totalWidth, y, y + rowHeight);

        content.setStrokingColor(0f, 0f, 0f);
    }

    public static void drawCellBackground(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height,
            boolean header,
            boolean zebra
    ) throws IOException {
        content.addRect(x, y, width, height);

        if (header) {
            content.setNonStrokingColor(220f / 255f, 230f / 255f, 242f / 255f);
        } else if (zebra) {
            content.setNonStrokingColor(246f / 255f, 248f / 255f, 251f / 255f);
        } else {
            content.setNonStrokingColor(1f, 1f, 1f);
        }

        content.fill();
        content.setNonStrokingColor(0f, 0f, 0f);
    }

    public static void drawCellText(PDPageContentStream content, PDType1Font font, float x, float y, String text, int size) throws IOException {
        String safe = cleanPdfText(safeText(text, estimateMaxChars(size)));

        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe);
        content.endText();
    }

    public static void drawHorizontalLine(PDPageContentStream content, float x1, float y, float x2) throws IOException {
        content.moveTo(x1, y);
        content.lineTo(x2, y);
        content.stroke();
    }

    public static void drawVerticalLine(PDPageContentStream content, float x, float y1, float y2) throws IOException {
        content.moveTo(x, y1);
        content.lineTo(x, y2);
        content.stroke();
    }

    public static float sum(float[] values) {
        float total = 0f;
        for (float v : values) {
            total += v;
        }
        return total;
    }

    public static String tag(String name, Object value) {
        return "    <" + name + ">" + escapeXml(value == null ? "" : String.valueOf(value)) + "</" + name + ">" + NEW_LINE;
    }

    public static String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    public static String escapeXml(String value) {
        return nvl(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static String nvl(Integer value) {
        return value == null ? "" : value.toString();
    }

    public static String nvl(String value) {
        return value == null ? "" : value;
    }

    public static String nvl(Boolean value) {
        return value == null ? "" : value.toString();
    }

    public static String nvl(AbstractField value) {
        return value == null ? "" : value.toString();
    }

    public static String nvl(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }

    public static String nvl(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    public static String safeText(String text, int maxChars) {
        String value = nvl(text);
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    public static int estimateMaxChars(int fontSize) {
        return switch (fontSize) {
            case 8 -> 42;
            case 9 -> 35;
            case 10 -> 40;
            case 12 -> 50;
            default -> 40;
        };
    }

    private static String cleanPdfText(String text) {
        String value = nvl(text)
                .replace("\uFFFD", "?")
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ")
                .replace("–", "-")
                .replace("—", "-")
                .replace("―", "-")
                .replace("−", "-")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("„", "\"")
                .replace("‟", "\"")
                .replace("‘", "'")
                .replace("’", "'")
                .replace("‚", "'")
                .replace("‛", "'")
                .replace("…", "...")
                .replace("•", "-")
                .replace("·", "-")
                .replace("™", "TM")
                .replace("®", "(R)")
                .replace("©", "(C)")
                .replace("°", " degrees ")
                .replace("µ", "u")
                .replace("×", "x")
                .replace("÷", "/")
                .replace("≤", "<=")
                .replace("≥", ">=")
                .replace("≠", "!=")
                .replace("→", "->")
                .replace("←", "<-")
                .replace("↔", "<->")
                .replace("✓", "OK")
                .replace("✔", "OK")
                .replace("✕", "x")
                .replace("✖", "x");

        StringBuilder cleaned = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (Character.isISOControl(ch)) {
                cleaned.append(' ');
                continue;
            }

            if (isLikelyWinAnsiSupported(ch)) {
                cleaned.append(ch);
            } else {
                cleaned.append('?');
            }
        }

        return cleaned.toString();
    }

    private static String fallbackPdfText(String text) {
        String value = nvl(text);
        StringBuilder fallback = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (ch >= 32 && ch <= 126) {
                fallback.append(ch);
            } else {
                fallback.append('?');
            }
        }

        return fallback.toString();
    }

    private static boolean isLikelyWinAnsiSupported(char ch) {
        if (ch >= 32 && ch <= 126) {
            return true;
        }

        if (ch >= 160 && ch <= 255) {
            return true;
        }

        return switch (ch) {
            case '€',
                 '‚', 'ƒ', '„', '…', '†', '‡',
                 'ˆ', '‰', 'Š', '‹', 'Œ',
                 'Ž',
                 '‘', '’', '“', '”', '•', '–', '—',
                 '˜', '™', 'š', '›', 'œ',
                 'ž', 'Ÿ' -> true;
            default -> false;
        };
    }
}