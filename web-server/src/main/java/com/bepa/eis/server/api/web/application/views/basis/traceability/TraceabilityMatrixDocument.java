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
public class TraceabilityMatrixDocument extends GenericXmlDocument implements TraceabilitySnapshotStore.Source {

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
        this(webSession, false);
    }

    TraceabilityMatrixDocument(WebSession webSession, boolean lazy) throws Exception {
        super(webSession);
        textMatcher = new RequirementTextMatcher(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.TRACEABILITY_MATRIX_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        buildTraceabilityMatrixDocument(lazy);

        Element matrixElement = buildMatrixElement();
        getRoot().appendChild(matrixElement);
        matrixElement.appendChild(matrixMetaData.getMetaElement(getDoc()));
        matrixElement.appendChild(matrixStylesData.getStyleElement(getDoc()));
        matrixElement.appendChild(matrixColumnsData.getColumnElement(getDoc()));
        matrixElement.appendChild(matrixRowsData.getRowElement(getDoc()));
        matrixElement.appendChild(matrixCellsData.getCellElement(getDoc()));
    }

    private void buildTraceabilityMatrixDocument(boolean lazy) throws SQLException {

        matrixMetaData = createMatrixMetaData();
        matrixStylesData = createMatrixStylesData();

        listOfSystemRequirementWrappers = getSystemRequirements();
        listOfStakeholderRequirementWrappers = getStakeholderRequirements(listOfSystemRequirementWrappers);

        setListOfStakeholderRequirementsOnAllSystemRequirements();

        listOfEntityRelations = getEntityRelations();
        relationIndex = new TraceabilityRelationIndex(listOfEntityRelations);

        mapOfStakeholderRequirementRelations  = buildMapRelations(STAKEHOLDER_REQUIREMENT, listOfEntityRelations);
        mapOfSystemRequirementRelations = buildMapRelations(SYSTEM_REQUIREMENT, listOfEntityRelations);

        matrixColumnsData = createMatrixColumnsData(listOfSystemRequirementWrappers, mapOfSystemRequirementRelations);
        matrixRowsData = createMatrixRowsData(listOfStakeholderRequirementWrappers, mapOfStakeholderRequirementRelations);
        matrixCellsData = lazy ? new MatrixCellsData(STYLE_NORMAL, VALUE_EMPTY)
                : applyRules(listOfStakeholderRequirementWrappers);
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

    public int rowCount() { return listOfStakeholderRequirementWrappers.size(); }
    public int columnCount() { return listOfSystemRequirementWrappers.size(); }

    // Called under the snapshot lock: prepared words are request-local mutable data.
    public List<BlockCell> calculateBlock(int rowStart, int columnStart, int rowEnd, int columnEnd) {
        List<BlockCell> cells = new ArrayList<>();
        for (int r = rowStart; r < rowEnd; r++) {
            for (int c = columnStart; c < columnEnd; c++) {
                RuleResult result = applyRule(listOfStakeholderRequirementWrappers.get(r), listOfSystemRequirementWrappers.get(c));
                if (!STYLE_NORMAL.equals(result.getStyle()) || !VALUE_EMPTY.equals(result.getValue())) {
                    cells.add(new BlockCell(r, c, result.getValue(), result.getStyle()));
                }
            }
        }
        return List.copyOf(cells);
    }

    record BlockCell(int rowIndex, int columnIndex, String value, String style) {}

    public void validatePair(int rowId, int columnId) {
        if (rowPosition(rowId) < 0 || columnPosition(columnId) < 0) throw new IllegalArgumentException("Requirement is not in this snapshot");
    }

    private int rowPosition(int id) {
        for (int i = 0; i < rowCount(); i++) if (listOfStakeholderRequirementWrappers.get(i).getEntityId().getValue() == id) return i;
        return -1;
    }

    private int columnPosition(int id) {
        for (int i = 0; i < columnCount(); i++) if (listOfSystemRequirementWrappers.get(i).getEntityId().getValue() == id) return i;
        return -1;
    }

    public TraceabilitySnapshotStore.CellUpdate updateRelation(int rowId, int columnId, EntityRelationRecord relation) {
        validatePair(rowId, columnId);
        int r = rowPosition(rowId), c = columnPosition(columnId);
        relationIndex.put(rowId, columnId, relation);
        RuleResult result = applyRule(listOfStakeholderRequirementWrappers.get(r), listOfSystemRequirementWrappers.get(c));
        return new TraceabilitySnapshotStore.CellUpdate(r, c, result.getValue(), result.getStyle(),
                relationIndex.hasStakeholderRelations(rowId) ? STYLE_NORMAL : STYLE_RED,
                relationIndex.hasSystemRelations(columnId) ? STYLE_NORMAL : STYLE_RED);
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
