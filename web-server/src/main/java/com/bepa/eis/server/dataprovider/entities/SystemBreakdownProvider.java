package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.views.project.systembreakdown.SystemBreakdownExportRow;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.SystemsBreakdownParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.system.*;
import com.bepa.eis.server.dataprovider.fields.strings.SBSCode;
import com.bepa.eis.server.dataprovider.fields.strings.SystemName;
import com.bepa.eis.server.dataprovider.fields.timestamp.DeadlineFinalized;
import com.bepa.eis.server.dataprovider.fields.timestamp.DeadlineNextTRL;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.bepa.eis.common.enums.entity.EntityDataElement.*;
import static com.bepa.eis.common.enums.entity.EntityDataElement.DEADLINEFINALIZED;
import static com.bepa.eis.common.enums.entity.EntityDataElement.DEADLINENEXTTRL;
import static com.bepa.eis.common.enums.entity.EntityDataElement.DEPARTMENTID;
import static com.bepa.eis.common.enums.entity.EntityDataElement.SYSTEMOWNERID;
import static com.bepa.eis.common.enums.entity.EntityDataElement.TRLID;

public class SystemBreakdownProvider extends EntityProvider {

    private final static EntityType entityType = EntityType.SYSTEMS_BREAKDOWN;

    public SystemBreakdownProvider(WebSession webSession) {
        super(webSession);
    }

    public Entities getListOfSystemsBreakdown() throws SQLException {
        return getListOfEntitiesByProjectId(EntityType.SYSTEMS_BREAKDOWN, getEntityDataElementForList());
    }

    public Entities getSystemBreakdownInfo(Integer entityId, Integer version) throws SQLException {
        return getEntityByEntityId(EntityType.SYSTEMS_BREAKDOWN, entityId, version, getEntityDataElementForEdit());
    }

    public Entities getSystemBreakdownInfo(Integer parentEntityId) throws SQLException {
        return getEntityForCreate(EntityType.SYSTEMS_BREAKDOWN,  parentEntityId);
    }

    public Entities getSystemBreakdownHistory(Integer entityId) throws SQLException {
        return getEntityHistoryByEntityId(EntityType.SYSTEMS_BREAKDOWN, entityId);
    }

    @Override
    public EntityDataElement[] getEntityDataElementForList() {

        return new EntityDataElement[]{
                SBSCODE,
                CODELEVEL,
                SYSTEMNAME,
                SYSTEMOWNERID,
                DEPARTMENTID,
                TRLID,
                DEADLINENEXTTRL,
                DEADLINEFINALIZED};
    }

    @Override
    public EntityDataElement[] getEntityDataElementForEdit() {

        return new EntityDataElement[]{
                SBSCODE,
                CODELEVEL,
                SYSTEMNAME,
                SYSTEMOWNERID,
                DEPARTMENTID,
                TRLID,
                DEADLINENEXTTRL,
                DEADLINEFINALIZED};
    }

    @Override
    public EntityDataElement[] getEntityDataElementForCreate() {

        return new EntityDataElement[]{
                SYSTEMNAME,
                SYSTEMOWNERID,
                DEPARTMENTID,
                TRLID,
                DEADLINENEXTTRL,
                DEADLINEFINALIZED};
    }

    @Override
    public void addAllFieldElementsForList(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity) {
        SBSCode sbsCode = (SBSCode) mapOfLoadedFields.get(EntityDataElement.SBSCODE.getId());
        if (sbsCode != null) {
            sbsCode.setTableWidth("95px");
            entity.addElement(sbsCode);
            entity.setSortKey(sbsCode.getValue());
        }

        CodeLevel codeLevel = (CodeLevel) mapOfLoadedFields.get(EntityDataElement.CODELEVEL.getId());
        if (codeLevel != null) {
            codeLevel.setTableWidth("95px");
            entity.addElement(codeLevel);
        }

        SystemName systemName = (SystemName) mapOfLoadedFields.get(EntityDataElement.SYSTEMNAME.getId());
        if (systemName != null) {
            entity.addElement(systemName);
        }

        SystemOwner systemOwner = (SystemOwner) mapOfLoadedFields.get(EntityDataElement.SYSTEMOWNERID.getId());
        if (systemOwner != null) {
            entity.addElement(systemOwner);
        }

        SystemDepartment systemDepartment = (SystemDepartment) mapOfLoadedFields.get(DEPARTMENTID.getId());
        if (systemDepartment != null) {
            entity.addElement(systemDepartment);
        }

        TRL trl = (TRL) mapOfLoadedFields.get(EntityDataElement.TRLID.getId());
        if (trl != null) {
            trl.setTableWidth("250px");
            entity.addElement(trl);
        }

        DeadlineNextTRL deadlineNextTRL = (DeadlineNextTRL) mapOfLoadedFields.get(EntityDataElement.DEADLINENEXTTRL.getId());
        if (deadlineNextTRL != null) {
            deadlineNextTRL.setTableWidth("150px");
            entity.addElement(deadlineNextTRL);
        }

        DeadlineFinalized deadlineFinalized = (DeadlineFinalized) mapOfLoadedFields.get(EntityDataElement.DEADLINEFINALIZED.getId());
        if (deadlineFinalized != null) {
            deadlineFinalized.setTableWidth("150px");
            entity.addElement(deadlineFinalized);
        }
    }

    @Override
    public void addAllFieldElementsForEdit(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity) {
        SBSCode sbsCode = (SBSCode) mapOfLoadedFields.get(EntityDataElement.SBSCODE.getId());
        if (sbsCode == null) {
            sbsCode = new SBSCode();
        }
        sbsCode.setFieldNotEditable();
        sbsCode.setFieldNotRequired();
        entity.addElement(sbsCode);

        CodeLevel codeLevel = (CodeLevel) mapOfLoadedFields.get(CODELEVEL.getId());
        if (codeLevel == null) {
            codeLevel = new CodeLevel();
        }
        codeLevel.setFieldNotVisible();
        entity.addElement(codeLevel);

        SystemName systemName = (SystemName) mapOfLoadedFields.get(EntityDataElement.SYSTEMNAME.getId());
        if (systemName == null) {
            systemName = new SystemName();
        }
        systemName.setFieldEditable();
        systemName.setFieldRequired();
        entity.addElement(systemName);

        SystemOwner systemOwner = (SystemOwner) mapOfLoadedFields.get(EntityDataElement.SYSTEMOWNERID.getId());
        if (systemOwner == null) {
            systemOwner = new SystemOwner(getWebSession());
        }
        systemOwner.setFieldEditable();
        systemOwner.setFieldRequired();
        entity.addElement(systemOwner);

        SystemDepartment systemDepartment = (SystemDepartment) mapOfLoadedFields.get(DEPARTMENTID.getId());
        if (systemDepartment == null) {
            systemDepartment = new SystemDepartment(getWebSession());
        }
        systemDepartment.setFieldEditable();
        systemDepartment.setFieldRequired();
        entity.addElement(systemDepartment);

        TRL trl = (TRL) mapOfLoadedFields.get(EntityDataElement.TRLID.getId());
        if (trl == null) {
            trl = new TRL(getWebSession());
        }
        trl.setFieldEditable();
        entity.addElement(trl);

        DeadlineNextTRL deadlineNextTRL = (DeadlineNextTRL) mapOfLoadedFields.get(EntityDataElement.DEADLINENEXTTRL.getId());
        if (deadlineNextTRL == null) {
            deadlineNextTRL = new DeadlineNextTRL();
        }
        deadlineNextTRL.setFieldEditable();
        entity.addElement(deadlineNextTRL);

        DeadlineFinalized deadlineFinalized = (DeadlineFinalized) mapOfLoadedFields.get(EntityDataElement.DEADLINEFINALIZED.getId());
        if (deadlineFinalized == null) {
            deadlineFinalized = new DeadlineFinalized();
        }
        deadlineFinalized.setFieldEditable();
        entity.addElement(deadlineFinalized);
    }

    @Override
    public void addAllFieldElementsForCreate(WebSession webSession, Entity entity, Integer parentEntityId) {

        SystemsBreakdownParentCodeSelector parentCodeSelector = new SystemsBreakdownParentCodeSelector(webSession);

        if ( parentEntityId == null) {
            SBSCodeType sbsCodeType = new SBSCodeType();
            sbsCodeType.setFieldEditable();
            sbsCodeType.setFieldRequired();
            entity.addElement(sbsCodeType);
        }

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(webSession, parentEntityId);
        SBSCode sbsCode = new SBSCode(true);
        sbsCode.setValue(nextCode);
        sbsCode.setFieldNotEditable();
        sbsCode.setFieldRequired();
        entity.addElement(sbsCode);

/* GFA
        SBSCodeParentSystem sbsCodeParentSystem = new SBSCodeParentSystem(webSession);
        sbsCodeParentSystem.setFieldEditable();
        sbsCodeParentSystem.setFieldRequired();
        entity.addElement(sbsCodeParentSystem);

 */

        SystemName systemName = new SystemName();
        systemName.setFieldEditable();
        systemName.setFieldRequired();
        entity.addElement(systemName);

        SystemOwner systemOwner = new SystemOwner(webSession);
        systemOwner.setFieldEditable();
        systemOwner.setFieldRequired();
        entity.addElement(systemOwner);

        SystemDepartment systemDepartment = new SystemDepartment(webSession);
        systemDepartment.setFieldEditable();
        systemDepartment.setFieldRequired();
        entity.addElement(systemDepartment);

        TRL trl =  new TRL (webSession);
        trl.setFieldEditable();
        entity.addElement(trl);

        DeadlineNextTRL deadlineNextTRL = new DeadlineNextTRL();
        deadlineNextTRL.setFieldEditable();
        entity.addElement(deadlineNextTRL);

        DeadlineFinalized deadlineFinalized = new DeadlineFinalized();
        deadlineFinalized.setFieldEditable();
        entity.addElement(deadlineFinalized);

    }

    public List<SystemBreakdownEntity> findAllForExport(boolean includeInactive)  {
        List<SystemBreakdownEntity> listOfEntitiesForExport = new ArrayList<>();
        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords(entityType, includeInactive);
            for (EntityRecord entityRecord : entityRecords) {
                SystemBreakdownEntity systemBreakdownEntity = SystemBreakdownEntity.map(entityRecord);
                listOfEntitiesForExport.add(systemBreakdownEntity);
            }
            listOfEntitiesForExport.sort(Comparator.comparing(SystemBreakdownEntity::getSbsCode));
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

//        if (level == null) {
//            level = codeSelector.getCodeLevel(requirementCode);
//        }


        entity.setSbsCode(requirementCode);
        entity.setSbsCodeLevel(level);
        entity.setSystemName(row.name());
//        entity.setRequirementDescription(row.description());
        entity.setActive(row.active() == null || row.active());

        entity.addAllDataElements();

        return entity;
    }

}
