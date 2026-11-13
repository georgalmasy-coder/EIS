package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.views.pro.dashboard.common.DashboardMetaData;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixEntityOrder;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
        editInterfacesElement.setAttribute("readOnly", String.valueOf(version != null));
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
        appendInterfaceRows(doc, editInterfacesElement, leftInterfaces, rightInterfaces);

        return editInterfacesElement;
    }

    private void appendInterfaceRows(Document doc, Element root, Element outgoing, Element incoming) throws SQLException {
        if (entityId == null || entityId < 0) {
            return;
        }

        InterfaceMatrixProvider provider = new InterfaceMatrixProvider(webSession, entityType);
        List<InterfaceMatrixProvider.EditInterfaceRecord> records = new ArrayList<>(
                provider.getInterfaceRecordsByFrom(entityId, entityType, version));
        records.addAll(provider.getInterfaceRecordsByTo(entityId, entityType, version));
        Map<Integer, Integer> positions = new HashMap<>();
        Element candidates = doc.createElement("entities");
        root.appendChild(candidates);
        for (var entity : InterfaceMatrixEntityOrder.getEntities(webSession, entityType)) {
            Integer id = entity.getEntityId().getValue();
            positions.put(id, positions.size());
            if (version == null) {
                Element candidate = doc.createElement("entity");
                addElement(doc, candidate, "entityId", id);
                addElement(doc, candidate, "id", entity.getCode());
                addElement(doc, candidate, "name", entity.getName());
                candidates.appendChild(candidate);
            }
        }
        if (version != null) {
            for (var record : records) {
                positions.putIfAbsent(record.fromEntityId(), positions.size());
                positions.putIfAbsent(record.toEntityId(), positions.size());
            }
        }
        appendInterfaceRows(doc, outgoing, incoming, entityId, records, positions);
    }

    static void appendInterfaceRows(Document doc, Element outgoing, Element incoming,
                                    Integer entityId, List<InterfaceMatrixProvider.EditInterfaceRecord> records,
                                    Map<Integer, Integer> positions) {
        records.sort(Comparator.comparingInt(record -> positions.getOrDefault(
                record.fromEntityId().equals(entityId) ? record.toEntityId() : record.fromEntityId(), Integer.MAX_VALUE)));

        for (var record : records) {
            boolean isOutgoing = record.fromEntityId().equals(entityId);
            Integer otherId = isOutgoing ? record.toEntityId() : record.fromEntityId();
            if (otherId.equals(entityId) || !positions.containsKey(otherId)
                    || (!isOutgoing && !record.toEntityId().equals(entityId))) {
                continue;
            }
            Element element = doc.createElement("interface");
            addElement(doc, element, "fromEntityId", record.fromEntityId());
            addElement(doc, element, "toEntityId", record.toEntityId());
            // Provider code/name fields describe the endpoint opposite the edited entity.
            addElement(doc, element, isOutgoing ? "toEntityCode" : "fromEntityCode", record.toEntityCode());
            addElement(doc, element, isOutgoing ? "toEntityName" : "fromEntityName", record.toEntityName());
            addElement(doc, element, "fromTrlId", record.fromTrlId());
            addElement(doc, element, "toTrlId", record.toTrlId());
            addElement(doc, element, "fromIrlId", record.irlId());
            addElement(doc, element, "fromClassificationIds", record.classificationIds());
            addElement(doc, element, "toIrlId", record.reverseIrlId());
            addElement(doc, element, "toClassificationIds", record.reverseClassificationIds());
            addElement(doc, element, "nextIrlMeeting", record.nextIrlMeeting());
            (isOutgoing ? outgoing : incoming).appendChild(element);
        }
    }

    private static void addElement(Document doc, Element parentElement, String elementName, Object value) {
        Element element = doc.createElement(elementName);
        element.setTextContent(value != null ? String.valueOf(value) : "");
        parentElement.appendChild(element);
    }
}
