package com.bepa.eis.common.providers.entityrelation;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class RelationProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(RelationProvider.class);

    private static final String GET_ENTITY_RELATIONS_BY_ENTITY_ID_SQL_1 =
            "SELECT ER.EntityRelationPK, ER.EntityType, ER.EntityId,  ER.RelatedEntityType, ER.RelatedEntityId, ER.RelationType,  ER.Version, ER.Latest, ER.CreatedById, ER.CreatedTime " +
                    "FROM ENTITY_RELATIONS ER " +
                    "WHERE ER.CustomerId = ? " +
                    "AND ER.ProjectId = ? " +
                    "AND ER.EntityType =  ? " +
                    "AND ER.EntityId = ? " +
                    "AND ER.RelatedEntityType =  ? " +
                    "AND ER.RelatedEntityId = ? " +
                    "AND ER.Latest = 1 ";

    private static final String GET_ENTITY_RELATIONS_BY_ENTITY_ID_SQL_2 =
            "SELECT ER.EntityRelationPK, ER.EntityType as RelatedEntityType, ER.EntityId as RelatedEntityId,  ER.RelatedEntityType as EntityType, ER.RelatedEntityId as EntityId, ER.RelationType,  ER.Version, ER.Latest, ER.CreatedById, ER.CreatedTime " +
                    "FROM ENTITY_RELATIONS ER " +
                    "WHERE ER.CustomerId = ? " +
                    "AND ER.ProjectId = ? " +
                    "AND ER.RelatedEntityType =  ? " +
                    "AND ER.RelatedEntityId = ? " +
                    "AND ER.EntityType =  ? " +
                    "AND ER.EntityId = ? " +
                    "AND ER.Latest = 1 ";

    private static final String CLEAR_LATEST_ENTITY_RELATION_SQL =
            "UPDATE ENTITY_RELATIONS SET Latest=0 WHERE EntityRelationPK = ?";

    private static final String INSERT_ENTITY_RELATION_SQL =
            "INSERT INTO ENTITY_RELATIONS " +
                    "(CustomerId, ProjectId, EntityType, EntityId, RelatedEntityType, RelatedEntityId, RelationType, Version, CreatedById, CreatedTime, Latest) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
    public RelationProvider(WebSession webSession) {
        super(webSession);
    }


    public EntityRelationRecord getEntityRelationByEntityTypeAndId(EntityType entityType, Integer entityId, EntityType relatedEntityType, Integer relatedEntityId) throws SQLException {
        EntityRelationRecord entityRelationRecord = null;

        if (getWebSession() != null && getWebSession().getProjectId() != null) {
            entityRelationRecord = getEntityRelationsByEntityId(GET_ENTITY_RELATIONS_BY_ENTITY_ID_SQL_1, entityType, entityId, relatedEntityType, relatedEntityId);

            if (entityRelationRecord == null) {
                entityRelationRecord = getEntityRelationsByEntityId(GET_ENTITY_RELATIONS_BY_ENTITY_ID_SQL_2, entityType, entityId, relatedEntityType, relatedEntityId);
            }

        }
        return entityRelationRecord;
    }

    private EntityRelationRecord getEntityRelationsByEntityId(String sql, EntityType entityType, Integer entityId, EntityType relatedEntityType, Integer relatedEntityId) throws SQLException {

        EntityRelationRecord entityRelationRecord = null;

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, entityType.getId(), 3);
            setInt(ps, entityId, 4);
            setInt(ps, relatedEntityType.getId(), 5);
            setInt(ps, relatedEntityId, 6);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entityRelationRecord = new EntityRelationRecord(getWebSession().getCustomerId(), getWebSession().getProjectId());
                    entityRelationRecord.setEntityRelationPK(rs.getInt("EntityRelationPK"));
                    entityRelationRecord.setVersion(rs.getInt("Version"));
                    entityRelationRecord.setLatest(rs.getBoolean("Latest"));

                    entityRelationRecord.setRelationType(mapToRelationType(rs.getInt("RelationType")));

                    entityRelationRecord.setCreatedDate(rs.getTimestamp("CreatedTime"));
                    entityRelationRecord.setCreatedByUserId(rs.getInt("CreatedById"));

                    entityRelationRecord.setEntityType(mapToEntityType(rs.getInt("EntityType")));
                    entityRelationRecord.setEntityId(rs.getInt("EntityId"));
                    entityRelationRecord.setRelatedEntityType(mapToEntityType(rs.getInt("RelatedEntityType")));
                    entityRelationRecord.setRelatedEntityId(rs.getInt("RelatedEntityId"));

                }
            }
        }

        return entityRelationRecord;
    }


    public void clearLatestIfExists(EntityRelationRecord relationRecord) {

        if (relationRecord != null && relationRecord.getEntityRelationPK() != null) {

            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(CLEAR_LATEST_ENTITY_RELATION_SQL)) {

                setInt(ps, relationRecord.getEntityRelationPK(), 1);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    log.debug("Entity relation with PK {} removed successfully", relationRecord.getEntityRelationPK());
                } else {
                    log.warn("No entity relation found with PK {}", relationRecord.getEntityRelationPK());
                }
            } catch (SQLException e) {
                log.error("Error removing entity relation with PK: {}", relationRecord.getEntityRelationPK(), e);
            }

        }

    }

    public void insertRelationRecord(RelationType relationType, EntityRelationRecord relationRecord) {

        if (relationRecord != null && relationType != null) {

            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(INSERT_ENTITY_RELATION_SQL)) {

                setInt(ps, getWebSession().getCustomerId(), 1);
                setInt(ps, getWebSession().getProjectId(), 2);
                setInt(ps, relationRecord.getEntityType().getId(), 3);
                setInt(ps, relationRecord.getEntityId(), 4);
                setInt(ps, relationRecord.getRelatedEntityType().getId(), 5);
                setInt(ps, relationRecord.getRelatedEntityId(), 6);
                setInt(ps, relationType.getId(), 7);
                setInt(ps, relationRecord.getNextVersion(), 8);

                setInt(ps, getWebSession().getUserId(), 9);
                setTimestamp(ps, new Timestamp(System.currentTimeMillis()), 10);
                setBoolean(ps, true, 11);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    log.debug("Entity relation successfully inserted {}", relationRecord);
                } else {
                    log.warn("No Entity relation inserted {}", relationRecord);
                }
            } catch (SQLException e) {
                log.error("Error inserting entity relation : {} {}", relationRecord, e);
            }

        }

    }

    private EntityType mapToEntityType(Integer entityTypeId) {
        return EntityType.fromId(entityTypeId);
    }

    private RelationType mapToRelationType(Integer relationTypeId) {
        return RelationType.valueOf(relationTypeId);
    }
}