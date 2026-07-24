package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.common.providers.entityrelation.RelationProvider;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.common.providers.misc.PerformanceProvider;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;
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

import static com.bepa.eis.common.enums.entity.EntityType.SYSTEM_REQUIREMENT;

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
                "/basis/entityrelations/createrelation",
                "/basis/stakeholderrequirement/relationlist",
                "/basis/systemrequirement/relationlist",
                "/basis/systemsbreakdown/relationlist",
                "/pro/logicalstructure/relationlist",
                "/pro/functionalstructure/relationlist"
        }
)
public class EntityRelationListServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(EntityRelationListServlet.class);
    private static final String CREATE_RELATION_PATH = "/basis/entityrelations/createrelation";
    private static final String PARAM_FROM_ENTITY_ID = "FromEntityId";
    private static final String PARAM_FROM_ENTITY_TYPE = "FromEntityType";
    private static final String PARAM_FROM_ENTITY_CODE = "FromEntityCode";
    private static final String PARAM_FROM_ENTITY_NAME = "FromEntityName";
    private static final String PARAM_TO_ENTITY_ID = "ToEntityId";
    private static final String PARAM_TO_ENTITY_TYPE = "ToEntityType";
    private static final String PARAM_TO_ENTITY_CODE = "ToEntityCode";
    private static final String PARAM_TO_ENTITY_NAME = "ToEntityName";
    private static final String PARAM_RELATION_TYPE_NAME = "RelationTypeName";

    private static final List<RelationType> CONFIRMED_AND_NOT_RELEVANT_RELATIONS = List.of(RelationType.CONFIRMED, RelationType.NOT_RELEVANT);
    private static final List<RelationType> CONFIRMED_RELATION = List.of(RelationType.CONFIRMED);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        WebSession webSession = getSession(request);

        String module = request.getServletPath();
        long startTime = System.currentTimeMillis();

        try {
            if (isCreateRelationRequest(request)) {
                handleCreateRelationRequest(webSession, request, response);
            } else {
                setXmlResponse(response);
                WebRequest requestData = parseRequest(request);
                RelationListResponse responseData = buildResponse(webSession, requestData);

                response.getWriter().write(toXmlString(responseData.document(), true));
            }

            PerformanceProvider performanceProvider = new PerformanceProvider(webSession);
            performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);

        } catch (Throwable throwable) {
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);

            if (isCreateRelationRequest(request)) {
                log.error("Failed to create relation", throwable);
                writeCreateRelationErrorResponse(response, throwable.getMessage());
            } else {
                log.error("Failed to build relation list response", throwable);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, throwable.getMessage());
            }
        }
    }

    private boolean isCreateRelationRequest(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return CREATE_RELATION_PATH.equalsIgnoreCase(servletPath);
    }

    private void handleCreateRelationRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        RelationCreateRequest relationCreateRequest;
        try {
            relationCreateRequest = parseCreateRequest(request);
        } catch (IllegalArgumentException e) {
            writeCreateRelationErrorResponse(response, null, e.getMessage());
            return;
        }

        EntityRelationRecord existingRelation = findExistingRelation(webSession, relationCreateRequest);

        if (existingRelation != null && ! existingRelation.getRelationType().isDeleted()) {
            writeCreateRelationErrorResponse(
                    response,
                    relationCreateRequest,
                    "A relation already exists between :\n\n"
                            +"From: " + entitySummary(relationCreateRequest.fromEntityType(), relationCreateRequest.fromEntityCode(), relationCreateRequest.fromEntityName())
                            + "\n"
                            +"To  : " + entitySummary(relationCreateRequest.toEntityType(), relationCreateRequest.toEntityCode(), relationCreateRequest.toEntityName())
                            + ".\n\nCurrent relation type: "
                            + existingRelation.getRelationType().getDescription()
            );
            return;
        }

        List<RelationType> allowedRelationTypes = resolveAllowedRelationTypes(webSession, relationCreateRequest);

        if (allowedRelationTypes.isEmpty()) {
            String errorMessage = "Not allowed to create a relation from '" + relationCreateRequest.fromEntityType.getDescription() + "' to '" + relationCreateRequest.toEntityType.getDescription() + "'.";
            writeCreateRelationErrorResponse(
                    response,
                    relationCreateRequest,
                    errorMessage );
            return;

        }

        String requestedRelationTypeName = relationCreateRequest.relationTypeName();

        if (requestedRelationTypeName.isBlank()) {
            writeCreateRelationTypesResponse(response, relationCreateRequest, allowedRelationTypes);
            return;
        }

        RelationType requestedRelationType = RelationType.valueOfDescription(requestedRelationTypeName);

        if (requestedRelationType == RelationType.INVALID_RELATION_TYPE) {
            writeCreateRelationErrorResponse(response, relationCreateRequest, "Unknown relation type: " + requestedRelationTypeName);
            return;
        }

        if (!allowedRelationTypes.contains(requestedRelationType)) {
            writeCreateRelationErrorResponse(
                    response,
                    "The relation type '" + requestedRelationTypeName + "' is not allowed for this entity pair."
            );
            return;
        }

        createRelation(webSession, relationCreateRequest, requestedRelationType);
        writeCreateRelationSuccessResponse(response, relationCreateRequest, requestedRelationType);
    }

    private RelationCreateRequest parseCreateRequest(HttpServletRequest request) {
        Integer fromEntityId = requiredIntegerParameter(request, PARAM_FROM_ENTITY_ID);
        EntityType fromEntityType = requiredEntityTypeParameter(request, PARAM_FROM_ENTITY_TYPE);
        String fromEntityCode = firstNonBlankParameter(request, PARAM_FROM_ENTITY_CODE);
        String fromEntityName = firstNonBlankParameter(request, PARAM_FROM_ENTITY_NAME);
        Integer toEntityId = requiredIntegerParameter(request, PARAM_TO_ENTITY_ID);
        EntityType toEntityType = requiredEntityTypeParameter(request, PARAM_TO_ENTITY_TYPE);
        String toEntityCode = firstNonBlankParameter(request, PARAM_TO_ENTITY_CODE);
        String toEntityName = firstNonBlankParameter(request, PARAM_TO_ENTITY_NAME);
        String relationTypeName = firstNonBlankParameter(request, PARAM_RELATION_TYPE_NAME);

        if (Objects.equals(fromEntityId, toEntityId) && fromEntityType == toEntityType) {
            throw new IllegalArgumentException("You cannot create a relation from an entity to itself.");
        }

        return new RelationCreateRequest(
                fromEntityType,
                fromEntityId,
                fromEntityCode,
                fromEntityName,
                toEntityType,
                toEntityId,
                toEntityCode,
                toEntityName,
                relationTypeName
        );
    }

    private List<RelationType> resolveAllowedRelationTypes(WebSession webSession, RelationCreateRequest request) {

        switch (request.fromEntityType()) {
            case STAKEHOLDER_REQUIREMENT -> {
                if (request.toEntityType() == SYSTEM_REQUIREMENT) {
                    return CONFIRMED_AND_NOT_RELEVANT_RELATIONS;
                }
            }
            case SYSTEM_REQUIREMENT -> {
                switch (request.toEntityType()) {
                    case STAKEHOLDER_REQUIREMENT -> {
                        return CONFIRMED_AND_NOT_RELEVANT_RELATIONS;
                    }
                    case SYSTEMS_BREAKDOWN -> {
                        return CONFIRMED_RELATION;
                    }
                }
            }
            case SYSTEMS_BREAKDOWN -> {
                switch (request.toEntityType()) {
                    case SYSTEM_REQUIREMENT, SYSTEMS_BREAKDOWN, LOGICAL_STRUCTURE -> {
                        return CONFIRMED_RELATION;
                    }
                }
            }
            case FUNCTIONAL_STRUCTURE -> {
                switch (request.toEntityType()) {
                    case LOGICAL_STRUCTURE, SYSTEM_REQUIREMENT -> {
                        return CONFIRMED_RELATION;
                    }
                }
            }
            case LOGICAL_STRUCTURE -> {
                switch (request.toEntityType()) {
                    case SYSTEMS_BREAKDOWN, FUNCTIONAL_STRUCTURE -> {
                        return CONFIRMED_RELATION;
                    }
                }
            }
        }

        return List.of();
    }

    private EntityRelationRecord findExistingRelation(WebSession webSession, RelationCreateRequest request) throws Exception {
        RelationProvider relationProvider = new RelationProvider(webSession);
        return relationProvider.getEntityRelationByEntityTypeAndId(
                request.fromEntityType(),
                request.fromEntityId(),
                request.toEntityType(),
                request.toEntityId()
        );
    }

    private void createRelation(WebSession webSession, RelationCreateRequest request, RelationType relationType) throws Exception {
        RelationProvider relationProvider = new RelationProvider(webSession);
        EntityRelationRecord newRelationRecord = new EntityRelationRecord(webSession.getCustomerId(), webSession.getProjectId());

        EntityRelationRecord existingRelation = findExistingRelation(webSession, request);

        if (existingRelation != null) {
            relationProvider.clearLatestIfExists(existingRelation);
            newRelationRecord.setVersion(existingRelation.getVersion());
        } else {
            newRelationRecord.setVersion(0);
        }

        newRelationRecord.setEntityType(request.fromEntityType());
        newRelationRecord.setEntityId(request.fromEntityId());
        newRelationRecord.setRelatedEntityType(request.toEntityType());
        newRelationRecord.setRelatedEntityId(request.toEntityId());
        newRelationRecord.setCreatedByUserId(webSession.getUserId());
        newRelationRecord.setRelationType(relationType);
        newRelationRecord.setLatest(true);

        relationProvider.insertRelationRecord(relationType, newRelationRecord);
    }

    private void writeCreateRelationTypesResponse(HttpServletResponse response, RelationCreateRequest request, List<RelationType> relationTypes) throws Exception {
        setXmlResponse(response);

        DocumentBuilderFactory factory = newSecureFactory();
        Document document = factory.newDocumentBuilder().newDocument();
        Element root = document.createElement("RelationCreateResponse");
        root.setAttribute("status", "options");
        document.appendChild(root);

        appendTextElement(document, root, "FromEntityType", valueOf(request.fromEntityType()));
        appendTextElement(document, root, "FromEntityId", String.valueOf(request.fromEntityId()));
        appendTextElement(document, root, "FromEntityCode", request.fromEntityCode());
        appendTextElement(document, root, "FromEntityName", request.fromEntityName());
        appendTextElement(document, root, "ToEntityType", valueOf(request.toEntityType()));
        appendTextElement(document, root, "ToEntityId", String.valueOf(request.toEntityId()));
        appendTextElement(document, root, "ToEntityCode", request.toEntityCode());
        appendTextElement(document, root, "ToEntityName", request.toEntityName());

        Element relationTypesNode = document.createElement("RelationTypes");
        root.appendChild(relationTypesNode);

        for (RelationType relationType : relationTypes) {
            Element relationTypeElement = document.createElement("RelationType");
            relationTypeElement.setAttribute("id", String.valueOf(relationType.getId()));
            relationTypeElement.setTextContent(relationType.getDescription());
            relationTypesNode.appendChild(relationTypeElement);
        }

        response.getWriter().write(toXmlString(document, true));
    }

    private void writeCreateRelationSuccessResponse(HttpServletResponse response, RelationCreateRequest request, RelationType relationType) throws Exception {
        setXmlResponse(response);

        DocumentBuilderFactory factory = newSecureFactory();
        Document document = factory.newDocumentBuilder().newDocument();
        Element root = document.createElement("RelationCreateResponse");
        root.setAttribute("status", "created");
        document.appendChild(root);

        appendTextElement(document, root, "FromEntityType", valueOf(request.fromEntityType()));
        appendTextElement(document, root, "FromEntityId", String.valueOf(request.fromEntityId()));
        appendTextElement(document, root, "FromEntityCode", request.fromEntityCode());
        appendTextElement(document, root, "FromEntityName", request.fromEntityName());
        appendTextElement(document, root, "ToEntityType", valueOf(request.toEntityType()));
        appendTextElement(document, root, "ToEntityId", String.valueOf(request.toEntityId()));
        appendTextElement(document, root, "ToEntityCode", request.toEntityCode());
        appendTextElement(document, root, "ToEntityName", request.toEntityName());

        Element relationTypeElement = document.createElement("RelationType");
        relationTypeElement.setAttribute("id", String.valueOf(relationType.getId()));
        relationTypeElement.setTextContent(relationType.getDescription());
        root.appendChild(relationTypeElement);

        response.getWriter().write(toXmlString(document, true));
    }

    private void writeCreateRelationErrorResponse(HttpServletResponse response, String message) throws IOException {
        writeCreateRelationErrorResponse(response, null, message);
    }

    private void writeCreateRelationErrorResponse(HttpServletResponse response, RelationCreateRequest request, String message) throws IOException {
        try {
            setXmlResponse(response);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            DocumentBuilderFactory factory = newSecureFactory();
            Document document = factory.newDocumentBuilder().newDocument();
            Element root = document.createElement("RelationCreateResponse");
            root.setAttribute("status", "error");
            document.appendChild(root);

            if (request != null) {
                appendTextElement(document, root, "FromEntityType", valueOf(request.fromEntityType()));
                appendTextElement(document, root, "FromEntityId", String.valueOf(request.fromEntityId()));
                appendTextElement(document, root, "FromEntityCode", request.fromEntityCode());
                appendTextElement(document, root, "FromEntityName", request.fromEntityName());
                appendTextElement(document, root, "ToEntityType", valueOf(request.toEntityType()));
                appendTextElement(document, root, "ToEntityId", String.valueOf(request.toEntityId()));
                appendTextElement(document, root, "ToEntityCode", request.toEntityCode());
                appendTextElement(document, root, "ToEntityName", request.toEntityName());
            }

            appendTextElement(document, root, "Message", message == null || message.isBlank() ? "Unknown error" : message);
            response.getWriter().write(toXmlString(document, true));
        } catch (Exception exception) {
            log.error("Failed to write create relation error response", exception);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message == null || message.isBlank() ? "Unknown error" : message);
        }
    }

    private Integer requiredIntegerParameter(HttpServletRequest request, String name) {
        String value = requiredParameter(request, name);
        Integer parsed = parseInteger(value);

        if (parsed == null) {
            throw new IllegalArgumentException("Invalid integer parameter: " + name);
        }

        return parsed;
    }

    private EntityType requiredEntityTypeParameter(HttpServletRequest request, String name) {
        Integer value = requiredIntegerParameter(request, name);
        return EntityType.fromId(value);
    }

    private String requiredParameter(HttpServletRequest request, String name) {
        String value = firstNonBlankParameter(request, name);

        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + name);
        }

        return value;
    }

    private String firstNonBlankParameter(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getParameter(name);

            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
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

        if (entityType == SYSTEM_REQUIREMENT) {
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
                    entityTypeId = SYSTEM_REQUIREMENT.getId();
                } else if (servletPath.contains("systemsbreakdown")) {
                    entityTypeId = EntityType.SYSTEMS_BREAKDOWN.getId();
                } else if (servletPath.contains("functionalstructure")) {
                    entityTypeId = EntityType.FUNCTIONAL_STRUCTURE.getId();
                } else if (servletPath.contains("logicalstructure")) {
                    entityTypeId = EntityType.LOGICAL_STRUCTURE.getId();
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

    private String entitySummary(EntityType entityType, String entityCode, String entityName) {
        String typeText = entityType == null ? "Entity" : entityType.getDescription();
        String codeText = (entityCode == null || entityCode.isBlank()) ? "-" : entityCode.trim();
        String nameText = (entityName == null || entityName.isBlank()) ? "-" : entityName.trim();
        return typeText + " " + codeText + " - " + nameText;
    }

    private record RelationCreateRequest(
            EntityType fromEntityType,
            Integer fromEntityId,
            String fromEntityCode,
            String fromEntityName,
            EntityType toEntityType,
            Integer toEntityId,
            String toEntityCode,
            String toEntityName,
            String relationTypeName
    ) {
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
