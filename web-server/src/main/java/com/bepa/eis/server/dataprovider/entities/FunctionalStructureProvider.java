package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.views.pro.functionalstructure.FunctionalStructureExportRow;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.FunctionalStructureParentCodeSelector;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.functional.FunctionalStructureEntity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FunctionalStructureProvider extends EntityProvider {

    private final static EntityType entityType = EntityType.FUNCTIONAL_STRUCTURE;

    public FunctionalStructureProvider(WebSession webSession) {
        super(webSession);
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    public Entities getListOfFunctionalStructures() throws SQLException {
        return getListOfEntitiesByProjectId(entityType);
    }

    public Entities getFunctionalStructureInfo(Integer entityId, Integer version) throws SQLException {
        return getEntityByEntityId(entityType, entityId, version);
    }

    public Entities getFunctionalStructureInfo(Integer parentEntityId) throws SQLException {
        return getEntityForCreate(entityType,  parentEntityId);
    }

    public Entities getFunctionalStructureHistory(Integer entityId) throws SQLException {
        return getEntityHistoryByEntityId(entityType, entityId);
    }

    public List<FunctionalStructureEntity> getAllFunctionalStructure(boolean includeInactive)  {
        List<FunctionalStructureEntity> listOfEntitiesForExport = new ArrayList<>();
        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords(entityType, includeInactive);
            for (EntityRecord entityRecord : entityRecords) {
                FunctionalStructureEntity functionalStructureEntity = new FunctionalStructureEntity(getWebSession(), entityRecord);
                listOfEntitiesForExport.add(functionalStructureEntity);
            }
            listOfEntitiesForExport.sort(Comparator.comparing(FunctionalStructureEntity::getFunctionalCodeString));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return listOfEntitiesForExport;
    }

    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows) {
        List<FunctionalStructureExportRow> rowsList = (List<FunctionalStructureExportRow>) rows;
        List<AbstractEntity> entities = new ArrayList<>();

        for (FunctionalStructureExportRow functionalStructureExportRow : rowsList) {
            FunctionalStructureEntity entity = toEntity(webSession, functionalStructureExportRow);
            entities.add(entity);
        }

        return entities;
    }

    private FunctionalStructureEntity toEntity(WebSession webSession, FunctionalStructureExportRow row) {
        FunctionalStructureEntity entity = new FunctionalStructureEntity(webSession);
        FunctionalStructureParentCodeSelector codeSelector = new FunctionalStructureParentCodeSelector(webSession);

        entity.setCustomerId(webSession.getCustomerId());
        entity.setProjectId(webSession.getProjectId());
        entity.setVersion(1);

        String requirementCode = row.id();

        if (requirementCode == null || requirementCode.isBlank()) {
            requirementCode = codeSelector.getNextAvailableCodeValue(webSession, "");
        }

        Integer level = row.level();

        if (level == null) {
            level = codeSelector.getCodeLevel(requirementCode);
        }

        entity.setFunctionalCode(requirementCode);
        entity.setFunctionalCodeLevel(level);
        entity.setFunctionalName(row.name());
        entity.setFunctionalDescription(row.description());
        entity.setActive(row.active() == null || row.active());

        entity.addAllDataElements();

        return entity;
    }

}
