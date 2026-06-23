package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systemsystemrequirement.SystemRequirementEntity;
import com.bepa.eis.common.enums.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    private static final int MINIMUM_TEXT_MATCH_COUNT = 3;

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
    private ConcurrentMap<String, EntityRelationRecord> mapOfEntityRelations;

    private MatrixColumnsData matrixColumnsData;
    private MatrixRowsData matrixRowsData;
    private MatrixCellsData matrixCellsData;

    protected TraceabilityMatrixDocument(WebSession webSession) throws Exception {
        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());

        buildTraceabilityMatrixDocument();

        Element matrixElement = buildMatrixElement();
        getRoot().appendChild(matrixElement);
        matrixElement.appendChild(matrixMetaData.getMetaElement(getDoc()));
        matrixElement.appendChild(matrixStylesData.getStyleElement(getDoc()));
        matrixElement.appendChild(matrixColumnsData.getColumnElement(getDoc()));
        matrixElement.appendChild(matrixRowsData.getRowElement(getDoc()));
        matrixElement.appendChild(matrixCellsData.getCellElement(getDoc()));
    }

    private void buildTraceabilityMatrixDocument() throws SQLException {
        matrixMetaData = createMatrixMetaData();
        matrixStylesData = createMatrixStylesData();

        listOfSystemRequirementWrappers = getSystemRequirements();
        listOfStakeholderRequirementWrappers = getStakeholderRequirements(listOfSystemRequirementWrappers);

        setListOfStakeholderRequirementsOnAllSystemRequirements();

        listOfEntityRelations = getEntityRelations();
        mapOfEntityRelations  = buildMapOfEntityRelations(listOfEntityRelations);

        setAllRelationsFromSystemRequirementToStakeholderRequirement(
                listOfSystemRequirementWrappers,
                mapOfEntityRelations
        );

        matrixColumnsData = createMatrixColumnsData(listOfSystemRequirementWrappers);
        matrixRowsData = createMatrixRowsData(listOfStakeholderRequirementWrappers);
        matrixCellsData = applyRules(listOfStakeholderRequirementWrappers);
    }

    private void setListOfStakeholderRequirementsOnAllSystemRequirements() {
        for (SystemRequirementWrapper systemRequirementWrapper : listOfSystemRequirementWrappers) {
            systemRequirementWrapper.setListOfStakeholderRequirements(listOfStakeholderRequirementWrappers);
        }
    }

    private ConcurrentMap<String, EntityRelationRecord> buildMapOfEntityRelations(List<EntityRelationRecord> listOfEntityRelations) {
        ConcurrentMap<String, EntityRelationRecord> mapOfEntityRelations = new ConcurrentHashMap<>();
        for (EntityRelationRecord relation : listOfEntityRelations) {

            String ent1 = relation.getEntityId() + "_" + relation.getEntityType().getId();
            String ent2  = relation.getRelatedEntityId() + "_" + relation.getRelatedEntityType().getId();

            String key1 = ent1 + "_" + ent2;
            String key2 = ent2 + "_" + ent1;

            mapOfEntityRelations.put(key1, relation);
            mapOfEntityRelations.put(key2, relation);
        }
        return mapOfEntityRelations;
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

    private MatrixColumnsData createMatrixColumnsData(List<SystemRequirementWrapper> listOfSystemRequirementWrappers) {
        return new MatrixColumnsData(listOfSystemRequirementWrappers);
    }

    private MatrixRowsData createMatrixRowsData(List<StakeholderRequirementWrapper> listOfStakeholderRequirementWrappers) {
        return new MatrixRowsData(listOfStakeholderRequirementWrappers);
    }

    private List<StakeholderRequirementWrapper> getStakeholderRequirements(
            List<SystemRequirementWrapper> listOfSystemRequirementWrappers
    ) throws SQLException {
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

        return listOfStakeholderRequirementWrappers;
    }

    private List<SystemRequirementWrapper> getSystemRequirements() throws SQLException {
        SystemRequirementProvider systemRequirementProvider =
                new SystemRequirementProvider(getWebSession());

        List<SystemRequirementEntity> systemRequirementEntities =
                systemRequirementProvider.getAllSystemRequirement(false);

        List<SystemRequirementWrapper> systemRequirementWrappers = new ArrayList<>();

        for (SystemRequirementEntity entity : systemRequirementEntities) {
            SystemRequirementWrapper systemRequirementWrapper = new SystemRequirementWrapper(entity);
            systemRequirementWrappers.add(systemRequirementWrapper);
        }

        return systemRequirementWrappers;
    }

    private List<EntityRelationRecord> getEntityRelations() throws SQLException {
        EntityRelationProvider entityRelationProvider = new EntityRelationProvider(getWebSession());

        return entityRelationProvider.getAllConfirmedAndNotRelevantEntityRelationRecordsByProjectId(
                STAKEHOLDER_REQUIREMENT,
                SYSTEM_REQUIREMENT
        );
    }

    private void setAllRelationsFromSystemRequirementToStakeholderRequirement(
            List<SystemRequirementWrapper> systemRequirementWrappers,
            ConcurrentMap<String, EntityRelationRecord> mapOfEntityRelations) {

        for (SystemRequirementWrapper systemRequirementWrapper : systemRequirementWrappers) {
            Integer currentEntityId = systemRequirementWrapper.getEntityId();
            EntityType currentEntityType = systemRequirementWrapper.getEntityType();

            for (EntityRelationRecord relation : mapOfEntityRelations.values()) {
                if (Objects.equals(relation.getEntityId(), currentEntityId) && relation.getEntityType() == currentEntityType) {

                    systemRequirementWrapper.addRelationToStakeholderRequirement(relation);

                    log.debug(
                            "Relation found for system requirement {} and stakeholder requirement {}",
                            currentEntityId,
                            relation.getRelatedEntityId()
                    );
                }
            }
/* GFA
            for (EntityRelationRecord relation : relations) {

                if (Objects.equals(relation.getEntityId(), currentEntityId) && relation.getEntityType() == currentEntityType) {

                    systemRequirementWrapper.addRelationToStakeholderRequirement(relation);

                    log.debug(
                            "Relation found for system requirement {} and stakeholder requirement {}",
                            currentEntityId,
                            relation.getRelatedEntityId()
                    );
                }
            }

 */
        }
    }

    private MatrixCellsData applyRules(List<StakeholderRequirementWrapper> stakeholderRequirementWrappers) {
        MatrixCellsData matrixCellsData = new MatrixCellsData();

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
        log.debug("It took {}ms to apply all rules", end - start);

        return matrixCellsData;
    }

    private RuleResult applyRule(StakeholderRequirementWrapper stakeholderRequirementWrapper, SystemRequirementWrapper systemRequirementWrapper) {

        if (hasConfirmedRelation(stakeholderRequirementWrapper, systemRequirementWrapper)) {
            return new RuleResult(VALUE_CONFIRMED_RELATION, STYLE_GREEN);
        }

        if (hasNotRelevantRelation(stakeholderRequirementWrapper, systemRequirementWrapper)) {
            return new RuleResult(VALUE_NOT_RELEVANT, STYLE_GRAY_ITALIC);
        }



        if (hasPossibleTextBasedRelation(stakeholderRequirementWrapper, systemRequirementWrapper)) {

/* GFA
            if (isSystemRequirementNotRelevantToStakeholderRequirement(systemRequirementWrapper)) {

                log.info(

                        "INSERT INTO ENTITY_RELATIONS (CustomerId, ProjectId ,EntityType ,EntityId, RelatedEntityType, RelatedEntityId, RelationType, Version, Latest, CreatedById, CreatedTime) " +
                                "VALUES ( 1 ,1  " +
                                ", " + stakeholderRequirementWrapper.getEntityType().getId() + " " +
                                ", " + stakeholderRequirementWrapper.getEntityId() + " " +
                                ", " + systemRequirementWrapper.getEntityType().getId() + " " +
                                ", " + systemRequirementWrapper.getEntityId() + " " +
                                ",2,1,1,1,GETDATE());"


                );

                return new RuleResult(VALUE_NOT_RELEVANT, STYLE_GRAY_ITALIC);
            }
*/

            return new RuleResult(VALUE_EMPTY, STYLE_YELLOW);
        }

        return new RuleResult(VALUE_EMPTY, STYLE_NORMAL);
    }

    private boolean hasConfirmedRelation(
            StakeholderRequirementWrapper stakeholderRequirementWrapper,
            SystemRequirementWrapper systemRequirementWrapper
    ) {

        String ent1 = stakeholderRequirementWrapper.getEntityId() + "_" + stakeholderRequirementWrapper.getEntityType().getId();
        String ent2  = systemRequirementWrapper.getEntityId() + "_" + systemRequirementWrapper.getEntityType().getId();

        String key1 = ent1 + "_" + ent2;
        String key2 = ent2 + "_" + ent1;

        EntityRelationRecord relation = mapOfEntityRelations.get(key1);
        if (relation == null) {
            relation = mapOfEntityRelations.get(key2);
        }

        return relation != null && relation.getRelationType() == RelationType.CONFIRMED;
    }

    private boolean hasNotRelevantRelation(
            StakeholderRequirementWrapper stakeholderRequirementWrapper,
            SystemRequirementWrapper systemRequirementWrapper
    ) {

        String ent1 = stakeholderRequirementWrapper.getEntityId() + "_" + stakeholderRequirementWrapper.getEntityType().getId();
        String ent2  = systemRequirementWrapper.getEntityId() + "_" + systemRequirementWrapper.getEntityType().getId();

        String key1 = ent1 + "_" + ent2;
        String key2 = ent2 + "_" + ent1;

        EntityRelationRecord relation = mapOfEntityRelations.get(key1);
        if (relation == null) {
            relation = mapOfEntityRelations.get(key2);
        }

        return relation != null && relation.getRelationType() == RelationType.NOT_RELEVANT;
    }

    private boolean hasPossibleTextBasedRelation(
            StakeholderRequirementWrapper stakeholderRequirementWrapper,
            SystemRequirementWrapper systemRequirementWrapper
    ) {
        String stakeholderText = buildSearchText(
                stakeholderRequirementWrapper.getRequirementCode(),
                stakeholderRequirementWrapper.getRequirementName(),
                stakeholderRequirementWrapper.getRequirementDescription()
        );

        String systemText = buildSearchText(
                systemRequirementWrapper.getRequirementCode(),
                systemRequirementWrapper.getRequirementName(),
                systemRequirementWrapper.getRequirementDescription()
        );

        return countMatchingWords(stakeholderText, systemText);
/*
        int matchCount = countMatchingWords(stakeholderText, systemText);

        return matchCount >= MINIMUM_TEXT_MATCH_COUNT;
*/
    }

    private boolean countMatchingWords(String sourceText, String targetText) {
        String[] words = sourceText.split("\\s+");
        int matchCount = 0;

        for (String word : words) {
            if (!isBlank(word) && targetText.contains(word)) {
                matchCount++;
                if (matchCount >= MINIMUM_TEXT_MATCH_COUNT) {
                    return true;
                }
            }
        }

        return false;
    }

    private int countMatchingWordsOLD(String sourceText, String targetText) {
        String[] words = sourceText.split("\\s+");
        int matchCount = 0;

        for (String word : words) {
            if (!isBlank(word) && targetText.contains(word)) {
                matchCount++;
            }
        }

        if (matchCount >= MINIMUM_TEXT_MATCH_COUNT) {
            log.debug("Match {} for {} and {}", matchCount, sourceText, targetText );
        }
        return matchCount;
    }

    private boolean hasMissingTraceability(
            StakeholderRequirementWrapper stakeholderRequirementWrapper,
            SystemRequirementWrapper systemRequirementWrapper
    ) {
        return !stakeholderRequirementWrapper.hasRelationToAnySystemRequirement()
                || !systemRequirementWrapper.hasRelationToAnyStakeholderRequirement();
    }

    private boolean isSystemRequirementNotRelevantToStakeholderRequirement(SystemRequirementWrapper systemRequirementWrapper) {
        return !Boolean.TRUE.equals(systemRequirementWrapper.isRelevantToStakeholderRequirement());
    }

    private String buildSearchText(
            String requirementCode,
            String requirementName,
            String requirementDescription
    ) {
        return normalizeText(
                trimToEmpty(requirementCode)
                        + " "
                        + trimToEmpty(requirementName)
                        + " "
                        + trimToEmpty(requirementDescription)
        );
    }

    private String normalizeText(String value) {
        return trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

}