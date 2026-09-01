package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.views.basis.stakeholderrequirement.StakeholderRequirementExportRow;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.StakeholderRequirementParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.common.enums.entity.EntityType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StakeholderRequirementProvider extends EntityProvider {

    private final static EntityType entityType = EntityType.STAKEHOLDER_REQUIREMENT;

    public StakeholderRequirementProvider(WebSession webSession) {
        super(webSession);
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    public Entities getListOfBasisRequirements() throws SQLException {
        return getListOfEntitiesByProjectId(entityType);
    }

    public Entities getBasisRequirementInfo(Integer entityId, Integer version) throws SQLException {
        return getEntityByEntityId(entityType, entityId, version);
    }

    public Entities getBasisRequirementInfo(Integer parentEntityId) throws SQLException {
        return getEntityForCreate(entityType,  parentEntityId);
    }

    public Entities getBasisRequirementHistory(Integer entityId) throws SQLException {
        return getEntityHistoryByEntityId(entityType, entityId);
    }

    public List<StakeholderRequirementEntity> getAllStakeholderRequirement(boolean includeInactive)  {
        List<StakeholderRequirementEntity> listOfEntitiesForExport = new ArrayList<>();
        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords(entityType, includeInactive);
           for (EntityRecord entityRecord : entityRecords) {
                StakeholderRequirementEntity basisSystemRequirementEntity = new StakeholderRequirementEntity(getWebSession(), entityRecord);
                listOfEntitiesForExport.add(basisSystemRequirementEntity);
            }
            listOfEntitiesForExport.sort(Comparator.comparing(StakeholderRequirementEntity::getRequirementCodeString));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return listOfEntitiesForExport;
    }

    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows) {
        List<StakeholderRequirementExportRow> rowsList = (List<StakeholderRequirementExportRow>) rows;
        List<AbstractEntity> entities = new ArrayList<>();

        for (StakeholderRequirementExportRow stakeholderRequirementExportRow : rowsList) {
            StakeholderRequirementEntity entity = toEntity(webSession, stakeholderRequirementExportRow);
            entities.add(entity);
        }

        return entities;
    }

    private StakeholderRequirementEntity toEntity(WebSession webSession, StakeholderRequirementExportRow row) {
        StakeholderRequirementEntity entity = new StakeholderRequirementEntity(webSession);
        StakeholderRequirementParentCodeSelector codeSelector = new StakeholderRequirementParentCodeSelector(webSession);

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

        entity.setRequirementCode(requirementCode);
        entity.setRequirementCodeLevel(level);
        entity.setRequirementName(row.name());
        entity.setRequirementDescription(row.description());
        entity.setActive(row.active() == null || row.active());

        entity.addAllDataElements();

        return entity;
    }

}
