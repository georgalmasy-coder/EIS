package com.bepa.eis.server.api.web.application.views.pro.dashboard.systemsteamwork;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.api.web.application.views.pro.dashboard.common.DashboardMetaData;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardSystemsTeamworkDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(DashboardSystemsTeamworkDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    private final Map<Integer, SystemBreakdownEntity> mapOfPhysicalStructures = new HashMap<>();
    private List<InterfaceMatrixProvider.InterfaceRecord> interfaces = new ArrayList<>();
    private final List<SystemsTeamworkRecord> listOfSystemsTeamworkRecords = new ArrayList<>();
    private record InterfaceKey(String fromId, String toId) {}
    private final Map<InterfaceKey, SystemsTeamworkRecord> interfacesByDirection = new HashMap<>();

    private DashboardMetaData dashboardMetaData;

    public DashboardSystemsTeamworkDocument(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.DASHBOARD_SYSTEMS_TEAMWORK_PAGE);
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

    private void dashboardDocument() throws SQLException {
        loadPhysicalStructures();
        buildSystemsTeamwork();
        dashboardMetaData = new DashboardMetaData(getWebSession());
    }

    private void loadPhysicalStructures() {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
        List<SystemBreakdownEntity> listOfPhysicalStructures = systemBreakdownProvider.getAllSystemBreakdown(false);

        for (SystemBreakdownEntity systemBreakdownEntity : listOfPhysicalStructures) {
            mapOfPhysicalStructures.put(systemBreakdownEntity.getEntityId().getValue(), systemBreakdownEntity);
        }
    }

    private void buildSystemsTeamwork() throws SQLException  {
        EntityType entityType = EntityType.SYSTEMS_BREAKDOWN;
        InterfaceMatrixProvider interfaceManagementProvider = new InterfaceMatrixProvider(getWebSession(), entityType);
        interfaces = interfaceManagementProvider.getAllInterfaceRecords(entityType);
        buildSystemsTeamwork(interfaces);
    }

    DashboardSystemsTeamworkDocument(WebSession session, List<SystemBreakdownEntity> activeSystems,
                                    List<InterfaceMatrixProvider.InterfaceRecord> interfaceRecords) throws Exception {
        super(session);
        rootElement = initXmlDocument(getClass().getSimpleName());
        topPanel = null;
        for (SystemBreakdownEntity system : activeSystems)
            mapOfPhysicalStructures.put(system.getEntityId().getValue(), system);
        buildSystemsTeamwork(interfaceRecords);
        getRoot().appendChild(getInterfaceElements());
    }

    private void buildSystemsTeamwork(List<InterfaceMatrixProvider.InterfaceRecord> interfaces) {
        int skipped = 0;
        InterfaceMatrixProvider.InterfaceRecord firstSkipped = null;
        if (interfaces != null) {
            for (InterfaceMatrixProvider.InterfaceRecord interfaceRecord : interfaces) {

                SystemsTeamworkRecord record = new SystemsTeamworkRecord();

                SystemBreakdownEntity fromEntity = mapOfPhysicalStructures.get(interfaceRecord.fromEntityId());
                SystemBreakdownEntity toEntity = mapOfPhysicalStructures.get(interfaceRecord.toEntityId());
                // Interfaces may outlive either endpoint's active version.
                if (fromEntity == null || toEntity == null) {
                    skipped++;
                    if (firstSkipped == null) firstSkipped = interfaceRecord;
                    continue;
                }

                record.setFromEntityId(fromEntity.getEntityId());
                record.setFromSbsCode(fromEntity.getSbsCode());
                record.setFromSystemName(fromEntity.getSystemName());
                record.setFromTrl(fromEntity.getTrl());
                record.setFromSystemOwner(fromEntity.getSystemOwner());
                record.setFromSystemDepartment(fromEntity.getSystemDepartment());
                record.setFromIrlId(interfaceRecord.irlId());
                record.setFromClassificationIds(interfaceRecord.classificationIds());
                record.setNextIrlMeeting(interfaceRecord.nextIrlMeeting());

                record.setToEntityId(toEntity.getEntityId());
                record.setToSbsCode(toEntity.getSbsCode());
                record.setToSystemName(toEntity.getSystemName());
                record.setToTrlId(toEntity.getTrl());
                record.setToSystemOwner(toEntity.getSystemOwner());
                record.setToSystemDepartment(toEntity.getSystemDepartment());

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

    private void addInterfaceToList(SystemsTeamworkRecord newRecord) {
        SystemsTeamworkRecord reverse = interfacesByDirection.get(
                new InterfaceKey(newRecord.getToEntityId(), newRecord.getFromEntityId()));
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
        for (SystemBreakdownEntity entity : mapOfPhysicalStructures.values()) {
            SystemsTeamworkRecord record = new SystemsTeamworkRecord();
            record.setFromEntityId(entity.getEntityId());
            record.setFromSbsCode(entity.getSbsCode());
            record.setFromSystemName(entity.getSystemName());
            record.setFromTrl(entity.getTrl());
            record.setFromSystemOwner(entity.getSystemOwner());
            record.setFromSystemDepartment(entity.getSystemDepartment());
            Element system = getDoc().createElement("system");
            addElement(system, "fromEntityId", record.getFromEntityId());
            addElement(system, "fromSbsCode", record.getFromSbsCode());
            addElement(system, "fromSystemName", record.getFromSystemName());
            addElement(system, "fromTrlId", record.getFromTrlId());
            addElement(system, "fromSystemOwnerId", record.getFromSystemOwnerId());
            addElement(system, "fromSystemDepartmentId", record.getFromSystemDepartmentId());
            systems.appendChild(system);
        }
        return systems;
    }

    private Element getInterfaceElements() {
        Element interfaceElements = getDoc().createElement("interfaces");
        if (listOfSystemsTeamworkRecords != null) {
            for (SystemsTeamworkRecord record : listOfSystemsTeamworkRecords) {
                Element interfaceElement = getDoc().createElement("interface");
                addElement(interfaceElement, "fromEntityId", record.getFromEntityId());
                addElement(interfaceElement, "fromSbsCode", record.getFromSbsCode());
                addElement(interfaceElement, "fromSystemName", record.getFromSystemName());
                addElement(interfaceElement, "fromTrlId", record.getFromTrlId());
                addElement(interfaceElement, "fromSystemOwnerId", record.getFromSystemOwnerId());
                addElement(interfaceElement, "fromSystemDepartmentId", record.getFromSystemDepartmentId());

                if (record.getFromIrlId() != null && !record.getFromIrlId().isBlank()) {
                    addElement(interfaceElement, "fromIrlId", record.getFromIrlId());
                }
                if (record.getFromClassificationIds() != null && !record.getFromClassificationIds().isEmpty()) {
                    addElement(interfaceElement, "fromClassificationIds", record.getFromClassificationIds());
                }

                addElement(interfaceElement, "toEntityId", record.getToEntityId());
                addElement(interfaceElement, "toSbsCode", record.getToSbsCode());
                addElement(interfaceElement, "toSystemName", record.getToSystemName());
                addElement(interfaceElement, "toTrlId", record.getToTrlId());
                addElement(interfaceElement, "toSystemOwnerId", record.getToSystemOwnerId());
                addElement(interfaceElement, "toSystemDepartmentId", record.getToSystemDepartmentId());

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
