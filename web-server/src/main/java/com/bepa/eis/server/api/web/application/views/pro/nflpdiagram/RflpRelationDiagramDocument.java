package com.bepa.eis.server.api.web.application.views.pro.nflpdiagram;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.views.basis.relationdiagram.RelationDiagramSnapshot;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider.DiagramRelation;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.*;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.functional.FunctionalStructureEntity;
import com.bepa.eis.server.entites.logical.LogicalStructureEntity;
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

public class RflpRelationDiagramDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(RflpRelationDiagramDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;
    private final boolean compact;
    private RelationDiagramSnapshot snapshot;

    private List<SystemRequirementEntity> listOfSystemRequirements;
    private List<StakeholderRequirementEntity> listOfStakeholderRequirements;
    private List<SystemBreakdownEntity> listOfSystemBreakdowns;
    private List<FunctionalStructureEntity> listOfFunctionalStructures;
    private List<LogicalStructureEntity> listOfLogicalStructures;

    private List<DiagramRelation> relations;
    private record RelationGroup(int fromType, int toType) {}
    private final Map<RelationGroup, List<DiagramRelation>> relationGroups = new HashMap<>();
    private static final Set<EntityType> DIAGRAM_TYPES = Set.of(
            STAKEHOLDER_REQUIREMENT, SYSTEM_REQUIREMENT, FUNCTIONAL_STRUCTURE, LOGICAL_STRUCTURE, SYSTEMS_BREAKDOWN);

    public RflpRelationDiagramDocument(WebSession webSession) throws Exception { this(webSession, false); }

    RflpRelationDiagramDocument(WebSession webSession, boolean compact) throws Exception {

        super(webSession);
        this.compact = compact;

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.RFLP_RELATION_DIAGRAM_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        relationDiagramDocument();
        if (compact) snapshot = buildSnapshot();

        Element relationDiagramElement = buildRelationDiagramElement();
        getRoot().appendChild(relationDiagramElement);

        relationDiagramElement.appendChild(getStakeholderRequirementElement());
        relationDiagramElement.appendChild(getSystemRequirementElement());
        relationDiagramElement.appendChild(getFunctionalStructureElement());
        relationDiagramElement.appendChild(getLogicalStructureElement());
        relationDiagramElement.appendChild(getSystemBreakdownElement());

        if (compact) return;
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToSystemRequirementRelations", STAKEHOLDER_REQUIREMENT, SYSTEM_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToFunctionalStructureRelations", STAKEHOLDER_REQUIREMENT, FUNCTIONAL_STRUCTURE));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToLogicalStructureRelations", STAKEHOLDER_REQUIREMENT, LOGICAL_STRUCTURE));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToSystemsBreakdownRelations", STAKEHOLDER_REQUIREMENT, SYSTEMS_BREAKDOWN));
        relationDiagramElement.appendChild(getRelationsElement("StakeholderRequirementToStakeholderRequirementRelations", STAKEHOLDER_REQUIREMENT, STAKEHOLDER_REQUIREMENT));

        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToStakeholderRequirementRelations", SYSTEM_REQUIREMENT, STAKEHOLDER_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToFunctionalStructureRelations", SYSTEM_REQUIREMENT, FUNCTIONAL_STRUCTURE));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToLogicalStructureRelations", SYSTEM_REQUIREMENT, LOGICAL_STRUCTURE));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToSystemsBreakdownRelations", SYSTEM_REQUIREMENT, SYSTEMS_BREAKDOWN));
        relationDiagramElement.appendChild(getRelationsElement("SystemRequirementToSystemRequirementRelations", SYSTEM_REQUIREMENT, SYSTEM_REQUIREMENT));

        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToStakeholderRequirementRelations", FUNCTIONAL_STRUCTURE, STAKEHOLDER_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToSystemRequirementRelations", FUNCTIONAL_STRUCTURE, SYSTEM_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToLogicalStructureRelations", FUNCTIONAL_STRUCTURE, LOGICAL_STRUCTURE));
        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToSystemsBreakdownRelations", FUNCTIONAL_STRUCTURE, SYSTEMS_BREAKDOWN));
        relationDiagramElement.appendChild(getRelationsElement("FunctionalStructureToFunctionalStructureRelations", FUNCTIONAL_STRUCTURE, FUNCTIONAL_STRUCTURE));

        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToStakeholderRequirementRelations", LOGICAL_STRUCTURE, STAKEHOLDER_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToSystemRequirementRelations", LOGICAL_STRUCTURE, SYSTEM_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToFunctionalStructureRelations", LOGICAL_STRUCTURE, FUNCTIONAL_STRUCTURE));
        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToSystemsBreakdownRelations", LOGICAL_STRUCTURE, SYSTEMS_BREAKDOWN));
        relationDiagramElement.appendChild(getRelationsElement("LogicalStructureToLogicalStructureRelations", LOGICAL_STRUCTURE, LOGICAL_STRUCTURE));

        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToStakeholderRequirementRelations", SYSTEMS_BREAKDOWN, STAKEHOLDER_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToSystemRequirementRelations", SYSTEMS_BREAKDOWN, SYSTEM_REQUIREMENT));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToFunctionalStructureRelations", SYSTEMS_BREAKDOWN, FUNCTIONAL_STRUCTURE));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToLogicalStructureRelations", SYSTEMS_BREAKDOWN, LOGICAL_STRUCTURE));
        relationDiagramElement.appendChild(getRelationsElement("SystemsBreakdownToSystemsBreakdownRelations", SYSTEMS_BREAKDOWN, SYSTEMS_BREAKDOWN));

    }

    RelationDiagramSnapshot snapshot() { return snapshot; }

    private RelationDiagramSnapshot buildSnapshot() {
        var nodes = new java.util.ArrayList<RelationDiagramSnapshot.Node>();
        for (var entity : listOfStakeholderRequirements) nodes.add(new RelationDiagramSnapshot.Node(
                formatEntityId(STAKEHOLDER_REQUIREMENT, entity.getEntityId().getValue()), STAKEHOLDER_REQUIREMENT.getId(), entity.getEntityId().getValue(),
                entity.getRequirementCode().getValue(), entity.getRequirementName().getValue(), entity.getRequirementDescription().getValue()));
        for (var entity : listOfSystemRequirements) nodes.add(new RelationDiagramSnapshot.Node(
                formatEntityId(SYSTEM_REQUIREMENT, entity.getEntityId().getValue()), SYSTEM_REQUIREMENT.getId(), entity.getEntityId().getValue(),
                entity.getRequirementCode().getValue(), entity.getRequirementName().getValue(), entity.getRequirementDescription().getValue()));
        for (var entity : listOfFunctionalStructures) nodes.add(new RelationDiagramSnapshot.Node(
                formatEntityId(FUNCTIONAL_STRUCTURE, entity.getEntityId().getValue()), FUNCTIONAL_STRUCTURE.getId(), entity.getEntityId().getValue(),
                entity.getFunctionalCode().getValue(), entity.getFunctionalName().getValue(), entity.getFunctionalDescription().getValue()));
        for (var entity : listOfLogicalStructures) nodes.add(new RelationDiagramSnapshot.Node(
                formatEntityId(LOGICAL_STRUCTURE, entity.getEntityId().getValue()), LOGICAL_STRUCTURE.getId(), entity.getEntityId().getValue(),
                entity.getLogicalCode().getValue(), entity.getLogicalName().getValue(), entity.getLogicalDescription().getValue()));
        for (var entity : listOfSystemBreakdowns) nodes.add(new RelationDiagramSnapshot.Node(
                formatEntityId(SYSTEMS_BREAKDOWN, entity.getEntityId().getValue()), SYSTEMS_BREAKDOWN.getId(), entity.getEntityId().getValue(),
                entity.getSbsCode().getValue(), entity.getSystemName().getValue(), entity.getDescription()));
        var edges = new java.util.ArrayList<RelationDiagramSnapshot.Edge>();
        for (var relation : relations) edges.add(new RelationDiagramSnapshot.Edge("relation-" + relation.relationId(),
                formatEntityId(EntityType.fromId(relation.entityType()), relation.entityId()),
                formatEntityId(EntityType.fromId(relation.relatedEntityType()), relation.relatedEntityId()), RelationType.CONFIRMED.getDescription()));
        return new RelationDiagramSnapshot(nodes, edges);
    }

    private void relationDiagramDocument () throws SQLException {
        long start = System.currentTimeMillis();

        listOfSystemRequirements = getSystemRequirements();
        log.info("Load System Requirements {}", System.currentTimeMillis()-start);
        listOfStakeholderRequirements = getStakeholderRequirements();
        log.info("Load Stakeholder Requirements {}", System.currentTimeMillis()-start);
        listOfSystemBreakdowns = getSystemBreakdowns();
        log.info("Load System Breakdowns {}", System.currentTimeMillis()-start);
        listOfFunctionalStructures = getFunctionalStructures();
        log.info("Load Functional Structures {}", System.currentTimeMillis()-start);
        listOfLogicalStructures = getLogicalStructures();
        log.info("Load Logical Structures {}", System.currentTimeMillis()-start);

        long relationStart = System.nanoTime();
        relations = new EntityRelationProvider(getWebSession()).getConfirmedDiagramRelations(DIAGRAM_TYPES);
        log.info("Load Entity Relations: {} records in {} ms (total {} ms)", relations.size(),
                (System.nanoTime() - relationStart) / 1_000_000, System.currentTimeMillis() - start);
        // The lazy snapshot consumes the flat list directly; only legacy XML needs groups.
        if (!compact) for (var relation : relations) {
            relationGroups.computeIfAbsent(new RelationGroup(relation.entityType(), relation.relatedEntityType()),
                    ignored -> new ArrayList<>()).add(relation);
        }
    }

    private List<StakeholderRequirementEntity> getStakeholderRequirements() {
        StakeholderRequirementProvider stakeholderRequirementProvider = new StakeholderRequirementProvider(getWebSession());
        List<StakeholderRequirementEntity> listOfStakeholderRequirementEntities;
        listOfStakeholderRequirementEntities = stakeholderRequirementProvider.getAllStakeholderRequirement(false);
        sortBySortKey(listOfStakeholderRequirementEntities);
        return listOfStakeholderRequirementEntities;
    }

    private List<SystemRequirementEntity> getSystemRequirements() {
        SystemRequirementProvider systemRequirementProvider = new SystemRequirementProvider(getWebSession());
        List<SystemRequirementEntity> listOfSystemRequirementEntities;
        listOfSystemRequirementEntities = systemRequirementProvider.getAllSystemRequirement(false);
        sortBySortKey(listOfSystemRequirementEntities);
        return listOfSystemRequirementEntities;
    }

    private List<FunctionalStructureEntity> getFunctionalStructures() {
        FunctionalStructureProvider functionalStructureProvider = new FunctionalStructureProvider(getWebSession());
        List<FunctionalStructureEntity> listOfFunctionalStructureEntities;
        listOfFunctionalStructureEntities = functionalStructureProvider.getAllFunctionalStructure(false);
        sortBySortKey(listOfFunctionalStructureEntities);
        return listOfFunctionalStructureEntities;
    }

    private List<LogicalStructureEntity> getLogicalStructures() {
        LogicalStructureProvider logicalStructureProvider = new LogicalStructureProvider(getWebSession());
        List<LogicalStructureEntity> listOfFunctionalStructureEntities;
        listOfFunctionalStructureEntities = logicalStructureProvider.getAllLogicalStructures(false);
        sortBySortKey(listOfFunctionalStructureEntities);
        return listOfFunctionalStructureEntities;
    }

    private List<SystemBreakdownEntity> getSystemBreakdowns() {
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
                if (compact) requirementElement.setAttribute("hasRelations", Boolean.toString(snapshot.hasRelations(id)));
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
                if (compact) requirementElement.setAttribute("hasRelations", Boolean.toString(snapshot.hasRelations(id)));
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
                if (!compact) addElement(functionalElement, "description", functionalStructure.getFunctionalDescription().getValue());
                if (compact) functionalElement.setAttribute("hasRelations", Boolean.toString(snapshot.hasRelations(id)));
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
                if (!compact) addElement(logicalElement, "description", logicalStructure.getLogicalDescription().getValue());
                if (compact) logicalElement.setAttribute("hasRelations", Boolean.toString(snapshot.hasRelations(id)));
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
                if (!compact) addElement(physicalElement, "description", system.getDescription());
                if (compact) physicalElement.setAttribute("hasRelations", Boolean.toString(snapshot.hasRelations(id)));
                systemRequirements.appendChild(physicalElement);
            }
        }
        return systemRequirements;
    }

    private Element getRelationsElement(String elementName, EntityType fromType, EntityType toType) {
        Element relationsElement = getDoc().createElement(elementName);
        for (var relation : relationGroups.getOrDefault(new RelationGroup(fromType.getId(), toType.getId()), List.of())) {
            Element relationElement = getDoc().createElement("relation");

            String fromId = formatEntityId(fromType, relation.entityId());
            addElement(relationElement, "from",fromId);
            String toId = formatEntityId(toType, relation.relatedEntityId());
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
