package com.bepa.eis.server.api.web.application.views.basis.relationdiagram;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;
import com.bepa.eis.common.enums.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.List;

import static com.bepa.eis.common.enums.entity.EntityType.*;

public class RelationDiagramDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(RelationDiagramDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    private List<SystemRequirementEntity> listOfSystemRequirements;
    private List<StakeholderRequirementEntity> listOfStakeholderRequirements;
    private List<SystemBreakdownEntity> listOfSystemBreakdowns;
    private List<EntityRelationRecord> listOfStakeholderRequirementToSystemRequirementRelations;
    private List<EntityRelationRecord> listOfSystemRequirementToSystemBreakdownRelations;
    private List<EntityRelationRecord> listOfStakeholderRequirementToSystemBreakdownRelations;

    private List<EntityRelationRecord> listOfStakeholderRequirementToStakeholderRequirementRelations;
    private List<EntityRelationRecord> listOfSystemRequirementToSystemRequirementRelations;
    private List<EntityRelationRecord> listOfSystemBreakdownToSystemBreakdownRelations;

    public RelationDiagramDocument(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.RELATION_DIAGRAM_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        relationDiagramDocument();

        Element relationDiagramElement = buildRelationDiagramElement();
        getRoot().appendChild(relationDiagramElement);

        relationDiagramElement.appendChild(getStakeholderRequirementElement());
        relationDiagramElement.appendChild(getSystemRequirementElement());
        relationDiagramElement.appendChild(getSystemBreakdownElement());
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToSystemRequirementRelations", listOfStakeholderRequirementToSystemRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToSystemsBreakdownRelations", listOfSystemRequirementToSystemBreakdownRelations));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToSystemsBreakdownRelations", listOfStakeholderRequirementToSystemBreakdownRelations));

        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToStakeholderRequirementRelations", listOfStakeholderRequirementToStakeholderRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToSystemRequirementRelations", listOfSystemRequirementToSystemRequirementRelations));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToSystemsBreakdownRelations", listOfSystemBreakdownToSystemBreakdownRelations));
    }

    private void relationDiagramDocument () throws SQLException {
        listOfSystemRequirements = getSystemRequirements();
        listOfStakeholderRequirements = getStakeholderRequirements();
        listOfSystemBreakdowns = getSystemBreakdowns();
        listOfStakeholderRequirementToSystemRequirementRelations = getEntityRelations(EntityType.STAKEHOLDER_REQUIREMENT, SYSTEM_REQUIREMENT);
        listOfSystemRequirementToSystemBreakdownRelations = getEntityRelations(SYSTEM_REQUIREMENT, SYSTEMS_BREAKDOWN);
        listOfStakeholderRequirementToSystemBreakdownRelations = getEntityRelations(STAKEHOLDER_REQUIREMENT, SYSTEMS_BREAKDOWN);

        listOfStakeholderRequirementToStakeholderRequirementRelations = getEntityRelations(STAKEHOLDER_REQUIREMENT, STAKEHOLDER_REQUIREMENT);
        listOfSystemRequirementToSystemRequirementRelations = getEntityRelations(SYSTEM_REQUIREMENT, SYSTEM_REQUIREMENT);
        listOfSystemBreakdownToSystemBreakdownRelations = getEntityRelations(SYSTEMS_BREAKDOWN, SYSTEMS_BREAKDOWN);

    }

    private List<StakeholderRequirementEntity> getStakeholderRequirements() throws SQLException {
        StakeholderRequirementProvider stakeholderRequirementProvider = new StakeholderRequirementProvider(getWebSession());
        List<StakeholderRequirementEntity> listOfStakeholderRequirementEntities;
        listOfStakeholderRequirementEntities = stakeholderRequirementProvider.getAllStakeholderRequirement(false);
        return listOfStakeholderRequirementEntities;
    }

    private List<SystemRequirementEntity> getSystemRequirements() throws SQLException {
        SystemRequirementProvider systemRequirementProvider = new SystemRequirementProvider(getWebSession());
        List<SystemRequirementEntity> listOfSystemRequirementEntities;
        listOfSystemRequirementEntities = systemRequirementProvider.getAllSystemRequirement(false);
        return listOfSystemRequirementEntities;
    }

    private List<SystemBreakdownEntity> getSystemBreakdowns() throws SQLException {
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

    private Element getSystemBreakdownElement() {
        Element systemRequirements = getDoc().createElement("systemsBreakdowns");
        if (listOfSystemBreakdowns != null) {
            for (SystemBreakdownEntity system : listOfSystemBreakdowns) {
                Element requirementElement = getDoc().createElement("systemsBreakdown");
                String id = formatEntityId(system.getEntityType(), system.getEntityId().getValue());
                requirementElement.setAttribute("id", id);
                addEntityTypeAttribute(requirementElement, SYSTEMS_BREAKDOWN);
                addEntityIdAttribute(requirementElement, system.getEntityId());
                addElement(requirementElement, "code", system.getSbsCode().getValue());
                addElement(requirementElement, "name", system.getSystemName().getValue());
                addElement(requirementElement, "description", system.getDescription());
                systemRequirements.appendChild(requirementElement);
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
        return getDoc().createElement("relationDiagram");
    }

    private String formatEntityId(EntityType entityType, Integer entityId) {
        return entityType.getShortDescription() + "-" + entityId;
    }

    private void addEntityTypeAttribute(Element element, EntityType entityType) {
        if (element != null) {
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
