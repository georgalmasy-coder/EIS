package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.common.providers.misc.PerformanceProvider;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import com.bepa.eis.server.entites.systemsystemrequirement.SystemRequirementEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.util.*;

/**
 * Dummy relation list endpoint used by the relation dialog.
 *
 * Request template:
 * <RelationListRequest>
 *   <EntityType>5</EntityType>
 *   <EntityId>123</EntityId>
 *   <EntityRelations>
 *     <EntityRelation>
 *       <EntityRelationPK>290</EntityRelationPK>
 *       <EntityId>123</EntityId>
 *       <EntityType>5</EntityType>
 *       <RelatedEntityId>2</RelatedEntityId>
 *       <RelatedEntityType>6</RelatedEntityType>
 *     </EntityRelation>
 *   </EntityRelations>
 * </RelationListRequest>
 *
 * Response template:
 * <RelationListResponse>
 *   <EntityType>5</EntityType>
 *   <EntityId>123</EntityId>
 *   <RelationLists>
 *     <RelationList>
 *       <Label>System requirements</Label>
 *       <ShowConfirmedRelation>true</ShowConfirmedRelation>
 *       <ShowNoRelevantRelation>true</ShowNoRelevantRelation>
 *       <RelationOptions>
 *         <RelationOption>
 *           <EntityId>601</EntityId>
 *           <EntityType>6</EntityType>
 *           <EntityTypeName>System requirement</EntityTypeName>
 *           <EntityCode>SYSREQ-601</EntityCode>
 *           <EntityName>Dummy system requirement</EntityName>
 *         </RelationOption>
 *       </RelationOptions>
 *     </RelationList>
 *   </RelationLists>
 * </RelationListResponse>
 */
@WebServlet(
        name = "EntityRelationListServlet",
        urlPatterns = {
                "/basis/entityrelations/relationlist",
                "/basis/stakeholderrequirement/relationlist",
                "/basis/systemrequirement/relationlist",
                "/basis/systemsbreakdown/relationlist"
        }
)
public class EntityRelationListServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(EntityRelationListServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        WebSession webSession = getSession(request);

        String module = request.getServletPath();
        long startTime = System.currentTimeMillis();

        setXmlResponse(response);

        try {
            WebRequest requestData = parseRequest(request);
            RelationListResponse responseData = buildResponse(webSession, requestData);

            response.getWriter().write(toXmlString(responseData.document(), true));

            PerformanceProvider performanceProvider = new PerformanceProvider(webSession);
            performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);

        } catch (Throwable throwable) {
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);

            log.error("Failed to build relation list response", throwable);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, throwable.getMessage());
        }
    }

    private WebRequest parseRequest(HttpServletRequest request) throws Exception {
        DocumentBuilderFactory factory = newSecureFactory();
        Document document = factory.newDocumentBuilder().parse(request.getInputStream());
        Element root = document.getDocumentElement();

        if (root == null) {
            throw new IllegalArgumentException("Missing relation list request root element.");
        }

        EntityType entityType = parseEntityType(root, request);
        EntityId entityId = new EntityId(parseInteger(textOf(root, "EntityId")));
        Set<String> existingRelations = parseExistingRelations(root);

        return new WebRequest(entityType, entityId, existingRelations);
    }

    private RelationListResponse buildResponse(WebSession webSession, WebRequest requestData) throws Exception {
        DocumentBuilderFactory factory = newSecureFactory();
        Document document = factory.newDocumentBuilder().newDocument();
        Element root = document.createElement("RelationListResponse");
        document.appendChild(root);

        appendTextElement(document, root, "EntityType", valueOf(requestData.entityType()));
        appendTextElement(document, root, "EntityId", requestData.entityId() == null ? "" : String.valueOf(requestData.entityId()));

        Element relationListsNode = document.createElement("RelationLists");
        root.appendChild(relationListsNode);

        for (RelationListSpec listSpec : resolveLists(webSession, requestData)) {
            relationListsNode.appendChild(buildRelationList(document, listSpec, requestData.existingRelations()));
        }

        return new RelationListResponse(document);
    }

    private Element buildRelationList(Document document, RelationListSpec listSpec, Set<String> existingRelations) {
        Element relationList = document.createElement("RelationList");

        appendTextElement(document, relationList, "Label", listSpec.label);
        appendTextElement(document, relationList, "ShowConfirmedRelation", Boolean.toString(listSpec.showConfirmedRelation));
        appendTextElement(document, relationList, "ShowNoRelevantRelation", Boolean.toString(listSpec.showNoRelevantRelation));

        Element optionsNode = document.createElement("RelationOptions");
        relationList.appendChild(optionsNode);

        for (RelationOption option : listSpec.options) {
            String relationKey = relationKey(option.entityType, option.entityId);

            if (existingRelations.contains(relationKey)) {
                continue;
            }

            Element optionNode = document.createElement("RelationOption");
            appendTextElement(document, optionNode, "EntityId", String.valueOf(option.entityId));
            appendTextElement(document, optionNode, "EntityType", valueOf(option.entityType));
            appendTextElement(document, optionNode, "EntityTypeName", option.entityTypeName);
            appendTextElement(document, optionNode, "EntityCode", option.entityCode);
            appendTextElement(document, optionNode, "EntityName", option.entityName);
            optionsNode.appendChild(optionNode);
        }

        return relationList;
    }

    private List<RelationListSpec> resolveLists(WebSession webSession, WebRequest requestData) {

        EntityType entityType = requestData.entityType();
        List<RelationListSpec> lists = new ArrayList<>();

        if (entityType == EntityType.STAKEHOLDER_REQUIREMENT) {
            lists.add(new RelationListSpec(
                    "System requirements",
                    true,
                    true,
                    buildSystemRequirementOptionList(webSession, requestData)
            ));
            return lists;
        }

        if (entityType == EntityType.SYSTEM_REQUIREMENT) {
            lists.add(new RelationListSpec(
                    "Stakeholder requirements",
                    true,
                    true,
                    buildStakeholderRequirementOptionList(webSession, requestData)
            ));

            lists.add(new RelationListSpec(
                    "Systems breakdowns",
                    true,
                    false,
                    buildSystemsBreadownOptionList(webSession, requestData)
            ));
            return lists;
        }

        if (entityType == EntityType.SYSTEMS_BREAKDOWN) {
            lists.add(new RelationListSpec(
                    "System requirements",
                    true,
                    false,
                    buildSystemRequirementOptionList(webSession, requestData)
            )) ;

            lists.add(new RelationListSpec(
                    "Systems breakdowns",
                    true,
                    false,
                    buildSystemsBreadownOptionList(webSession, requestData)
            ));
            return lists;
        }

        /* ???
        lists.add(new RelationListSpec(
                "Relations",
                true,
                true,
                buildSystemRequirementOptionList(webSession, requestData)));

        return lists;

         */

        return lists;
    }

    private List<RelationOption> buildStakeholderRequirementOptionList(WebSession webSession, WebRequest requestData) {

        List<StakeholderRequirementEntity> loadedStakeholderRequirements = getStakeholderRequirements(webSession);
        List<StakeholderRequirementEntity> stakeholderRequirements = new ArrayList<>();

        for (StakeholderRequirementEntity stakeholderRequirement : loadedStakeholderRequirements) {
            if (!hasExistingRelation(stakeholderRequirement.getEntityType(), stakeholderRequirement.getEntityId(), requestData)) {
                stakeholderRequirements.add(stakeholderRequirement);
            }
        }

        List<RelationOption> options = new ArrayList<>();
        for (StakeholderRequirementEntity stakeholderRequirement : stakeholderRequirements) {
            EntityType entityType = stakeholderRequirement.getEntityType();
            options.add(new RelationOption(stakeholderRequirement.getEntityId(), entityType, entityType.getDescription(), stakeholderRequirement.getRequirementCode().getValue(), stakeholderRequirement.getRequirementName().getValue()));
        }
        return options;
    }


    private List<RelationOption> buildSystemRequirementOptionList(WebSession webSession, WebRequest requestData) {

        List<SystemRequirementEntity> loadedSystemRequirements = getSystemRequirements(webSession);
        List<SystemRequirementEntity> systemRequirements = new ArrayList<>();

        for (SystemRequirementEntity systemRequirement : loadedSystemRequirements) {
            if (!hasExistingRelation(systemRequirement.getEntityType(), systemRequirement.getEntityId(), requestData)) {
                systemRequirements.add(systemRequirement);
            }
        }

        List<RelationOption> options = new ArrayList<>();
        for (SystemRequirementEntity systemRequirement : systemRequirements) {
            EntityType entityType = systemRequirement.getEntityType();
            options.add(new RelationOption(systemRequirement.getEntityId(), entityType, entityType.getDescription(), systemRequirement.getRequirementCode().getValue(), systemRequirement.getRequirementName().getValue()));
        }
        return options;
    }

    private List<RelationOption> buildSystemsBreadownOptionList(WebSession webSession, WebRequest requestData) {

        List<SystemBreakdownEntity> loadedSystemBreakdowns = getSystemBreakdowns(webSession);
        List<SystemBreakdownEntity> systemsBreakdowns = new ArrayList<>();

        for (SystemBreakdownEntity systemBreakdown : loadedSystemBreakdowns) {
            if (!hasExistingRelation(systemBreakdown.getEntityType(), systemBreakdown.getEntityId(), requestData)) {
                systemsBreakdowns.add(systemBreakdown);
            }
        }

        List<RelationOption> options = new ArrayList<>();
        for (SystemBreakdownEntity systemsBreakdown : systemsBreakdowns) {
            EntityType entityType = systemsBreakdown.getEntityType();
            options.add(new RelationOption(systemsBreakdown.getEntityId(), entityType, entityType.getDescription(), systemsBreakdown.getSbsCode().getValue(), systemsBreakdown.getSystemName().getValue()));
        }
        return options;
    }

    private boolean hasExistingRelation(EntityType currEntityType, EntityId currEntityId, WebRequest requestData) {

        if (currEntityType == requestData.entityType && Objects.equals(currEntityId, requestData.entityId))  {
            return true;
        }

        for (String relationKey : requestData.existingRelations()) {
            String[] parts = relationKey.split(":");
            EntityType entityType = EntityType.fromId(Integer.parseInt(parts[0]));
            Integer entityId = Integer.valueOf(parts[1]);

            if (currEntityType == entityType && Objects.equals(currEntityId.getValue(), entityId))  {
                return true;
            }
        }
        return false;
    }

    private List<StakeholderRequirementEntity> getStakeholderRequirements(WebSession webSession) {
        StakeholderRequirementProvider stakeholderRequirementProvider = new StakeholderRequirementProvider(webSession);
        List<StakeholderRequirementEntity> listOfStakeholderRequirementEntities;
        listOfStakeholderRequirementEntities = stakeholderRequirementProvider.getAllStakeholderRequirement(false);
        return listOfStakeholderRequirementEntities;
    }

    private List<SystemRequirementEntity> getSystemRequirements(WebSession webSession) {
        SystemRequirementProvider systemRequirementProvider = new SystemRequirementProvider(webSession);
        List<SystemRequirementEntity> listOfSystemRequirementEntities;
        listOfSystemRequirementEntities = systemRequirementProvider.getAllSystemRequirement(false);
        return listOfSystemRequirementEntities;
    }

    private List<SystemBreakdownEntity> getSystemBreakdowns(WebSession webSession)  {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(webSession);
        List<SystemBreakdownEntity> listOfSystemBreakdownEntities;
        listOfSystemBreakdownEntities = systemBreakdownProvider.getAllSystemBreakdown(false);
        return listOfSystemBreakdownEntities;
    }

    private Set<String> parseExistingRelations(Element root) {
        Set<String> keys = new HashSet<>();
        Element relationsNode = firstChild(root, "EntityRelations");

        if (relationsNode == null) {
            return keys;
        }

        for (Element relationNode : children(relationsNode, "EntityRelation")) {
            Integer relatedEntityType = parseInteger(textOf(relationNode, "RelatedEntityType"));
            EntityId relatedEntityId = new EntityId(parseInteger(textOf(relationNode, "RelatedEntityId")));

            if (relatedEntityType != null) {
                keys.add(relationKey(relatedEntityType, relatedEntityId));
            }
        }

        return keys;
    }

    private EntityType parseEntityType(Element root, HttpServletRequest request) {
        Integer entityTypeId = parseInteger(textOf(root, "EntityType"));

        if (entityTypeId == null) {
            entityTypeId = parseInteger(request.getParameter("entityType"));
        }

        if (entityTypeId == null) {
            String servletPath = request.getServletPath();

            if (servletPath != null) {
                if (servletPath.contains("stakeholderrequirement")) {
                    entityTypeId = EntityType.STAKEHOLDER_REQUIREMENT.getId();
                } else if (servletPath.contains("systemrequirement")) {
                    entityTypeId = EntityType.SYSTEM_REQUIREMENT.getId();
                } else if (servletPath.contains("systemsbreakdown")) {
                    entityTypeId = EntityType.SYSTEMS_BREAKDOWN.getId();
                }
            }
        }

        if (entityTypeId == null) {
            throw new IllegalArgumentException("Missing entity type for relation list request.");
        }

        return EntityType.fromId(entityTypeId);
    }

    private DocumentBuilderFactory newSecureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);

        return factory;
    }

    private void appendTextElement(Document document, Element parent, String tagName, String value) {
        Element element = document.createElement(tagName);
        element.setTextContent(value == null ? "" : value);
        parent.appendChild(element);
    }

    private String textOf(Element parent, String tagName) {
        Element child = firstChild(parent, tagName);

        if (child == null || child.getTextContent() == null) {
            return "";
        }

        return child.getTextContent().trim();
    }

    private Element firstChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }

        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            if (parent.getChildNodes().item(i) instanceof Element element && tagName.equals(element.getTagName())) {
                return element;
            }
        }

        return null;
    }

    private List<Element> children(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();

        if (parent == null) {
            return result;
        }

        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            if (parent.getChildNodes().item(i) instanceof Element element && tagName.equals(element.getTagName())) {
                result.add(element);
            }
        }

        return result;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String valueOf(EntityType entityType) {
        return entityType == null ? "" : String.valueOf(entityType.getId());
    }

    private String relationKey(EntityType entityType, EntityId entityId) {
        return relationKey(entityType == null ? null : entityType.getId(), entityId);
    }

    private String relationKey(Integer entityTypeId, EntityId entityId) {
        return valueOf(entityTypeId) + ":" + (entityId == null ? "" : entityId);
    }

    private String valueOf(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record WebRequest(EntityType entityType, EntityId entityId, Set<String> existingRelations) {
    }

    private record RelationListResponse(Document document) {
    }

    private record RelationListSpec(String label, boolean showConfirmedRelation, boolean showNoRelevantRelation, List<RelationOption> options) {
    }

    private record RelationOption(EntityId entityId, EntityType entityType, String entityTypeName, String entityCode, String entityName) {
    }
}
