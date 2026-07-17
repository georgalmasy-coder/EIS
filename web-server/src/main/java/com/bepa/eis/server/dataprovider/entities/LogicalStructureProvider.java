package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.views.pro.logicalstructure.LogicalStructureExportRow;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.LogicalStructureParentCodeSelector;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.logical.LogicalStructureEntity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LogicalStructureProvider extends EntityProvider {

    private final static EntityType entityType = EntityType.LOGICAL_STRUCTURE;

    public LogicalStructureProvider(WebSession webSession) {
        super(webSession);
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    public Entities getListOfLogicalStructures() throws SQLException {
        return getListOfEntitiesByProjectId(entityType);
    }

    public Entities getLogicalStructureInfo(Integer entityId, Integer version) throws SQLException {
        return getEntityByEntityId(entityType, entityId, version);
    }

    public Entities getLogicalStructureInfo(Integer parentEntityId) throws SQLException {
        return getEntityForCreate(entityType,  parentEntityId);
    }

    public Entities getLogicalStructureHistory(Integer entityId) throws SQLException {
        return getEntityHistoryByEntityId(entityType, entityId);
    }

    public List<LogicalStructureEntity> getAllLogicalStructures(boolean includeInactive)  {
        List<LogicalStructureEntity> listOfEntitiesForExport = new ArrayList<>();
        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords(entityType, includeInactive);
            for (EntityRecord entityRecord : entityRecords) {
                LogicalStructureEntity logicalStructureEntity = new LogicalStructureEntity(getWebSession(), entityRecord);
                listOfEntitiesForExport.add(logicalStructureEntity);
            }
            listOfEntitiesForExport.sort(Comparator.comparing(LogicalStructureEntity::getCode));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return listOfEntitiesForExport;
    }

    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows) {
        List<LogicalStructureExportRow> rowsList = (List<LogicalStructureExportRow>) rows;
        List<AbstractEntity> entities = new ArrayList<>();

        for (LogicalStructureExportRow logicalStructureExportRow : rowsList) {
            LogicalStructureEntity entity = toEntity(webSession, logicalStructureExportRow);
            entities.add(entity);
        }

        return entities;
    }

    private LogicalStructureEntity toEntity(WebSession webSession, LogicalStructureExportRow row) {
        LogicalStructureEntity entity = new LogicalStructureEntity(webSession);
        LogicalStructureParentCodeSelector codeSelector = new LogicalStructureParentCodeSelector(webSession);

        entity.setCustomerId(webSession.getCustomerId());
        entity.setProjectId(webSession.getProjectId());
        entity.setVersion(1);

        String logicalCode = row.id();

        if (logicalCode == null || logicalCode.isBlank()) {
            logicalCode = codeSelector.getNextAvailableCodeValue(webSession, "");
        }

        Integer level = row.level();

        if (level == null) {
            level = codeSelector.getCodeLevel(logicalCode);
        }

        entity.setLogicalCode(logicalCode);
        entity.setLogicalCodeLevel(level);
        entity.setLogicalName(row.name());
        entity.setLogicalDescription(row.description());
        entity.setActive(row.active() == null || row.active());

        entity.addAllDataElements();

        return entity;
    }

}
