package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER_REQUIREMENT;
import static com.bepa.eis.common.enums.entity.EntityType.SYSTEM_REQUIREMENT;

/**
 * Traceability matrix rules:
 *
 * 1. Existing relation:
 *    value = "X", style = "green"
 *
 * 2. Possible text-based relation:
 *    2a. If system requirement is not relevant to stakeholder requirements:
 *        value = "NR", style = "grayItalic"
 *
 *    2b. Otherwise:
 *        value = "", style = "yellow"
 *
 * 3. Missing traceability:
 *    value = "", style = "red"
 *
 * 4. No rule matched:
 *    value = "", style = "normal"
 */
public class TraceabilityMatrixDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(TraceabilityMatrixDocument.class);

    private final RequirementTextMatcher textMatcher;
    private final Map<Object, RequirementTextMatcher.PreparedWords> requirementWords = new IdentityHashMap<>();

    protected static final String STYLE_NORMAL = "normal";
    protected static final String STYLE_RED = "red";
    protected static final String STYLE_YELLOW = "yellow";
    protected static final String STYLE_GREEN = "green";
    protected static final String STYLE_GRAY_ITALIC = "grayItalic";

    protected static final String VALUE_EMPTY = "";
    protected static final String VALUE_CONFIRMED_RELATION = "X";
    protected static final String VALUE_NOT_RELEVANT = "NR";

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    private MatrixMetaData matrixMetaData;
    private MatrixStylesData matrixStylesData;

    private List<SystemRequirementWrapper> listOfSystemRequirementWrappers;
    private List<StakeholderRequirementWrapper> listOfStakeholderRequirementWrappers;
    private List<EntityRelationRecord> listOfEntityRelations;
    private TraceabilityRelationIndex relationIndex;

    private ConcurrentMap<Integer, RelationToEntity> mapOfStakeholderRequirementRelations;
    private ConcurrentMap<Integer, RelationToEntity> mapOfSystemRequirementRelations;


    private MatrixColumnsData matrixColumnsData;
    private MatrixRowsData matrixRowsData;
    private MatrixCellsData matrixCellsData;

    protected TraceabilityMatrixDocument(WebSession webSession) throws Exception {
        super(webSession);

        long startTime = System.currentTimeMillis();

        textMatcher = new RequirementTextMatcher(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.TRACEABILITY_MATRIX_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        buildTraceabilityMatrixDocument();

        log.info("buildTraceabilityMatrixDocument took {} ms", System.currentTimeMillis() - startTime);

        Element matrixElement = buildMatrixElement();
        getRoot().appendChild(matrixElement);
        matrixElement.appendChild(matrixMetaData.getMetaElement(getDoc()));
        matrixElement.appendChild(matrixStylesData.getStyleElement(getDoc()));
        matrixElement.appendChild(matrixColumnsData.getColumnElement(getDoc()));
        matrixElement.appendChild(matrixRowsData.getRowElement(getDoc()));
        matrixElement.appendChild(matrixCellsData.getCellElement(getDoc()));
        log.info("buildTraceabilityMatrixDocument build xml took {} ms", System.currentTimeMillis() - startTime);
    }

    private void buildTraceabilityMatrixDocument() throws SQLException {
        long startTime = System.currentTimeMillis();

        matrixMetaData = createMatrixMetaData();
        log.info("buildTraceabilityMatrixDocument create matrixMetaData took {} ms", System.currentTimeMillis() - startTime);
        matrixStylesData = createMatrixStylesData();
        log.info("buildTraceabilityMatrixDocument create matrixStylesData took {} ms", System.currentTimeMillis() - startTime);

        listOfSystemRequirementWrappers = getSystemRequirements();
        log.info("buildTraceabilityMatrixDocument getSystemRequirements took {} ms", System.currentTimeMillis() - startTime);
        listOfStakeholderRequirementWrappers = getStakeholderRequirements(listOfSystemRequirementWrappers);
        log.info("buildTraceabilityMatrixDocument getStakeholderRequirements took {} ms", System.currentTimeMillis() - startTime);

        setListOfStakeholderRequirementsOnAllSystemRequirements();

        listOfEntityRelations = getEntityRelations();
        relationIndex = new TraceabilityRelationIndex(listOfEntityRelations);

        mapOfStakeholderRequirementRelations  = buildMapRelations(STAKEHOLDER_REQUIREMENT, listOfEntityRelations);
        log.info("buildTraceabilityMatrixDocument build mapOfStakeholderRequirementRelations took {} ms", System.currentTimeMillis() - startTime);
        mapOfSystemRequirementRelations = buildMapRelations(SYSTEM_REQUIREMENT, listOfEntityRelations);
        log.info("buildTraceabilityMatrixDocument build mapOfSystemRequirementRelations took {} ms", System.currentTimeMillis() - startTime);

        matrixColumnsData = createMatrixColumnsData(listOfSystemRequirementWrappers, mapOfSystemRequirementRelations);
        log.info("buildTraceabilityMatrixDocument create matrixColumnsData took {} ms", System.currentTimeMillis() - startTime);
        matrixRowsData = createMatrixRowsData(listOfStakeholderRequirementWrappers, mapOfStakeholderRequirementRelations);
        log.info("buildTraceabilityMatrixDocument create matrixRowsData took {} ms", System.currentTimeMillis() - startTime);
        matrixCellsData = applyRules(listOfStakeholderRequirementWrappers);
        log.info("buildTraceabilityMatrixDocument applyRules took {} ms", System.currentTimeMillis() - startTime);
    }

    private void setListOfStakeholderRequirementsOnAllSystemRequirements() {
        for (SystemRequirementWrapper systemRequirementWrapper : listOfSystemRequirementWrappers) {
            systemRequirementWrapper.setListOfStakeholderRequirements(listOfStakeholderRequirementWrappers);
        }
    }

    private ConcurrentMap<Integer, RelationToEntity> buildMapRelations(EntityType entityType, List<EntityRelationRecord> listOfEntityRelations) {
        ConcurrentMap<Integer, RelationToEntity> map = new ConcurrentHashMap<>();

        for (EntityRelationRecord relation : listOfEntityRelations) {
            if (relation.getEntityType() == entityType) {
                RelationToEntity relationToEntity = new RelationToEntity(relation.getEntityId(), relation.getEntityType(), relation.getRelatedEntityId(), relation.getRelatedEntityType(), relation.getRelationType());
                map.put(relation.getEntityId(), relationToEntity);
            }
        }

        for (EntityRelationRecord relation : listOfEntityRelations) {
            if (relation.getRelatedEntityType() == entityType) {
                RelationToEntity relationToEntity = new RelationToEntity(relation.getRelatedEntityId(), relation.getRelatedEntityType(), relation.getEntityId(), relation.getEntityType(), relation.getRelationType());
                map.put(relation.getRelatedEntityId(), relationToEntity);
            }
        }
        return map;
    }

    private MatrixMetaData createMatrixMetaData() {
        MatrixMetaData matrixMetaData = new MatrixMetaData();
        matrixMetaData.setTitle("Traceability Matrix");
        matrixMetaData.setColumnGroupLabel(SYSTEM_REQUIREMENT.getDescription());
        matrixMetaData.setRowGroupLabel(STAKEHOLDER_REQUIREMENT.getDescription());
        matrixMetaData.setGeneratedAt();
        return matrixMetaData;
    }

    private MatrixStylesData createMatrixStylesData() {
        return new MatrixStylesData();
    }

    private MatrixColumnsData createMatrixColumnsData(List<SystemRequirementWrapper> listOfSystemRequirementWrappers, ConcurrentMap<Integer, RelationToEntity> mapOfSystemRequirementRelations) {
        return new MatrixColumnsData(listOfSystemRequirementWrappers, mapOfSystemRequirementRelations);
    }

    private MatrixRowsData createMatrixRowsData(List<StakeholderRequirementWrapper> listOfStakeholderRequirementWrappers, ConcurrentMap<Integer, RelationToEntity> mapOfStakeholderRequirementRelations) {
        return new MatrixRowsData(listOfStakeholderRequirementWrappers, mapOfStakeholderRequirementRelations);
    }

    private List<StakeholderRequirementWrapper> getStakeholderRequirements(
            List<SystemRequirementWrapper> listOfSystemRequirementWrappers
    ) {
        StakeholderRequirementProvider stakeholderRequirementProvider =
                new StakeholderRequirementProvider(getWebSession());

        List<StakeholderRequirementWrapper> listOfStakeholderRequirementWrappers = new ArrayList<>();

        List<StakeholderRequirementEntity> stakeholderRequirementEntities =
                stakeholderRequirementProvider.getAllStakeholderRequirement(false);

        for (StakeholderRequirementEntity entity : stakeholderRequirementEntities) {
            StakeholderRequirementWrapper stakeholderRequirementWrapper =
                    new StakeholderRequirementWrapper(entity, listOfSystemRequirementWrappers);

            listOfStakeholderRequirementWrappers.add(stakeholderRequirementWrapper);
        }

        listOfStakeholderRequirementWrappers.sort(
                Comparator.comparing(
                        StakeholderRequirementWrapper::getSortKey,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                )
        );

        return listOfStakeholderRequirementWrappers;
    }

    private List<SystemRequirementWrapper> getSystemRequirements() {
        SystemRequirementProvider systemRequirementProvider =
                new SystemRequirementProvider(getWebSession());

        List<SystemRequirementEntity> systemRequirementEntities =
                systemRequirementProvider.getAllSystemRequirement(false);

        List<SystemRequirementWrapper> systemRequirementWrappers = new ArrayList<>();

        for (SystemRequirementEntity entity : systemRequirementEntities) {
            SystemRequirementWrapper systemRequirementWrapper = new SystemRequirementWrapper(entity);
            systemRequirementWrappers.add(systemRequirementWrapper);
        }

        systemRequirementWrappers.sort(
                Comparator.comparing(
                        SystemRequirementWrapper::getSortKey,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                )
        );

        return systemRequirementWrappers;
    }

    private List<EntityRelationRecord> getEntityRelations() throws SQLException {
        EntityRelationProvider entityRelationProvider = new EntityRelationProvider(getWebSession());

        return entityRelationProvider.getAllConfirmedAndNotRelevantEntityRelationRecordsByProjectId(
                STAKEHOLDER_REQUIREMENT,
                SYSTEM_REQUIREMENT
        );
    }

    private MatrixCellsData applyRules(List<StakeholderRequirementWrapper> stakeholderRequirementWrappers) {
        MatrixCellsData matrixCellsData = new MatrixCellsData(STYLE_NORMAL, VALUE_EMPTY);

        long start = System.currentTimeMillis();

        int rowIndex = 0;

        for (StakeholderRequirementWrapper stakeholderRequirementWrapper : stakeholderRequirementWrappers) {
            int columnIndex = 0;

            for (SystemRequirementWrapper systemRequirementWrapper : stakeholderRequirementWrapper.getListOfSystemRequirements()) {

                RuleResult ruleResult = applyRule(stakeholderRequirementWrapper, systemRequirementWrapper);

                matrixCellsData.addCell(
                        rowIndex,
                        columnIndex,
                        ruleResult.getValue(),
                        ruleResult.getStyle()
                );

                columnIndex++;
            }

            rowIndex++;
        }

        long end = System.currentTimeMillis();
        log.info("It took {}ms to apply all rules", end - start);

        return matrixCellsData;
    }

    private RuleResult applyRule(StakeholderRequirementWrapper stakeholderRequirementWrapper, SystemRequirementWrapper systemRequirementWrapper) {

        EntityRelationRecord relation = relationIndex.get(stakeholderRequirementWrapper.getEntityId().getValue(),
                systemRequirementWrapper.getEntityId().getValue());
        if (relation != null && relation.getRelationType() == RelationType.CONFIRMED) {
            return new RuleResult(VALUE_CONFIRMED_RELATION, STYLE_GREEN);
        }

        if (relation != null && relation.getRelationType() == RelationType.NOT_RELEVANT) {
            return new RuleResult(VALUE_NOT_RELEVANT, STYLE_GRAY_ITALIC);
        }

        if (hasPossibleTextBasedRelation(stakeholderRequirementWrapper, systemRequirementWrapper)) {
            return new RuleResult(VALUE_EMPTY, STYLE_YELLOW);
        }

        return new RuleResult(VALUE_EMPTY, STYLE_NORMAL);
    }

    private boolean hasPossibleTextBasedRelation(
            StakeholderRequirementWrapper stakeholderRequirementWrapper,
            SystemRequirementWrapper systemRequirementWrapper
    ) {
        RequirementTextMatcher.PreparedWords stakeholderWords = requirementWords.computeIfAbsent(stakeholderRequirementWrapper,
                ignored -> textMatcher.prepare(stakeholderRequirementWrapper.getRequirementName(),
                        stakeholderRequirementWrapper.getRequirementDescription()));
        RequirementTextMatcher.PreparedWords systemWords = requirementWords.computeIfAbsent(systemRequirementWrapper,
                ignored -> textMatcher.prepare(systemRequirementWrapper.getRequirementName(),
                        systemRequirementWrapper.getRequirementDescription()));
        return textMatcher.matches(stakeholderWords, systemWords);
    }

    private Element buildMatrixElement() {
        Element matrixElement = getDoc().createElement("traceabilityMatrix");

        Integer rowCount = listOfStakeholderRequirementWrappers.size();
        Integer columnCount = listOfSystemRequirementWrappers.size();

        matrixElement.setAttribute("version", "1");
        matrixElement.setAttribute("rowCount", rowCount.toString());
        matrixElement.setAttribute("columnCount", columnCount.toString());
        matrixElement.setAttribute("defaultCellStyle", STYLE_NORMAL);
        matrixElement.setAttribute("defaultCellValue", VALUE_EMPTY);

        return matrixElement;
    }

    protected static class RelationToEntity {
        Integer entityId;
        EntityType entityType;
        Integer relatedEntityId;
        EntityType relatedEntityType;
        RelationType relationType;

        RelationToEntity(Integer entityId, EntityType entityType, Integer relatedEntityId, EntityType relatedEntityType, RelationType relationType) {
            this.entityId = entityId;
            this.entityType = entityType;
            this.relatedEntityId = relatedEntityId;
            this.relatedEntityType = relatedEntityType;
            this.relationType = relationType;
        }

    }

}
