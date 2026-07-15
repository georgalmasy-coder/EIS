package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.views.basis.stakeholder.StakeholderExportRow;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.StakeholderRequirementParentCodeSelector;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.stakeholder.StakeholderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StakeholderProvider extends EntityProvider {

    private static final Logger log = LoggerFactory.getLogger(StakeholderProvider.class);

    private final static EntityType entityType = EntityType.STAKEHOLDER;

    public StakeholderProvider(WebSession webSession) {
        super(webSession);
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    public Entities getListOfStakeholders() throws SQLException {
        return getListOfEntitiesByProjectId(entityType);
    }

    public Entities getStakeholderInfo(Integer entityId, Integer version) throws SQLException {
        return getEntityByEntityId(entityType, entityId, version);
    }

    public Entities getStakeholderInfo() throws SQLException {
        return getEntityForCreate(entityType, null);
    }

    public Entities getStakeholderHistory(Integer entityId) throws SQLException {
        return getEntityHistoryByEntityId(entityType, entityId);
    }

    public List<StakeholderEntity> getAllStakeholder(boolean includeInactive)  {
        List<StakeholderEntity> listOfEntitiesForExport = new ArrayList<>();
        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords(entityType, includeInactive);
            for (EntityRecord entityRecord : entityRecords) {
                StakeholderEntity stakeholderEntity = new StakeholderEntity(getWebSession(),entityRecord);
                listOfEntitiesForExport.add(stakeholderEntity);
            }
            listOfEntitiesForExport.sort(Comparator.comparing(StakeholderEntity::getStakeholderName));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return listOfEntitiesForExport;
    }


    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows) {
        List<StakeholderExportRow> rowsList = (List<StakeholderExportRow>) rows;
        List<AbstractEntity> entities = new ArrayList<>();

        for (StakeholderExportRow stakeholderExportRow : rowsList) {
            StakeholderEntity entity = toEntity(webSession, stakeholderExportRow);
            entities.add(entity);
        }

        return entities;
    }

    private StakeholderEntity toEntity(WebSession webSession, StakeholderExportRow row) {
        StakeholderEntity entity = new StakeholderEntity(webSession);
        StakeholderRequirementParentCodeSelector codeSelector = new StakeholderRequirementParentCodeSelector(webSession);

        entity.setCustomerId(webSession.getCustomerId());
        entity.setProjectId(webSession.getProjectId());
        entity.setVersion(1);

/* GFA
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


 */
        entity.addAllDataElements();

        return entity;
    }

}
