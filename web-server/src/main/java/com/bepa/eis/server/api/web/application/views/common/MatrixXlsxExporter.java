package com.bepa.eis.server.api.web.application.views.common;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds an editable workbook from a currently displayed matrix. */
public final class MatrixXlsxExporter {
    public record Snapshot(String title, String project, List<Double> widths, List<Double> heights, List<MatrixCell> cells) {}
    public record MatrixCell(int row, int column, int rowSpan, int colSpan, String text,
                      String background, String foreground, boolean bold, short rotation, boolean centered) {}
    private record StyleKey(String background, String foreground, boolean bold, short rotation, boolean centered) {}
    private static final int MATRIX_ROW = 3;

    public static byte[] create(Snapshot snapshot) throws IOException {
        validate(snapshot);
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(snapshot.title()));
            sheet.createRow(0).createCell(0).setCellValue(snapshot.title());
            sheet.createRow(1).createCell(0).setCellValue(snapshot.project());
            var titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            var titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            sheet.getRow(0).getCell(0).setCellStyle(titleStyle);
            sheet.getRow(0).setHeightInPoints(24);
            for (int c = 0; c < snapshot.widths().size(); c++) {
                // Excel stores column width in 1/256 of the default font's character width.
                sheet.setColumnWidth(c, (int) Math.min(255 * 256, Math.max(5, (snapshot.widths().get(c) - 5) / 7) * 256));
            }
            for (int r = 0; r < snapshot.heights().size(); r++) {
                sheet.createRow(r + MATRIX_ROW).setHeightInPoints(
                        (float) Math.min(409, Math.max(20, snapshot.heights().get(r))));
            }
            Map<StyleKey, XSSFCellStyle> styles = new HashMap<>();
            for (var source : snapshot.cells()) {
                var key = new StyleKey(source.background(), source.foreground(), source.bold(), source.rotation(), source.centered());
                var style = styles.computeIfAbsent(key, k -> createStyle(workbook, k));
                int firstRow = source.row() + MATRIX_ROW;
                int lastRow = firstRow + source.rowSpan() - 1;
                int lastColumn = source.column() + source.colSpan() - 1;
                for (int r = firstRow; r <= lastRow; r++) {
                    for (int c = source.column(); c <= lastColumn; c++) {
                        sheet.getRow(r).createCell(c).setCellStyle(style);
                    }
                }
                // String cells preserve names beginning with '=' without creating formulas.
                sheet.getRow(firstRow).getCell(source.column()).setCellValue(source.text());
                if (source.rowSpan() > 1 || source.colSpan() > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, source.column(), lastColumn));
                }
            }
            sheet.createFreezePane(2, MATRIX_ROW + 1);
            sheet.setDisplayGridlines(false);
            sheet.setRepeatingRows(new CellRangeAddress(MATRIX_ROW, MATRIX_ROW, -1, -1));
            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setPaperSize(PrintSetup.A3_PAPERSIZE);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.setFitToPage(true);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static XSSFCellStyle createStyle(XSSFWorkbook workbook, StyleKey key) {
        var style = workbook.createCellStyle();
        style.setFillForegroundColor(color(key.background()));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(key.centered() ? HorizontalAlignment.CENTER : HorizontalAlignment.GENERAL);
        style.setRotation(key.rotation());
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        var font = workbook.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setBold(key.bold());
        font.setColor(color(key.foreground()));
        style.setFont(font);
        return style;
    }

    private static XSSFColor color(String hex) {
        if (hex == null || !hex.matches("[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Invalid matrix color.");
        }
        return new XSSFColor(java.util.HexFormat.of().parseHex(hex), new DefaultIndexedColorMap());
    }

    private static void validate(Snapshot snapshot) {
        if (snapshot == null || snapshot.widths() == null || snapshot.heights() == null
                || snapshot.cells() == null || snapshot.cells().isEmpty()
                || snapshot.widths().size() > 16384 || snapshot.heights().size() > 1048576 - MATRIX_ROW
                || (long) snapshot.widths().size() * snapshot.heights().size() > 1_000_000) {
            throw new IllegalArgumentException("Invalid or oversized matrix.");
        }
        for (var cell : snapshot.cells()) {
            if (cell.row() < 0 || cell.column() < 0 || cell.rowSpan() < 1 || cell.colSpan() < 1
                    || (long) cell.row() + cell.rowSpan() > snapshot.heights().size()
                    || (long) cell.column() + cell.colSpan() > snapshot.widths().size()
                    || cell.text() == null || cell.text().length() > 32767
                    || (cell.rotation() != 0 && cell.rotation() != 90)) {
                throw new IllegalArgumentException("Invalid matrix cell.");
            }
        }
    }
}
