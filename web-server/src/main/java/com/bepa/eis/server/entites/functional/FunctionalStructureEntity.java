package com.bepa.eis.server.entites.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.FunctionalStructureParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.functional.*;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;

import static com.bepa.eis.common.enums.entity.EntityType.FUNCTIONAL_STRUCTURE;

public class FunctionalStructureEntity extends AbstractEntity {

    private FunctionalCode functionCode;
    private CodeLevel functionalCodeLevel;
    private FunctionalName functionalName;
    private FunctionalDescription functionalDescription;
    private FunctionOwner functionOwner;
    private FunctionStatus functionStatus;
    private FunctionBehaviorType functionBehaviorType;
    private FunctionCategory functionCategory;
    private FunctionCriticality functionCriticality;
    private FunctionVerificationStatus functionVerificationStatus;
    private FunctionLevel functionLevel;
    private FunctionOperatingMode functionOperatingMode;
    private FunctionResponsibleDomain functionResponsibleDomain;
    private FunctionConfigurationVariant functionConfigurationVariant;
    private FunctionApplicability functionApplicability;

    @Override
    public EntityType getEntityType() {
        return FUNCTIONAL_STRUCTURE;
    }

    @Override
    public String getCode() {
        return functionCode.getValue();
    }

    @Override
    public String getName() {
        return functionalName.getValue();
    }

    @Override
    public String getDescription() {
        return functionalDescription.getValue();
    }

    @Override
    public String getSortKey() {
        return getSortKeyValue(getCode());
    }

    @Override
    public void initializeFields() {
        functionCode = new FunctionalCode();
        functionalCodeLevel = new CodeLevel();
        functionalName = new FunctionalName();
        functionalDescription = new FunctionalDescription();
        functionOwner = new FunctionOwner(getWebSession());
        functionStatus = new FunctionStatus(getWebSession());
        functionBehaviorType = new FunctionBehaviorType(getWebSession());
        functionCategory = new FunctionCategory(getWebSession());
        functionCriticality = new FunctionCriticality(getWebSession());
        functionVerificationStatus = new FunctionVerificationStatus(getWebSession());
        functionLevel = new FunctionLevel(getWebSession());
        functionOperatingMode = new FunctionOperatingMode(getWebSession());
        functionResponsibleDomain = new FunctionResponsibleDomain(getWebSession());
        functionConfigurationVariant = new FunctionConfigurationVariant(getWebSession());
        functionApplicability = new FunctionApplicability(getWebSession());
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
        entityElement.addElement(functionCode);
        entityElement.addElement(functionalCodeLevel);
        entityElement.addElement(functionalName);
        entityElement.addElement(functionalDescription);
        entityElement.addElement(functionOwner);
        entityElement.addElement(functionStatus);
        entityElement.addElement(functionBehaviorType);
        entityElement.addElement(functionCategory);
        entityElement.addElement(functionCriticality);
        entityElement.addElement(functionVerificationStatus);
        entityElement.addElement(functionLevel);
        entityElement.addElement(functionOperatingMode);
        entityElement.addElement(functionResponsibleDomain);
        entityElement.addElement(functionConfigurationVariant);
        entityElement.addElement(functionApplicability);
    }

    @Override
    public void addAllFieldElementsForEdit(Entity entityElement) {
        functionCode.setFieldNotEditable();
        entityElement.addElement(functionCode);

        functionalCodeLevel.setFieldNotVisible();
        entityElement.addElement(functionalCodeLevel);

        functionalName.setFieldEditable();
        entityElement.addElement(functionalName);

        functionalDescription.setFieldEditable();
        functionalDescription.setFieldRequired();
        entityElement.addElement(functionalDescription);

        functionOwner.setFieldEditable();
        entityElement.addElement(functionOwner);

        functionStatus.setFieldEditable();
        entityElement.addElement(functionStatus);

        functionBehaviorType.setFieldEditable();
        entityElement.addElement(functionBehaviorType);

        functionCategory.setFieldEditable();
        entityElement.addElement(functionCategory);

        functionCriticality.setFieldEditable();
        entityElement.addElement(functionCriticality);

        functionVerificationStatus.setFieldEditable();
        entityElement.addElement(functionVerificationStatus);

        functionLevel.setFieldEditable();
        functionLevel.setFieldNotRequired();
        entityElement.addElement(functionLevel);

        functionOperatingMode.setFieldEditable();
        functionOperatingMode.setFieldNotRequired();
        entityElement.addElement(functionOperatingMode);

        functionResponsibleDomain.setFieldEditable();
        functionResponsibleDomain.setFieldNotRequired();
        entityElement.addElement(functionResponsibleDomain);

        functionConfigurationVariant.setFieldEditable();
        functionConfigurationVariant.setFieldNotRequired();
        entityElement.addElement(functionConfigurationVariant);

        functionApplicability.setFieldEditable();
        functionApplicability.setFieldNotRequired();
        entityElement.addElement(functionApplicability);

    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {
        FunctionalStructureParentCodeSelector parentCodeSelector = new FunctionalStructureParentCodeSelector(getWebSession());

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(getWebSession(), parentEntityId);
        functionCode = new FunctionalCode(true);
        functionCode.setValue(nextCode);
        functionCode.setFieldNotEditable();
        functionCode.setFieldRequired();
        entityElement.addElement(functionCode);

        functionalName.setFieldEditable();
        entityElement.addElement(functionalName);

        functionalDescription.setFieldEditable();
        functionalDescription.setFieldRequired();
        entityElement.addElement(functionalDescription);

        functionOwner.setFieldEditable();
        functionOwner.setFieldNotRequired();
        entityElement.addElement(functionOwner);

        functionStatus.setFieldEditable();
        functionStatus.setFieldNotRequired();
        entityElement.addElement(functionStatus);

        functionBehaviorType.setFieldEditable();
        functionBehaviorType.setFieldNotRequired();
        entityElement.addElement(functionBehaviorType);

        functionCategory.setFieldEditable();
        functionCategory.setFieldNotRequired();
        entityElement.addElement(functionCategory);

        functionCriticality.setFieldEditable();
        functionCriticality.setFieldNotRequired();
        entityElement.addElement(functionCriticality);

        functionVerificationStatus.setFieldEditable();
        functionVerificationStatus.setFieldNotRequired();
        entityElement.addElement(functionVerificationStatus);

        functionLevel.setFieldEditable();
        functionLevel.setFieldNotRequired();
        entityElement.addElement(functionLevel);

        functionOperatingMode.setFieldEditable();
        functionOperatingMode.setFieldNotRequired();
        entityElement.addElement(functionOperatingMode);

        functionResponsibleDomain.setFieldEditable();
        functionResponsibleDomain.setFieldNotRequired();
        entityElement.addElement(functionResponsibleDomain);

        functionConfigurationVariant.setFieldEditable();
        functionConfigurationVariant.setFieldNotRequired();
        entityElement.addElement(functionConfigurationVariant);

        functionApplicability.setFieldEditable();
        functionApplicability.setFieldNotRequired();
        entityElement.addElement(functionApplicability);

    }

    public FunctionalStructureEntity() {}

    public FunctionalStructureEntity(WebSession webSession) {
        super(webSession);
    }

    public FunctionalStructureEntity(WebSession webSession, EntityRecord entityRecord) {
        super(webSession, entityRecord);

        for (EntityElementRecord elementRecord : entityRecord.getEntityElementRecords()) {

            EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

            if (entityDataElement != null) {
                switch (entityDataElement) {
                    case FUNCTIONCODE:
                        functionCode.setValue(elementRecord.getStringValue());
                        break;
                    case CODELEVEL:
                        functionalCodeLevel.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONNAME:
                        functionalName.setValue(elementRecord.getStringValue());
                        break;
                    case FUNCTIONDESCRIPTION:
                        functionalDescription.setValue(elementRecord.getStringValue());
                        break;
                    case FUNCTIONOWNERID:
                        functionOwner.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONSTATUSID:
                        functionStatus.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONBEHAVIORTYPEID:
                        functionBehaviorType.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONCATEGORYID:
                        functionCategory.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONCRITICALITYID:
                        functionCriticality.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONVERIFICATIONID:
                        functionVerificationStatus.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONLEVELID:
                        functionLevel.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONOPERATINGMODEID:
                        functionOperatingMode.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONRESPONSIBLEDOMAINID:
                        functionResponsibleDomain.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONCONFIGURATIONVARIANTID:
                        functionConfigurationVariant.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONAPPLICABILITYID:
                        functionApplicability.setValue(elementRecord.getIntegerValue());
                        break;

                }
            }
        }
    }

    public void setFunctionalCode(String functionalCode) {
        this.functionCode.setValue(functionalCode);
    }

    public FunctionalCode getFunctionalCode() {
        return functionCode;
    }

    public String getFunctionalCodeString() {
        return functionCode.getValue();
    }

    public void setFunctionalCodeLevel(Integer functionalCodeLevel) {
        this.functionalCodeLevel.setValue(functionalCodeLevel);
    }

    public CodeLevel geFunctionalCodeLevel() {
        return functionalCodeLevel;
    }

    public void setFunctionalName(String functionalName) {
        this.functionalName.setValue(functionalName);
    }

    public FunctionalName getFunctionalName() {
        return functionalName;
    }

    public void setFunctionalDescription(String functionalDescription) {
        this.functionalDescription.setValue(functionalDescription);
    }

    public FunctionalDescription getFunctionalDescription() {
        return functionalDescription;
    }

    public void setOwner(Integer ownerId) {
        this.functionOwner.setValue(ownerId);
    }

    public Integer getOwnerId() {
        return functionOwner.getValue();
    }
    public FunctionOwner getOwner() {
        return functionOwner;
    }

    public void setStatusId(Integer statusId) {
        this.functionStatus.setValue(statusId);
    }

    public Integer getStatusId() {
        return functionStatus.getValue();
    }

    public FunctionStatus getStatus() {
        return functionStatus;
    }

    public void setBehaviorTypeId(Integer behaviorTypeId) {
        this.functionBehaviorType.setValue(behaviorTypeId);
    }

    public Integer getBehaviorTypeId() {
        return functionBehaviorType.getValue();
    }

    public FunctionBehaviorType getBehaviorType() {
        return functionBehaviorType;
    }

    public void setFunctionCategoryId(Integer functionCategoryId) {
        this.functionCategory.setValue(functionCategoryId);
    }

    public Integer getFunctionCategoryId() {
        return functionCategory.getValue();
    }

    public FunctionCategory getFunctionCategory() {
        return functionCategory;
    }

    public void setFunctionCriticalityId(Integer functionCriticalityId) {
        this.functionCriticality.setValue(functionCriticalityId);
    }

    public Integer getFunctionCriticalityId() {
        return functionCriticality.getValue();
    }

    public FunctionCriticality getFunctionCriticality() {
        return functionCriticality;
    }

    public void setFunctionVerificationStatusId(Integer functionVerificationId) {
        this.functionVerificationStatus.setValue(functionVerificationId);
    }

    public Integer getFunctionVerificationStatusId() {
        return functionVerificationStatus.getValue();
    }

    public FunctionVerificationStatus getFunctionVerificationStatus() {
        return functionVerificationStatus;
    }

    public void setFunctionLevelId(Integer functionLevelId) {
        this.functionLevel.setValue(functionLevelId);
    }

    public Integer getFunctionLevelId() {
        return functionLevel.getValue();
    }

    public FunctionLevel getFunctionLevel() {
        return functionLevel;
    }

    public void setFunctionOperatingModeId(Integer functionOperatingModeId) {
        this.functionOperatingMode.setValue(functionOperatingModeId);
    }

    public Integer getFunctionOperatingModeId() {
        return functionOperatingMode.getValue();
    }

    public FunctionOperatingMode getFunctionOperatingMode() {
        return functionOperatingMode;
    }

    public void setFunctionResponsibleDomainId(Integer functionResponsibleDomainId) {
        this.functionResponsibleDomain.setValue(functionResponsibleDomainId);
    }

    public Integer getFunctionResponsibleDomainId() {
        return functionResponsibleDomain.getValue();
    }

    public FunctionResponsibleDomain getFunctionResponsibleDomain() {
        return functionResponsibleDomain;
    }

    public void setFunctionConfigurationVariantId(Integer functionConfigurationVariantId) {
        this.functionConfigurationVariant.setValue(functionConfigurationVariantId);
    }

    public Integer getFunctionConfigurationVariantId() {
        return functionConfigurationVariant.getValue();
    }

    public FunctionConfigurationVariant getFunctionConfigurationVariant() {
        return functionConfigurationVariant;
    }

    public void setFunctionApplicabilityId(Integer functionApplicabilityId) {
        this.functionApplicability.setValue(functionApplicabilityId);
    }

    public Integer getFunctionApplicabilityId() {
        return functionApplicability.getValue();
    }

    public FunctionApplicability getFunctionApplicability() {
        return functionApplicability;
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(FunctionalCode.FIELD_NAME, getFunctionalCode().getValue()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, geFunctionalCodeLevel().getValue()));
        addDataElement(new StringDataElement(FunctionalName.FIELD_NAME, getFunctionalName().getValue()));
        addDataElement(new StringDataElement(FunctionalDescription.FIELD_NAME, getFunctionalDescription().getValue()));
        addDataElement(new IntegerDataElement(FunctionOwner.FIELD_NAME, getOwnerId()));
        addDataElement(new IntegerDataElement(FunctionStatus.FIELD_NAME, getStatusId()));
        addDataElement(new IntegerDataElement(FunctionBehaviorType.FIELD_NAME, getBehaviorTypeId()));
        addDataElement(new IntegerDataElement(FunctionCategory.FIELD_NAME, getFunctionCategoryId()));
        addDataElement(new IntegerDataElement(FunctionCriticality.FIELD_NAME, getFunctionCriticalityId()));
        addDataElement(new IntegerDataElement(FunctionVerificationStatus.FIELD_NAME, getFunctionVerificationStatusId()));
        addDataElement(new IntegerDataElement(FunctionLevel.FIELD_NAME, getFunctionLevelId()));
        addDataElement(new IntegerDataElement(FunctionOperatingMode.FIELD_NAME, getFunctionOperatingModeId()));
        addDataElement(new IntegerDataElement(FunctionResponsibleDomain.FIELD_NAME, getFunctionResponsibleDomainId()));
        addDataElement(new IntegerDataElement(FunctionConfigurationVariant.FIELD_NAME, getFunctionConfigurationVariantId()));
        addDataElement(new IntegerDataElement(FunctionApplicability.FIELD_NAME, getFunctionApplicabilityId()));

    }

}
