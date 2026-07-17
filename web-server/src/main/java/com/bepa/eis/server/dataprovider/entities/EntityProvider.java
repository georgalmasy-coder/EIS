package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.views.basis.baseline.Baseline;
import com.bepa.eis.server.api.web.application.views.common.EntityAttachmentProvider;
import com.bepa.eis.server.api.web.application.views.common.EntityLinkProvider;
import com.bepa.eis.server.api.web.application.views.common.EntityNoteProvider;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.dataprovider.entities.common.*;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.booleans.Latest;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.*;
import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.datatypes.AbstractDataElement;
import com.bepa.eis.server.entites.functional.FunctionalStructureEntity;
import com.bepa.eis.server.entites.logical.LogicalStructureEntity;
import com.bepa.eis.server.entites.project.ProjectEntity;
import com.bepa.eis.server.entites.stakeholder.StakeholderEntity;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

abstract public class EntityProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(EntityProvider.class);

    public EntityProvider(WebSession webSession) {
        super(webSession);
    }

    abstract public EntityType getEntityType();

    abstract public List<AbstractEntity> toEntities(WebSession webSession, Object rows) throws SQLException;

    private static final String INSERT_ENTITY_SQL =
            "INSERT INTO [ENTITY] (" +
                    "  [CustomerId], " +
                    "  [ProjectId], " +
                    "  [EntityId], " +
                    "  [Version], " +
                    "  [ChangedByUserId], " +
                    "  [ChangedDateTime], " +
                    "  [Latest], " +
                    "  [EntityType], " +
                    "  [Active] " +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ENTITY_ELEMENT_SQL =
            "INSERT INTO [ENTITY_ELEMENT] (" +
                    "  [CustomerId], " +
                    "  [ProjectId], " +
                    "  [EntityId], " +
                    "  [Version], " +
                    "  [EntityType], " +
                    "  [EntityDataElementType], " +
                    "  [#FIELD#]" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_ALL_LATEST_ENTITY_SQL =
            "SELECT E.CustomerId, E.ProjectId, E.EntityId, E.EntityType, E.Version, E.ChangedByUserId, E.ChangedDateTime, E.Latest, E.Active, " +
                    " EE.EntityDataElementType, EE.IntegerValue, EE.DoubleValue, EE.CurrencyValue, EE.StringValue, EE.LocalDateValue, EE.LocalDateTimeValue, EE.BooleanValue " +
                    "FROM ENTITY E, ENTITY_ELEMENT EE " +
                    "WHERE E.CustomerId = EE.CustomerId " +
                    "AND E.ProjectId = EE.ProjectId " +
                    "AND E.EntityType = EE.EntityType " +
                    "AND E.EntityId = EE.EntityId " +
                    "AND E.Version = EE.Version " +
                    "AND E.LATEST = 1 " +
                    "#TYPE_CONDITION# " +
                    "AND E.CustomerId = ? " +
                    "AND E.ProjectId = ? " +
                    "AND E.EntityType = ? ";

    private static final String SELECT_LATEST_ENTITY_BY_ID_SQL =
            "SELECT E.CustomerId, E.ProjectId, E.EntityId, E.EntityType, E.Version, E.ChangedByUserId, E.ChangedDateTime, E.Latest, E.Active, " +
                    " EE.EntityDataElementType, EE.IntegerValue, EE.DoubleValue, EE.CurrencyValue, EE.StringValue, EE.LocalDateValue, EE.LocalDateTimeValue, EE.BooleanValue " +
                    "FROM ENTITY E, ENTITY_ELEMENT EE " +
                    "WHERE E.CustomerId = EE.CustomerId " +
                    "AND E.ProjectId = EE.ProjectId " +
                    "AND E.EntityType = EE.EntityType " +
                    "AND E.EntityId = EE.EntityId " +
                    "AND E.Version = EE.Version " +
                    "#VERSION_CONDITION# " +
                    "#TYPE_CONDITION# " +
                    "AND E.CustomerId = ? " +
                    "AND E.ProjectId = ? " +
                    "AND E.EntityType = ? " +
                    "AND E.EntityId = ? ";


    private static final String SELECT_HISTORY_ENTITY_BY_ID_SQL =
            "SELECT E.CustomerId, E.ProjectId, E.EntityId, E.EntityType, E.Version, E.ChangedByUserId, E.ChangedDateTime, E.Latest, E.Active " +
            "FROM ENTITY E " +
            "WHERE E.CustomerId = ? " +
            "AND E.ProjectId = ? " +
            "AND E.EntityType = ? " +
            "AND E.EntityId = ? " +
            "ORDER BY E.Version DESC";

    private static final String MAX_ENTITY_ID_SQL =
            "SELECT MAX(EntityId) as MAX_ENTITY_ID " +
            "FROM ENTITY " +
            "WHERE CustomerId = ? " +
            "AND ProjectId = ? " +
            "AND EntityType = ? ";

    private static final String MAX_VERSION_SQL =
            "SELECT MAX(Version) as MAX_VERSION " +
                    "FROM ENTITY " +
                    "WHERE CustomerId = ? " +
                    "AND ProjectId = ? " +
                    "AND EntityType = ? " +
                    "AND EntityId = ? ";

    private static final String SELECT_ACTIVE_ENTITY_COUNT_SQL =
            "SELECT COUNT(EntityId) AS ACTIVE_ENTITY_COUNT " +
                    "FROM ENTITY " +
                    "WHERE CustomerId = ? " +
                    "AND ProjectId = ? " +
                    "AND EntityType = ? " +
                    "AND Active = 1 " +
                    "AND Latest = 1 ";


    private static final String GET_ACTIVE_STATUS_SQL =
            "SELECT Active " +
                    "FROM ENTITY " +
                    "WHERE CustomerId = ? " +
                    "AND ProjectId = ? " +
                    "AND EntityType = ? " +
                    "AND EntityId = ? " +
                    "AND Latest = 1 ";

    private static final String UPDATE_ENTITY_ACTIVE_STATUS_SQL =
            "UPDATE ENTITY " +
            "SET Active = ? " +
            "WHERE EntityPK IN ( "+
            "   SELECT EntityPK  " +
            "   FROM ENTITY E, ENTITY_ELEMENT EE " +
            "   WHERE E.CustomerId = ? " +
            "   AND E.CustomerId = EE.CustomerId " +
            "   AND E.ProjectId = ? " +
            "   AND E.ProjectId = EE.ProjectId " +
            "   AND E.EntityType = ? " +
            "   AND E.EntityType = EE.EntityType " +
            "   AND E.Version = EE.Version " +
            "   AND E.Latest = 1 " +
            "   AND E.EntityId = EE.EntityId " +
            "   AND EE.EntityDataElementType = ? " +
            "   AND EE.StringValue like ? " +
            ")";

    private static final String CLEAR_LATEST_ON_SQL =
            "UPDATE ENTITY " +
                    "SET LATEST=0 " +
                    "WHERE CustomerId = ? " +
                    "AND ProjectId = ? " +
                    "AND EntityType = ? " +
                    "AND EntityId = ? ";


    private static final String SELECT_BASELINE_ENTITY_BY_PROJECT_ID_SQL =
            "SELECT E.CustomerId, E.ProjectId, E.EntityId, E.EntityType, E.Version, E.ChangedByUserId, E.ChangedDateTime, E.Latest, E.Active " +
                    "FROM ENTITY E " +
                    "WHERE E.CustomerId = ? " +
                    "AND E.ProjectId = ? " +
                    "AND E.EntityType = ? " +
                    "AND E.ChangedDateTime > ? " +
                    "AND E.ChangedDateTime <= ? " +
                    "ORDER BY E.ProjectId, E.EntityType, E.EntityId, E.Version";

    private static final String SELECT_COLUMN_FROM_ENTITY_SQL =
            "SELECT E.EntityId, EE.StringValue AS StringValue " +
                    "FROM ENTITY E, ENTITY_ELEMENT EE " +
                    "WHERE E.CustomerId = EE.CustomerId " +
                    "AND E.ProjectId = EE.ProjectId " +
                    "AND E.EntityType = EE.EntityType " +
                    "AND E.EntityId = EE.EntityId " +
                    "AND E.Version = EE.Version " +
                    "AND E.LATEST = 1 " +
                    "AND E.CustomerId = ? " +
                    "AND E.ProjectId = ? " +
                    "AND E.EntityType = ? " +
                    "AND E.EntityId = ? " +
                    "AND EE.EntityDataElementType = ?";


    public List<EntityRecord> getListOfEntityRecords(EntityType entityType, boolean includeInactive) throws SQLException {

        ConcurrentMap<String, EntityRecord> mapOfEntities = buildMapOfEntities(getWebSession(), entityType, null, null, entityType.getDataElements());

        List<EntityRecord> entityRecords = new ArrayList<>();

        for (EntityRecord entityRecord : mapOfEntities.values()) {

            if (includeInactive || entityRecord.isActive()) {
                entityRecords.add(entityRecord);
            }
        }

        return entityRecords;
    }

    public Entities getEntityForCreate(EntityType entityType, Integer parentEntityId) throws SQLException {
        log.info("getEntityForCreate : {}", entityType.getDescription());

        Entities entities = new Entities(getWebSession(), entityType.getSingleRootElementName());

        AbstractEntity entity = createEntityObject(null, entityType);

        /* Create new entity where all the entity data elements are added tp */
        Entity entityElement = entities.getNewEntity(entityType.getEntityElementName());

        /* Default entity data elements */
        entityElement.addElement(new ProjectId(getWebSession().getProjectId()));
        entityElement.addElement(new CustomerId(getWebSession().getCustomerId()));
        entityElement.addElement(new EntityId(null));

        Version version = new Version(1);
        version.setFieldNotVisible();
        entityElement.addElement(version);

        entity.addAllFieldElementsForCreate(entityElement, parentEntityId);

        Active active = new Active(true);
        active.setFieldEditable();
        active.setValue(true);
        entityElement.addElement(active);

        entities.addEntity(entityElement);
        return entities;
    }

    public Entities getEntityByEntityId(EntityType entityType, Integer entityId, Integer version) throws SQLException {
        return getListOfEntitiesByProjectId(entityType, entityId, version);
    }

    public Entities getListOfEntitiesByProjectId(EntityType entityType) throws SQLException {
        Entities entities = getListOfEntitiesByProjectId(entityType, null, null);
        entities.sortBy(EntityComparators.bySortKey());
        return entities;
    }

    public Entities getListOfEntitiesByProjectId(EntityType entityType, Integer entityId, Integer historyVersion) throws SQLException {

        log.info("getListOfEntities : {} {}", entityType.getDescription(), entityId != null ? " by id : entityId " + entityId : " version : " + historyVersion);

        Entities entities = new Entities(getWebSession(), entityId != null ? entityType.getSingleRootElementName() : entityType.getMultipleRootElementName());

        ConcurrentMap<String, EntityRecord> mapOfEntities = buildMapOfEntities(getWebSession(), entityType, entityId, historyVersion, entityType.getDataElements());

        for (EntityRecord entityRecord : mapOfEntities.values()) {

            AbstractEntity entity = createEntityObject(entityRecord, entityType);

            /* Create new entityElement where all the entityElement data elements are added tp */
            Entity entityElement = entities.getNewEntity(entityType.getEntityElementName());

            /* Default entityElement data elements */

            entityElement.addElement(entity.getCustomerId());
            entityElement.addElement(entity.getProjectId());
            entityElement.addElement(entity.getEntityId());

            Version version = entity.getVersion();
            version.setFieldNotVisible();
            entityElement.addElement(version);

            Latest latest = entity.getLatest();
            latest.setFieldNotVisible();
            entityElement.addElement(latest);

            Active active = entity.getActive();

            if (entityId == null) {
                entity.addAllFieldElementsForList(entityElement);
                active.setTableWidth("85px");
                entityElement.addElement(active);
            } else {
                entity.addAllFieldElementsForEdit(entityElement);
                active.setFieldEditable();
            }

            /* And finally add another 3 default entityElement data elements */
            ChangedDateTime changedDateTime = entity.getChangedDate();
            changedDateTime.setFieldNotEditable();

            ChangedBy changedBy = entity.getChangedByUser();
            changedBy.setValue(entityRecord.getChangedByUserId());
            changedBy.setFieldNotEditable();

            if (historyVersion == null) {
                changedBy.setTableWidth("175x");
                changedDateTime.setTableWidth("150px");
            } else {
                changedBy.setFieldNotVisible();
                changedDateTime.setFieldNotVisible();
            }

            entityElement.addElement(changedBy);
            entityElement.addElement(changedDateTime);
            entityElement.addElement(active);

            entities.addEntity(entityElement);
        }

        log.debug("Number of entities loaded : {} {}", entityType.getDescription(), mapOfEntities.size());

        return entities;
    }

    private AbstractEntity createEntityObject(EntityRecord entityRecord, EntityType entityType) {
        AbstractEntity entity;

        if (entityRecord != null) {
            switch (entityType) {
                case STAKEHOLDER -> entity = new StakeholderEntity(getWebSession(), entityRecord);
                case STAKEHOLDER_REQUIREMENT -> entity = new StakeholderRequirementEntity(getWebSession(), entityRecord);
                case SYSTEM_REQUIREMENT -> entity = new SystemRequirementEntity(getWebSession(), entityRecord);
                case SYSTEMS_BREAKDOWN -> entity = new SystemBreakdownEntity(getWebSession(), entityRecord);
                case LOGICAL_STRUCTURE -> entity = new LogicalStructureEntity(getWebSession(), entityRecord);
                case FUNCTIONAL_STRUCTURE -> entity = new FunctionalStructureEntity(getWebSession(), entityRecord);
                case PROJECT -> entity = new ProjectEntity(getWebSession(), entityRecord);
                default -> throw new IllegalArgumentException("Invalid entity type : " + entityType);
            }
        } else {
            switch (entityType) {
                case STAKEHOLDER -> entity = new StakeholderEntity(getWebSession());
                case STAKEHOLDER_REQUIREMENT -> entity = new StakeholderRequirementEntity(getWebSession());
                case SYSTEM_REQUIREMENT -> entity = new SystemRequirementEntity(getWebSession());
                case SYSTEMS_BREAKDOWN -> entity = new SystemBreakdownEntity(getWebSession());
                case LOGICAL_STRUCTURE -> entity = new LogicalStructureEntity(getWebSession());
                case FUNCTIONAL_STRUCTURE -> entity = new FunctionalStructureEntity(getWebSession());
                case PROJECT -> entity = new ProjectEntity(getWebSession());
                default -> throw new IllegalArgumentException("Invalid entity type : " + entityType);
            }

        }
        return entity;
    }

    public ConcurrentMap<String, EntityRecord> buildMapOfEntities(WebSession session, EntityType entityType, Integer entityId, Integer historyVersion, EntityDataElement[] entityDataElements) throws SQLException {
        ConcurrentMap<String, EntityRecord> mapOfEntities = new ConcurrentHashMap<>();

        String dataElementIds = null;
        for (EntityDataElement entityDataElement : entityType.getDataElements()) {
            if (dataElementIds == null) {
                dataElementIds = "" + entityDataElement.getId();
            } else {
                dataElementIds += "," + entityDataElement.getId();
            }
        }

        Integer customerId = session.getCustomerId();
        Integer projectId = session.getProjectId();
        Integer entityTypeId = entityType.getId();

        String entitySql = entityId != null ? SELECT_LATEST_ENTITY_BY_ID_SQL : SELECT_ALL_LATEST_ENTITY_SQL;
        entitySql = entitySql.replace("#TYPE_CONDITION#", getElementTypeWhereClause(entityDataElements));

        String versionCondition = historyVersion != null ? " AND E.VERSION = " + historyVersion : " AND E.LATEST = 1 ";
        entitySql = entitySql.replace("#VERSION_CONDITION#", versionCondition);

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(entitySql)) {

            setInt(ps, customerId, 1);
            setInt(ps, projectId, 2);
            setInt(ps, entityTypeId, 3);
            if (entityId != null) {
                setInt(ps, entityId, 4);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    EntityRecord entityRecord = new EntityRecord(
                            getWebSession(),
                            rs.getInt(CustomerId.FIELD_NAME),
                            rs.getInt(ProjectId.FIELD_NAME),
                            rs.getInt(EntityId.FIELD_NAME),
                            rs.getInt(EntityTypeId.FIELD_NAME),
                            rs.getInt(Version.FIELD_NAME),
                            rs.getInt(ChangedBy.FIELD_NAME),
                            rs.getTimestamp(ChangedDateTime.FIELD_NAME),
                            rs.getBoolean(Latest.FIELD_NAME),
                            rs.getBoolean(Active.FIELD_NAME)
                    );
                    String entityKey = entityRecord.getKey();

                    if (mapOfEntities.containsKey(entityKey)) {
                        entityRecord = mapOfEntities.get(entityKey);
                    } else {
                        mapOfEntities.put(entityKey, entityRecord);
                    }

                    EntityElementRecord entityElementRecord = new EntityElementRecord(
                            rs.getInt("EntityDataElementType"),
                            getNullableInteger(rs),
                            getNullableDouble(rs),
                            getNullableBigDecimal(rs),
                            getNullableString(rs),
                            getNullableDate(rs),
                            getNullableTimestamp(rs),
                            getNullableBoolean(rs));

                    entityRecord.addEntityElementRecord(entityElementRecord);

                }
            }
        }

        return mapOfEntities;
    }

    /**
     * Retrieves the history of entities associated with a specific entity ID.
     * The method processes the entity records to include metadata such as
     * project ID, customer ID, version, and information about who and when
     * the entity was last changed.
     *
     * @param entityType the type of the entity for which the history is being retrieved.
     * @param entityId   the unique identifier of the entity for which history data is required.
     * @return an instance of {@code Entities} containing the historical records of the specified entity.
     * @throws SQLException if any database access error occurs while retrieving the entity history.
     */
    public Entities getEntityHistoryByEntityId(EntityType entityType, Integer entityId) throws SQLException {

        String historyElementName = "entityHistories";
        Entities entities = new Entities(getWebSession(), historyElementName);
        log.debug("getEntityHistoryByEntityId : {} by id {}", entityType.getDescription(), entityId);

        List<EntityRecord> listOfEntityHistory = buildMapOfEntityHistory(entityType, entityId);

        for (EntityRecord entityRecord : listOfEntityHistory) {

            Entity entity = entities.getNewEntity("entityHistory");

            ProjectId projectId = new ProjectId(entityRecord.getProjectId());
            projectId.setFieldNotVisible();
            entity.addElement(projectId);

            CustomerId customerId = new CustomerId(entityRecord.getCustomerId());
            customerId.setFieldNotVisible();
            entity.addElement(customerId);

            Version version = new Version(entityRecord.getVersion());
            version.setFieldNotEditable();
            entity.addElement(version);

            ChangedBy changedBy = new ChangedBy(getWebSession());
            changedBy.setValue(entityRecord.getChangedByUserId());
            changedBy.setFieldNotEditable();
            entity.addElement(changedBy);

            ChangedDateTime changedDateTime = new ChangedDateTime(entityRecord.getChangedDateTime());
            changedDateTime.setFieldNotEditable();
            entity.addElement(changedDateTime);

            Latest latest = new Latest(entityRecord.isLatest());
            latest.setFieldNotEditable();
            latest.setFieldNotVisible();
            entity.addElement(latest);

            entities.addEntity(entity);
        }

        return entities;
    }

    private List<EntityRecord> buildMapOfEntityHistory(EntityType entityType, Integer entityId) throws SQLException {
        List<EntityRecord> listOfEntityRecords = new ArrayList<>();

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_HISTORY_ENTITY_BY_ID_SQL)) {

            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, entityType.getId(), 3);
            setInt(ps, entityId, 4);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    EntityRecord entityRecord = new EntityRecord(
                            getWebSession(),
                            rs.getInt(CustomerId.FIELD_NAME),
                            rs.getInt(ProjectId.FIELD_NAME),
                            rs.getInt(EntityId.FIELD_NAME),
                            rs.getInt(EntityTypeId.FIELD_NAME),
                            rs.getInt(Version.FIELD_NAME),
                            rs.getInt(ChangedBy.FIELD_NAME),
                            rs.getTimestamp(ChangedDateTime.FIELD_NAME),
                            rs.getBoolean(Latest.FIELD_NAME),
                            rs.getBoolean(Active.FIELD_NAME)
                    );

                    listOfEntityRecords.add(entityRecord);

                }
            }
        }

        return listOfEntityRecords;
    }

    private Integer getNullableInteger(ResultSet rs) throws SQLException {
        int value = rs.getInt("IntegerValue");
        return rs.wasNull() ? null : value;
    }

    private Double getNullableDouble(ResultSet rs) throws SQLException {
        double value = rs.getDouble("DoubleValue");
        return rs.wasNull() ? null : value;
    }

    private BigDecimal getNullableBigDecimal(ResultSet rs) throws SQLException {
        BigDecimal value = rs.getBigDecimal("CurrencyValue");
        return rs.wasNull() ? null : value;
    }

    private String getNullableString(ResultSet rs) throws SQLException {
        String value = rs.getString("StringValue");
        return rs.wasNull() ? null : value;
    }

    private Date getNullableDate(ResultSet rs) throws SQLException {
        Date value = rs.getDate("LocalDateValue");
        return rs.wasNull() ? null : value;
    }

    private Timestamp getNullableTimestamp(ResultSet rs) throws SQLException {
        Timestamp value = rs.getTimestamp("LocalDateTimeValue");
        return rs.wasNull() ? null : value;
    }

    private Boolean getNullableBoolean(ResultSet rs) throws SQLException {
        boolean value = rs.getBoolean("BooleanValue");
        return rs.wasNull() ? null : value;
    }


    private String getElementTypeWhereClause(EntityDataElement[] entityDataElements) {

        String dataElementIds = null;
        if (entityDataElements != null) {
            for (EntityDataElement entityDataElement : entityDataElements) {
                if (dataElementIds == null) {
                    dataElementIds = "" + entityDataElement.getId();
                } else {
                    dataElementIds += "," + entityDataElement.getId();
                }
            }
        }
        return dataElementIds != null ? "AND EE.EntityDataElementType IN (" + dataElementIds + ")" : "";
    }

    public EntityKey persist(AbstractEntity entity) throws Exception {

        EntityKey entityKey = null;
        Integer currentEntityId;
        Integer currentVersion;
        Boolean prevIsActive;

        if (entity != null && entity.getCustomerId() != null && entity.getProjectId() != null && entity.getEntityType() != null) {

            if (entity.getEntityId().isBlankOrEmpty()) {
                // Create new entity
                currentEntityId = findNextAvailableEntityId(entity);
                currentVersion = 1;
                prevIsActive = true;
            } else {
                currentEntityId = entity.getEntityId().getValue();
                currentVersion = findNextAvailableVersion(entity);
                prevIsActive = getPrevActiveStatus(entity);
            }

            // Set or update Entity Id / Version
            entity.setEntityId(currentEntityId);
            entity.setVersion(currentVersion);
            entity.setPrevActiveStatus(prevIsActive);

            entityKey = new EntityKey(
                    entity.getCustomerId().getValue(),
                    entity.getProjectId().getValue(),
                    currentEntityId,
                    entity.getEntityType(),
                    currentVersion);

            entity.setChangedByUserId(getWebSession().getUserId());

            // Persist entity
            insertOrUpdateEntity(entity);

            sendNotification(entity);

            log.debug("persist : {} by id {}", entity.getEntityType().getDescription(), currentEntityId);
        }

        return entityKey;
    }

    private void sendNotification(AbstractEntity entity) throws Exception {
        if (entity.getEntityType() == EntityType.SYSTEMS_BREAKDOWN) {
            SystemBreakdownEntity systemBreakdownEntity = (SystemBreakdownEntity) entity;

            String sbs = systemBreakdownEntity.getSbsCode().getValue();
            String name = systemBreakdownEntity.getSystemName().getValue();

            User changedBy = null;
            if (systemBreakdownEntity.getChangedByUser() != null) {
                changedBy = systemBreakdownEntity.getChangedByUser().getUser();
            }

            //GFAlog.debug("sendNotification : {} by id {} sbs {} name {} change {} owner {]", entity.getEntityType().getDescription(), entity.getEntityId(), sbs, name, changedBy, ownerUser);
        }

    }

    private Integer findNextAvailableEntityId(AbstractEntity entity) throws SQLException {
        int nextAvailableEntityId;
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(MAX_ENTITY_ID_SQL)) {

            setInt(ps, entity.getCustomerId().getValue(), 1);
            setInt(ps, entity.getProjectId().getValue(), 2);
            setInt(ps, entity.getEntityType().getId(), 3);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    nextAvailableEntityId = 1;
                } else {
                    nextAvailableEntityId = rs.getInt("MAX_ENTITY_ID");
                    nextAvailableEntityId++;
                }
            }
        }
        return nextAvailableEntityId;
    }

    private Boolean getPrevActiveStatus(AbstractEntity entity) throws SQLException {
        Boolean prevActiveStatus;
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_ACTIVE_STATUS_SQL)) {

            setInt(ps, entity.getCustomerId().getValue(), 1);
            setInt(ps, entity.getProjectId().getValue(), 2);
            setInt(ps, entity.getEntityType().getId(), 3);
            setInt(ps, entity.getEntityId().getValue(), 4);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    prevActiveStatus = null;
                } else {
                    prevActiveStatus = rs.getBoolean("Active");
                }
            }
        }
        return prevActiveStatus;
    }

    private Integer findNextAvailableVersion(AbstractEntity entity) throws SQLException {
        int nextAvailableVersion;
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(MAX_VERSION_SQL)) {

            setInt(ps, entity.getCustomerId().getValue(), 1);
            setInt(ps, entity.getProjectId().getValue(), 2);
            setInt(ps, entity.getEntityType().getId(), 3);
            setInt(ps, entity.getEntityId().getValue(), 4);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    nextAvailableVersion = 1;
                } else {
                    nextAvailableVersion = rs.getInt("MAX_VERSION");
                    nextAvailableVersion++;
                }
            }
        }
        return nextAvailableVersion;
    }

    private void insertOrUpdateEntity(AbstractEntity entity) throws SQLException {

        try (Connection con = getDataSource().getConnection()) {
            con.setAutoCommit(false);

            clearLatestIndicatorOnEntity(con, entity);

            try {
                insertNewEntity(con, entity);
                insertDataElements(con, entity);
                getNoteProvider().insertEntityNotes(con, entity);
                getLinkProvider().insertEntityLinks(con, entity);
                getAttachmentProvider().insertEntityAttachment(con, entity);
                getEntityRelationProvider().insertEntityRelations(con, entity);
                updateActiveStatus(con, entity);

                if (entity instanceof ProjectEntity projectEntity) {
                    projectEntity.persistProject();
                }

                con.commit();
                log.info("Entity successfully updated {}", entity.getEntityId());

            } catch (Exception e) {
                try {
                    if (!con.isClosed()) {
                        con.rollback();
                    }
                } catch (SQLException rollbackEx) {
                    log.error("Rollback failed", rollbackEx);
                }

                log.error("Transaction rolled back while creating entities", e);
                throw new SQLException("Error creating entities", e);
            } finally {
                try {
                    if (!con.isClosed()) {
                        con.setAutoCommit(true);
                    }
                } catch (SQLException autoCommitEx) {
                    log.warn("Could not reset autoCommit", autoCommitEx);
                }
            }
        }
    }

    private void clearLatestIndicatorOnEntity(Connection con, AbstractEntity entity) throws SQLException {
        if (! entity.getEntityId().isBlankOrEmpty()) {
            try (PreparedStatement ps = con.prepareStatement(CLEAR_LATEST_ON_SQL)) {

                ps.setInt(1, entity.getCustomerId().getValue());
                ps.setInt(2, entity.getProjectId().getValue());
                ps.setInt(3, entity.getEntityType().getId());
                ps.setInt(4, entity.getEntityId().getValue());
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    log.warn("No entity found for clear latest on : {} by id {}", entity.getEntityType().getDescription(), entity.getEntityId());
                }
            }
        }

    }

    private void insertNewEntity(Connection con, AbstractEntity entity) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_ENTITY_SQL)) {

            ps.setInt(1, entity.getCustomerId().getValue());
            ps.setInt(2, entity.getProjectId().getValue());
            ps.setInt(3, entity.getEntityId().getValue());
            ps.setInt(4, entity.getVersion().getValue());
            ps.setInt(5, entity.getChangedBy().getValue());
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.setBoolean(7, true);
            ps.setInt(8, entity.getEntityType().getId());
            ps.setBoolean(9, entity.isActive());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new SQLException("Insert entity failed for entityId=" + entity.getEntityId());
            }
        }

    }

    private void insertDataElements(Connection con, AbstractEntity entity) throws SQLException {
        for (AbstractDataElement dataElement : entity.getListOfDataElements()) {

            String valueFieldName = dataElement.getElementType().getValueFieldName();
            String insertSqlStatement = INSERT_ENTITY_ELEMENT_SQL.replace("#FIELD#", valueFieldName);

            try (PreparedStatement ps = con.prepareStatement(insertSqlStatement)) {

                ps.setInt(1, entity.getCustomerId().getValue());
                ps.setInt(2, entity.getProjectId().getValue());
                ps.setInt(3, entity.getEntityId().getValue());
                ps.setInt(4, entity.getVersion().getValue());
                ps.setInt(5, entity.getEntityType().getId());
                ps.setInt(6, dataElement.getEntityDataElement().getId());

                switch (dataElement.getElementType()) {
                    case INTEGER -> {
                        if (dataElement.getIntegerValue() != null) {
                            ps.setInt(7, dataElement.getIntegerValue());
                        } else {
                            ps.setNull(7, java.sql.Types.INTEGER);
                        }
                    }
                    case DOUBLE -> {
                        if (dataElement.getDoubleValue() != null) {
                            ps.setDouble(7, dataElement.getDoubleValue());
                        } else {
                            ps.setNull(7, java.sql.Types.DOUBLE);
                        }
                    }
                    case CURRENCY -> {
                        if (dataElement.getCurrencyValue() != null) {
                            ps.setBigDecimal(7, dataElement.getCurrencyValue());
                        } else {
                            ps.setNull(7, java.sql.Types.DECIMAL);
                        }
                    }
                    case NOTE, STRING -> {
                        if (dataElement.getStringValue() != null) {
                            ps.setString(7, dataElement.getStringValue());
                        } else {
                            ps.setNull(7, java.sql.Types.VARCHAR);
                        }
                    }
                    case LOCAL_DATE -> {
                        if (dataElement.getLocalDateValue() != null) {
                            ps.setDate(7, Date.valueOf(dataElement.getLocalDateValue()));
                        } else {
                            ps.setNull(7, java.sql.Types.DATE);
                        }
                    }
                    case LOCAL_DATETIME -> {
                        if (dataElement.getLocalDateTimeValue() != null) {
                            ps.setTimestamp(7, Timestamp.valueOf(dataElement.getLocalDateTimeValue()));
                        } else {
                            ps.setNull(7, java.sql.Types.TIMESTAMP);
                        }
                    }
                    case BOOLEAN -> {
                        if (dataElement.getBooleanValue() != null) {
                            ps.setBoolean(7, dataElement.getBooleanValue());
                        } else {
                            ps.setNull(7, java.sql.Types.BOOLEAN);
                        }
                    }
                    default -> throw new IllegalArgumentException(
                            "Invalid data element type: " + dataElement.getElementType()
                    );
                }

                int rows = ps.executeUpdate();

                if (rows == 0) {
                    throw new SQLException("Insert entity element failed for entityId=" + entity.getEntityId());
                }
            }
        }
    }

    private void updateActiveStatus(Connection con, AbstractEntity entity) throws SQLException {

        if (entity.hasActiveStatusChanged()) {

            EntityDataElement entityCodeColumn = entity.getEntityType().getEntityCodeColumn();

            try (PreparedStatement ps = con.prepareStatement(UPDATE_ENTITY_ACTIVE_STATUS_SQL)) {

                ps.setBoolean(1, entity.isActive());
                ps.setInt(2, entity.getCustomerId().getValue());
                ps.setInt(3, entity.getProjectId().getValue());
                ps.setInt(4, entity.getEntityType().getId());
                ps.setInt(5, entityCodeColumn.getId());
                ps.setString(6, entity.getCode()+".%");

                int rows = ps.executeUpdate();

                if (rows >= 0) {
                    log.info("Active status on {} {} entities has been updated to {}",  rows, entity.getEntityType().getDescription(), entity.isActive());
                }

            }
        }
    }

    public int getActiveEntityCount(Integer customerId, Integer projectId, EntityType entityType) {

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ACTIVE_ENTITY_COUNT_SQL)) {

            setInt(ps, customerId, 1);
            setInt(ps, projectId, 2);
            setInt(ps, entityType.getId(), 3);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("ACTIVE_ENTITY_COUNT") : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public List<EntityRecord> getEntityRecords(EntityType entityType, Baseline baseline) throws SQLException {
        Timestamp start;
        Timestamp end;

        if (baseline.getPreviousBaselineDateTime() != null) {
            start = baseline.getPreviousBaselineDateTime();
        } else {
            start = Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 0, 0));
        }

        if (baseline.getChangedDateTime() != null) {
            end = baseline.getChangedDateTime();
        } else {
            end = Timestamp.valueOf(LocalDateTime.now());
        }

        List<EntityRecord> listOfEntityRecords = new ArrayList<>();

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASELINE_ENTITY_BY_PROJECT_ID_SQL)) {

            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, entityType.getId(), 3);
            setTimestamp(ps, start, 4);
            setTimestamp(ps, end, 5);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    EntityRecord entityRecord = new EntityRecord(
                            getWebSession(),
                            rs.getInt(CustomerId.FIELD_NAME),
                            rs.getInt(ProjectId.FIELD_NAME),
                            rs.getInt(EntityId.FIELD_NAME),
                            rs.getInt(EntityTypeId.FIELD_NAME),
                            rs.getInt(Version.FIELD_NAME),
                            rs.getInt(ChangedBy.FIELD_NAME),
                            rs.getTimestamp(ChangedDateTime.FIELD_NAME),
                            rs.getBoolean(Latest.FIELD_NAME),
                            rs.getBoolean(Active.FIELD_NAME)
                    );

                    listOfEntityRecords.add(entityRecord);

                }
            }
        }

        return listOfEntityRecords;
    }

    public String getEntityColumnValue(EntityType entityType, EntityDataElement entityDataElement, Integer entityId) {

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_COLUMN_FROM_ENTITY_SQL)) {

            ps.setInt(1, getWebSession().getCustomerId());
            ps.setInt(2, getWebSession().getProjectId());
            ps.setInt(3, entityType.getId());
            ps.setInt(4, entityId);
            ps.setInt(5, entityDataElement.getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("StringValue");
            }

        } catch (SQLException e) {
            log.error("Error loading all entities including Code : {}", e.getMessage());
        }
        return "";
    }

    private EntityAttachmentProvider getAttachmentProvider() {
        return new EntityAttachmentProvider(getWebSession());
    }

    private EntityLinkProvider getLinkProvider() {
        return new EntityLinkProvider(getWebSession());
    }

    private EntityNoteProvider getNoteProvider() {
        return new EntityNoteProvider(getWebSession());
    }

    private EntityRelationProvider getEntityRelationProvider() {
        return new EntityRelationProvider(getWebSession());
    }
}
