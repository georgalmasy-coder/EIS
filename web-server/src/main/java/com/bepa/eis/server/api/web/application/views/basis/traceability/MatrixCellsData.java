package com.bepa.eis.server.api.web.application.views.basis.traceability;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class MatrixCellsData {

    private final List<Cell> cellList = new ArrayList<>() ;

    protected void addCell(Integer rowindex, Integer colindex, String value, String style) {
        cellList.add(new Cell(rowindex, colindex, value, style));
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    protected Element getCellElement(Document doc) {
        Element stylesElement = doc.createElement("cells");
        for (Cell cell : cellList) {
            Element cellElement = doc.createElement("cell");
            addStyleAttribute(cellElement, "row", cell.rowindex.toString());
            addStyleAttribute(cellElement, "col", cell.colindex.toString());
            addStyleAttribute(cellElement, "value", cell.value);
            addStyleAttribute(cellElement, "style", cell.style);
            stylesElement.appendChild(cellElement);
        }
        return stylesElement;
    }

    private void addStyleAttribute(Element styleElement, String name, String value) {
        if (value != null) {
            styleElement.setAttribute(name, value);
        }
    }

    private static class Cell {
        Integer rowindex;
        Integer colindex;
        String value;
        String style;
        public Cell(Integer rowindex, Integer colindex, String value, String style) {
            this.rowindex = rowindex;
            this.colindex = colindex;
            this.value = value;
            this.style = style;
        }
    }
}
