package com.bepa.eis.server.api.web.application.views.pro.dashboard.irlmonitor;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.api.web.application.views.pro.dashboard.common.DashboardMetaData;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import com.bepa.eis.server.dataprovider.entities.*;
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

public class DashboardIrlDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(DashboardIrlDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    private final List<PhysicalStructureWrapper> listOfPhysicalStructures = new ArrayList<>();
    private final Map<Integer, PhysicalStructureWrapper> mapOfPhysicalStructures = new HashMap<>();
    private DashboardMetaData dashboardMetaData;

    private final Timestamp now = Timestamp.from(Instant.now());
    private LocalDate today = now.toLocalDateTime().toLocalDate();

    public DashboardIrlDocument(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.DASHBOARD_IRLS_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        dashboardDocument();

        Element dashboardElement = buildDashboardIrlElement();
        getRoot().appendChild(dashboardElement);

        dashboardElement.appendChild(getTrlMetaDataElement());
        dashboardElement.appendChild(getIrlMetaDataElement());
        dashboardElement.appendChild(getPhysicalStructuresElement());
    }

    private void dashboardDocument() throws SQLException {
        buildPhysicalStructures();
        calculateIrlCounts();
        dashboardMetaData = new DashboardMetaData(getWebSession());
    }

    private void buildPhysicalStructures() throws SQLException {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
        List<SystemBreakdownEntity> listOfSystemBreakdownEntities;
        listOfSystemBreakdownEntities = systemBreakdownProvider.getAllSystemBreakdown(false);

        for (SystemBreakdownEntity systemBreakdownEntity : listOfSystemBreakdownEntities) {
            PhysicalStructureWrapper physicalStructureWrapper = new PhysicalStructureWrapper(systemBreakdownEntity);
            listOfPhysicalStructures.add(physicalStructureWrapper);
            mapOfPhysicalStructures.put(systemBreakdownEntity.getEntityId().getValue(), physicalStructureWrapper);
        }
    }

    private void calculateIrlCounts() throws SQLException  {
        InterfaceMatrixProvider interfaceManagementProvider = new InterfaceMatrixProvider(
                getWebSession(),
                EntityType.SYSTEMS_BREAKDOWN
        );
        List<InterfaceMatrixProvider.InterfaceRecord> interfaces = interfaceManagementProvider.getAllInterfaceRecords();

        for (InterfaceMatrixProvider.InterfaceRecord interfaceRecord : interfaces) {
            PhysicalStructureWrapper fromSys = mapOfPhysicalStructures.get(interfaceRecord.fromEntityId());
            fromSys.countIrl(interfaceRecord.irlId());

            PhysicalStructureWrapper toSys = mapOfPhysicalStructures.get(interfaceRecord.toEntityId());
            toSys.countIrl(interfaceRecord.irlId());

        }
    }

    private Element getTrlMetaDataElement() {
        return dashboardMetaData.getTrlElement(getDoc());
    }

    private Element getIrlMetaDataElement() {
        return dashboardMetaData.getIrlElement(getDoc());
    }

    private Element getPhysicalStructuresElement() {
        Element systemRequirements = getDoc().createElement("physicalStructures");
        if (listOfPhysicalStructures != null) {
            for (PhysicalStructureWrapper system : listOfPhysicalStructures) {
                Element physicalElement = getDoc().createElement("physicalStructure");
                addElement(physicalElement, "entityId", system.getEntityId());
                addElement(physicalElement, "id", system.getSbsCode());
                addElement(physicalElement, "name", system.getSystemName());
                addElement(physicalElement, "description", system.getDescription());
                addElement(physicalElement, "trlId", getTrlValue(system.getTrl()));
                addElement(physicalElement, "daysNextTrl", getDaysUntil(system.getDeadlineNextTrl()));

                addIrlCounts(physicalElement, system);

                systemRequirements.appendChild(physicalElement);
            }
        }
        return systemRequirements;
    }

    private Element addIrlCounts(Element physicalElement, PhysicalStructureWrapper system) {
        Element irlCountsElement = getDoc().createElement("irlCounts");
        for (Integer irlId : system.getIrlCounts().keyset()) {
            Element irlCountElement = getDoc().createElement("irlCount");
            addElement(irlCountElement, "irlId", irlId.toString());
            addElement(irlCountElement, "count", system.getIrlCounts().get(irlId).toString());
            irlCountsElement.appendChild(irlCountElement);
        }
        physicalElement.appendChild(irlCountsElement);
        return physicalElement;
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

    private static class PhysicalStructureWrapper {
        SystemBreakdownEntity physicalStructureEntity;
        IrlCounts irlCounts = new IrlCounts();

        PhysicalStructureWrapper(SystemBreakdownEntity systemBreakdownEntity) {
            this.physicalStructureEntity = systemBreakdownEntity;
        }

        String getEntityId() {
            if (physicalStructureEntity.getEntityId() == null ||
                physicalStructureEntity.getEntityId().getValue() == null) {
                return "";
            }
            return physicalStructureEntity.getEntityId().getValue().toString();
        }

        String getSbsCode() {
            if (physicalStructureEntity.getSbsCode() == null ||
                physicalStructureEntity.getSbsCode().getValue() == null) {
                return "";
            }
            return physicalStructureEntity.getSbsCode().getValue();
        }

        String getSystemName() {
            if (physicalStructureEntity.getSystemName() == null ||
                physicalStructureEntity.getSystemName().getValue() == null) {
                return "";
            }
            return physicalStructureEntity.getSystemName().getValue();
        }

        String getDescription() {
            if (physicalStructureEntity.getDescription() == null) {
                return "";
            }
            return physicalStructureEntity.getDescription();
        }

        TRL getTrl() {
            if (physicalStructureEntity.getTrl() == null) {
                return null;
            }
            return physicalStructureEntity.getTrl();
        }

        LocalDate getDeadlineNextTrl() {
            if (physicalStructureEntity.getDeadlineNextTrl() == null) {
                return null;
            }
            return physicalStructureEntity.getDeadlineNextTrl();
        }

        void countIrl(Integer irlId) {
            irlCounts.increment(irlId);
        }

        IrlCounts getIrlCounts() {
            return irlCounts;
        }

    }

    private static class IrlCounts {

        Map<Integer, Integer> irlCounts = new HashMap<>();

        IrlCounts() {}

        void increment(Integer irlId) {
            Integer count = irlCounts.get(irlId);
            count = count == null ? 1 : count + 1;
            irlCounts.put(irlId, count);
        }

        Set<Integer> keyset() {
            return irlCounts.keySet();
        }

        Integer get(Integer irlId) {
            return irlCounts.get(irlId);
        }
    }

}
