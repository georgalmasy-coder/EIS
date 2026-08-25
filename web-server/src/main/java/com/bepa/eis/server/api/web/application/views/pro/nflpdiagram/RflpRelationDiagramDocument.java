package com.bepa.eis.server.api.web.application.views.pro.nflpdiagram;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.*;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.functional.FunctionalStructureEntity;
import com.bepa.eis.server.entites.logical.LogicalStructureEntity;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.List;

import static com.bepa.eis.common.enums.entity.EntityType.*;

public class RflpRelationDiagramDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(RflpRelationDiagramDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    private List<SystemRequirementEntity> listOfSystemRequirements;
    private List<StakeholderRequirementEntity> listOfStakeholderRequirements;
    private List<SystemBreakdownEntity> listOfSystemBreakdowns;
    private List<FunctionalStructureEntity> listOfFunctionalStructures;
    private List<LogicalStructureEntity> listOfLogicalStructures;

    private List<EntityRelationRecord> listOfStakeholderRequirementToSystemRequirementRelations;
    private List<EntityRelationRecord> listOfStakeholderRequirementToFunctionalStructureRelations;
    private List<EntityRelationRecord> listOfStakeholderRequirementToLogicalStructureRelations;
    private List<EntityRelationRecord> listOfStakeholderRequirementToSystemsBreakdownRelations;
    private List<EntityRelationRecord> listOfStakeholderRequirementToStakeholderRequirementRelations;

    private List<EntityRelationRecord> listOfSystemRequirementToStakeholderRelations;
    private List<EntityRelationRecord> listOfSystemRequirementToFunctionalStructureRelations;
    private List<EntityRelationRecord> listOfSystemRequirementToLogicalStructureRelations;
    private List<EntityRelationRecord> listOfSystemRequirementToSystemsBreakdownRelations;
    private List<EntityRelationRecord> listOfSystemRequirementToSystemRequirementRelations;

    private List<EntityRelationRecord> listOfFunctionalStructureToStakeholderRequirementRelations;
    private List<EntityRelationRecord> listOfFunctionalStructureToSystemRequirementRelations;
    private List<EntityRelationRecord> listOfFunctionalStructureToLogicalStructureRelations;
    private List<EntityRelationRecord> listOfFunctionalStructureToSystemsBreakdownRelations;
    private List<EntityRelationRecord> listOfFunctionalStructureToFunctionalStructureRelations;

    private List<EntityRelationRecord> listOfLogicalStructureToStakeholderRequirementRelations;
    private List<EntityRelationRecord> listOfLogicalStructureToSystemRequirementRelations;
    private List<EntityRelationRecord> listOfLogicalStructureToFunctionalStructureRelations;
    private List<EntityRelationRecord> listOfLogicalStructureToSystemsBreakdownRelations;
    private List<EntityRelationRecord> listOfLogicalStructureToLogicalStructureRelations;

    private List<EntityRelationRecord> listOfSystemBreakdownToStakeholderRequirementRelations;
    private List<EntityRelationRecord> listOfSystemBreakdownToSystemRequirementRelations;
    private List<EntityRelationRecord> listOfSystemBreakdownToLogicalStructureRelations;
    private List<EntityRelationRecord> listOfSystemBreakdownToFunctionalStructureRelations;
    private List<EntityRelationRecord> listOfSystemBreakdownToSystemBreakdownRelations;


    public RflpRelationDiagramDocument(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.RFLP_RELATION_DIAGRAM_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        relationDiagramDocument();

        Element relationDiagramElement = buildRelationDiagramElement();
        getRoot().appendChild(relationDiagramElement);

        relationDiagramElement.appendChild(getStakeholderRequirementElement());
        relationDiagramElement.appendChild(getSystemRequirementElement());
        relationDiagramElement.appendChild(getFunctionalStructureElement());
        relationDiagramElement.appendChild(getLogicalStructureElement());
        relationDiagramElement.appendChild(getSystemBreakdownElement());

        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToSystemRequirementRelations", listOfStakeholderRequirementToSystemRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToFunctionalStructureRelations", listOfStakeholderRequirementToFunctionalStructureRelations));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToLogicalStructureRelations", listOfStakeholderRequirementToLogicalStructureRelations));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToSystemsBreakdownRelations", listOfStakeholderRequirementToSystemsBreakdownRelations));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToStakeholderRequirementRelations", listOfStakeholderRequirementToStakeholderRequirementRelations));


        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToStakeholderRequirementRelations", listOfSystemRequirementToStakeholderRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToFunctionalStructureRelations", listOfSystemRequirementToFunctionalStructureRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToLogicalStructureRelations", listOfSystemRequirementToLogicalStructureRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToSystemsBreakdownRelations", listOfSystemRequirementToSystemsBreakdownRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToSystemRequirementRelations", listOfSystemRequirementToSystemRequirementRelations));

        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToStakeholderRequirementRelations", listOfFunctionalStructureToStakeholderRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToSystemRequirementRelations", listOfFunctionalStructureToSystemRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToLogicalStructureRelations", listOfFunctionalStructureToLogicalStructureRelations));
        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToSystemsBreakdownRelations", listOfFunctionalStructureToSystemsBreakdownRelations));
        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToFunctionalStructureRelations", listOfFunctionalStructureToFunctionalStructureRelations));

        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToStakeholderRequirementRelations", listOfLogicalStructureToStakeholderRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToSystemRequirementRelations", listOfLogicalStructureToSystemRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToFunctionalStructureRelations", listOfLogicalStructureToFunctionalStructureRelations));
        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToSystemsBreakdownRelations", listOfLogicalStructureToSystemsBreakdownRelations));
        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToLogicalStructureRelations", listOfLogicalStructureToLogicalStructureRelations));

        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToStakeholderRequirementRelations", listOfSystemBreakdownToStakeholderRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToSystemRequirementRelations", listOfSystemBreakdownToSystemRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToFunctionalStructureRelations", listOfSystemBreakdownToFunctionalStructureRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToLogicalStructureRelations", listOfSystemBreakdownToLogicalStructureRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToSystemsBreakdownRelations", listOfSystemBreakdownToSystemBreakdownRelations));

    }

    private void relationDiagramDocument () throws SQLException {
        listOfSystemRequirements = getSystemRequirements();
        listOfStakeholderRequirements = getStakeholderRequirements();
        listOfSystemBreakdowns = getSystemBreakdowns();
        listOfFunctionalStructures = getFunctionalStructures();
        listOfLogicalStructures = getLogicalStructures();

        listOfStakeholderRequirementToSystemRequirementRelations = getEntityRelations(EntityType.STAKEHOLDER_REQUIREMENT, EntityType.SYSTEM_REQUIREMENT);
        listOfStakeholderRequirementToSystemsBreakdownRelations = getEntityRelations(STAKEHOLDER_REQUIREMENT, SYSTEMS_BREAKDOWN);
        listOfStakeholderRequirementToFunctionalStructureRelations = getEntityRelations(STAKEHOLDER_REQUIREMENT, FUNCTIONAL_STRUCTURE);
        listOfStakeholderRequirementToLogicalStructureRelations = getEntityRelations(STAKEHOLDER_REQUIREMENT, LOGICAL_STRUCTURE);
        listOfStakeholderRequirementToStakeholderRequirementRelations = getEntityRelations(STAKEHOLDER_REQUIREMENT, STAKEHOLDER_REQUIREMENT);


        listOfSystemRequirementToStakeholderRelations = getEntityRelations(EntityType.SYSTEM_REQUIREMENT, STAKEHOLDER_REQUIREMENT);
        listOfSystemRequirementToFunctionalStructureRelations = getEntityRelations(EntityType.SYSTEM_REQUIREMENT, FUNCTIONAL_STRUCTURE);
        listOfSystemRequirementToLogicalStructureRelations = getEntityRelations(SYSTEM_REQUIREMENT, LOGICAL_STRUCTURE);
        listOfSystemRequirementToSystemsBreakdownRelations = getEntityRelations(SYSTEM_REQUIREMENT, SYSTEMS_BREAKDOWN);
        listOfSystemRequirementToSystemRequirementRelations = getEntityRelations(SYSTEM_REQUIREMENT, SYSTEM_REQUIREMENT);


        listOfFunctionalStructureToStakeholderRequirementRelations = getEntityRelations(FUNCTIONAL_STRUCTURE, STAKEHOLDER_REQUIREMENT);
        listOfFunctionalStructureToSystemRequirementRelations = getEntityRelations(FUNCTIONAL_STRUCTURE, SYSTEM_REQUIREMENT);
        listOfFunctionalStructureToLogicalStructureRelations = getEntityRelations(FUNCTIONAL_STRUCTURE, LOGICAL_STRUCTURE);
        listOfFunctionalStructureToSystemsBreakdownRelations = getEntityRelations(FUNCTIONAL_STRUCTURE, SYSTEMS_BREAKDOWN);
        listOfFunctionalStructureToFunctionalStructureRelations = getEntityRelations(FUNCTIONAL_STRUCTURE, FUNCTIONAL_STRUCTURE);


        listOfLogicalStructureToStakeholderRequirementRelations = getEntityRelations(LOGICAL_STRUCTURE, STAKEHOLDER_REQUIREMENT);
        listOfLogicalStructureToSystemRequirementRelations = getEntityRelations(LOGICAL_STRUCTURE, SYSTEM_REQUIREMENT);
        listOfLogicalStructureToFunctionalStructureRelations = getEntityRelations(LOGICAL_STRUCTURE, FUNCTIONAL_STRUCTURE);;
        listOfLogicalStructureToSystemsBreakdownRelations = getEntityRelations(LOGICAL_STRUCTURE, SYSTEMS_BREAKDOWN);
        listOfLogicalStructureToLogicalStructureRelations = getEntityRelations(LOGICAL_STRUCTURE, LOGICAL_STRUCTURE);


        listOfSystemBreakdownToStakeholderRequirementRelations = getEntityRelations(SYSTEMS_BREAKDOWN, STAKEHOLDER_REQUIREMENT);
        listOfSystemBreakdownToSystemRequirementRelations = getEntityRelations(SYSTEMS_BREAKDOWN, SYSTEM_REQUIREMENT);
        listOfSystemBreakdownToFunctionalStructureRelations = getEntityRelations(SYSTEMS_BREAKDOWN, FUNCTIONAL_STRUCTURE);
        listOfSystemBreakdownToLogicalStructureRelations = getEntityRelations(SYSTEMS_BREAKDOWN, LOGICAL_STRUCTURE);
        listOfSystemBreakdownToSystemBreakdownRelations = getEntityRelations(SYSTEMS_BREAKDOWN, SYSTEMS_BREAKDOWN);
    }

    private List<StakeholderRequirementEntity> getStakeholderRequirements() {
        StakeholderRequirementProvider stakeholderRequirementProvider = new StakeholderRequirementProvider(getWebSession());
        List<StakeholderRequirementEntity> listOfStakeholderRequirementEntities;
        listOfStakeholderRequirementEntities = stakeholderRequirementProvider.getAllStakeholderRequirement(false);
        return listOfStakeholderRequirementEntities;
    }

    private List<SystemRequirementEntity> getSystemRequirements() {
        SystemRequirementProvider systemRequirementProvider = new SystemRequirementProvider(getWebSession());
        List<SystemRequirementEntity> listOfSystemRequirementEntities;
        listOfSystemRequirementEntities = systemRequirementProvider.getAllSystemRequirement(false);
        return listOfSystemRequirementEntities;
    }

    private List<FunctionalStructureEntity> getFunctionalStructures() {
        FunctionalStructureProvider functionalStructureProvider = new FunctionalStructureProvider(getWebSession());
        List<FunctionalStructureEntity> listOfFunctionalStructureEntities;
        listOfFunctionalStructureEntities = functionalStructureProvider.getAllFunctionalStructure(false);
        return listOfFunctionalStructureEntities;
    }

    private List<LogicalStructureEntity> getLogicalStructures() {
        LogicalStructureProvider logicalStructureProvider = new LogicalStructureProvider(getWebSession());
        List<LogicalStructureEntity> listOfFunctionalStructureEntities;
        listOfFunctionalStructureEntities = logicalStructureProvider.getAllLogicalStructures(false);
        return listOfFunctionalStructureEntities;
    }

    private List<SystemBreakdownEntity> getSystemBreakdowns() {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
        List<SystemBreakdownEntity> listOfSystemBreakdownEntities;
        listOfSystemBreakdownEntities = systemBreakdownProvider.getAllSystemBreakdown(false);
        return listOfSystemBreakdownEntities;
    }


    private List<EntityRelationRecord> getEntityRelations(EntityType entityType, EntityType relatedEentityType) throws SQLException {
        EntityRelationProvider entityRelationProvider = new EntityRelationProvider(getWebSession());
        return entityRelationProvider.getAllConfirmedEntityRelationRecordsByProjectId(entityType, relatedEentityType);
    }

    private Element getStakeholderRequirementElement() {
        Element stakeholderRequirements = getDoc().createElement("stakeholderRequirements");
        if (listOfStakeholderRequirements != null) {
            for (StakeholderRequirementEntity requirement : listOfStakeholderRequirements) {
                Element requirementElement = getDoc().createElement("requirement");
                String id = formatEntityId(requirement.getEntityType(), requirement.getEntityId().getValue());
                requirementElement.setAttribute("id", id);
                addEntityTypeAttribute(requirementElement, STAKEHOLDER_REQUIREMENT);
                addEntityIdAttribute(requirementElement, requirement.getEntityId());
                addElement(requirementElement, "code", requirement.getRequirementCode().getValue());
                addElement(requirementElement, "name", requirement.getRequirementName().getValue());
                addElement(requirementElement, "description", requirement.getRequirementDescription().getValue());
                stakeholderRequirements.appendChild(requirementElement);
            }
        }
        return stakeholderRequirements;
    }

    private Element getSystemRequirementElement() {
        Element systemRequirements = getDoc().createElement("systemRequirements");
        if (listOfSystemRequirements != null) {
            for (SystemRequirementEntity requirement : listOfSystemRequirements) {
                Element requirementElement = getDoc().createElement("requirement");
                String id = formatEntityId(requirement.getEntityType(), requirement.getEntityId().getValue());
                requirementElement.setAttribute("id", id);
                addEntityTypeAttribute(requirementElement, SYSTEM_REQUIREMENT);
                addEntityIdAttribute(requirementElement, requirement.getEntityId());
                addElement(requirementElement, "code", requirement.getRequirementCode().getValue());
                addElement(requirementElement, "name", requirement.getRequirementName().getValue());
                addElement(requirementElement, "description", requirement.getRequirementDescription().getValue());
                systemRequirements.appendChild(requirementElement);
            }
        }
        return systemRequirements;
    }

    private Element getFunctionalStructureElement() {
        Element functionalStructures = getDoc().createElement("functionalStructures");
        if (listOfFunctionalStructures != null) {
            for (FunctionalStructureEntity functionalStructure : listOfFunctionalStructures) {
                Element functionalElement = getDoc().createElement("functional");
                String id = formatEntityId(functionalStructure.getEntityType(), functionalStructure.getEntityId().getValue());
                functionalElement.setAttribute("id", id);
                addEntityTypeAttribute(functionalElement, FUNCTIONAL_STRUCTURE);
                addEntityIdAttribute(functionalElement, functionalStructure.getEntityId());
                addElement(functionalElement, "code", functionalStructure.getFunctionalCode().getValue());
                addElement(functionalElement, "name", functionalStructure.getFunctionalName().getValue());
                addElement(functionalElement, "description", functionalStructure.getFunctionalDescription().getValue());
                functionalStructures.appendChild(functionalElement);
            }
        }
        return functionalStructures;
    }

    private Element getLogicalStructureElement() {
        Element logicalStructures = getDoc().createElement("logicalStructures");
        if (listOfLogicalStructures != null) {
            for (LogicalStructureEntity logicalStructure : listOfLogicalStructures) {
                Element logicalElement = getDoc().createElement("logical");
                String id = formatEntityId(logicalStructure.getEntityType(), logicalStructure.getEntityId().getValue());
                logicalElement.setAttribute("id", id);
                addEntityTypeAttribute(logicalElement, LOGICAL_STRUCTURE);
                addEntityIdAttribute(logicalElement, logicalStructure.getEntityId());
                addElement(logicalElement, "code", logicalStructure.getLogicalCode().getValue());
                addElement(logicalElement, "name", logicalStructure.getLogicalName().getValue());
                addElement(logicalElement, "description", logicalStructure.getLogicalDescription().getValue());
                logicalStructures.appendChild(logicalElement);
            }
        }
        return logicalStructures;
    }


    private Element getSystemBreakdownElement() {
        Element systemRequirements = getDoc().createElement("systemsBreakdowns");
        if (listOfSystemBreakdowns != null) {
            for (SystemBreakdownEntity system : listOfSystemBreakdowns) {
                Element physicalElement = getDoc().createElement("systemsBreakdown");
                String id = formatEntityId(system.getEntityType(), system.getEntityId().getValue());
                physicalElement.setAttribute("id", id);
                addEntityTypeAttribute(physicalElement, SYSTEMS_BREAKDOWN);
                addEntityIdAttribute(physicalElement, system.getEntityId());
                addElement(physicalElement, "code", system.getSbsCode().getValue());
                addElement(physicalElement, "name", system.getSystemName().getValue());
                addElement(physicalElement, "description", system.getDescription());
                systemRequirements.appendChild(physicalElement);
            }
        }
        return systemRequirements;
    }


    private Element getRelationsElement(String elementName, List<EntityRelationRecord> listOfEntityRelationRecords) {
        Element relationsElement = getDoc().createElement(elementName);
        for (EntityRelationRecord relation : listOfEntityRelationRecords) {
            Element relationElement = getDoc().createElement("relation");

            String fromId = formatEntityId(relation.getEntityType(), relation.getEntityId());
            addElement(relationElement, "from",fromId);
            String toId = formatEntityId(relation.getRelatedEntityType(), relation.getRelatedEntityId());
            addElement(relationElement, "to", toId);
            addElement(relationElement, "type", relation.getRelationType().getDescription());

            relationsElement.appendChild(relationElement);
        }

        return relationsElement;
    }


    private void addElement(Element parentelement, String elementName, String value) {
        Element element = getDoc().createElement(elementName);
        element.setTextContent(value);
        parentelement.appendChild(element);
    }

    private Element buildRelationDiagramElement() {
        return getDoc().createElement("NflpRelationDiagram");
    }

    private String formatEntityId(EntityType entityType, Integer entityId) {
        return entityType.getShortDescription() + "-" + entityId;
    }

    private void addEntityTypeAttribute(Element element, EntityType entityType) {
        if (element != null && entityType != null) {
            Integer type = entityType.getId();
            element.setAttribute("entityType",type.toString());
        }
    }

    private void addEntityIdAttribute(Element element, EntityId entityId) {
        if (element != null && entityId != null) {
            element.setAttribute("entityId",entityId.toString());
        }
    }

}
