package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.views.pro.dashboard.common.DashboardMetaData;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.List;

public class EditInterfaceElements {

    private final WebSession webSession;
    private final EntityType entityType;
    private final Integer entityId;

    public EditInterfaceElements(WebSession webSession, EntityType entityType, Integer entityId) {
        this.webSession = webSession;
        this.entityType = entityType;
        this.entityId = entityId;
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
        editInterfacesElement.appendChild(getInterfaceRowsElement(doc));

        return editInterfacesElement;
    }

    private Element getInterfaceRowsElement(Document doc) throws SQLException {
        Element interfacesElement = doc.createElement("interfaces");

        if (entityId == null || entityId < 0) {
            return interfacesElement;
        }

        InterfaceMatrixProvider interfaceMatrixProvider = new InterfaceMatrixProvider(webSession, entityType);
        List<InterfaceMatrixProvider.EditInterfaceRecord> records =
                interfaceMatrixProvider.getLatestInterfaceRecordsByFrom(entityId, entityType);

        for (InterfaceMatrixProvider.EditInterfaceRecord record : records) {
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
            interfacesElement.appendChild(interfaceElement);
        }

        return interfacesElement;
    }

    private void addElement(Document doc, Element parentElement, String elementName, Object value) {
        Element element = doc.createElement(elementName);
        element.setTextContent(value != null ? String.valueOf(value) : "");
        parentElement.appendChild(element);
    }
}
