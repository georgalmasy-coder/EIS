package com.bepa.eis.server.api.web.application.views.basis.traceability;

import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Standalone regression suite; optionally supply a matrix XML file to verify its cells. */
public final class MatrixCellsDataTest {
    public static void main(String[] args) throws Exception {
        MatrixCellsData data = new MatrixCellsData("normal", "");
        data.addCell(0, 0, "", "normal");
        data.addCell(1, 7, "", "yellow");
        data.addCell(2, 8, "X", "green");
        data.addCell(3, 9, "NR", "grayItalic");
        data.addCell(4, 10, "", "red");
        data.addCell(5, 11, "<value> & æøå", "normal");
        Element cells = element(data);
        check(cells.getChildNodes().getLength() == 5, "Omit only the default cell");
        checkCell((Element) cells.getChildNodes().item(0), 1, 7, "", "yellow");
        checkCell((Element) cells.getChildNodes().item(1), 2, 8, "X", "green");
        checkCell((Element) cells.getChildNodes().item(2), 3, 9, "NR", "grayItalic");
        checkCell((Element) cells.getChildNodes().item(3), 4, 10, "", "red");
        checkCell((Element) cells.getChildNodes().item(4), 5, 11, "<value> & æøå", "normal");

        MatrixCellsData defaults = new MatrixCellsData("normal", "");
        for (int r = 0; r < 100; r++) {
            for (int c = 0; c < 100; c++) defaults.addCell(r, c, "", "normal");
        }
        check(!element(defaults).hasChildNodes(), "All-default matrix remains empty");
        MatrixCellsData custom = new MatrixCellsData("yellow", "default");
        custom.addCell(0, 0, "default", "yellow");
        custom.addCell(0, 1, "", "yellow");
        check(element(custom).getChildNodes().getLength() == 1, "Respect explicit defaults and empty overrides");
        if (args.length > 0) verifyFile(Path.of(args[0]));
        System.out.println("MatrixCellsData regression checks passed");
    }

    private static Element element(MatrixCellsData data) throws Exception {
        return data.getCellElement(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
    }

    private static void checkCell(Element cell, int row, int column, String value, String style) {
        check(cell.getAttribute("row").equals(Integer.toString(row)), "Row index preserved");
        check(cell.getAttribute("col").equals(Integer.toString(column)), "Column index preserved");
        check(cell.getAttribute("value").equals(value), "Value preserved");
        check(cell.getAttribute("style").equals(style), "Style preserved");
    }

    private static void verifyFile(Path path) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        MatrixCellsData data = null;
        String defaultStyle = "normal", defaultValue = "";
        long total = 0, retained = 0, expectedHash = 1;
        try (var input = Files.newInputStream(path)) {
            var reader = factory.createXMLStreamReader(input);
            try {
                while (reader.hasNext()) {
                    if (reader.next() != XMLStreamConstants.START_ELEMENT) continue;
                    if (reader.getLocalName().equals("traceabilityMatrix")) {
                        defaultStyle = reader.getAttributeValue(null, "defaultCellStyle");
                        defaultValue = reader.getAttributeValue(null, "defaultCellValue");
                        data = new MatrixCellsData(defaultStyle, defaultValue);
                    } else if (reader.getLocalName().equals("cell")) {
                        int row = Integer.parseInt(reader.getAttributeValue(null, "row"));
                        int column = Integer.parseInt(reader.getAttributeValue(null, "col"));
                        String value = reader.getAttributeValue(null, "value");
                        String style = reader.getAttributeValue(null, "style");
                        data.addCell(row, column, value, style);
                        total++;
                        if (!Objects.equals(value, defaultValue) || !Objects.equals(style, defaultStyle)) {
                            retained++;
                            expectedHash = 31 * expectedHash + Objects.hash(row, column, value, style);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        }
        check(data != null, "Matrix present in input");
        Element cells = element(data);
        check(cells.getChildNodes().getLength() == retained, "Retain all non-default cells from input");
        long actualHash = 1;
        for (var node = cells.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element cell = (Element) node;
            actualHash = 31 * actualHash + Objects.hash(Integer.parseInt(cell.getAttribute("row")),
                    Integer.parseInt(cell.getAttribute("col")), cell.getAttribute("value"), cell.getAttribute("style"));
        }
        check(actualHash == expectedHash, "Preserve coordinates, values, styles and order from input");
        System.out.printf("Input cells: %d; retained XML cells: %d; omitted defaults: %d%n", total, retained, total - retained);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
