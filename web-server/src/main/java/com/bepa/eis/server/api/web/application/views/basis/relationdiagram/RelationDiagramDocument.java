package com.bepa.eis.server.api.web.application.views.basis.relationdiagram;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider.DiagramRelation;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import static com.bepa.eis.common.enums.entity.EntityType.*;

public class RelationDiagramDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(RelationDiagramDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;
    private final boolean compact;
    private RelationDiagramSnapshot snapshot;

    private List<SystemRequirementEntity> listOfSystemRequirements;
    private List<StakeholderRequirementEntity> listOfStakeholderRequirements;
    private List<SystemBreakdownEntity> listOfSystemBreakdowns;

    private List<DiagramRelation> relations;
    private static final Set<EntityType> DIAGRAM_TYPES = Set.of(
            STAKEHOLDER_REQUIREMENT, SYSTEM_REQUIREMENT, SYSTEMS_BREAKDOWN);
    // Legacy basis XML has one group per unordered type pair; retain the stored endpoint direction.
    private record RelationGroup(int first, int second) {
        static RelationGroup of(int from, int to) { return new RelationGroup(Math.min(from, to), Math.max(from, to)); }
    }
    private final Map<RelationGroup, List<DiagramRelation>> relationGroups = new HashMap<>();

    public RelationDiagramDocument(WebSession webSession) throws Exception {
        this(webSession, false);
    }

    RelationDiagramDocument(WebSession webSession, boolean compact) throws Exception {

        super(webSession);
        this.compact = compact;

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.RELATION_DIAGRAM_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        relationDiagramDocument();
        if (compact) snapshot = buildSnapshot();

        Element relationDiagramElement = buildRelationDiagramElement();
        getRoot().appendChild(relationDiagramElement);

        relationDiagramElement.appendChild(getStakeholderRequirementElement());
        relationDiagramElement.appendChild(getSystemRequirementElement());
        relationDiagramElement.appendChild(getSystemBreakdownElement());
        if (compact) return;
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToSystemRequirementRelations", STAKEHOLDER_REQUIREMENT, SYSTEM_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToSystemsBreakdownRelations", SYSTEM_REQUIREMENT, SYSTEMS_BREAKDOWN));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToSystemsBreakdownRelations", STAKEHOLDER_REQUIREMENT, SYSTEMS_BREAKDOWN));

        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToStakeholderRequirementRelations", STAKEHOLDER_REQUIREMENT, STAKEHOLDER_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToSystemRequirementRelations", SYSTEM_REQUIREMENT, SYSTEM_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToSystemsBreakdownRelations", SYSTEMS_BREAKDOWN, SYSTEMS_BREAKDOWN));
    }

    RelationDiagramSnapshot snapshot() { return snapshot; }

    private RelationDiagramSnapshot buildSnapshot() {
        var nodes = new java.util.ArrayList<RelationDiagramSnapshot.Node>();
        for (var entity : listOfStakeholderRequirements) nodes.add(new RelationDiagramSnapshot.Node(
                formatEntityId(STAKEHOLDER_REQUIREMENT, entity.getEntityId().getValue()), 5, entity.getEntityId().getValue(),
                entity.getRequirementCode().getValue(), entity.getRequirementName().getValue(), entity.getRequirementDescription().getValue()));
        for (var entity : listOfSystemRequirements) nodes.add(new RelationDiagramSnapshot.Node(
                formatEntityId(SYSTEM_REQUIREMENT, entity.getEntityId().getValue()), 6, entity.getEntityId().getValue(),
                entity.getRequirementCode().getValue(), entity.getRequirementName().getValue(), entity.getRequirementDescription().getValue()));
        for (var entity : listOfSystemBreakdowns) nodes.add(new RelationDiagramSnapshot.Node(
                formatEntityId(SYSTEMS_BREAKDOWN, entity.getEntityId().getValue()), 2, entity.getEntityId().getValue(),
                entity.getSbsCode().getValue(), entity.getSystemName().getValue(), entity.getDescription()));
        var edges = new java.util.ArrayList<RelationDiagramSnapshot.Edge>();
        for (var relation : relations) edges.add(new RelationDiagramSnapshot.Edge("relation-" + relation.relationId(),
                formatEntityId(EntityType.fromId(relation.entityType()), relation.entityId()),
                formatEntityId(EntityType.fromId(relation.relatedEntityType()), relation.relatedEntityId()), RelationType.CONFIRMED.getDescription()));
        return new RelationDiagramSnapshot(nodes, edges);
    }

    private void relationDiagramDocument () throws SQLException {
        long start = System.nanoTime();
        listOfSystemRequirements = getSystemRequirements();
        listOfStakeholderRequirements = getStakeholderRequirements();
        listOfSystemBreakdowns = getSystemBreakdowns();

        long relationStart = System.nanoTime();
        relations = new EntityRelationProvider(getWebSession()).getConfirmedDiagramRelations(DIAGRAM_TYPES);
        log.info("Load Entity Relations: {} records in {} ms (total {} ms)", relations.size(),
                (System.nanoTime() - relationStart) / 1_000_000, (System.nanoTime() - start) / 1_000_000);
        if (!compact) for (var relation : relations) {
            relationGroups.computeIfAbsent(RelationGroup.of(relation.entityType(), relation.relatedEntityType()),
                    ignored -> new ArrayList<>()).add(relation);
        }
    }

    private List<StakeholderRequirementEntity> getStakeholderRequirements() throws SQLException {
        StakeholderRequirementProvider stakeholderRequirementProvider = new StakeholderRequirementProvider(getWebSession());
        List<StakeholderRequirementEntity> listOfStakeholderRequirementEntities;
        listOfStakeholderRequirementEntities = stakeholderRequirementProvider.getAllStakeholderRequirement(false);
        sortBySortKey(listOfStakeholderRequirementEntities);
        return listOfStakeholderRequirementEntities;
    }

    private List<SystemRequirementEntity> getSystemRequirements() throws SQLException {
        SystemRequirementProvider systemRequirementProvider = new SystemRequirementProvider(getWebSession());
        List<SystemRequirementEntity> listOfSystemRequirementEntities;
        listOfSystemRequirementEntities = systemRequirementProvider.getAllSystemRequirement(false);
        sortBySortKey(listOfSystemRequirementEntities);
        return listOfSystemRequirementEntities;
    }

    private List<SystemBreakdownEntity> getSystemBreakdowns() throws SQLException {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
        List<SystemBreakdownEntity> listOfSystemBreakdownEntities;
        listOfSystemBreakdownEntities = systemBreakdownProvider.getAllSystemBreakdown(false);
        sortBySortKey(listOfSystemBreakdownEntities);
        return listOfSystemBreakdownEntities;
    }

    private <T extends AbstractEntity> void sortBySortKey(List<T> entities) {
        entities.sort(
                Comparator.comparing(
                        AbstractEntity::getSortKey,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                )
        );
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
                if (!compact) addElement(requirementElement, "description", requirement.getRequirementDescription().getValue());
                if (compact) {
                    requirementElement.setAttribute("hasRelations", Boolean.toString(snapshot.hasRelations(id)));
                    requirementElement.setAttribute("relationCount", Integer.toString(snapshot.relationCount(id)));
                }
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
                if (!compact) addElement(requirementElement, "description", requirement.getRequirementDescription().getValue());
                if (compact) {
                    requirementElement.setAttribute("hasRelations", Boolean.toString(snapshot.hasRelations(id)));
                    requirementElement.setAttribute("relationCount", Integer.toString(snapshot.relationCount(id)));
                }
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
                if (!compact) addElement(requirementElement, "description", system.getDescription());
                if (compact) {
                    requirementElement.setAttribute("hasRelations", Boolean.toString(snapshot.hasRelations(id)));
                    requirementElement.setAttribute("relationCount", Integer.toString(snapshot.relationCount(id)));
                }
                systemRequirements.appendChild(requirementElement);
            }
        }
        return systemRequirements;
    }

    private Element getRelationsElement(String elementName, EntityType fromType, EntityType toType) {
        Element relationsElement = getDoc().createElement(elementName);
        for (var relation : relationGroups.getOrDefault(RelationGroup.of(fromType.getId(), toType.getId()), List.of())) {
            Element relationElement = getDoc().createElement("relation");

            String fromId = formatEntityId(EntityType.fromId(relation.entityType()), relation.entityId());
            addElement(relationElement, "from",fromId);
            String toId = formatEntityId(EntityType.fromId(relation.relatedEntityType()), relation.relatedEntityId());
            addElement(relationElement, "to", toId);
            addElement(relationElement, "type", RelationType.CONFIRMED.getDescription());

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
