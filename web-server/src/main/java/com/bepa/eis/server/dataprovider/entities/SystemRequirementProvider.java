package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.views.basis.systemrequirement.SystemRequirementExportRow;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;
import com.bepa.eis.common.enums.entity.EntityType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SystemRequirementProvider extends EntityProvider {

    private final static EntityType entityType = EntityType.SYSTEM_REQUIREMENT;

    public SystemRequirementProvider(WebSession webSession) {
        super(webSession);
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    public Entities getListOfSystemRequirements() throws SQLException {
        return getListOfEntitiesByProjectId(getEntityType());
    }

    public Entities getSystemRequirementInfo(Integer entityId, Integer version) throws SQLException {
        return getEntityByEntityId(getEntityType(), entityId, version);
    }

    public Entities getSystemRequirementInfo(Integer parentEntityId) throws SQLException {
        return getEntityForCreate(getEntityType(), parentEntityId);
    }

    public Entities getSystemRequirementHistory(Integer entityId) throws SQLException {
        return getEntityHistoryByEntityId(getEntityType(), entityId);
    }

    public List<SystemRequirementEntity> getAllSystemRequirement(boolean includeInactive)  {
        List<SystemRequirementEntity> listOfEntitiesForExport = new ArrayList<>();

        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords( getEntityType(), includeInactive);
            for (EntityRecord entityRecord : entityRecords) {
                SystemRequirementEntity systemRequirementEntity = new SystemRequirementEntity(getWebSession(), entityRecord);
                listOfEntitiesForExport.add(systemRequirementEntity);
            }

            listOfEntitiesForExport.sort(Comparator.comparing(SystemRequirementEntity::getCode));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return listOfEntitiesForExport;
    }

    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows) {
        List<SystemRequirementExportRow> rowsList = (List<SystemRequirementExportRow>) rows;
        List<AbstractEntity> entities = new ArrayList<>();

        for (SystemRequirementExportRow systemBreakdownExportRow : rowsList) {
            SystemRequirementEntity entity = toEntity(webSession, systemBreakdownExportRow);
            entities.add(entity);
        }

        return entities;
    }

    private SystemRequirementEntity toEntity(WebSession webSession, SystemRequirementExportRow row) {
        SystemRequirementEntity entity = new SystemRequirementEntity(webSession);

        entity.setCustomerId(webSession.getCustomerId());
        entity.setProjectId(webSession.getProjectId());
        entity.setVersion(1);

        String requirementCode = row.id();
        Integer level = row.level();

        entity.setRequirementCode(requirementCode);
        entity.setRequirementCodeLevel(level);
        entity.setRequirementName(row.name());
        entity.setRequirementDescription(row.description());
        entity.setActive(row.active() == null || row.active());

        entity.addAllDataElements();

        return entity;
    }

}
