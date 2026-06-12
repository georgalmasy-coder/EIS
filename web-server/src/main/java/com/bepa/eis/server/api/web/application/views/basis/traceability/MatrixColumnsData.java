package com.bepa.eis.server.api.web.application.views.basis.traceability;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class MatrixColumnsData {

    private final List<Column> columnList = new ArrayList<>();

    public MatrixColumnsData(List<SystemRequirementWrapper> listOfSystemRequirementWrapperEntities) {

        int index = 0;
        for (SystemRequirementWrapper entity : listOfSystemRequirementWrapperEntities) {

            String style = findColumnStyle(entity);

            String label = valueOrDefault(entity.getRequirementCode(), "??") + " " + valueOrDefault(entity.getRequirementName(), "Unknown");
            columnList.add(new Column(
                    index++,
                    entity.getEntityId().toString(),
                    entity.getRequirementCode(),
                    label,
                    entity.getRequirementName(),
                    entity.getRequirementDescription(),
                    style
            ));
        }
    }

    private String findColumnStyle(SystemRequirementWrapper systemRequirementWrapper) {
        if (hasMissingTraceability(systemRequirementWrapper)) {
            return TraceabilityMatrixDocument.STYLE_RED;
        }
        return TraceabilityMatrixDocument.STYLE_NORMAL;
    }

    private boolean hasMissingTraceability(SystemRequirementWrapper systemRequirementWrapper) {
        return !systemRequirementWrapper.hasRelationToAnyStakeholderRequirement();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public Element getColumnElement(Document doc) {
        Element columnsElement = doc.createElement("columns");
        for (Column column : columnList) {
            Element columnElement = doc.createElement("column");
            addColumnAttribute(columnElement, "index", column.index.toString());
            addColumnAttribute(columnElement, "id", column.id);
            addColumnAttribute(columnElement, "code", column.code);
            addColumnAttribute(columnElement, "label", column.label);
            addColumnAttribute(columnElement, "name", column.name);
            addColumnAttribute(columnElement, "description", column.description);
            addColumnAttribute(columnElement, "style", column.style);
            columnsElement.appendChild(columnElement);
        }
        return columnsElement;
    }

    private void addColumnAttribute(Element columnElement, String name, String value) {
        if (value != null) {
            columnElement.setAttribute(name, value);
        }
    }

    private static class Column {
        Integer index;
        String id;
        String code;
        String label;
        String name;
        String description;
        String style;

        public Column(Integer index, String id, String code, String label, String name, String description, String style) {
            this.index = index;
            this.id = id;
            this.code = code;
            this.label = label;
            this.name = name;
            this.description = description;
            this.style = style;
        }
    }
}