package com.bepa.eis.server.entites.systemrequirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.SystemRequirementParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.*;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.timestamp.RequirementCaptureDate;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.LocalDateDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Comparator;

import static com.bepa.eis.common.enums.entity.EntityType.SYSTEM_REQUIREMENT;

public class SystemRequirementEntity extends AbstractEntity {

    private static final Logger log = LoggerFactory.getLogger(SystemRequirementEntity.class);

    private SystemRequirementCode requirementCode;
    private CodeLevel requirementCodeLevel;
    private RequirementName requirementName;
    private RequirementDescription requirementDescription;
    private RequirementVerificationStatus requirementVerificationStatus;
    private RequirementRationaleStatement requirementRationalStatement;
    private RequirementBusinessPriority requirementBusinessPriority;
    private RequirementCaptureDate requirementCaptureDate;
    private RequirementOwner requirementOwner;
    private RequirementStatus requirementStatus;

    private RequirementHighlevelCapability requirementHighlevelCapability;
    private RequirementType requirementType;
    private RequirementFrequency requirementFrequency;
    private RequirementPerformance requirementPerformance;
    private RequirementVerificationStatement requirementVerificationStatement;
    private RequirementTechnicalPriority requirementTechnicalPriority;

    @Override
    public EntityType getEntityType() {
        return SYSTEM_REQUIREMENT;
    }

    @Override
    public String getCode() {
        return requirementCode.getValue();
    }

    @Override
    public String getName() {
        return requirementName.getValue();
    }

    @Override
    public String getDescription() {
        return requirementDescription.getValue();
    }

    @Override
    public String getSortKey() {
        return getSortKeyValue(getRequirementCode().getSortKey());
    }

    @Override
    public void initializeFields() {
        requirementCode = new SystemRequirementCode();
        requirementCodeLevel = new CodeLevel();
        requirementName = new RequirementName();
        requirementDescription = new RequirementDescription();
        requirementVerificationStatus = new RequirementVerificationStatus(getWebSession());
        requirementRationalStatement = new RequirementRationaleStatement();
        requirementBusinessPriority = new RequirementBusinessPriority(getWebSession());
        requirementCaptureDate = new RequirementCaptureDate();
        requirementOwner = new RequirementOwner(getWebSession());
        requirementStatus = new RequirementStatus (getWebSession());
        requirementHighlevelCapability = new RequirementHighlevelCapability();
        requirementType = new RequirementType(getWebSession());
        requirementFrequency = new RequirementFrequency(getWebSession());
        requirementPerformance = new RequirementPerformance(getWebSession());
        requirementVerificationStatement = new RequirementVerificationStatement(getWebSession());
        requirementTechnicalPriority = new RequirementTechnicalPriority(getWebSession());
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
        entityElement.addElement(requirementCode);
        entityElement.addElement(requirementCodeLevel);
        entityElement.addElement(requirementName);
        entityElement.addElement(requirementDescription);
        entityElement.addElement(requirementVerificationStatus);
        entityElement.addElement(requirementRationalStatement);
        entityElement.addElement(requirementBusinessPriority);
        entityElement.addElement(requirementCaptureDate);
        entityElement.addElement(requirementOwner);
        entityElement.addElement(requirementStatus);

        entityElement.addElement(requirementHighlevelCapability);
        entityElement.addElement(requirementType);
        entityElement.addElement(requirementFrequency);
        entityElement.addElement(requirementPerformance);
        entityElement.addElement(requirementVerificationStatus);
        entityElement.addElement(requirementVerificationStatement);
        entityElement.addElement(requirementTechnicalPriority);
    }

    @Override
    public void addAllFieldElementsForEdit(Entity entityElement) {
        requirementCode.setFieldNotEditable();
        entityElement.addElement(requirementCode);

        requirementCodeLevel.setFieldNotVisible();
        entityElement.addElement(requirementCodeLevel);

        requirementName.setFieldEditable();
        entityElement.addElement(requirementName);

        requirementDescription.setFieldEditable();
        requirementDescription.setFieldRequired();
        entityElement.addElement(requirementDescription);

        requirementVerificationStatus.setFieldEditable();
        entityElement.addElement(requirementVerificationStatus);

        requirementRationalStatement.setFieldEditable();
        entityElement.addElement(requirementRationalStatement);

        requirementCaptureDate.setFieldEditable();
        entityElement.addElement(requirementCaptureDate);

        requirementBusinessPriority.setFieldEditable();
        entityElement.addElement(requirementBusinessPriority);

        requirementTechnicalPriority.setFieldEditable();
        entityElement.addElement(requirementTechnicalPriority);

        requirementVerificationStatement.setFieldEditable();
        entityElement.addElement(requirementVerificationStatement);

        requirementStatus.setFieldEditable();
        entityElement.addElement(requirementStatus);

        requirementHighlevelCapability.setFieldEditable();
        entityElement.addElement(requirementHighlevelCapability);

        requirementType.setFieldEditable();
        entityElement.addElement(requirementType);

        requirementFrequency.setFieldEditable();
        entityElement.addElement(requirementFrequency);

        requirementPerformance.setFieldEditable();
        entityElement.addElement(requirementPerformance);

        requirementOwner.setFieldEditable();
        entityElement.addElement(requirementOwner);
    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {

        SystemRequirementParentCodeSelector parentCodeSelector = new SystemRequirementParentCodeSelector(getWebSession());

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(getWebSession(), parentEntityId);
        requirementCode = new SystemRequirementCode(true);
        requirementCode.setValue(nextCode);
        requirementCode.setFieldNotEditable();
        requirementCode.setFieldRequired();
        entityElement.addElement(requirementCode);


        requirementName.setFieldEditable();
        requirementName.setFieldRequired();
        entityElement.addElement(requirementName);

        requirementDescription.setFieldEditable();
        entityElement.addElement(requirementDescription);

        requirementVerificationStatement.setFieldEditable();
        entityElement.addElement(requirementVerificationStatement);

        requirementVerificationStatus.setFieldEditable();
        entityElement.addElement(requirementVerificationStatus);

        requirementRationalStatement.setFieldEditable();
        entityElement.addElement(requirementRationalStatement);

        requirementCaptureDate.setFieldEditable();
        entityElement.addElement(requirementCaptureDate);

        requirementBusinessPriority.setFieldEditable();
        entityElement.addElement(requirementBusinessPriority);

        requirementTechnicalPriority.setFieldEditable();
        entityElement.addElement(requirementTechnicalPriority);

        requirementStatus.setFieldEditable();
        entityElement.addElement(requirementStatus);

        requirementHighlevelCapability.setFieldEditable();
        entityElement.addElement(requirementHighlevelCapability);

        requirementType.setFieldEditable();
        entityElement.addElement(requirementType);

        requirementFrequency.setFieldEditable();
        entityElement.addElement(requirementFrequency);

        requirementPerformance.setFieldEditable();
        entityElement.addElement(requirementPerformance);

        requirementOwner.setFieldEditable();
        entityElement.addElement(requirementOwner);
    }

    public SystemRequirementEntity() {}

    public SystemRequirementEntity(WebSession session) {
        super(session);
        initializeFields();
    }

    public SystemRequirementEntity(WebSession webSession, EntityRecord entityRecord) {
        super(webSession, entityRecord);
        initializeFields();

        for (EntityElementRecord elementRecord : entityRecord.getEntityElementRecords()) {

            EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

            if (entityDataElement != null) {
                switch (entityDataElement) {
                    case SYSTEMREQCODE:
                        requirementCode.setValue(elementRecord.getStringValue());
                        break;
                    case CODELEVEL:
                        requirementCodeLevel.setValue(elementRecord.getIntegerValue());
                        break;
                    case REQNAME:
                        requirementName.setValue(elementRecord.getStringValue());
                        break;
                    case REQDESCRIPTION:
                        requirementDescription.setValue(elementRecord.getStringValue());
                        break;
                    case REQVERIFICATIONSTATUSID:
                        requirementVerificationStatus.setValue(elementRecord.getIntegerValue());
                        break;
                    case REQRATIONALESTATEMENT:
                        requirementRationalStatement.setValue(elementRecord.getStringValue());
                        break;
                    case REQBUSINESSPRIORITYID:
                        requirementBusinessPriority.setValue(elementRecord.getIntegerValue());
                        break;
                    case REQCAPTUREDATE:
                        requirementCaptureDate.setValue(elementRecord.getLocalDateValue());
                        break;
                    case REQOWNERID:
                        requirementOwner.setValue(elementRecord.getIntegerValue());
                        break;
                    case REQSTATUSID:
                        requirementStatus.setValue(elementRecord.getIntegerValue());
                        break;
                    case REQHIGHLEVELCAPABILITY:
                        requirementHighlevelCapability.setValue(elementRecord.getStringValue());
                        break;
                    case REQTYPEID:
                        requirementType.setValue(elementRecord.getIntegerValue());
                        break;
                    case REQFREQUENCYID:
                        requirementFrequency.setValue(elementRecord.getIntegerValue());
                        break;
                    case REQPERFORMANCE:
                        requirementPerformance.setValue(elementRecord.getStringValue());
                        break;
                    case REQVERIFICATIONSTATEMENTID:
                        requirementVerificationStatement.setValue(elementRecord.getIntegerValue());
                        break;
                    case REQTECHNICALPRIORITYID:
                        requirementTechnicalPriority.setValue(elementRecord.getIntegerValue());
                        break;
                    default:
                        log.error("Unknown entity data element: " + entityDataElement.getFieldName());
                        break;
                }
            }
        }
    }

    public void setRequirementCode(String requirementCode) {
        this.requirementCode.setValue(requirementCode);
    }

    public SystemRequirementCode getRequirementCode() {
        return requirementCode;
    }

    public void setRequirementCodeLevel(Integer requirementCodeLevel) {
        this.requirementCodeLevel.setValue(requirementCodeLevel);
    }
    public CodeLevel getRequirementCodeLevel() {
        return requirementCodeLevel;
    }

    public void setRequirementName(String requirementName) {
        this.requirementName.setValue(requirementName);
    }
    public RequirementName getRequirementName() {
        return requirementName;
    }

    public void setRequirementDescription(String requirementDescription) {
        this.requirementDescription.setValue(requirementDescription);
    }

    public RequirementDescription getRequirementDescription() {
        return requirementDescription;
    }

    public RequirementVerificationStatus getVerificationStatus() {
        return requirementVerificationStatus;
    }

    public void setRationalStatement(String rationalStatement) {
        this.requirementRationalStatement.setValue(rationalStatement);
    }

    public RequirementRationaleStatement getRationalStatement() {
        return requirementRationalStatement;
    }

    public void setBusinessPriority(Integer businessPriorityId) {
        this.requirementBusinessPriority.setValue(businessPriorityId);
    }

    public RequirementBusinessPriority getBusinessPriority() {
        return requirementBusinessPriority;
    }

    public void setCaptureDate(String captureDate) {
        this.requirementCaptureDate.setValue(captureDate);
    }

    public void setCaptureDate(LocalDate captureDate) {
        this.requirementCaptureDate.setValue(captureDate);
    }

    public RequirementCaptureDate getCaptureDate() {
        return requirementCaptureDate;
    }

    public void setOwner(Integer ownerId) {
        this.requirementOwner.setValue(ownerId);
    }

    public Integer getOwnerId() {
        return requirementOwner.getValue();
    }
    public RequirementOwner getOwner() {
        return requirementOwner;
    }

    public void setStatusId(Integer statusId) {
        this.requirementStatus.setValue(statusId);
    }

    public Integer getStatusId() {
        return requirementStatus.getValue();
    }

    public RequirementStatus getStatus() {
        return requirementStatus;
    }

    public void setHighlevelCapability(String requirementHighlevelCapability) {
        this.requirementHighlevelCapability.setValue(requirementHighlevelCapability);
    }

    public RequirementHighlevelCapability getRequirementHighlevelCapability() {
        return requirementHighlevelCapability;
    }

    public void setRequirementTypeId(Integer typeId) {
        this.requirementType.setValue(typeId);
    }

    public RequirementType getRequirementType() {
        return requirementType;
    }

    public void setRequirementFrequencyId(Integer requirementFrequencyId) {
        this.requirementFrequency.setValue(requirementFrequencyId);
    }

    public RequirementFrequency getRequirementFrequency() {
        return requirementFrequency;
    }

    public void setRequirementPerformance(String requirementPerformance) {
        this.requirementPerformance.setValue(requirementPerformance);
    }

    public RequirementPerformance getRequirementPerformance() {
        return requirementPerformance;
    }

    public void setVerificationStatus(Integer requirementVerificationStatusId) {
        this.requirementVerificationStatus.setValue(requirementVerificationStatusId);
    }

    public RequirementVerificationStatus getRequirementVerificationStatus() {
        return requirementVerificationStatus;
    }

    public void setRequirementVerificationStatementId(Integer requirementVerificationStatementId) {
        this.requirementVerificationStatement.setValue(requirementVerificationStatementId);
    }

    public RequirementVerificationStatement getRequirementVerificationStatement() {
        return requirementVerificationStatement;
    }

    public void setRequirementTechnicalPriority(Integer requirementTechnicalPriorityId) {
        this.requirementTechnicalPriority.setValue(requirementTechnicalPriorityId);
    }

    public RequirementTechnicalPriority getRequirementTechnicalPriority() {
        return requirementTechnicalPriority;
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(SystemRequirementCode.FIELD_NAME, getRequirementCode().getValue()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, getRequirementCodeLevel().getValue()));
        addDataElement(new StringDataElement(RequirementName.FIELD_NAME, getRequirementName().getValue()));
        addDataElement(new StringDataElement(RequirementDescription.FIELD_NAME, getRequirementDescription().getValue()));
        addDataElement(new IntegerDataElement(RequirementVerificationStatus.FIELD_NAME, getVerificationStatus().getValue()));
        addDataElement(new StringDataElement(RequirementRationaleStatement.FIELD_NAME, getRationalStatement().getValue()));
        addDataElement(new IntegerDataElement(RequirementBusinessPriority.FIELD_NAME, getBusinessPriority().getValue()));
        addDataElement(new LocalDateDataElement(RequirementCaptureDate.FIELD_NAME, getCaptureDate().getValue()));
        addDataElement(new IntegerDataElement(RequirementOwner.FIELD_NAME, getOwnerId()));
        addDataElement(new IntegerDataElement(RequirementStatus.FIELD_NAME, getStatusId()));

        addDataElement(new StringDataElement(RequirementHighlevelCapability.FIELD_NAME, getRequirementHighlevelCapability().getValue()));
        addDataElement(new IntegerDataElement(RequirementType.FIELD_NAME, getRequirementType().getValue()));
        addDataElement(new IntegerDataElement(RequirementFrequency.FIELD_NAME, getRequirementFrequency().getValue()));
        addDataElement(new StringDataElement(RequirementPerformance.FIELD_NAME, getRequirementPerformance().getValue()));
        addDataElement(new IntegerDataElement(RequirementVerificationStatus.FIELD_NAME, getRequirementVerificationStatus().getValue()));
        addDataElement(new IntegerDataElement(RequirementVerificationStatement.FIELD_NAME, getRequirementVerificationStatement().getValue()));
        addDataElement(new IntegerDataElement(RequirementTechnicalPriority.FIELD_NAME, getRequirementTechnicalPriority().getValue()));
    }

}
