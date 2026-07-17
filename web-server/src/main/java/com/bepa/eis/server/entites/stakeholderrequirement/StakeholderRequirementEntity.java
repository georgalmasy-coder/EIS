package com.bepa.eis.server.entites.stakeholderrequirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.StakeholderRequirementParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.RequirementVerificationStatus;
import com.bepa.eis.server.dataprovider.fields.lookups.stakeholder.Stakeholder;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;

import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER_REQUIREMENT;

public class StakeholderRequirementEntity extends AbstractEntity {

    private StakeholderRequirementCode requirementCode;
    private CodeLevel requirementCodeLevel;
    private RequirementName requirementName;
    private RequirementDescription requirementDescription;
    private Stakeholder stakeholder;

    @Override
    public EntityType getEntityType() {
        return STAKEHOLDER_REQUIREMENT;
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
    public void initializeFields() {
        requirementCode = new StakeholderRequirementCode();
        requirementCodeLevel = new CodeLevel();
        requirementName = new RequirementName();
        requirementDescription = new RequirementDescription();
        stakeholder = new Stakeholder(getWebSession());
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
        entityElement.addElement(requirementCode);
        entityElement.addElement(requirementCodeLevel);
        entityElement.addElement(requirementName);
        entityElement.addElement(requirementDescription);
        entityElement.addElement(stakeholder);
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

        stakeholder.setFieldEditable();
        stakeholder.setFieldNotRequired();
        entityElement.addElement(stakeholder);
    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {
        StakeholderRequirementParentCodeSelector parentCodeSelector = new StakeholderRequirementParentCodeSelector(getWebSession());

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(getWebSession(), parentEntityId);
        requirementCode = new StakeholderRequirementCode(true);
        requirementCode.setValue(nextCode);
        requirementCode.setFieldNotEditable();
        requirementCode.setFieldRequired();
        entityElement.addElement(requirementCode);

        requirementName.setFieldEditable();
        entityElement.addElement(requirementName);

        requirementDescription.setFieldEditable();
        requirementDescription.setFieldRequired();
        entityElement.addElement(requirementDescription);

        stakeholder.setFieldEditable();
        stakeholder.setFieldNotRequired();
        entityElement.addElement(stakeholder);
    }

    public StakeholderRequirementEntity() {}

    public StakeholderRequirementEntity(WebSession webSession) {
        super(webSession);
    }

    public StakeholderRequirementEntity(WebSession webSession, EntityRecord entityRecord) {
        super(webSession, entityRecord);

        for (EntityElementRecord elementRecord : entityRecord.getEntityElementRecords()) {

            EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

            if (entityDataElement != null) {
                switch (entityDataElement) {
                    case BASISREQCODE:
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
                    case STAKEHOLDER:
                        stakeholder.setValue(elementRecord.getIntegerValue());
                        break;

                }
            }
        }
    }

    public void setRequirementCode(String requirementCode) {
        this.requirementCode.setValue(requirementCode);
    }

    public StakeholderRequirementCode getRequirementCode() {
        return requirementCode;
    }

    public String getRequirementCodeString() {
        return requirementCode.getValue();
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

    public void setStakeholderId(Integer stakeholderId) {
        this.stakeholder.setValue(stakeholderId);
    }

    public Stakeholder getStakeholder() {
        return stakeholder;
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(StakeholderRequirementCode.FIELD_NAME, getRequirementCode().getValue()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, getRequirementCodeLevel().getValue()));
        addDataElement(new StringDataElement(RequirementName.FIELD_NAME, getRequirementName().getValue()));
        addDataElement(new StringDataElement(RequirementDescription.FIELD_NAME, getRequirementDescription().getValue()));
        addDataElement(new IntegerDataElement(Stakeholder.FIELD_NAME, getStakeholder().getValue()));
    }

}
