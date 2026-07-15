package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.TrlRecord;
import com.bepa.eis.server.api.web.application.views.basis.systemsbreakdown.SystemBreakdownExportRow;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.lookups.system.*;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.bepa.eis.common.enums.entity.EntityDataElement.DEADLINENEXTTRL;
import static com.bepa.eis.common.enums.entity.EntityDataElement.TRLID;

public class SystemBreakdownProvider extends EntityProvider {

    private static final Logger log = LoggerFactory.getLogger(SystemBreakdownProvider.class);

    private final static EntityType entityType = EntityType.SYSTEMS_BREAKDOWN;

    private static final String GET_ALL_ACTIVE_TRL_RECORDS_SQL =
		"SELECT E.CustomerId, E.ProjectId, E.EntityId, " +
                "(SELECT TOP 1 IntegerValue " +
                " FROM ENTITY_ELEMENT EE " +
                " WHERE EE.CustomerId = E.CustomerId " +
                " AND EE.ProjectId = E.ProjectId " +
                " AND EE.EntityId = E.EntityId " +
                " AND EE.EntityType = E.EntityType " +
                " AND EE.Version = E.Version " +
                " AND EE.EntityDataElementType = ? " +
                ") AS Trl, " +
                "(SELECT TOP 1 LocalDateValue " +
                " FROM ENTITY_ELEMENT EE " +
                " WHERE EE.CustomerId = E.CustomerId " +
                " AND EE.ProjectId = E.ProjectId " +
                " AND EE.EntityId = E.EntityId " +
                " AND EE.EntityType = E.EntityType " +
                " AND EE.Version = E.Version " +
                " AND EE.EntityDataElementType = ? " +
                ") AS DeadlineNextTRL " +
                "FROM ENTITY E " +
                "WHERE E.CustomerId = ? " +
                "AND E.ProjectId = ?  " +
                "AND E.EntityType = ? " +
                "AND E.Latest = 1 " +
                "AND E.Active = 1 ";

    public SystemBreakdownProvider(WebSession webSession) {
        super(webSession);
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    public Entities getListOfSystemsBreakdown() throws SQLException {
        return getListOfEntitiesByProjectId(getEntityType());
    }

    public Entities getSystemBreakdownInfo(Integer entityId, Integer version) throws SQLException {
        return getEntityByEntityId(getEntityType(), entityId, version);
    }

    public Entities getSystemBreakdownInfo(Integer parentEntityId) throws SQLException {
        return getEntityForCreate(getEntityType(),  parentEntityId);
    }

    public Entities getSystemBreakdownHistory(Integer entityId) throws SQLException {
        return getEntityHistoryByEntityId(getEntityType(), entityId);
    }

    public List<SystemBreakdownEntity> findAllForExport(boolean includeInactive)  {
        List<SystemBreakdownEntity> listOfEntitiesForExport = new ArrayList<>();
        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords(entityType, includeInactive);
            for (EntityRecord entityRecord : entityRecords) {
                SystemBreakdownEntity systemBreakdownEntity = new SystemBreakdownEntity(getWebSession(), entityRecord);
                listOfEntitiesForExport.add(systemBreakdownEntity);
            }
            listOfEntitiesForExport.sort(Comparator.comparing(SystemBreakdownEntity::getCode));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return listOfEntitiesForExport;
    }

    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows) {
        List<SystemBreakdownExportRow> rowsList = (List<SystemBreakdownExportRow>) rows;
        List<AbstractEntity> entities = new ArrayList<>();

        for (SystemBreakdownExportRow systemBreakdownExportRow : rowsList) {
            SystemBreakdownEntity entity = toEntity(webSession, systemBreakdownExportRow);
            entities.add(entity);
        }

        return entities;
    }

    private SystemBreakdownEntity toEntity(WebSession webSession, SystemBreakdownExportRow row) {
        SystemBreakdownEntity entity = new SystemBreakdownEntity(webSession);

        entity.setCustomerId(webSession.getCustomerId());
        entity.setProjectId(webSession.getProjectId());
        entity.setVersion(1);

        String requirementCode = row.id();

        Integer level = row.level();

        entity.setSbsCode(requirementCode);
        entity.setSbsCodeLevel(level);
        entity.setSystemName(row.name());
        entity.setActive(row.active() == null || row.active());

        entity.addAllDataElements();

        return entity;
    }

    public List<TrlRecord> getListOfTrlRecords(Integer customerId, Integer projectId) {

        List<TrlRecord> trlRecords = new ArrayList<>();

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_ALL_ACTIVE_TRL_RECORDS_SQL)) {

            ps.setInt(1, TRLID.getId());
            ps.setInt(2, DEADLINENEXTTRL.getId());

            ps.setInt(3, customerId);
            ps.setInt(4, projectId);
            ps.setInt(5, entityType.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                TrlRecord trlRecord = new TrlRecord(
                        rs.getInt("CustomerId"),
                        rs.getInt("ProjectId"),
                        rs.getInt("EntityId"),
                        rs.getInt("Trl"),
                        rs.getTimestamp("DeadlineNextTrl")
                        );
                trlRecords.add(trlRecord);
            }

        } catch (SQLException e) {
            log.error("Error loading all entities including Code : {}", e.getMessage());
        }
        return trlRecords;
    }

    public List<SystemBreakdownEntity> getAllSystemBreakdown(boolean includeInactive)  {
        List<SystemBreakdownEntity> listOfEntities = new ArrayList<>();

        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords(entityType, includeInactive);
            for (EntityRecord entityRecord : entityRecords) {
                SystemBreakdownEntity systemRequirementEntity = new SystemBreakdownEntity(getWebSession(), entityRecord);
                listOfEntities.add(systemRequirementEntity);
            }

            listOfEntities.sort(Comparator.comparing(SystemBreakdownEntity::getCode));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return listOfEntities;
    }


}
