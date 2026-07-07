package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.common.providers.entityrelation.RelationProvider;
import com.bepa.eis.server.dataprovider.fields.integers.ids.*;
import com.bepa.eis.server.dataprovider.fields.lookups.common.CreatedBy;
import com.bepa.eis.server.dataprovider.fields.lookups.common.EntityRelationType;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.timestamp.CreatedDateTime;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EntityRelationProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(EntityRelationProvider.class);

    private static final String GET_ENTITY_RELATIONS_BY_ENTITY_ID_SQL_1 =
            "SELECT ER.EntityRelationPK, ER.RelationType, ER.EntityType, ER.EntityId,  ER.RelatedEntityType, ER.RelatedEntityId, ER.CreatedById, ER.CreatedTime " +
            "FROM ENTITY E, ENTITY_RELATIONS ER " +
            "WHERE E.CustomerId = ER.CustomerId " +
            "AND E.ProjectId = ER.ProjectId " +
            "AND E.EntityType = ER.EntityType " +
            "AND E.EntityId = ER.EntityId " +
            "AND E.CustomerId = ? " +
            "AND E.ProjectId = ? " +
            "AND ER.EntityType =  ? " +
            "AND ER.EntityId = ? " +
            "AND ER.RelationType IN (1,2) " + // confirmed / not relevant
            "AND ER.Latest = 1 " +
            "AND E.Latest = 1 ";

    private static final String GET_ENTITY_RELATIONS_BY_ENTITY_ID_SQL_2 =
            "SELECT ER.EntityRelationPK, ER.RelationType, ER.EntityType AS RelatedEntityType, ER.EntityId AS RelatedEntityId, ER.RelatedEntityType AS EntityType, ER.RelatedEntityId AS EntityId, ER.CreatedById, ER.CreatedTime " +
            "FROM ENTITY E, ENTITY_RELATIONS ER " +
            "WHERE E.CustomerId = ER.CustomerId " +
            "AND E.ProjectId = ER.ProjectId " +
            "AND E.EntityType = ER.RelatedEntityType " +
            "AND E.EntityId = ER.RelatedEntityId " +
            "AND E.CustomerId = ? " +
            "AND E.ProjectId = ? " +
            "AND ER.RelatedEntityType =  ? " +
            "AND ER.RelatedEntityId = ? " +
            "AND ER.RelationType IN (1,2) " + // confirmed / not relevant
            "AND ER.Latest = 1 " +
            "AND E.Latest = 1 ";

    private static final String GET_CONFIRMED_AND_NOT_RELEVANT_ENTITY_RELATIONS_BY_PROJECT_ID_SQL =
            "SELECT ER.EntityRelationPK, ER.RelationType, E1.EntityType, E1.EntityId,  E2.EntityType AS RelatedEntityType, E2.EntityId AS RelatedEntityId, ER.CreatedById, ER.CreatedTime " +
                    "FROM ENTITY E1, ENTITY_RELATIONS ER, ENTITY E2  " +
                    "WHERE E1.CustomerId = ER.CustomerId " +
                    "AND E1.CustomerId = E2.CustomerId " +

                    "AND E1.ProjectId = ER.ProjectId " +
                    "AND E1.ProjectId = E2.ProjectId " +

                    "AND E1.EntityType = ER.EntityType " +
                    "AND E1.EntityId = ER.EntityId " +

                    "AND E2.EntityType = ER.RelatedEntityType " +
                    "AND E2.EntityId = ER.RelatedEntityId " +

                    "AND E1.CustomerId = ? " +
                    "AND E1.ProjectId = ? " +

                    "AND ((E1.EntityType =  ? AND E2.EntityType = ?) OR (E1.EntityType =  ? AND E2.EntityType = ?)) " +

//                    "AND E1.EntityType =  ? " +
//                    "AND E2.EntityType = ? " +

                    "AND E1.Active = 1 " +
                    "AND E2.Active = 1 " +
                    "AND ER.Latest = 1 " +
                    "AND ER.RelationType IN (1,2) " + // confirmed / not relevant
                    "AND E1.Latest = 1 " +
                    "AND E2.Latest = 1 ";

    private static final String GET_CONFIRMED_ENTITY_RELATIONS_BY_PROJECT_ID_SQL =
            "SELECT ER.EntityRelationPK, ER.RelationType, E1.EntityType, E1.EntityId,  E2.EntityType AS RelatedEntityType, E2.EntityId AS RelatedEntityId, ER.CreatedById, ER.CreatedTime " +
                    "FROM ENTITY E1, ENTITY_RELATIONS ER, ENTITY E2  " +
                    "WHERE E1.CustomerId = ER.CustomerId " +
                    "AND E1.CustomerId = E2.CustomerId " +

                    "AND E1.ProjectId = ER.ProjectId " +
                    "AND E1.ProjectId = E2.ProjectId " +

                    "AND E1.EntityType = ER.EntityType " +
                    "AND E1.EntityId = ER.EntityId " +

                    "AND E2.EntityType = ER.RelatedEntityType " +
                    "AND E2.EntityId = ER.RelatedEntityId " +

                    "AND E1.CustomerId = ? " +
                    "AND E1.ProjectId = ? " +

                    "AND ((E1.EntityType =  ? AND E2.EntityType = ?) OR (E1.EntityType =  ? AND E2.EntityType = ?)) " +

//                    "AND E1.EntityType =  ? " +
//                    "AND E2.EntityType = ? " +

                    "AND E1.Active = 1 " +
                    "AND E2.Active = 1 " +
                    "AND ER.Latest = 1 " +
                    "AND ER.RelationType = 1 " + // confirmed
                    "AND E1.Latest = 1 " +
                    "AND E2.Latest = 1 ";

    private static final String GET_ENTITY_INFO_BY_ENTITY_ID_SQL =
        "SELECT EE.StringValue " +
        "FROM ENTITY E, ENTITY_ELEMENT EE " +
        "WHERE E.CustomerId = EE.CustomerId " +
        "AND E.ProjectId = EE.ProjectId " +
        "AND E.EntityType = EE.EntityType " +
        "AND E.EntityId = EE.EntityId " +
        "AND E.Version = EE.Version " +

        "AND E.CustomerId = ? " +
        "AND E.ProjectId = ? " +
        "AND E.EntityType = ? " +
        "AND E.EntityId = ? " +
        "and EE.EntityDataElementType = ?";

    public EntityRelationProvider(WebSession webSession) {
        super(webSession);
    }

    public EntityRelations getEntityRelationsByEntityId(EntityType currEntityType, Integer currEntityId) throws SQLException {

        EntityRelations entityRelations = new EntityRelations(getWebSession());

        if (getWebSession() != null && getWebSession().getProjectId() != null) {
            findRelationsToOtherEntities(GET_ENTITY_RELATIONS_BY_ENTITY_ID_SQL_1, entityRelations, currEntityType, currEntityId);
            findRelationsToOtherEntities(GET_ENTITY_RELATIONS_BY_ENTITY_ID_SQL_2, entityRelations, currEntityType, currEntityId);
        }
        return entityRelations;
    }

    public List<EntityRelationRecord> getAllConfirmedAndNotRelevantEntityRelationRecordsByProjectId(EntityType currEntityType, EntityType relatedEntityType) throws SQLException {

        List<EntityRelationRecord> entityRelationRecordList = new ArrayList<>();

        if (getWebSession() != null && getWebSession().getProjectId() != null) {

            findRelationsToOtherEntities(GET_CONFIRMED_AND_NOT_RELEVANT_ENTITY_RELATIONS_BY_PROJECT_ID_SQL, entityRelationRecordList, relatedEntityType, currEntityType);
            log.debug("Number of relations found : {}", entityRelationRecordList.size());
        }

        return entityRelationRecordList;
    }

    public List<EntityRelationRecord> getAllConfirmedEntityRelationRecordsByProjectId(EntityType currEntityType, EntityType relatedEntityType) throws SQLException {

        List<EntityRelationRecord> entityRelationRecordList = new ArrayList<>();

        if (getWebSession() != null && getWebSession().getProjectId() != null) {

            findRelationsToOtherEntities(GET_CONFIRMED_ENTITY_RELATIONS_BY_PROJECT_ID_SQL, entityRelationRecordList, relatedEntityType, currEntityType);
            log.debug("Number of relations found : {}", entityRelationRecordList.size());
        }

        return entityRelationRecordList;
    }

    private void findRelationsToOtherEntities(String sql, EntityRelations entityRelations, EntityType entityType, Integer currEntityId) throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, entityType.getId(), 3);
            setInt(ps, currEntityId, 4);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    EntityRelation entityRelation = entityRelations.getNewEntityRelation();


                    if (currEntityId != rs.getInt(EntityId.FIELD_NAME)) {
                        throw new SQLException("EntityRelationProvider: currEntityId != rs.getInt(EntityId.FIELD_NAME)");
                    }

                    entityRelation.setEntityRelationPK(rs.getInt(EntityRelationPK.FIELD_NAME));
                    entityRelation.setEntityId(rs.getInt(EntityId.FIELD_NAME));
                    entityRelation.setEntityTypeId(rs.getInt(EntityTypeId.FIELD_NAME));
                    entityRelation.setRelatedEntityId(rs.getInt(RelatedEntityId.FIELD_NAME));
                    entityRelation.setRelatedEntityTypeId(rs.getInt(RelatedEntityTypeId.FIELD_NAME));
                    entityRelation.setCreatedBy(getWebSession(), rs.getInt(CreatedBy.FIELD_NAME));
                    entityRelation.setCreatedDateTime(rs.getTimestamp(CreatedDateTime.FIELD_NAME));
                    addRelatedEntityCodeColumn(entityRelation, entityRelation.getRelatedEntityType(),  entityRelation.getRelatedEntityId());
                    addRelatedEntityNameColumn(entityRelation, entityRelation.getRelatedEntityType(),  entityRelation.getRelatedEntityId());

                    entityRelation.setRelationType(RelationType.valueOf(rs.getInt(EntityRelationType.FIELD_NAME)));
                    entityRelations.addEntityRelation(entityRelation);
                }
            }
        }
    }

    private void findRelationsToOtherEntities(String sql,  List<EntityRelationRecord> entityRelationRecordList, EntityType currEntityType, EntityType relatedEntityType) throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, currEntityType.getId(), 3);
            setInt(ps, relatedEntityType.getId(), 4);
            setInt(ps, relatedEntityType.getId(), 5);
            setInt(ps, currEntityType.getId(), 6);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    EntityRelationRecord relationRecord  = new EntityRelationRecord(getWebSession().getCustomerId(), getWebSession().getProjectId());

                    relationRecord.setEntityId(rs.getInt(EntityId.FIELD_NAME));
                    relationRecord.setEntityType(EntityType.fromId(rs.getInt(EntityTypeId.FIELD_NAME)));

                    relationRecord.setRelatedEntityId(rs.getInt(RelatedEntityId.FIELD_NAME));
                    relationRecord.setRelatedEntityType(EntityType.fromId(rs.getInt(RelatedEntityTypeId.FIELD_NAME)));

                    relationRecord.setCreatedByUserId(rs.getInt(CreatedBy.FIELD_NAME));
                    relationRecord.setCreatedDate(rs.getTimestamp(CreatedDateTime.FIELD_NAME));
                    relationRecord.setRelationType(RelationType.valueOf(rs.getInt(EntityRelationType.FIELD_NAME)));
                    entityRelationRecordList.add(relationRecord);
                }
            }
        }
    }

    private void addRelatedEntityCodeColumn(EntityRelation entityRelation, EntityType relatedEntityType,  RelatedEntityId relatedEntityId) throws SQLException {
        if (entityRelation != null && relatedEntityType != null && relatedEntityId != null) {
            EntityDataElement entityCodeColumn = relatedEntityType.getEntityCodeColumn();
            if (entityCodeColumn != null) {
                RelatedEntityCode code = new RelatedEntityCode();
                String value = getEntityInfo(relatedEntityType,  relatedEntityId, entityCodeColumn);
                code.setValue(value);
                entityRelation.addElement(code);
            }
        }
    }

    private void addRelatedEntityNameColumn(EntityRelation entityRelation, EntityType relatedEntityType,  RelatedEntityId relatedEntityId) throws SQLException {

        if (entityRelation != null && relatedEntityType != null && relatedEntityId != null) {
            EntityDataElement entityNameColumn = relatedEntityType.getEntityNameColumn();
            if (entityNameColumn != null) {
                RelatedEntityName name = new RelatedEntityName();
                String value = getEntityInfo(relatedEntityType,  relatedEntityId, entityNameColumn);
                name.setValue(value);
                entityRelation.addElement(name);
            }
        }
    }

    private String getEntityInfo(EntityType relatedEntityType,  RelatedEntityId relatedEntityId, EntityDataElement dataElement) throws SQLException {
        String value = "";
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_ENTITY_INFO_BY_ENTITY_ID_SQL)) {

            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, relatedEntityType.getId(), 3);
            setInt(ps, relatedEntityId.getValue(), 4);
            setInt(ps, dataElement.getId(), 5);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    value = rs.getString("StringValue");
                }
            }
        }

        return value;
    }

    public void insertEntityRelations(Connection con, AbstractEntity entity) throws SQLException {

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

}
