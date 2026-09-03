package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MatrixInterfaceCellsData {

    private static final DateTimeFormatter DANISH_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final List<InterfaceMatrixProvider.InterfaceRecord> cellList;

    public MatrixInterfaceCellsData(WebSession webSession, EntityType entityType) throws SQLException {
        InterfaceMatrixProvider interfaceMatrixProvider = new InterfaceMatrixProvider(webSession, entityType);
        this.cellList = interfaceMatrixProvider.getLatestInterfaceRecords();
    }

    protected Element getCellElement(Document doc) {
        Element cellsElement = doc.createElement("cells");

        for (InterfaceMatrixProvider.InterfaceRecord cell : cellList) {
            Element cellElement = doc.createElement("cell");
            setAttribute(cellElement, "fromEntityId", cell.fromEntityId());
            setAttribute(cellElement, "toEntityId", cell.toEntityId());
            setAttribute(cellElement, "irlId", cell.irlId());
            setAttribute(cellElement, "classificationIds", cell.classificationIds());
            setAttribute(cellElement, "nextIrlMeeting", cell.nextIrlMeeting());
            setAttribute(cellElement, "changedBy", cell.changedByUserId());
            setAttribute(cellElement, "changed", formatChangedDateTime(cell.changedDateTime()));
            cellsElement.appendChild(cellElement);
        }

        return cellsElement;
    }

    private void setAttribute(Element element, String name, Object value) {
        if (value == null) {
            return;
        }

        String text = String.valueOf(value);

        if (text.isBlank()) {
            return;
        }

        element.setAttribute(name, text);
    }

    private String formatChangedDateTime(Timestamp changedDateTime) {
        if (changedDateTime == null) {
            return "";
        }

        return changedDateTime.toLocalDateTime().format(DANISH_DATE_TIME_FORMATTER);
    }
}
