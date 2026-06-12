package com.bepa.eis.server.api.web.application.views.basis.traceability;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class MatrixRowsData {

    private final List<Row> rowList = new ArrayList<>();

    protected MatrixRowsData(List<StakeholderRequirementWrapper> listOfStakeholderRequirementWrappers) {
        int index = 0;
        for (StakeholderRequirementWrapper entity : listOfStakeholderRequirementWrappers) {
            String label = valueOrDefault(entity.getRequirementCode(), "??") + " " + valueOrDefault(entity.getRequirementName(), "Unknown");
            String style = findRowStyle(entity);
            rowList.add(new Row(
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

    private String findRowStyle(StakeholderRequirementWrapper stakeholderRequirementWrapper) {
        if (hasMissingTraceability(stakeholderRequirementWrapper)) {
            return TraceabilityMatrixDocument.STYLE_RED;
        }
        return TraceabilityMatrixDocument.STYLE_NORMAL;
    }

    private boolean hasMissingTraceability(StakeholderRequirementWrapper stakeholderRequirementWrapper) {
        return !stakeholderRequirementWrapper.hasRelationToAnySystemRequirement();
    }

    protected Element getRowElement(Document doc) {
        Element rowsElement = doc.createElement("rows");
        for (Row row : rowList) {
            Element rowElement = doc.createElement("row");
            addRowAttribute(rowElement, "index", row.index.toString());
            addRowAttribute(rowElement, "id", row.id);
            addRowAttribute(rowElement, "code", row.code);
            addRowAttribute(rowElement, "label", row.label);
            addRowAttribute(rowElement, "name", row.name);
            addRowAttribute(rowElement, "description", row.description);
            addRowAttribute(rowElement, "style", row.style);
            rowsElement.appendChild(rowElement);
        }
        return rowsElement;
    }

    private void addRowAttribute(Element rowElement, String name, String value) {
        if (value != null) {
            rowElement.setAttribute(name, value);
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static class Row {
        Integer index;
        String id;
        String code;
        String label;
        String name;
        String description;
        String style;

        public Row(Integer index, String id, String code, String label, String name, String description, String style) {
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