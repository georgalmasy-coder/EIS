package com.bepa.eis.server.api.web.application.views.pro.dashboard.teamwork;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.api.web.application.views.pro.dashboard.common.DashboardMetaData;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class DashboardTeamworkDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(DashboardTeamworkDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    protected final Map<Integer, AbstractEntity> mapOfEntities = new HashMap<>();
    private List<InterfaceMatrixProvider.InterfaceRecord> interfaces = new ArrayList<>();
    private final List<TeamworkRecord> listOfSystemsTeamworkRecords = new ArrayList<>();
    private record InterfaceKey(String fromId, String toId) {}
    private final Map<InterfaceKey, TeamworkRecord> interfacesByDirection = new HashMap<>();

    private DashboardMetaData dashboardMetaData;

    abstract protected PageType getPageType();
    abstract protected EntityType getEntityType();
    abstract protected void loadEntities();

    public DashboardTeamworkDocument(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(getPageType());
        rootElement.addElement(topPanel.getTopPanelElements());

        dashboardDocument();

        Element dashboardElement = buildDashboardIrlElement();
        getRoot().appendChild(dashboardElement);

        dashboardElement.appendChild(getTrlMetaDataElement());
        dashboardElement.appendChild(getIrlMetaDataElement());
        dashboardElement.appendChild(getClassificationMetaDataElement());
        dashboardElement.appendChild(getUserMetaDataElement());
        dashboardElement.appendChild(getUserMetaDataElement());
        dashboardElement.appendChild(getDepartmentMetaDataElement());
        dashboardElement.appendChild(getInterfaceElements());
        dashboardElement.appendChild(getSystemElements());
    }

    DashboardTeamworkDocument(WebSession session, List<AbstractEntity> activeSystems,
                              List<InterfaceMatrixProvider.InterfaceRecord> interfaceRecords) throws Exception {
        super(session);
        rootElement = initXmlDocument(getClass().getSimpleName());
        topPanel = null;
        for (AbstractEntity entity : activeSystems) {
            mapOfEntities.put(entity.getEntityId().getValue(), entity);
        }
        buildSystemsTeamwork(interfaceRecords);
        getRoot().appendChild(getInterfaceElements());
    }

    private void dashboardDocument() throws SQLException {
        loadEntities();
        buildSystemsTeamwork();
        dashboardMetaData = new DashboardMetaData(getWebSession());
    }

    private void buildSystemsTeamwork() throws SQLException  {
        InterfaceMatrixProvider interfaceManagementProvider = new InterfaceMatrixProvider(getWebSession(), getEntityType());
        interfaces = interfaceManagementProvider.getAllInterfaceRecords(getEntityType());
        buildSystemsTeamwork(interfaces);
    }

    private void buildSystemsTeamwork(List<InterfaceMatrixProvider.InterfaceRecord> interfaces) {
        int skipped = 0;
        InterfaceMatrixProvider.InterfaceRecord firstSkipped = null;
        if (interfaces != null) {
            for (InterfaceMatrixProvider.InterfaceRecord interfaceRecord : interfaces) {

                TeamworkRecord record = new TeamworkRecord();

                AbstractEntity fromEntity = mapOfEntities.get(interfaceRecord.fromEntityId());
                AbstractEntity toEntity = mapOfEntities.get(interfaceRecord.toEntityId());
                // Interfaces may outlive either endpoint's active version.
                if (fromEntity == null || toEntity == null) {
                    skipped++;
                    if (firstSkipped == null) firstSkipped = interfaceRecord;
                    continue;
                }

                record.setFromEntityId(fromEntity.getEntityId());
                record.setFromEntityCode(fromEntity.getCode());
                record.setFromEntityName(fromEntity.getName());
                record.setFromEntityTrl(fromEntity.getEntityTrl());
                record.setFromEntityOwner(fromEntity.getEntityOwner());
                record.setFromDepartment(fromEntity.getEntityDepartment());
                record.setFromIrlId(interfaceRecord.irlId());
                record.setFromClassificationIds(interfaceRecord.classificationIds());
                record.setNextIrlMeeting(interfaceRecord.nextIrlMeeting());

                record.setToEntityId(toEntity.getEntityId());
                record.setToCode(toEntity.getCode());
                record.setToName(toEntity.getName());
                record.setToTrlId(toEntity.getEntityTrl());
                record.setToOwner(toEntity.getEntityOwner());
                record.setToDepartment(toEntity.getEntityDepartment());

                addInterfaceToList(record);
            }
        }
        if (skipped > 0) {
            log.warn("Systems teamwork skipped {} interfaces with an endpoint outside active systems. customerId={}, projectId={}, firstFromEntityId={}, firstToEntityId={}",
                    skipped, getWebSession() == null ? null : getWebSession().getCustomerId(),
                    getWebSession() == null ? null : getWebSession().getProjectId(),
                    firstSkipped.fromEntityId(), firstSkipped.toEntityId());
        }
    }

    private void addInterfaceToList(TeamworkRecord newRecord) {
        TeamworkRecord reverse = interfacesByDirection.get(new InterfaceKey(newRecord.getToEntityId(), newRecord.getFromEntityId()));
        if (reverse != null) {
            reverse.setToIrlId(newRecord.getFromIrlId());
            reverse.setToClassificationIds(newRecord.getFromClassificationIds());
            return;
        }
        listOfSystemsTeamworkRecords.add(newRecord);
        // Preserve the first matching row and input order, as the former list scan did.
        interfacesByDirection.putIfAbsent(
                new InterfaceKey(newRecord.getFromEntityId(), newRecord.getToEntityId()), newRecord);
    }

    private Element getTrlMetaDataElement() {
        return dashboardMetaData.getTrlElement(getDoc());
    }

    private Element getIrlMetaDataElement() {
        return dashboardMetaData.getIrlElement(getDoc());
    }

    private Element getClassificationMetaDataElement() {
        return dashboardMetaData.getClassificationElement(getDoc());
    }

    private Element getUserMetaDataElement() {
        return dashboardMetaData.getUserElement(getDoc());
    }

    private Element getDepartmentMetaDataElement() {
        return dashboardMetaData.getDepartmentElement(getDoc());
    }

    private Element getSystemElements() {
        Element systems = getDoc().createElement("systems");
        for (AbstractEntity entity : mapOfEntities.values()) {
            TeamworkRecord record = new TeamworkRecord();
            record.setFromEntityId(entity.getEntityId());
            record.setFromEntityCode(entity.getCode());
            record.setFromEntityName(entity.getName());
            record.setFromEntityTrl(entity.getEntityTrl());
            record.setFromEntityOwner(entity .getEntityOwner());
            record.setFromDepartment(entity.getEntityDepartment());
            Element system = getDoc().createElement("system");
            addElement(system, "fromEntityId", record.getFromEntityId());
            addElement(system, "fromSbsCode", record.getFromEntityCode());
            addElement(system, "fromSystemName", record.getFromEntityName());
            addElement(system, "fromTrlId", record.getFromTrlId());
            addElement(system, "fromSystemOwnerId", record.getFromOwnerId());
            addElement(system, "fromSystemDepartmentId", record.getFromDepartmentId());
            systems.appendChild(system);
        }
        return systems;
    }

    private Element getInterfaceElements() {
        Element interfaceElements = getDoc().createElement("interfaces");
        if (listOfSystemsTeamworkRecords != null) {
            for (TeamworkRecord record : listOfSystemsTeamworkRecords) {
                Element interfaceElement = getDoc().createElement("interface");
                addElement(interfaceElement, "fromEntityId", record.getFromEntityId());
                addElement(interfaceElement, "fromSbsCode", record.getFromEntityCode());
                addElement(interfaceElement, "fromSystemName", record.getFromEntityName());
                addElement(interfaceElement, "fromTrlId", record.getFromTrlId());
                addElement(interfaceElement, "fromSystemOwnerId", record.getFromOwnerId());
                addElement(interfaceElement, "fromSystemDepartmentId", record.getFromDepartmentId());

                if (record.getFromIrlId() != null && !record.getFromIrlId().isBlank()) {
                    addElement(interfaceElement, "fromIrlId", record.getFromIrlId());
                }
                if (record.getFromClassificationIds() != null && !record.getFromClassificationIds().isEmpty()) {
                    addElement(interfaceElement, "fromClassificationIds", record.getFromClassificationIds());
                }

                addElement(interfaceElement, "toEntityId", record.getToEntityId());
                addElement(interfaceElement, "toSbsCode", record.getToEntityCode());
                addElement(interfaceElement, "toSystemName", record.getToName());
                //addElement(interfaceElement, "toTrlId", record.getToTrlId());
                addElement(interfaceElement, "toSystemOwnerId", record.getToOwnerId());
                addElement(interfaceElement, "toSystemDepartmentId", record.getToDepartmentId());

                if (record.getToIrlId() != null && !record.getToIrlId().isBlank()) {
                    addElement(interfaceElement, "toIrlId", record.getToIrlId());
                }
                if (record.getToClassificationIds() != null && !record.getToClassificationIds().isEmpty()) {
                    addElement(interfaceElement, "toClassificationIds", record.getToClassificationIds());
                }

                if (record.getNextIrlMeeting() != null && !record.getNextIrlMeeting().isBlank()) {
                    addElement(interfaceElement, "nextIrlMeeting", record.getNextIrlMeeting());
                }

                interfaceElements.appendChild(interfaceElement);
            }
        }
        return interfaceElements;
    }

    private void addElement(Element parentelement, String elementName, String value) {
        Element element = getDoc().createElement(elementName);
        element.setTextContent(value);
        parentelement.appendChild(element);
    }

    private Element buildDashboardIrlElement() {
        return getDoc().createElement("dashboardIrl");
    }

}
