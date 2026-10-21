package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.server.api.web.application.views.common.MatrixXlsxExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Round-trip the actual browser snapshot through POI and reopen the XLSX. */
public final class InterfaceMatrixXlsxTest {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of("web-server/target/interface-xlsx-test");
        var snapshot = new ObjectMapper().readValue(directory.resolve("snapshot.json").toFile(), MatrixXlsxExporter.Snapshot.class);
        byte[] bytes = MatrixXlsxExporter.create(snapshot);
        Files.write(directory.resolve("N2-diagram.xlsx"), bytes);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            check(sheet.getRow(0).getCell(0).getStringCellValue().contains("N²"), "Title");
            check(sheet.getRow(1).getCell(0).getStringCellValue().equals("Projekt æøå"), "Unicode project");
            check(sheet.getRow(4).getCell(1).getStringCellValue().equals("P-1\nKøling & strøm"), "Header content");
            var relation = sheet.getRow(4).getCell(3);
            check(relation.getStringCellValue().contains("Classification: Power"), "Interface content");
            check(relation.getStringCellValue().contains("Overdue"), "Overdue indicator");
            check(relation.getCellStyle().getFillForegroundXSSFColor().getARGBHex().endsWith("9DCCA8"), "IRL color");
            check(sheet.getMergedRegions().stream().anyMatch(region -> region.formatAsString().equals("A5:A6")), "Merged axis");
            check(sheet.getPaneInformation().isFreezePane(), "Frozen headers");
            check(sheet.getRow(5).getCell(3).getCellType() == CellType.STRING, "Text stays text");
            check(sheet.getRow(5).getCell(3).getStringCellValue().contains("=Safe string"), "Formula-like name preserved");
        }
        var invalid = new MatrixXlsxExporter.MatrixCell(-1, 0, 1, 1, "", "FFFFFF", "000000", false, (short) 0, false);
        try {
            MatrixXlsxExporter.create(new MatrixXlsxExporter.Snapshot(snapshot.title(), snapshot.project(), snapshot.widths(), snapshot.heights(), java.util.List.of(invalid)));
            throw new AssertionError("Invalid coordinates accepted");
        } catch (IllegalArgumentException expected) {
            // Expected: malformed cells must not produce a corrupt workbook.
        }
        testTraceability();
        System.out.println("Passed: XLSX round-trip preserves the filtered diagram, colors, Unicode, merged axis and text cells.");
    }

    private static void testTraceability() throws Exception {
        Path directory = Path.of("web-server/target/traceability-matrix-test");
        var snapshot = new ObjectMapper().readValue(directory.resolve("snapshot.json").toFile(), MatrixXlsxExporter.Snapshot.class);
        byte[] bytes = MatrixXlsxExporter.create(snapshot);
        Files.write(directory.resolve("Traceability-matrix.xlsx"), bytes);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Traceability Matrix");
            check(sheet != null, "Traceability worksheet name");
            check(sheet.getRow(3).getCell(2).getStringCellValue().equals("2"), "Column header");
            check(sheet.getRow(4).getCell(1).getStringCellValue().equals("1"), "Row header");
            String[] colors = {"00FF00", "CCCCCC", "FFFF00"};
            for (int c = 2; c < 5; c++) {
                var cell = sheet.getRow(4).getCell(c);
                check(cell.getStringCellValue().isEmpty(), "Relation cell has no X or NR");
                check(cell.getCellStyle().getFillForegroundXSSFColor().getARGBHex().endsWith(colors[c - 2]), "Relation color");
            }
        }
        var iconSnapshot = new ObjectMapper().readValue(directory.resolve("icon-snapshot.json").toFile(), MatrixXlsxExporter.Snapshot.class);
        byte[] iconBytes = MatrixXlsxExporter.create(iconSnapshot);
        Files.write(directory.resolve("Traceability-icons.xlsx"), iconBytes);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(iconBytes))) {
            var sheet = workbook.getSheet("Traceability Matrix");
            String[] symbols = {"✓", "⊘", "?"};
            String[] colors = {"287A45", "62666B", "996600"};
            for (int c = 2; c < 5; c++) {
                var cell = sheet.getRow(4).getCell(c);
                check(cell.getStringCellValue().equals(symbols[c - 2]), "Status symbol exported");
                check(cell.getCellStyle().getAlignment() == org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER, "Status symbol horizontally centered");
                check(cell.getCellStyle().getVerticalAlignment() == org.apache.poi.ss.usermodel.VerticalAlignment.CENTER, "Status symbol vertically centered");
                check(cell.getCellStyle().getFillForegroundXSSFColor().getARGBHex().endsWith("FFFFFF"), "Icon view has white background");
                check(cell.getCellStyle().getFont().getXSSFColor().getARGBHex().endsWith(colors[c - 2]), "Status symbol color");
            }
        }
        System.out.println("Passed: traceability XLSX preserves row and column headers and colored, empty relation cells.");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
