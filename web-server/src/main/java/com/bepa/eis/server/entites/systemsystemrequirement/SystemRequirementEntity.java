package com.bepa.eis.server.entites.systemsystemrequirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.booleans.RelevantToStakeholderRequirement;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.*;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.timestamp.AbstractDate;
import com.bepa.eis.server.dataprovider.fields.timestamp.RequirementCaptureDate;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.datatypes.BooleanDataElement;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.LocalDateDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

import static com.bepa.eis.common.enums.entity.EntityType.SYSTEM_REQUIREMENT;

public class SystemRequirementEntity extends AbstractEntity {

    private static final Logger log = LoggerFactory.getLogger(SystemRequirementEntity.class);

    private String requirementCode;
    private Integer requirementCodeLevel;
    private String requirementName;
    private String requirementDescription;
    private Integer verificationStatusId;
    private String rationalStatement;
    private Integer businessPriorityId;
    private String captureDate;
    private Integer ownerId;
    private Integer statusId;
    private Boolean isRelevantToStakeholderRequirement;

    private String requirementHighlevelCapability;
    private Integer typeId;
    private Integer requirementFrequencyId;
    private String requirementPerformance;
    private Integer requirementVerificationStatusId;
    private Integer requirementVerificationStatementId;
    private Integer requirementTechnicalPriorityId;

    @Override
    public EntityType getEntityType() {
        return SYSTEM_REQUIREMENT;
    }

    @Override
    public String getCode() {
        return requirementCode;
    }

    @Override
    public String getName() {
        return requirementName;
    }

    @Override
    public String getDescription() {
        return requirementDescription;
    }

    public SystemRequirementEntity() {}

    public SystemRequirementEntity(WebSession session) {
        super(session);
        setChangedByUserId(session.getUserId());
    }

    public void setRequirementCode(String requirementCode) {
        this.requirementCode = requirementCode;
    }

    public String getRequirementCode() {
        return requirementCode;
    }

    public void setRequirementCodeLevel(Integer requirementCodeLevel) {
        this.requirementCodeLevel = requirementCodeLevel;
    }
    public Integer getRequirementCodeLevel() {
        return requirementCodeLevel;
    }

    public void setRequirementName(String requirementName) {
        this.requirementName = requirementName;
    }
    public String getRequirementName() {
        return requirementName;
    }

    public void setRequirementDescription(String requirementDescription) {
        this.requirementDescription = requirementDescription;
    }

    public String getRequirementDescription() {
        return requirementDescription;
    }

    public void setVerificationStatusId(Integer verificationStatusId) {
        this.verificationStatusId = verificationStatusId;
    }
    public Integer getVerificationStatusId() {
        return verificationStatusId;
    }
    public RequirementVerificationStatus getVerificationStatus() {
        RequirementVerificationStatus requirementVerificationStatus = new RequirementVerificationStatus(getWebSession());
        requirementVerificationStatus.setValue(verificationStatusId);
        return requirementVerificationStatus;
    }

    public void setRationalStatement(String rationalStatement) {
        this.rationalStatement = rationalStatement;
    }
    public String getRationalStatement() {
        return rationalStatement;
    }

    public void setBusinessPriorityId(Integer businessPriorityId) {
        this.businessPriorityId = businessPriorityId;
    }
    public Integer getBusinessPriorityId() {
        return businessPriorityId;
    }    public RequirementBusinessPriority getBusinessPriority() {
        RequirementBusinessPriority requirementBusinessPriority = new RequirementBusinessPriority(getWebSession());
        requirementBusinessPriority.setValue(businessPriorityId);
        return requirementBusinessPriority;
    }

    public void setCaptureDate(String captureDate) {
        this.captureDate = captureDate;
    }
    public void setCaptureDate(LocalDate captureDate) {
        this.captureDate = captureDate != null ? captureDate.format(AbstractDate.FORMATTER_DATE) : "";
    }
    public String getCaptureDate() {
        return captureDate;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }
    public Integer getOwnerId() {
        return ownerId;
    }
    public RequirementOwner getOwner() {
        RequirementOwner requirementOwner = new RequirementOwner(getWebSession());
        requirementOwner.setValue(ownerId);
        return requirementOwner;
    }


    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }
    public Integer getStatusId() {
        return statusId;
    }
    public RequirementStatus getStatus() {
        RequirementStatus requirementStatus = new RequirementStatus(getWebSession());
        requirementStatus.setValue(statusId);
        return requirementStatus;
    }

    public void setRelevantToStakeholderRequirement(Boolean relevantToStakeholderRequirement) {
        isRelevantToStakeholderRequirement = relevantToStakeholderRequirement;
    }

    public Boolean isRelevantToStakeholderRequirement() {
        return isRelevantToStakeholderRequirement;
    }


    public void setRequirementHighlevelCapability(String requirementHighlevelCapability) {
        this.requirementHighlevelCapability = requirementHighlevelCapability;
    }
    public String getRequirementHighlevelCapability() {
        return requirementHighlevelCapability;
    }

    public void setRequirementTypeId(Integer typeId) {
        this.typeId = typeId;
    }
    public Integer getRequirementTypeId() {
        return typeId;
    }
    public RequirementType getRequirementType() {
        RequirementType requirementType = new RequirementType(getWebSession());
        requirementType.setValue(typeId);
        return requirementType;
    }

    public void setRequirementFrequencyId(Integer requirementFrequencyId) {
        this.requirementFrequencyId = requirementFrequencyId;
    }
    public Integer getRequirementFrequencyId() {
        return requirementFrequencyId;
    }
    public RequirementFrequency getRequirementFrequency() {
        RequirementFrequency requirementFrequency = new RequirementFrequency(getWebSession());
        requirementFrequency.setValue(requirementFrequencyId);
        return requirementFrequency;
    }

    public void setRequirementPerformance(String requirementPerformance) {
        this.requirementPerformance = requirementPerformance;
    }
    public String getRequirementPerformance() {
        return requirementPerformance;
    }

    public void setRequirementVerificationStatusId(Integer requirementVerificationStatusId) {
        this.requirementVerificationStatusId = requirementVerificationStatusId;
    }
    public Integer getRequirementVerificationStatusId() {
        return requirementVerificationStatusId;
    }
    public RequirementVerificationStatus getRequirementVerificationStatus() {
        RequirementVerificationStatus requirementVerificationStatus = new RequirementVerificationStatus(getWebSession());
        requirementVerificationStatus.setValue(requirementVerificationStatusId);
        return requirementVerificationStatus;
    }

    public void setRequirementVerificationStatementId(Integer requirementVerificationStatementId) {
        this.requirementVerificationStatementId = requirementVerificationStatementId;
    }
    public Integer getRequirementVerificationStatementId() {
        return requirementVerificationStatementId;
    }
    public RequirementVerificationStatement getRequirementVerificationStatement() {
        RequirementVerificationStatement requirementVerificationStatement = new RequirementVerificationStatement(getWebSession());
        requirementVerificationStatement.setValue(requirementVerificationStatementId);
        return requirementVerificationStatement;
    }

    public void setRequirementTechnicalPriorityIdtId(Integer requirementTechnicalPriorityId) {
        this.requirementTechnicalPriorityId = requirementTechnicalPriorityId;
    }
    public Integer getRequirementTechnicalPriorityId() {
        return requirementTechnicalPriorityId;
    }
    public RequirementTechnicalPriority getRequirementTechnicalPriority() {
        RequirementTechnicalPriority requirementTechnicalPriority = new RequirementTechnicalPriority(getWebSession());
        requirementTechnicalPriority.setValue(requirementTechnicalPriorityId);
        return requirementTechnicalPriority;
    }

    public static SystemRequirementEntity map(EntityRecord entity) {

        SystemRequirementEntity requirementEntity = null;

        if (entity != null) {

            requirementEntity = new SystemRequirementEntity(entity.getWebSession());

            requirementEntity.setEntityId(entity.getEntityId());
            requirementEntity.setCustomerId(entity.getCustomerId());
            requirementEntity.setProjectId(entity.getProjectId());
            requirementEntity.setVersion(entity.getVersion());
            requirementEntity.setChangedByUserId(entity.getChangedByUserId());
            requirementEntity.setDateOfChange(entity.getChangedDateTime());
            requirementEntity.setActive(entity.isActive());

            for (EntityElementRecord elementRecord : entity.getEntityElementRecords()) {

                EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

                if (entityDataElement != null) {
                    switch (entityDataElement) {
                        case SYSTEMREQCODE :
                            requirementEntity.setRequirementCode(elementRecord.getStringValue());
                            break;
                        case CODELEVEL :
                            requirementEntity.setRequirementCodeLevel(elementRecord.getIntegerValue());
                            break;
                        case REQNAME :
                            requirementEntity.setRequirementName(elementRecord.getStringValue());
                            break;
                        case REQDESCRIPTION :
                            requirementEntity.setRequirementDescription(elementRecord.getStringValue());
                            break;
                        case REQVERIFICATIONSTATUSID :
                            requirementEntity.setVerificationStatusId(elementRecord.getIntegerValue());
                            break;
                        case REQRATIONALESTATEMENT :
                            requirementEntity.setRationalStatement(elementRecord.getStringValue());
                            break;
                        case REQBUSINESSPRIORITYID :
                            requirementEntity.setBusinessPriorityId(elementRecord.getIntegerValue());
                            break;
                        case REQCAPTUREDATE :
                            requirementEntity.setCaptureDate(elementRecord.getLocalDateValue());
                            break;
                        case REQOWNERID :
                            requirementEntity.setOwnerId(elementRecord.getIntegerValue());
                            break;
                        case REQSTATUSID:
                            requirementEntity.setStatusId(elementRecord.getIntegerValue());
                            break;
                        case REQRELEVANTTOSTAKEHOLDER:
                            requirementEntity.setRelevantToStakeholderRequirement(elementRecord.getBooleanValue());
                            break;
                    }
                }

            }

        }

        return requirementEntity;
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(SystemRequirementCode.FIELD_NAME, getRequirementCode()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, getRequirementCodeLevel()));
        addDataElement(new StringDataElement(RequirementName.FIELD_NAME, getRequirementName()));
        addDataElement(new StringDataElement(RequirementDescription.FIELD_NAME, getRequirementDescription()));
        addDataElement(new IntegerDataElement(RequirementVerificationStatus.FIELD_NAME, getVerificationStatusId()));
        addDataElement(new StringDataElement(RequirementRationaleStatement.FIELD_NAME, getRationalStatement()));
        addDataElement(new IntegerDataElement(RequirementBusinessPriority.FIELD_NAME, getBusinessPriorityId()));
        addDataElement(new LocalDateDataElement(RequirementCaptureDate.FIELD_NAME, getCaptureDate()));
        addDataElement(new IntegerDataElement(RequirementOwner.FIELD_NAME, getOwnerId()));
        addDataElement(new IntegerDataElement(RequirementStatus.FIELD_NAME, getStatusId()));
        addDataElement(new BooleanDataElement(RelevantToStakeholderRequirement.FIELD_NAME, isRelevantToStakeholderRequirement()));

        addDataElement(new StringDataElement(RequirementHighlevelCapability.FIELD_NAME, getRequirementHighlevelCapability()));
        addDataElement(new IntegerDataElement(RequirementType.FIELD_NAME, getRequirementTypeId()));
        addDataElement(new IntegerDataElement(RequirementFrequency.FIELD_NAME, getRequirementFrequencyId()));
        addDataElement(new StringDataElement(RequirementPerformance.FIELD_NAME, getRequirementPerformance()));
        addDataElement(new IntegerDataElement(RequirementVerificationStatus.FIELD_NAME, getRequirementVerificationStatusId()));
        addDataElement(new IntegerDataElement(RequirementVerificationStatement.FIELD_NAME, getRequirementVerificationStatementId()));
        addDataElement(new IntegerDataElement(RequirementTechnicalPriority.FIELD_NAME, getRequirementTechnicalPriorityId()));
    }

}
