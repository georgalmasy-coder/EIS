package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.common.providers.entityrelation.RelationProvider;
import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.baseline.Baseline;
import com.bepa.eis.server.dataprovider.entities.common.*;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.fields.booleans.AbstractBoolean;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.booleans.Latest;
import com.bepa.eis.server.dataprovider.fields.integers.AbstractInteger;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.*;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.strings.AbstractString;
import com.bepa.eis.server.dataprovider.fields.timestamp.AbstractDate;
import com.bepa.eis.server.dataprovider.fields.timestamp.AbstractDateTime;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.configuration.EntityConfiguration;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.datatypes.AbstractDataElement;
import com.bepa.eis.server.entites.project.ProjectEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
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

    abstract public EntityDataElement[] getEntityDataElementForList();

    abstract public void addAllFieldElementsForList(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity);

    abstract public EntityDataElement[] getEntityDataElementForEdit();

    abstract public EntityDataElement[] getEntityDataElementForCreate();

    abstract public void addAllFieldElementsForEdit(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity);

    abstract public void addAllFieldElementsForCreate(WebSession webSession, Entity entity, Integer parentEntityId);

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


    private static final String INSERT_ENTITY_NOTE_SQL =
            "INSERT INTO ENTITY_NOTES (" +
                    "  CustomerId, " +
                    "  ProjectId, " +
                    "  EntityId, " +
                    "  Version, " +
                    "  EntityType, " +
                    "  NoteText, " +
                    "  CreatedById, " +
                    "  CreatedTime" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ENTITY_ATTACHMENT_SQL =
            "INSERT INTO ENTITY_ATTACHMENTS (" +
                    "  CustomerId, " +
                    "  ProjectId, " +
                    "  EntityId, " +
                    "  EntityType, " +
                    "  FileName, " +
                    "  ContentType, " +
                    "  FileSize, " +
                    "  FileData, " +
                    "  Description, " +
                    "  CreatedById, " +
                    "  CreatedTime, " +
                    "  IsDeleted" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DELETE_ENTITY_ATTACHMENT_SQL =
            "DELETE FROM ENTITY_ATTACHMENTS " +
            "WHERE EntityAttachmentPK = ?";


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

        /* Create new entity where all the entity data elements are added tp */
        Entity entity = entities.getNewEntity(entityType.getEntityElementName());

        /* Default entity data elements */
        entity.addElement(new ProjectId(getWebSession().getProjectId()));
        entity.addElement(new CustomerId(getWebSession().getCustomerId()));
        entity.addElement(new EntityId(null));
        entity.addElement(new ParentEntityId(parentEntityId));

        Version version = new Version(1);
        version.setFieldNotVisible();
        entity.addElement(version);

        addAllFieldElementsForCreate(getWebSession(), entity, parentEntityId);

        Active active = new Active(true);
        active.setFieldEditable();
        active.setValue(true);
        entity.addElement(active);

        entities.addEntity(entity);
        return entities;
    }

    public Entities getEntityByEntityId(EntityType entityType, Integer entityId, Integer version, EntityDataElement[] entityDataElements) throws SQLException {
        return getListOfEntitiesByProjectId(entityType, entityId, version, entityDataElements);
    }

    public Entities getListOfEntitiesByProjectId(EntityType entityType, EntityDataElement[] entityDataElements) throws SQLException {
        Entities entities = getListOfEntitiesByProjectId(entityType, null, null, entityDataElements);

        entities.sortBy(EntityComparators.bySortKey());
        return entities;
    }

    public Entities getListOfEntitiesByProjectId(EntityType entityType, Integer entityId, Integer historyVersion, EntityDataElement[] entityDataElements) throws SQLException {

        log.info("getListOfEntities : {} {}", entityType.getDescription(), entityId != null ? " by id : entityId " + entityId : " version : " + historyVersion);

        Entities entities = new Entities(getWebSession(), entityId != null ? entityType.getSingleRootElementName() : entityType.getMultipleRootElementName());

        ConcurrentMap<String, EntityRecord> mapOfEntities = buildMapOfEntities(getWebSession(), entityType, entityId, historyVersion, entityDataElements);

        for (EntityRecord entityRecord : mapOfEntities.values()) {

            /* Create new entity where all the entity data elements are added tp */
            Entity entity = entities.getNewEntity(entityType.getEntityElementName());

            /* Default entity data elements */
            entity.addElement(new ProjectId(entityRecord.getProjectId()));
            entity.addElement(new CustomerId(entityRecord.getCustomerId()));
            entity.addElement(new EntityId(entityRecord.getEntityId()));

            Version version = new Version(entityRecord.getVersion());
            version.setFieldNotVisible();
            entity.addElement(version);

            Latest latest = new Latest(entityRecord.isLatest());
            latest.setFieldNotVisible();
            entity.addElement(latest);

            /* Build map of all loaded fields */
            ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields = buildMapOfLoadedFields(entityRecord);

            Active active = new Active(entityRecord.isActive());

            if (entityId == null) {
                addAllFieldElementsForList(mapOfLoadedFields, entity);
                active.setTableWidth("85px");
                entity.addElement(active);
            } else {
                addAllFieldElementsForEdit(mapOfLoadedFields, entity);
                active.setFieldEditable();
            }

            /* And finally add another 3 default entity data elements */
            ChangedDateTime changedDateTime = new ChangedDateTime(entityRecord.getChangedDateTime());
            changedDateTime.setFieldNotEditable();

            ChangedBy changedBy = new ChangedBy(getWebSession());
            changedBy.setValue(entityRecord.getChangedByUserId());
            changedBy.setFieldNotEditable();

            if (historyVersion == null) {
                changedBy.setTableWidth("175x");
                changedDateTime.setTableWidth("150px");
            } else {
                changedBy.setFieldNotVisible();
                changedDateTime.setFieldNotVisible();
            }

            entity.addElement(changedBy);
            entity.addElement(changedDateTime);
            entity.addElement(active);

            entities.addEntity(entity);
        }

        log.debug("Number of entities loaded : {} {}", entityType.getDescription(), mapOfEntities.size());

        return entities;
    }

    private ConcurrentHashMap<Integer, AbstractField> buildMapOfLoadedFields(EntityRecord entityRecord) {

        ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields = new ConcurrentHashMap<>();

        for (EntityElementRecord entityElementRecord : entityRecord.getEntityElementRecords()) {

            EntityDataElement entityDataElement = EntityConfiguration.getInstance().getEntityDataElement(entityElementRecord.getEntityDataElementType());
            Integer elementId = entityDataElement.getId();

            AbstractField abstractField = MapDataElementType.toFieldObject(entityDataElement);

            switch (entityDataElement.getEntityElementType()) {
                case BOOLEAN -> {
                    AbstractBoolean ab = (AbstractBoolean) abstractField;
                    if (ab != null) {
                        ab.setValue(entityElementRecord.getBooleanValue());
                        mapOfLoadedFields.put(elementId, ab);
                    } else {
                        throw new IllegalArgumentException("Invalid field object");
                    }
                }
                case CURRENCY -> {
                    entityElementRecord.getCurrencyValue();
                    new IllegalArgumentException("TO BE IMPLEMENTED : " + entityDataElement.getEntityElementType());
                }
                case DOUBLE -> {
                    entityElementRecord.getDoubleValue();
                    new IllegalArgumentException("TO BE IMPLEMENTED : " + entityDataElement.getEntityElementType());
                }
                case INTEGER -> {
                    AbstractInteger ai = (AbstractInteger) abstractField;

                    if (ai != null) {
                        if (ai instanceof AbstractLookup abstractLookup) {
                            abstractLookup.setWebSession(getWebSession());
                            abstractLookup.setValue(entityElementRecord.getIntegerValue());
                            mapOfLoadedFields.put(elementId, abstractLookup);
                        } else if (ai instanceof AbstractInteger abstractInteger) {
                            abstractInteger.setValue(entityElementRecord.getIntegerValue());
                            mapOfLoadedFields.put(elementId, abstractInteger);
                        }
                    }
                }
                case LOCAL_DATE -> {
                    entityElementRecord.getLocalDateValue();
                    AbstractDate ad = (AbstractDate) abstractField;

                    if (ad != null) {
                        ad.setValue(entityElementRecord.getLocalDateValue());
                        mapOfLoadedFields.put(elementId, ad);
                    }
                }

                case LOCAL_DATETIME -> {
                    entityElementRecord.getLocalDateTimeValue();
                    AbstractDateTime adt = (AbstractDateTime) abstractField;
                    if (adt != null) {
                        adt.setValue(entityElementRecord.getLocalDateTimeValue());
                        mapOfLoadedFields.put(elementId, adt);
                    }
                }

                case NOTE, STRING -> {
                    AbstractString as = (AbstractString) abstractField;
                    if (as != null) {
                        as.setValue(entityElementRecord.getStringValue());
                        mapOfLoadedFields.put(elementId, as);
                    }
                }
                default ->
                        throw new IllegalArgumentException("Invalid entity data element type: " + entityDataElement.getEntityElementType());
            }
        }

        return mapOfLoadedFields;
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

            if (entity.getEntityId() == null) {
                // Create new entity
                currentEntityId = findNextAvailableEntityId(entity);
                currentVersion = 1;
                prevIsActive = true;
            } else {
                currentEntityId = entity.getEntityId();
                currentVersion = findNextAvailableVersion(entity);
                prevIsActive = getPrevActiveStatus(entity);
            }

            // Set or update Entity Id / Version
            entity.setEntityId(currentEntityId);
            entity.setVersion(currentVersion);
            entity.setPrevActiveStatus(prevIsActive);

            entityKey = new EntityKey(entity.getCustomerId(), entity.getProjectId(), currentEntityId, entity.getEntityType(), currentVersion);

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

            String sbs = systemBreakdownEntity.getSbsCode();
            String name = systemBreakdownEntity.getSystemName();

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

            setInt(ps, entity.getCustomerId(), 1);
            setInt(ps, entity.getProjectId(), 2);
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

            setInt(ps, entity.getCustomerId(), 1);
            setInt(ps, entity.getProjectId(), 2);
            setInt(ps, entity.getEntityType().getId(), 3);
            setInt(ps, entity.getEntityId(), 4);

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

            setInt(ps, entity.getCustomerId(), 1);
            setInt(ps, entity.getProjectId(), 2);
            setInt(ps, entity.getEntityType().getId(), 3);
            setInt(ps, entity.getEntityId(), 4);

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
                insertEntityNotes(con, entity);
                insertEntityAttachment(con, entity);
                insertEntityRelations(con, entity);
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
        if (entity.getEntityId() != null) {
            try (PreparedStatement ps = con.prepareStatement(CLEAR_LATEST_ON_SQL)) {

                ps.setInt(1, entity.getCustomerId());
                ps.setInt(2, entity.getProjectId());
                ps.setInt(3, entity.getEntityType().getId());
                ps.setInt(4, entity.getEntityId());
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    log.warn("No entity found for clear latest on : {} by id {}", entity.getEntityType().getDescription(), entity.getEntityId());
                }
            }
        }

    }

    private void insertNewEntity(Connection con, AbstractEntity entity) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_ENTITY_SQL)) {

            ps.setInt(1, entity.getCustomerId());
            ps.setInt(2, entity.getProjectId());
            ps.setInt(3, entity.getEntityId());
            ps.setInt(4, entity.getVersion());
            ps.setInt(5, entity.getChangedByUserId());
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

                ps.setInt(1, entity.getCustomerId());
                ps.setInt(2, entity.getProjectId());
                ps.setInt(3, entity.getEntityId());
                ps.setInt(4, entity.getVersion());
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

    private void insertEntityNotes(Connection con, AbstractEntity entity) throws SQLException {

        for (NoteRecord noteRecord : entity.getListOfEntityNotes()) {

            try (PreparedStatement ps = con.prepareStatement(INSERT_ENTITY_NOTE_SQL)) {

                ps.setInt(1, entity.getCustomerId());
                ps.setInt(2, entity.getProjectId());
                ps.setInt(3, entity.getEntityId());
                ps.setInt(4, entity.getVersion());
                ps.setInt(5, entity.getEntityType().getId());
                ps.setString(6, noteRecord.getNoteText());
                ps.setInt(7, noteRecord.getChangedByUserId());
                ps.setTimestamp(8, Timestamp.valueOf(noteRecord.getChangedDate()));

                int rows = ps.executeUpdate();

                if (rows == 0) {
                    throw new SQLException("Insert entity note failed for entityId=" + entity.getEntityId());
                }
            }
        }
    }

    private void insertEntityAttachment(Connection con, AbstractEntity entity) throws SQLException {

        for (AttachmentRecord attachmentRecord : entity.getListOfEntityAttachments()) {

            if (attachmentRecord.getEntityAttachmentPK() == null) {

                try (PreparedStatement ps = con.prepareStatement(INSERT_ENTITY_ATTACHMENT_SQL)) {

                    ps.setInt(1, entity.getCustomerId());
                    ps.setInt(2, entity.getProjectId());
                    ps.setInt(3, entity.getEntityId());
                    ps.setInt(4, entity.getEntityType().getId());

                    ps.setString(5, attachmentRecord.getFileName());
                    ps.setString(6, attachmentRecord.getContentType());
                    ps.setInt(7, attachmentRecord.getFileSize());
                    ps.setBytes(8, attachmentRecord.getFileDataAsBinary());
                    ps.setString(9, attachmentRecord.getDescription());

                    ps.setInt(10, attachmentRecord.getChangedByUserId());
                    ps.setTimestamp(11, Timestamp.valueOf(attachmentRecord.getChangedDate()));
                    ps.setBoolean(12, attachmentRecord.isFileDeleted());

                    int rows = ps.executeUpdate();

                    if (rows == 0) {
                        throw new SQLException("Insert entity attachment failed for entityId = " + entity.getEntityId());
                    }
                }
            } else {
                if (attachmentRecord.isFileDeleted() ) {
                    log.info("Deleting attachment for entityId : {} - {}", entity.getEntityId(), attachmentRecord.getFileName());

                    try (PreparedStatement ps = con.prepareStatement(DELETE_ENTITY_ATTACHMENT_SQL)) {
                        ps.setInt(1, attachmentRecord.getEntityAttachmentPK());
                        int rows = ps.executeUpdate();
                        if (rows != 1) {
                            throw new SQLException("Deleting entity attachment failed for EntityAttachmentPK = " + attachmentRecord.getEntityAttachmentPK());
                        }
                    }

                }
                log.info("Updating attachment for entityId : {} - {}", entity.getEntityId(), attachmentRecord.getFileName());
            }
        }
    }

    private void insertEntityRelations(Connection con, AbstractEntity entity) throws SQLException {

        for (EntityRelationRecord relationRecord : entity.getListOfEntityRelationRecords()) {

            EntityRelationRecord existingRelationRecord = getExistingEntityRelationRecord(con, relationRecord);
            if (existingRelationRecord == null) {
                getRelationProvider().insertRelationRecord(con, relationRecord.getRelationType(), relationRecord);
                log.debug("Entity relations inserted for entityId : {} - {}", entity.getEntityId(), entity.getEntityType().getDescription());
            } else {
                if (existingRelationRecord.getRelationType() != relationRecord.getRelationType()) {
                    relationRecord.setVersion(existingRelationRecord.getVersion());
                    getRelationProvider().clearLatestIfExists(con, existingRelationRecord);
                    getRelationProvider().insertRelationRecord(con, relationRecord.getRelationType(), relationRecord);

                }
            }
        }
        log.debug("Entity relations inserted for entityId : {} - {}", entity.getEntityId(), entity.getEntityType().getDescription());
    }

    private EntityRelationRecord getExistingEntityRelationRecord(Connection con, EntityRelationRecord relationRecord) throws SQLException {
        return  getRelationProvider().getEntityRelationByEntityTypeAndId(
                con,
                relationRecord.getEntityType(),
                relationRecord.getEntityId(),
                relationRecord.getRelatedEntityType(),
                relationRecord.getRelatedEntityId());
    }

    private RelationProvider getRelationProvider() {
        return new RelationProvider(getWebSession());
    }

    private void updateActiveStatus(Connection con, AbstractEntity entity) throws SQLException {

        if (entity.hasActiveStatusChanged()) {

            EntityDataElement entityCodeColumn = entity.getEntityType().getEntityCodeColumn();

            try (PreparedStatement ps = con.prepareStatement(UPDATE_ENTITY_ACTIVE_STATUS_SQL)) {

                ps.setBoolean(1, entity.isActive());
                ps.setInt(2, entity.getCustomerId());
                ps.setInt(3, entity.getProjectId());
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
}
