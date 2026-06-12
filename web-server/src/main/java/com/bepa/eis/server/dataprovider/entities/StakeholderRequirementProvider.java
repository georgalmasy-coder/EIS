package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.views.basis.stakeholder.StakeholderRequirementExportRow;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.StakeholderRequirementParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.bepa.eis.common.enums.entity.EntityDataElement.*;

public class StakeholderRequirementProvider extends EntityProvider {

    private final static EntityType entityType = EntityType.STAKEHOLDER_REQUIREMENT;

    public StakeholderRequirementProvider(WebSession webSession) {
        super(webSession);
    }

    public Entities getListOfBasisRequirements() throws SQLException {
        return getListOfEntitiesByProjectId(entityType, getEntityDataElementForList());
    }

    public Entities getBasisRequirementInfo(Integer entityId, Integer version) throws SQLException {
        return getEntityByEntityId(entityType, entityId, version, getEntityDataElementForEdit());
    }

    public Entities getBasisRequirementInfo(Integer parentEntityId) throws SQLException {
        return getEntityForCreate(entityType,  parentEntityId);
    }

    public Entities getBasisRequirementHistory(Integer entityId) throws SQLException {
        return getEntityHistoryByEntityId(entityType, entityId);
    }

    @Override
    public EntityDataElement[] getEntityDataElementForList() {
        return new EntityDataElement[]{
                BASISREQCODE,
                CODELEVEL,
                REQNAME,
                REQDESCRIPTION};
    }

    @Override
    public EntityDataElement[] getEntityDataElementForEdit() {
        return new EntityDataElement[]{
                BASISREQCODE,
                CODELEVEL,
                REQNAME,
                REQDESCRIPTION};
    }

    @Override
    public EntityDataElement[] getEntityDataElementForCreate() {
        return new EntityDataElement[]{
                REQNAME,
                REQDESCRIPTION};
    }

    @Override
    public void addAllFieldElementsForList(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity) {
        StakeholderRequirementCode requirementCode = (StakeholderRequirementCode) mapOfLoadedFields.get(EntityDataElement.BASISREQCODE.getId());
        if (requirementCode == null) {
            requirementCode = new StakeholderRequirementCode();
        }
        requirementCode.setTableWidth("50px");
        entity.addElement(requirementCode);
        entity.setSortKey(requirementCode.getValue());

        CodeLevel codeLevel = (CodeLevel) mapOfLoadedFields.get(EntityDataElement.CODELEVEL.getId());
        if (codeLevel == null) {
            codeLevel = new CodeLevel();
        }
        codeLevel.setTableWidth("35px");
        entity.addElement(codeLevel);

        RequirementName requirementName = (RequirementName) mapOfLoadedFields.get(EntityDataElement.REQNAME.getId());
        if (requirementName == null) {
            requirementName = new RequirementName();
        }
        entity.addElement(requirementName);

        RequirementDescription requirementDescription = (RequirementDescription) mapOfLoadedFields.get(REQDESCRIPTION.getId());
        if (requirementDescription == null) {
            requirementDescription = new RequirementDescription();
        }
        entity.addElement(requirementDescription);

    }

    @Override
    public void addAllFieldElementsForEdit(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity) {
        StakeholderRequirementCode requirementCode = (StakeholderRequirementCode) mapOfLoadedFields.get(EntityDataElement.BASISREQCODE.getId());
        if (requirementCode == null) {
            requirementCode = new StakeholderRequirementCode();
        }
        requirementCode.setFieldNotEditable();
        requirementCode.setFieldNotRequired();
        entity.addElement(requirementCode);

        CodeLevel codeLevel = (CodeLevel) mapOfLoadedFields.get(EntityDataElement.CODELEVEL.getId());
        if (codeLevel == null) {
            codeLevel = new CodeLevel();
        }
        codeLevel.setFieldNotVisible();
        entity.addElement(codeLevel);

        RequirementName requirementName = (RequirementName) mapOfLoadedFields.get(EntityDataElement.REQNAME.getId());
        if (requirementName == null) {
            requirementName = new RequirementName();
        }
        requirementName.setFieldEditable();
        requirementName.setFieldRequired();
        entity.addElement(requirementName);

        RequirementDescription requirementDescription = (RequirementDescription) mapOfLoadedFields.get(EntityDataElement.REQDESCRIPTION.getId());
        if (requirementDescription == null) {
            requirementDescription = new RequirementDescription();
        }
        requirementDescription.setFieldEditable();
        requirementDescription.setFieldRequired();
        entity.addElement(requirementDescription);

    }

    @Override
    public void addAllFieldElementsForCreate(WebSession webSession, Entity entity, Integer parentEntityId) {

        StakeholderRequirementParentCodeSelector parentCodeSelector = new StakeholderRequirementParentCodeSelector(webSession);

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(webSession, parentEntityId);
        StakeholderRequirementCode requirementCode = new StakeholderRequirementCode(true);
        requirementCode.setValue(nextCode);
        requirementCode.setFieldNotEditable();
        requirementCode.setFieldRequired();
        entity.addElement(requirementCode);


        RequirementName requirementName = new RequirementName();
        requirementName.setFieldEditable();
        requirementName.setFieldRequired();
        entity.addElement(requirementName);

        RequirementDescription requirementDescription = new RequirementDescription();
        requirementDescription.setFieldEditable();
        requirementDescription.setFieldRequired();
        entity.addElement(requirementDescription);
    }

    public List<StakeholderRequirementEntity> getAllStakeholderRequirement(boolean includeInactive)  {
        List<StakeholderRequirementEntity> listOfEntitiesForExport = new ArrayList<>();
        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords(entityType, includeInactive);
            for (EntityRecord entityRecord : entityRecords) {
                StakeholderRequirementEntity basisSystemRequirementEntity = StakeholderRequirementEntity.map(entityRecord);
                listOfEntitiesForExport.add(basisSystemRequirementEntity);
            }
            listOfEntitiesForExport.sort(Comparator.comparing(StakeholderRequirementEntity::getRequirementCode));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return listOfEntitiesForExport;
    }

    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows) throws SQLException {
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
