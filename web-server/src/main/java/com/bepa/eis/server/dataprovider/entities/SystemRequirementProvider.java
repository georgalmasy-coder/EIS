package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.views.basis.system.BasisSystemRequirementExportRow;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.fields.booleans.RelevantToStakeholderRequirement;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.SystemRequirementParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.*;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.timestamp.RequirementCaptureDate;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.systemsystemrequirement.SystemRequirementEntity;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.bepa.eis.common.enums.entity.EntityDataElement.*;

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
        return getListOfEntitiesByProjectId(entityType, getEntityDataElementForList());
    }

    public Entities getSystemRequirementInfo(Integer entityId, Integer version) throws SQLException {
        return getEntityByEntityId(entityType, entityId, version, getEntityDataElementForEdit());
    }

    public Entities getSystemRequirementInfo(Integer parentEntityId) throws SQLException {
        return getEntityForCreate(entityType, parentEntityId);
    }

    public Entities getSystemRequirementHistory(Integer entityId) throws SQLException {
        return getEntityHistoryByEntityId(entityType, entityId);
    }

    @Override
    public EntityDataElement[] getEntityDataElementForList() {

        return new EntityDataElement[]{
                SYSTEMREQCODE,
                CODELEVEL,
                REQNAME,
                REQDESCRIPTION,
                REQVERIFICATIONSTATUSID,
                REQRATIONALESTATEMENT,
                REQBUSINESSPRIORITYID,
                REQOWNERID,
                REQCAPTUREDATE,
                REQSTATUSID};
    }

    @Override
    public EntityDataElement[] getEntityDataElementForEdit() {

        return new EntityDataElement[]{
                SYSTEMREQCODE,
                REQNAME,
                REQDESCRIPTION,
                REQVERIFICATIONSTATUSID,
                REQRATIONALESTATEMENT,
                REQBUSINESSPRIORITYID,
                REQOWNERID,
                REQSTATUSID,
                REQCAPTUREDATE,
                REQRELEVANTTOSTAKEHOLDER,
                REQHIGHLEVELCAPABILITY,
                REQTYPEID,
                REQFREQUENCYID,
                REQPERFORMANCE,
                REQVERIFICATIONSTATEMENTID,
                REQTECHNICALPRIORITYID,
        };
    }

    @Override
    public EntityDataElement[] getEntityDataElementForCreate() {

        return new EntityDataElement[]{
                REQNAME,
                REQDESCRIPTION,
                REQVERIFICATIONSTATUSID,
                REQRATIONALESTATEMENT,
                REQBUSINESSPRIORITYID,
                REQOWNERID,
                REQSTATUSID,
                REQCAPTUREDATE,
                REQRELEVANTTOSTAKEHOLDER,
                REQHIGHLEVELCAPABILITY,
                REQTYPEID,
                REQFREQUENCYID,
                REQPERFORMANCE,
                REQVERIFICATIONSTATEMENTID,
                REQTECHNICALPRIORITYID,
        };
    }

    @Override
    public void addAllFieldElementsForList(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity) {
        SystemRequirementCode requirementCode = (SystemRequirementCode) mapOfLoadedFields.get(EntityDataElement.SYSTEMREQCODE.getId());
        if (requirementCode != null) {
            requirementCode.setTableWidth("50px");
            entity.addElement(requirementCode);
            entity.setSortKey(requirementCode.getValue());
        }

        CodeLevel codeLevel = (CodeLevel) mapOfLoadedFields.get(EntityDataElement.CODELEVEL.getId());
        if (codeLevel != null) {
            codeLevel.setTableWidth("35px");
            entity.addElement(codeLevel);
        }

        RequirementName requirementName = (RequirementName) mapOfLoadedFields.get(EntityDataElement.REQNAME.getId());
        if (requirementName != null) {
            requirementName.setTableWidth("200px");
            entity.addElement(requirementName);
        }

        RequirementDescription requirementDescription = (RequirementDescription) mapOfLoadedFields.get(REQDESCRIPTION.getId());
        if (requirementDescription != null) {
            requirementDescription.setTableWidth("100px");
            entity.addElement(requirementDescription);
        }

        RequirementVerificationStatus requirementVerificationStatus = (RequirementVerificationStatus) mapOfLoadedFields.get(REQVERIFICATIONSTATUSID.getId());
        if (requirementVerificationStatus != null) {
            entity.addElement(requirementVerificationStatus);
        }

        RequirementRationaleStatement requirementRationaleStatement = (RequirementRationaleStatement) mapOfLoadedFields.get(REQRATIONALESTATEMENT.getId());
        if (requirementRationaleStatement != null) {
            entity.addElement(requirementRationaleStatement);
        }

        RequirementCaptureDate requirementCaptureDate = (RequirementCaptureDate) mapOfLoadedFields.get(REQCAPTUREDATE.getId());
        if (requirementCaptureDate != null) {
            entity.addElement(requirementCaptureDate);
        }

        RequirementStatus requirementStatus = (RequirementStatus) mapOfLoadedFields.get(REQSTATUSID.getId());
        if (requirementStatus != null) {
            entity.addElement(requirementStatus);
        }

        RequirementBusinessPriority businessPriority = (RequirementBusinessPriority) mapOfLoadedFields.get(REQBUSINESSPRIORITYID.getId());
        if (businessPriority != null) {
            entity.addElement(businessPriority);
        }

        RequirementOwner requirementOwner = (RequirementOwner) mapOfLoadedFields.get(REQOWNERID.getId());
        if (requirementOwner != null) {
            entity.addElement(requirementOwner);
        }

    }

    @Override
    public void addAllFieldElementsForEdit(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity) {

        SystemRequirementCode requirementCode = (SystemRequirementCode) mapOfLoadedFields.get(EntityDataElement.SYSTEMREQCODE.getId());
        if (requirementCode == null) {
            requirementCode = new SystemRequirementCode();
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

        RequirementVerificationStatus requirementVerificationStatus = (RequirementVerificationStatus) mapOfLoadedFields.get(REQVERIFICATIONSTATUSID.getId());
        if (requirementVerificationStatus == null) {
            requirementVerificationStatus = new RequirementVerificationStatus(getWebSession());
        }
        requirementVerificationStatus.setFieldEditable();
        requirementVerificationStatus.setFieldRequired();
        entity.addElement(requirementVerificationStatus);

        RequirementRationaleStatement requirementRationaleStatement = (RequirementRationaleStatement) mapOfLoadedFields.get(REQRATIONALESTATEMENT.getId());
        if (requirementRationaleStatement == null) {
            requirementRationaleStatement = new RequirementRationaleStatement();
        }
        requirementRationaleStatement.setFieldEditable();
        requirementRationaleStatement.setFieldRequired();
        entity.addElement(requirementRationaleStatement);

        RequirementCaptureDate requirementCaptureDate = (RequirementCaptureDate) mapOfLoadedFields.get(REQCAPTUREDATE.getId());
        if (requirementCaptureDate == null) {
            requirementCaptureDate = new RequirementCaptureDate();
        }
        requirementCaptureDate.setFieldEditable();
        requirementCaptureDate.setFieldRequired();
        entity.addElement(requirementCaptureDate);

        RequirementBusinessPriority requirementBusinessPriority = (RequirementBusinessPriority) mapOfLoadedFields.get(REQBUSINESSPRIORITYID.getId());
        if (requirementBusinessPriority == null) {
            requirementBusinessPriority = new RequirementBusinessPriority(getWebSession());
        }
        requirementBusinessPriority.setFieldEditable();
        requirementBusinessPriority.setFieldRequired();
        entity.addElement(requirementBusinessPriority);

        RequirementTechnicalPriority requirementTechnicalPriority = (RequirementTechnicalPriority) mapOfLoadedFields.get(REQTECHNICALPRIORITYID.getId());
        if (requirementTechnicalPriority == null) {
            requirementTechnicalPriority = new RequirementTechnicalPriority(getWebSession());
        }
        requirementTechnicalPriority.setFieldEditable();
        requirementTechnicalPriority.setFieldRequired();
        entity.addElement(requirementTechnicalPriority);


        RequirementVerificationStatement requirementVerificationStatement = (RequirementVerificationStatement) mapOfLoadedFields.get(REQVERIFICATIONSTATEMENTID.getId());
        if (requirementVerificationStatement == null) {
            requirementVerificationStatement = new RequirementVerificationStatement(getWebSession());
        }
        requirementVerificationStatement.setFieldEditable();
        requirementVerificationStatement.setFieldRequired();
        entity.addElement(requirementVerificationStatement);

        RequirementStatus requirementStatus = (RequirementStatus) mapOfLoadedFields.get(REQSTATUSID.getId());
        if (requirementStatus == null) {
            requirementStatus = new RequirementStatus(getWebSession());
        }
        requirementStatus.setFieldEditable();
        requirementStatus.setFieldRequired();
        entity.addElement(requirementStatus);

        RequirementHighlevelCapability requirementHighlevelCapability = (RequirementHighlevelCapability) mapOfLoadedFields.get(REQHIGHLEVELCAPABILITY.getId());
        if (requirementHighlevelCapability == null) {
            requirementHighlevelCapability = new RequirementHighlevelCapability(getWebSession());
        }
        requirementHighlevelCapability.setFieldEditable();
        requirementHighlevelCapability.setFieldRequired();
        entity.addElement(requirementHighlevelCapability);

        RequirementType requirementType = (RequirementType) mapOfLoadedFields.get(REQTYPEID.getId());
        if (requirementType == null) {
            requirementType = new RequirementType(getWebSession());
        }
        requirementType.setFieldEditable();
        requirementType.setFieldRequired();
        entity.addElement(requirementType);

        RequirementFrequency requirementFrequency = (RequirementFrequency) mapOfLoadedFields.get(REQFREQUENCYID.getId());
        if (requirementFrequency == null) {
            requirementFrequency = new RequirementFrequency(getWebSession());
        }
        requirementFrequency.setFieldEditable();
        requirementFrequency.setFieldRequired();
        entity.addElement(requirementFrequency);

        RequirementPerformance requirementPerformance = (RequirementPerformance) mapOfLoadedFields.get(REQPERFORMANCE.getId());
        if (requirementPerformance == null) {
            requirementPerformance = new RequirementPerformance(getWebSession());
        }
        requirementPerformance.setFieldEditable();
        requirementPerformance.setFieldRequired();
        entity.addElement(requirementPerformance);

        RequirementOwner requirementOwner = (RequirementOwner) mapOfLoadedFields.get(REQOWNERID.getId());
        if (requirementOwner == null) {
            requirementOwner = new RequirementOwner(getWebSession());
        }
        requirementOwner.setFieldEditable();
        requirementOwner.setFieldRequired();
        entity.addElement(requirementOwner);

        RelevantToStakeholderRequirement relevantToStakeholderRequirement = (RelevantToStakeholderRequirement) mapOfLoadedFields.get(REQRELEVANTTOSTAKEHOLDER.getId());
        if (relevantToStakeholderRequirement == null) {
            relevantToStakeholderRequirement = new RelevantToStakeholderRequirement(true);
        }
        relevantToStakeholderRequirement.setFieldEditable();
        relevantToStakeholderRequirement.setFieldNotRequired();
        entity.addElement(relevantToStakeholderRequirement);
    }

    @Override
    public void addAllFieldElementsForCreate(WebSession webSession, Entity entity, Integer parentEntityId) {

        SystemRequirementParentCodeSelector parentCodeSelector = new SystemRequirementParentCodeSelector(webSession);
//        entity.addElement(parentCodeSelector);

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(webSession, parentEntityId);
        SystemRequirementCode requirementCode = new SystemRequirementCode(true);
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

        RequirementVerificationStatement requirementVerificationStatement = new RequirementVerificationStatement();
        requirementVerificationStatement.setFieldEditable();
        requirementVerificationStatement.setFieldRequired();
        entity.addElement(requirementVerificationStatement);

        RequirementVerificationStatus requirementVerificationStatus = new RequirementVerificationStatus();
        requirementVerificationStatus.setFieldEditable();
        requirementVerificationStatus.setFieldRequired();
        entity.addElement(requirementVerificationStatus);

        RequirementRationaleStatement requirementRationaleStatement = new RequirementRationaleStatement();
        requirementRationaleStatement.setFieldEditable();
        requirementRationaleStatement.setFieldRequired();
        entity.addElement(requirementRationaleStatement);

        RequirementCaptureDate requirementCaptureDate = new RequirementCaptureDate();
        requirementCaptureDate.setFieldEditable();
        requirementCaptureDate.setFieldRequired();
        entity.addElement(requirementCaptureDate);

        RequirementBusinessPriority requirementBusinessPriority = new RequirementBusinessPriority(webSession);
        requirementBusinessPriority.setFieldEditable();
        requirementBusinessPriority.setFieldRequired();
        entity.addElement(requirementBusinessPriority);

        RequirementTechnicalPriority requirementTechnicalPriority = new RequirementTechnicalPriority(webSession);
        requirementTechnicalPriority.setFieldEditable();
        requirementTechnicalPriority.setFieldRequired();
        entity.addElement(requirementTechnicalPriority);

        RequirementStatus requirementStatus = new RequirementStatus(webSession);
        requirementStatus.setFieldEditable();
        requirementStatus.setFieldRequired();
        entity.addElement(requirementStatus);

        RequirementHighlevelCapability requirementHighlevelCapability = new RequirementHighlevelCapability(webSession);
        requirementHighlevelCapability.setFieldEditable();
        requirementHighlevelCapability.setFieldRequired();
        entity.addElement(requirementHighlevelCapability);

        RequirementType requirementType = new RequirementType(webSession);
        requirementType.setFieldEditable();
        requirementType.setFieldRequired();
        entity.addElement(requirementType);

        RequirementFrequency requirementFrequency = new RequirementFrequency(webSession);
        requirementFrequency.setFieldEditable();
        requirementFrequency.setFieldRequired();
        entity.addElement(requirementFrequency);


        RequirementPerformance requirementPerformance = new RequirementPerformance(webSession);
        requirementPerformance.setFieldEditable();
        requirementPerformance.setFieldRequired();
        entity.addElement(requirementPerformance);

        RequirementOwner requirementOwner = new RequirementOwner(webSession);
        requirementOwner.setFieldEditable();
        requirementOwner.setFieldRequired();
        entity.addElement(requirementOwner);

        RelevantToStakeholderRequirement relevantToStakeholderRequirement = new RelevantToStakeholderRequirement(true);
        relevantToStakeholderRequirement.setFieldEditable();
        relevantToStakeholderRequirement.setFieldNotRequired();
        entity.addElement(relevantToStakeholderRequirement);
    }

    public List<SystemRequirementEntity> getAllSystemRequirement(boolean includeInactive)  {
        List<SystemRequirementEntity> listOfEntitiesForExport = new ArrayList<>();

        try {
            List<EntityRecord> entityRecords = getListOfEntityRecords(entityType, includeInactive);
            for (EntityRecord entityRecord : entityRecords) {
                SystemRequirementEntity systemRequirementEntity = SystemRequirementEntity.map(entityRecord);
                listOfEntitiesForExport.add(systemRequirementEntity);
            }

            listOfEntitiesForExport.sort(Comparator.comparing(SystemRequirementEntity::getRequirementCode));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return listOfEntitiesForExport;
    }

    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows) {
        List<BasisSystemRequirementExportRow> rowsList = (List<BasisSystemRequirementExportRow>) rows;
        List<AbstractEntity> entities = new ArrayList<>();

        for (BasisSystemRequirementExportRow systemBreakdownExportRow : rowsList) {
            SystemRequirementEntity entity = toEntity(webSession, systemBreakdownExportRow);
            entities.add(entity);
        }

        return entities;
    }

    private SystemRequirementEntity toEntity(WebSession webSession, BasisSystemRequirementExportRow row) {
        SystemRequirementEntity entity = new SystemRequirementEntity(webSession);
//        BasisStakeholderRequirementParentCodeSelector codeSelector = new BasisStakeholderRequirementParentCodeSelector(webSession);

        entity.setCustomerId(webSession.getCustomerId());
        entity.setProjectId(webSession.getProjectId());
        entity.setVersion(1);

        String requirementCode = row.id();

  //      if (requirementCode == null || requirementCode.isBlank()) {
//            requirementCode = codeSelector.getNextAvailableCodeValue(webSession, "");
//        }

        Integer level = row.level();

//        if (level == null) {
//            level = codeSelector.getCodeLevel(requirementCode);
//        }


        entity.setRequirementCode(requirementCode);
        entity.setRequirementCodeLevel(level);
        entity.setRequirementName(row.name());
//        entity.setRequirementDescription(row.description());
        entity.setActive(row.active() == null || row.active());

        entity.addAllDataElements();

        return entity;
    }

}
