package com.bepa.eis.server.api.web.application.views.pro.dashboard.systemsteamwork;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.api.web.application.views.pro.dashboard.common.DashboardMetaData;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.fields.lookups.system.TRL;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DashboardSystemsTeamworkDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(DashboardSystemsTeamworkDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    private final Map<Integer, SystemBreakdownEntity> mapOfPhysicalStructures = new HashMap<>();
    private List<InterfaceMatrixProvider.InterfaceRecord> interfaces = new ArrayList<>();
    private final List<SystemsTeamworkRecord> listOfSystemsTeamworkRecords = new ArrayList<>();

    private DashboardMetaData dashboardMetaData;

    private final Timestamp now = Timestamp.from(Instant.now());
    private LocalDate today = now.toLocalDateTime().toLocalDate();

    public DashboardSystemsTeamworkDocument(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
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
    }

    private void dashboardDocument() throws SQLException {
        loadPhysicalStructures();
        buildSystemsTeamwork();
        dashboardMetaData = new DashboardMetaData(getWebSession());
    }

    private void loadPhysicalStructures() throws SQLException {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
        List<SystemBreakdownEntity> listOfPhysicalStructures = systemBreakdownProvider.getAllSystemBreakdown(false);

        for (SystemBreakdownEntity systemBreakdownEntity : listOfPhysicalStructures) {
            mapOfPhysicalStructures.put(systemBreakdownEntity.getEntityId().getValue(), systemBreakdownEntity);
        }
    }

    private void buildSystemsTeamwork() throws SQLException  {
        InterfaceMatrixProvider interfaceManagementProvider = new InterfaceMatrixProvider(getWebSession());
        interfaces = interfaceManagementProvider.getAllInterfaceRecords();

        if (interfaces != null) {
            for (InterfaceMatrixProvider.InterfaceRecord interfaceRecord : interfaces) {

                SystemsTeamworkRecord record = new SystemsTeamworkRecord();

                SystemBreakdownEntity fromEntity = mapOfPhysicalStructures.get(interfaceRecord.fromEntityId());
                SystemBreakdownEntity toEntity = mapOfPhysicalStructures.get(interfaceRecord.toEntityId());

                record.setFromEntityId(fromEntity.getEntityId());
                record.setFromSbsCode(fromEntity.getSbsCode());
                record.setFromSystemName(fromEntity.getSystemName());
                record.setFromTrl(fromEntity.getTrl());
                record.setFromSystemOwner(fromEntity.getSystemOwner());
                record.setFromSystemDepartment(fromEntity.getSystemDepartment());
                record.setFromIrlId(interfaceRecord.irlId());
                record.setFromClassificationIds(interfaceRecord.classificationIds());

                record.setToEntityId(fromEntity.getEntityId());
                record.setToSbsCode(fromEntity.getSbsCode());
                record.setToSystemName(fromEntity.getSystemName());

                record.setToEntityId(toEntity.getEntityId());
                record.setToSbsCode(toEntity.getSbsCode());
                record.setToSystemName(toEntity.getSystemName());
                record.setToTrlId(toEntity.getTrl());
                record.setToSystemOwner(toEntity.getSystemOwner());
                record.setToSystemDepartment(toEntity.getSystemDepartment());

                addInterfaceToList(record);
//                record.setToIrlId(interfaceRecord.irlId());
//                record.setToClassificationIds(interfaceRecord.classificationIds());
//                listOfSystemsTeamworkRecords.add(record);
            }
        }
    }

    private void addInterfaceToList(SystemsTeamworkRecord newRecord) {
        for ( SystemsTeamworkRecord recordFromList  : listOfSystemsTeamworkRecords) {

            if (recordFromList.getFromEntityId().equals(newRecord.getToEntityId()) &&
                recordFromList.getToEntityId().equals(newRecord.getFromEntityId())) {

                // We found an instance of this interface, so we don't need to add it again.
                // but we need to update the IRL and Classification Ids.
                recordFromList.setToIrlId(newRecord.getFromIrlId());
                recordFromList.setToClassificationIds(newRecord.getFromClassificationIds());

                return;
            }
        }
        listOfSystemsTeamworkRecords.add(newRecord);
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

                interfaceElements.appendChild(interfaceElement);
            }
        }
        return interfaceElements;
    }

    private String getTrlValue(TRL trl) {
        if (trl != null && trl.getValue() != null) {
            return trl.getValue().toString();
        }
        return "";
    }

    private String getDaysUntil(LocalDate deadlineNextTRL) {
        Long days = daysUntil(deadlineNextTRL);
        return days != null && days > 0 ? daysUntil(deadlineNextTRL) + " days" : "Overdue";
    }

    private Long daysUntil(LocalDate nextTrlDeadline) {
        return nextTrlDeadline == null ? 0L : ChronoUnit.DAYS.between(today,nextTrlDeadline);
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
