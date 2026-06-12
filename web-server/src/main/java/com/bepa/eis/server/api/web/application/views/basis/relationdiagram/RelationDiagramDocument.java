package com.bepa.eis.server.api.web.application.views.basis.relationdiagram;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.common.EntityRelationRecord;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systemsystemrequirement.SystemRequirementEntity;
import com.bepa.eis.common.enums.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.List;

public class RelationDiagramDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(RelationDiagramDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    private List<SystemRequirementEntity> listOfSystemRequirements;
    private List<StakeholderRequirementEntity> listOfStakeholderRequirements;
    private List<EntityRelationRecord> listOfEntityRelations;

    public RelationDiagramDocument(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());

        relationDiagramDocument();

        Element relationDiagramElement = buildRelationDiagramElement();
        getRoot().appendChild(relationDiagramElement);

        relationDiagramElement.appendChild(getStakeholderRequirementElement());
        relationDiagramElement.appendChild(getSystemRequirementElement());
        relationDiagramElement.appendChild(getRelationsElement());

    }

    private void relationDiagramDocument () throws SQLException {
        listOfSystemRequirements = getSystemRequirements();
        listOfStakeholderRequirements = getStakeholderRequirements();
        listOfEntityRelations = getEntityRelations();
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

    private List<EntityRelationRecord> getEntityRelations() throws SQLException {
        EntityRelationProvider entityRelationProvider = new EntityRelationProvider(getWebSession());
        return entityRelationProvider.getAllActiveEntityRelationRecordsByProjectId(EntityType.STAKEHOLDER_REQUIREMENT, EntityType.SYSTEM_REQUIREMENT);
    }

    private Element getStakeholderRequirementElement() {
        Element stakeholderRequirements = getDoc().createElement("stakeholderRequirements");
        if (listOfStakeholderRequirements != null) {
            for (StakeholderRequirementEntity requirement : listOfStakeholderRequirements) {
                Element requirementElement = getDoc().createElement("requirement");
                String id = formatEntityId(requirement.getEntityType(), requirement.getEntityId());
                requirementElement.setAttribute("id", id);
                addElement(requirementElement, "id", requirement.getRequirementCode());
                addElement(requirementElement, "name", requirement.getRequirementName());
                addElement(requirementElement, "description", requirement.getRequirementDescription());
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
                String id = formatEntityId(requirement.getEntityType(), requirement.getEntityId());
                requirementElement.setAttribute("id", id);
                addElement(requirementElement, "id", requirement.getRequirementCode());
                addElement(requirementElement, "name", requirement.getRequirementName());
                addElement(requirementElement, "description", requirement.getRequirementDescription());
                systemRequirements.appendChild(requirementElement);
            }
        }
        return systemRequirements;
    }

    private Element getRelationsElement() {
        Element relationsElement = getDoc().createElement("relations");
        for (EntityRelationRecord relation : listOfEntityRelations) {
            Element relationElement = getDoc().createElement("relation");
            String fromId = formatEntityId(relation.getEntityType(), relation.getEntityId());
            addElement(relationElement, "from",fromId);
            String toId = formatEntityId(relation.getRelatedEntityType(), relation.getRelatedEntityId());
            addElement(relationElement, "to", toId);
            relationsElement.appendChild(relationElement);
        }

        return relationsElement;
    }


    private void addElement(Element parentelement, String elementName, String value) {
        Element element = getDoc().createElement(elementName);
        element.setTextContent(value);
        parentelement.appendChild(element);
    }

    private void addAttribute(Element element, String attributeName, String attributeValue) {
        if (element != null && attributeName != null && attributeValue != null) {
            element.setAttribute(attributeName, attributeValue);
        }
    }

    private Element buildRelationDiagramElement() {
        return getDoc().createElement("relationDiagram");
    }

    private String formatEntityId(EntityType entityType, Integer entityId) {
        return entityType.getShortDescription() + "-" + entityId;
    }

}
