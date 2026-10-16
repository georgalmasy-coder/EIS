package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.views.pro.dashboard.common.DashboardMetaData;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixEntityOrder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.List;
import java.util.Comparator;
import java.util.Map;

public class EditInterfaceElements {

    private final WebSession webSession;
    private final EntityType entityType;
    private final Integer entityId;
    private final Integer version;

    public EditInterfaceElements(WebSession webSession, EntityType entityType, Integer entityId) {
        this(webSession, entityType, entityId, null);
    }

    public EditInterfaceElements(WebSession webSession, EntityType entityType, Integer entityId, Integer version) {
        this.webSession = webSession;
        this.entityType = entityType;
        this.entityId = entityId;
        this.version = version;
    }

    public Element getEditInterfaceElements(Document doc) throws SQLException {
        Element editInterfacesElement = doc.createElement("editInterfaces");
        editInterfacesElement.setAttribute("entityType", String.valueOf(entityType.getId()));
        editInterfacesElement.setAttribute("entityTypeName", entityType.getDescription());
        if (entityId != null) {
            editInterfacesElement.setAttribute("entityId", String.valueOf(entityId));
        }

        DashboardMetaData dashboardMetaData = new DashboardMetaData(webSession);
        editInterfacesElement.appendChild(dashboardMetaData.getTrlElement(doc));
        editInterfacesElement.appendChild(dashboardMetaData.getIrlElement(doc));
        editInterfacesElement.appendChild(dashboardMetaData.getClassificationElement(doc));
        Element leftInterfaces = doc.createElement("leftInterfaces");
        Element rightInterfaces = doc.createElement("rightInterfaces");
        editInterfacesElement.appendChild(leftInterfaces);
        editInterfacesElement.appendChild(rightInterfaces);
        appendInterfaceRows(doc, leftInterfaces, rightInterfaces);

        return editInterfacesElement;
    }

    private void appendInterfaceRows(Document doc, Element leftInterfaces, Element rightInterfaces) throws SQLException {
        if (entityId == null || entityId < 0) {
            return;
        }

        InterfaceMatrixProvider interfaceMatrixProvider = new InterfaceMatrixProvider(webSession, entityType);
        List<InterfaceMatrixProvider.EditInterfaceRecord> records =
                interfaceMatrixProvider.getInterfaceRecordsByFrom(entityId, entityType, version);
        if (records.isEmpty()) {
            return;
        }
        Map<Integer, Integer> positions = InterfaceMatrixEntityOrder.getPositions(webSession, entityType);
        if (version != null) {
            // Keep historical interfaces even when a target is no longer in the current matrix.
            positions.putIfAbsent(entityId, positions.size());
            for (var record : records) positions.putIfAbsent(record.toEntityId(), positions.size());
        }
        appendInterfaceRows(doc, leftInterfaces, rightInterfaces, entityId, records, positions);
    }

    static void appendInterfaceRows(Document doc, Element leftInterfaces, Element rightInterfaces,
                                    Integer entityId, List<InterfaceMatrixProvider.EditInterfaceRecord> records,
                                    Map<Integer, Integer> positions) {
        Integer currentPosition = positions.get(entityId);
        if (currentPosition == null) {
            return;
        }
        records.sort(Comparator.comparingInt(record -> positions.getOrDefault(record.toEntityId(), Integer.MAX_VALUE)));

        for (InterfaceMatrixProvider.EditInterfaceRecord record : records) {
            Integer targetPosition = positions.get(record.toEntityId());
            // The diagonal is not an interface cell; targets absent from the matrix are not shown.
            if (targetPosition == null || targetPosition.equals(currentPosition)) {
                continue;
            }
            Element interfaceElement = doc.createElement("interface");
            addElement(doc, interfaceElement, "fromEntityId", record.fromEntityId());
            addElement(doc, interfaceElement, "toEntityId", record.toEntityId());
            addElement(doc, interfaceElement, "toEntityCode", record.toEntityCode());
            addElement(doc, interfaceElement, "toEntityName", record.toEntityName());
            addElement(doc, interfaceElement, "fromTrlId", record.fromTrlId());
            addElement(doc, interfaceElement, "toTrlId", record.toTrlId());
            addElement(doc, interfaceElement, "fromIrlId", record.irlId());
            addElement(doc, interfaceElement, "fromClassificationIds", record.classificationIds());
            addElement(doc, interfaceElement, "toIrlId", record.reverseIrlId());
            addElement(doc, interfaceElement, "toClassificationIds", record.reverseClassificationIds());
            addElement(doc, interfaceElement, "nextIrlMeeting", record.nextIrlMeeting());
            (targetPosition < currentPosition ? leftInterfaces : rightInterfaces).appendChild(interfaceElement);
        }

    }

    private static void addElement(Document doc, Element parentElement, String elementName, Object value) {
        Element element = doc.createElement(elementName);
        element.setTextContent(value != null ? String.valueOf(value) : "");
        parentElement.appendChild(element);
    }
}
