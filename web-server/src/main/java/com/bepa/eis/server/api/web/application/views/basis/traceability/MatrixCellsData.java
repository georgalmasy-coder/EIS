package com.bepa.eis.server.api.web.application.views.basis.traceability;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MatrixCellsData {

    private final List<Cell> cellList = new ArrayList<>() ;
    private final String defaultStyle;
    private final String defaultValue;

    protected MatrixCellsData(String defaultStyle, String defaultValue) {
        this.defaultStyle = defaultStyle;
        this.defaultValue = defaultValue;
    }

    protected void addCell(Integer rowindex, Integer colindex, String value, String style) {
        // Missing cells are represented by the defaults on traceabilityMatrix.
        if (Objects.equals(defaultValue, value) && Objects.equals(defaultStyle, style)) {
            return;
        }
        cellList.add(new Cell(rowindex, colindex, value, style));
    }

    protected Element getCellElement(Document doc) {
        Element cellsElement = doc.createElement("cells");
        for (Cell cell : cellList) {
            Element cellElement = doc.createElement("cell");
            addStyleAttribute(cellElement, "row", cell.rowindex.toString());
            addStyleAttribute(cellElement, "col", cell.colindex.toString());
            addStyleAttribute(cellElement, "value", cell.value);
            addStyleAttribute(cellElement, "style", cell.style);
            cellsElement.appendChild(cellElement);
        }
        return cellsElement;
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
