package com.bepa.eis.server.entites.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.LogicalStructureParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.logical.*;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;

import static com.bepa.eis.common.enums.entity.EntityType.LOGICAL_STRUCTURE;

public class LogicalStructureEntity extends AbstractEntity {

    private LogicalCode logicalCode;
    private CodeLevel logicalCodeLevel;
    private LogicalName logicalName;
    private LogicalDescription logicalDescription;
    private LogicalOwner logicalOwner;
    private LogicalVerificationStatus logicalVerificationStatus;
    private LogicalCriticality logicalCriticality;
    private LogicalElementCategory logicalElementCategory;
    private LogicalLevel logicalLevel;
    private LogicalType logicalType;
    private LogicalResponsibleDomain logicalResponsibleDomain;
    private LogicalLifecycleStatus logicalLifecycleStatus;
    private LogicalMaturity logicalMaturity;
    private LogicalAllocationStatus logicalAllocationStatus;
    private LogicalRealizationStatus logicalRealizationStatus;
    private LogicalSafetyClassification logicalSafetyClassification;
    private LogicalSecurityClassification logicalSecurityClassification;
    private LogicalRedundancyType logicalRedundancyType;
    private LogicalConfigurationVariant logicalConfigurationVariant;
    private LogicalApplicability logicalApplicability;

    @Override
    public EntityType getEntityType() {
        return LOGICAL_STRUCTURE;
    }

    @Override
    public String getCode() {
        return logicalCode.getValue();
    }

    @Override
    public String getName() {
        return logicalName.getValue();
    }

    @Override
    public String getDescription() {
        return logicalDescription.getValue();
    }

    @Override
    public String getSortKey() {
        return getSortKeyValue(getCode());
    }

    @Override
    public void initializeFields() {
        logicalCode = new LogicalCode();
        logicalCodeLevel = new CodeLevel();
        logicalName = new LogicalName();
        logicalDescription = new LogicalDescription();

        logicalOwner = new LogicalOwner(getWebSession());
        logicalVerificationStatus = new LogicalVerificationStatus(getWebSession());
        logicalCriticality = new LogicalCriticality(getWebSession());
        logicalElementCategory = new LogicalElementCategory(getWebSession());
        logicalLevel = new LogicalLevel(getWebSession());
        logicalType = new LogicalType(getWebSession());
        logicalResponsibleDomain = new LogicalResponsibleDomain(getWebSession());
        logicalLifecycleStatus = new LogicalLifecycleStatus(getWebSession());
        logicalMaturity = new LogicalMaturity(getWebSession());
        logicalAllocationStatus = new LogicalAllocationStatus(getWebSession());
        logicalRealizationStatus = new LogicalRealizationStatus(getWebSession());
        logicalSafetyClassification = new LogicalSafetyClassification(getWebSession());
        logicalSecurityClassification = new LogicalSecurityClassification(getWebSession());
        logicalRedundancyType = new LogicalRedundancyType(getWebSession());
        logicalConfigurationVariant = new LogicalConfigurationVariant(getWebSession());
        logicalApplicability = new LogicalApplicability(getWebSession());
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
        entityElement.addElement(logicalCode);
        entityElement.addElement(logicalCodeLevel);
        entityElement.addElement(logicalName);
        entityElement.addElement(logicalDescription);
        entityElement.addElement(logicalOwner);
        entityElement.addElement(logicalVerificationStatus);
        entityElement.addElement(logicalCriticality);
        entityElement.addElement(logicalElementCategory);
        entityElement.addElement(logicalLevel);
        entityElement.addElement(logicalType);
        entityElement.addElement(logicalResponsibleDomain);
        entityElement.addElement(logicalLifecycleStatus);
        entityElement.addElement(logicalMaturity);
        entityElement.addElement(logicalAllocationStatus);
        entityElement.addElement(logicalRealizationStatus);
        entityElement.addElement(logicalSafetyClassification);
        entityElement.addElement(logicalSecurityClassification);
        entityElement.addElement(logicalRedundancyType);
        entityElement.addElement(logicalConfigurationVariant);
        entityElement.addElement(logicalApplicability);
    }

    @Override
    public void addAllFieldElementsForEdit(Entity entityElement) {
        logicalCode.setFieldNotEditable();
        entityElement.addElement(logicalCode);

        logicalCodeLevel.setFieldNotVisible();
        entityElement.addElement(logicalCodeLevel);

        logicalName.setFieldEditable();
        entityElement.addElement(logicalName);

        logicalDescription.setFieldEditable();
        logicalDescription.setFieldRequired();
        entityElement.addElement(logicalDescription);

        logicalOwner.setFieldEditable();
        logicalOwner.setFieldNotRequired();
        entityElement.addElement(logicalOwner);

        logicalVerificationStatus.setFieldEditable();
        logicalVerificationStatus.setFieldNotRequired();
        entityElement.addElement(logicalVerificationStatus);

        logicalCriticality.setFieldEditable();
        logicalCriticality.setFieldNotRequired();
        entityElement.addElement(logicalCriticality);

        logicalElementCategory.setFieldEditable();
        logicalElementCategory.setFieldNotRequired();
        entityElement.addElement(logicalElementCategory);

        logicalLevel.setFieldEditable();
        logicalLevel.setFieldNotRequired();
        entityElement.addElement(logicalLevel);

        logicalType.setFieldEditable();
        logicalType.setFieldNotRequired();
        entityElement.addElement(logicalType);

        logicalResponsibleDomain.setFieldEditable();
        logicalResponsibleDomain.setFieldNotRequired();
        entityElement.addElement(logicalResponsibleDomain);

        logicalLifecycleStatus.setFieldEditable();
        logicalLifecycleStatus.setFieldNotRequired();
        entityElement.addElement(logicalLifecycleStatus);

        logicalMaturity.setFieldEditable();
        logicalMaturity.setFieldNotRequired();
        entityElement.addElement(logicalMaturity);

        logicalAllocationStatus.setFieldEditable();
        logicalAllocationStatus.setFieldNotRequired();
        entityElement.addElement(logicalAllocationStatus);

        logicalRealizationStatus.setFieldEditable();
        logicalRealizationStatus.setFieldNotRequired();
        entityElement.addElement(logicalRealizationStatus);

        logicalSafetyClassification.setFieldEditable();
        logicalSafetyClassification.setFieldNotRequired();
        entityElement.addElement(logicalSafetyClassification);

        logicalSecurityClassification.setFieldEditable();
        logicalSecurityClassification.setFieldNotRequired();
        entityElement.addElement(logicalSecurityClassification);

        logicalRedundancyType.setFieldEditable();
        logicalRedundancyType.setFieldNotRequired();
        entityElement.addElement(logicalRedundancyType);

        logicalConfigurationVariant.setFieldEditable();
        logicalConfigurationVariant.setFieldNotRequired();
        entityElement.addElement(logicalConfigurationVariant);

        logicalApplicability.setFieldEditable();
        logicalApplicability.setFieldNotRequired();
        entityElement.addElement(logicalApplicability);
    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {
        LogicalStructureParentCodeSelector parentCodeSelector = new LogicalStructureParentCodeSelector(getWebSession());

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(getWebSession(), parentEntityId);
        logicalCode = new LogicalCode(true);
        logicalCode.setValue(nextCode);
        logicalCode.setFieldNotEditable();
        logicalCode.setFieldRequired();
        entityElement.addElement(logicalCode);

        logicalName.setFieldEditable();
        entityElement.addElement(logicalName);

        logicalDescription.setFieldEditable();
        logicalDescription.setFieldRequired();
        entityElement.addElement(logicalDescription);

        logicalOwner.setFieldEditable();
        logicalOwner.setFieldNotRequired();
        entityElement.addElement(logicalOwner);

        logicalVerificationStatus.setFieldEditable();
        logicalVerificationStatus.setFieldNotRequired();
        entityElement.addElement(logicalVerificationStatus);

        logicalCriticality.setFieldEditable();
        logicalCriticality.setFieldNotRequired();
        entityElement.addElement(logicalCriticality);

        logicalLevel.setFieldEditable();
        logicalLevel.setFieldNotRequired();
        entityElement.addElement(logicalLevel);

        logicalType.setFieldEditable();
        logicalType.setFieldNotRequired();
        entityElement.addElement(logicalType);

        logicalResponsibleDomain.setFieldEditable();
        logicalResponsibleDomain.setFieldNotRequired();
        entityElement.addElement(logicalResponsibleDomain);

        logicalLifecycleStatus.setFieldEditable();
        logicalLifecycleStatus.setFieldNotRequired();
        entityElement.addElement(logicalLifecycleStatus);

        logicalMaturity.setFieldEditable();
        logicalMaturity.setFieldNotRequired();
        entityElement.addElement(logicalMaturity);

        logicalAllocationStatus.setFieldEditable();
        logicalAllocationStatus.setFieldNotRequired();
        entityElement.addElement(logicalAllocationStatus);

        logicalRealizationStatus.setFieldEditable();
        logicalRealizationStatus.setFieldNotRequired();
        entityElement.addElement(logicalRealizationStatus);

        logicalSafetyClassification.setFieldEditable();
        logicalSafetyClassification.setFieldNotRequired();
        entityElement.addElement(logicalSafetyClassification);

        logicalSecurityClassification.setFieldEditable();
        logicalSecurityClassification.setFieldNotRequired();
        entityElement.addElement(logicalSecurityClassification);

        logicalRedundancyType.setFieldEditable();
        logicalRedundancyType.setFieldNotRequired();
        entityElement.addElement(logicalRedundancyType);

        logicalConfigurationVariant.setFieldEditable();
        logicalConfigurationVariant.setFieldNotRequired();
        entityElement.addElement(logicalConfigurationVariant);

        logicalApplicability.setFieldEditable();
        logicalApplicability.setFieldNotRequired();
        entityElement.addElement(logicalApplicability);

    }

    public LogicalStructureEntity() {}

    public LogicalStructureEntity(WebSession webSession) {
        super(webSession);
    }

    public LogicalStructureEntity(WebSession webSession, EntityRecord entityRecord) {
        super(webSession, entityRecord);

        for (EntityElementRecord elementRecord : entityRecord.getEntityElementRecords()) {

            EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

            if (entityDataElement != null) {
                switch (entityDataElement) {
                    case LOGICALCODE:
                        logicalCode.setValue(elementRecord.getStringValue());
                        break;
                    case CODELEVEL:
                        logicalCodeLevel.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALNAME:
                        logicalName.setValue(elementRecord.getStringValue());
                        break;
                    case LOGICALDESCRIPTION:
                        logicalDescription.setValue(elementRecord.getStringValue());
                        break;
                    case LOGICALOWNERID:
                        logicalOwner.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALVERIFICATIONID:
                        logicalVerificationStatus.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALCRITICALITYID:
                        logicalCriticality.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALELEMENTCATEGORYID:
                        logicalElementCategory.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALLEVELID:
                        logicalLevel.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALTYPEID:
                        logicalType.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALRESPONSIBLEDOMAINID:
                        logicalResponsibleDomain.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALLIFECYCLESTATUSID:
                        logicalLifecycleStatus.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALMATURITYID:
                        logicalMaturity.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALALLOCATIONSTATUSID:
                        logicalAllocationStatus.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALREALIZATIONSTATUSID:
                        logicalRealizationStatus.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALSAFETYCLASSIFICATIONID:
                        logicalSafetyClassification.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALSECURITYCLASSIFICATIONID:
                        logicalSecurityClassification.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALREDUNDANCYTYPEID:
                        logicalRedundancyType.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALCONFIGURATIONVARIANTID:
                        logicalConfigurationVariant.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALAPPLICABILITYID:
                        logicalApplicability.setValue(elementRecord.getIntegerValue());
                        break;
                }
            }
        }
    }

    public void setLogicalCode(String logicalCode) {
        this.logicalCode.setValue(logicalCode);
    }

    public LogicalCode getLogicalCode() {
        return logicalCode;
    }

    public String getLogicalCodeString() {
        return getCode();
    }

    public void setLogicalCodeLevel(Integer logicalCodeCodeLevel) {
        this.logicalCodeLevel.setValue(logicalCodeCodeLevel);
    }

    public CodeLevel getLogicalCodeLevel() {
        return logicalCodeLevel;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName.setValue(logicalName);
    }

    public LogicalName getLogicalName() {
        return logicalName;
    }

    public void setLogicalDescription(String logicalDescription) {
        this.logicalDescription.setValue(logicalDescription);
    }

    public LogicalDescription getLogicalDescription() {
        return logicalDescription;
    }

    public void setOwner(Integer ownerId) {
        this.logicalOwner.setValue(ownerId);
    }

    public Integer getOwnerId() {
        return logicalOwner.getValue();
    }
    public LogicalOwner getOwner() {
        return logicalOwner;
    }

    public void setLogicalVerificationStatusId(Integer logicalVerificationId) {
        this.logicalVerificationStatus.setValue(logicalVerificationId);
    }

    public Integer getLogicalVerificationStatusId() {
        return logicalVerificationStatus.getValue();
    }

    public LogicalVerificationStatus getLogicalVerificationStatus() {
        return logicalVerificationStatus;
    }

    public void setLogicalCriticalityId(Integer functionCriticalityId) {
        this.logicalCriticality.setValue(functionCriticalityId);
    }

    public Integer getLogicalCriticalityId() {
        return logicalCriticality.getValue();
    }

    public LogicalCriticality getLogicalCriticality() {
        return logicalCriticality;
    }


    public void setLogicalElementCategoryId(Integer logicalElementCategoryId) {
        this.logicalElementCategory.setValue(logicalElementCategoryId);
    }

    public Integer getLogicalElementCategoryId() {
        return logicalElementCategory.getValue();
    }

    public LogicalElementCategory getLogicalElementCategory() {
        return logicalElementCategory;
    }

    public LogicalLevel getLogicalLevel() {
        return logicalLevel;
    }

    public void setLogicalLevelId(Integer logicalLevelId) {
        this.logicalLevel.setValue(logicalLevelId);
    }

    public Integer getLogicalLevelId() {
        return logicalLevel.getValue();
    }


    public LogicalType getLogicalType() {
        return logicalType;
    }

    public void setLogicalTypeId(Integer logicalTypeId) {
        this.logicalType.setValue(logicalTypeId);
    }

    public Integer getLogicalTypeId() {
        return logicalType.getValue();
    }

    public LogicalResponsibleDomain getLogicalResponsibleDomain() {
        return logicalResponsibleDomain;
    }

    public void setLogicalResponsibleDomainId(Integer logicalResponsibleDomainId) {
        this.logicalResponsibleDomain.setValue(logicalResponsibleDomainId);
    }

    public Integer getLogicalResponsibleDomainId() {
        return logicalResponsibleDomain.getValue();
    }

    public LogicalLifecycleStatus getLogicalLifecycleStatus() {
        return logicalLifecycleStatus;
    }

    public void setLogicalLifecycleStatusId(Integer logicalLifecycleStatusId) {
        this.logicalLifecycleStatus.setValue(logicalLifecycleStatusId);
    }

    public Integer getLogicalLifecycleStatusId() {
        return logicalLifecycleStatus.getValue();
    }

    public LogicalMaturity getLogicalMaturity() {
        return logicalMaturity;
    }

    public void setLogicalMaturityId(Integer logicalMaturityId) {
        this.logicalMaturity.setValue(logicalMaturityId);
    }

    public Integer getLogicalMaturityId() {
        return logicalMaturity.getValue();
    }

    public LogicalAllocationStatus getLogicalAllocationStatus() {
        return logicalAllocationStatus;
    }

    public void setLogicalAllocationStatusId(Integer logicalAllocationStatusId) {
        this.logicalAllocationStatus.setValue(logicalAllocationStatusId);
    }

    public Integer getLogicalAllocationStatusId() {
        return logicalAllocationStatus.getValue();
    }

    public LogicalRealizationStatus getLogicalRealizationStatus() {
        return logicalRealizationStatus;
    }

    public void setLogicalRealizationStatusId(Integer logicalRealizationStatusId) {
        this.logicalRealizationStatus.setValue(logicalRealizationStatusId);
    }

    public Integer getLogicalRealizationStatusId() {
        return logicalRealizationStatus.getValue();
    }

    public LogicalSafetyClassification getLogicalSafetyClassification() {
        return logicalSafetyClassification;
    }

    public void setLogicalSafetyClassificationId(Integer logicalSafetyClassificationId) {
        this.logicalSafetyClassification.setValue(logicalSafetyClassificationId);
    }

    public Integer getLogicalSafetyClassificationId() {
        return logicalSafetyClassification.getValue();
    }

    public LogicalSecurityClassification getLogicalSecurityClassification() {
        return logicalSecurityClassification;
    }

    public void setLogicalSecurityClassificationId(Integer logicalSecurityClassificationId) {
        this.logicalSecurityClassification.setValue(logicalSecurityClassificationId);
    }

    public Integer getLogicalSecurityClassificationId() {
        return logicalSecurityClassification.getValue();
    }

    public LogicalRedundancyType getLogicalRedundancyType() {
        return logicalRedundancyType;
    }

    public void setLogicalRedundancyTypeId(Integer logicalRedundancyTypeId) {
        this.logicalRedundancyType.setValue(logicalRedundancyTypeId);
    }

    public Integer getLogicalRedundancyTypeId() {
        return logicalRedundancyType.getValue();
    }

    public LogicalConfigurationVariant getLogicalConfigurationVariant() {
        return logicalConfigurationVariant;
    }

    public void setLogicalConfigurationVariantId(Integer logicalConfigurationVariantId) {
        this.logicalConfigurationVariant.setValue(logicalConfigurationVariantId);
    }

    public Integer getLogicalConfigurationVariantId() {
        return logicalConfigurationVariant.getValue();
    }

    public LogicalApplicability getLogicalApplicability() {
        return logicalApplicability;
    }

    public void setLogicalApplicabilityId(Integer logicalApplicabilityId) {
        this.logicalApplicability.setValue(logicalApplicabilityId);
    }

    public Integer getLogicalApplicabilityId() {
        return logicalApplicability.getValue();
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(LogicalCode.FIELD_NAME, getLogicalCode().getValue()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, getLogicalCodeLevel().getValue()));
        addDataElement(new StringDataElement(LogicalName.FIELD_NAME, getLogicalName().getValue()));
        addDataElement(new StringDataElement(LogicalDescription.FIELD_NAME, getLogicalDescription().getValue()));
        addDataElement(new IntegerDataElement(LogicalOwner.FIELD_NAME, getOwnerId()));
        addDataElement(new IntegerDataElement(LogicalVerificationStatus.FIELD_NAME, getLogicalVerificationStatusId()));
        addDataElement(new IntegerDataElement(LogicalCriticality.FIELD_NAME, getLogicalCriticalityId()));
        addDataElement(new IntegerDataElement(LogicalElementCategory.FIELD_NAME, getLogicalElementCategoryId()));
        addDataElement(new IntegerDataElement(LogicalLevel.FIELD_NAME, getLogicalLevelId()));
        addDataElement(new IntegerDataElement(LogicalType.FIELD_NAME, getLogicalTypeId()));
        addDataElement(new IntegerDataElement(LogicalResponsibleDomain.FIELD_NAME, getLogicalResponsibleDomainId()));
        addDataElement(new IntegerDataElement(LogicalLifecycleStatus.FIELD_NAME, getLogicalLifecycleStatusId()));
        addDataElement(new IntegerDataElement(LogicalMaturity.FIELD_NAME, getLogicalMaturityId()));
        addDataElement(new IntegerDataElement(LogicalAllocationStatus.FIELD_NAME, getLogicalAllocationStatusId()));
        addDataElement(new IntegerDataElement(LogicalRealizationStatus.FIELD_NAME, getLogicalRealizationStatusId()));
        addDataElement(new IntegerDataElement(LogicalSafetyClassification.FIELD_NAME, getLogicalSafetyClassificationId()));
        addDataElement(new IntegerDataElement(LogicalSecurityClassification.FIELD_NAME, getLogicalSecurityClassificationId()));
        addDataElement(new IntegerDataElement(LogicalRedundancyType.FIELD_NAME, getLogicalRedundancyTypeId()));
        addDataElement(new IntegerDataElement(LogicalConfigurationVariant.FIELD_NAME, getLogicalConfigurationVariantId()));
        addDataElement(new IntegerDataElement(LogicalApplicability.FIELD_NAME, getLogicalApplicabilityId()));
    }

}
