package com.bepa.eis.server.entites.stakeholderrequirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;

import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER_REQUIREMENT;

public class StakeholderRequirementEntity extends AbstractEntity {

    private String requirementCode;
    private Integer requirementCodeLevel;
    private String requirementName;
    private String requirementDescription;

    @Override
    public EntityType getEntityType() {
        return STAKEHOLDER_REQUIREMENT;
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

    public StakeholderRequirementEntity() {}

    public StakeholderRequirementEntity(WebSession session) {
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

    public void addAllDataElements() {
        addDataElement(new StringDataElement(StakeholderRequirementCode.FIELD_NAME, getRequirementCode()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, getRequirementCodeLevel()));
        addDataElement(new StringDataElement(RequirementName.FIELD_NAME, getRequirementName()));
        addDataElement(new StringDataElement(RequirementDescription.FIELD_NAME, getRequirementDescription()));
    }

    public static StakeholderRequirementEntity map(EntityRecord entity) {

        StakeholderRequirementEntity requirementEntity = null;

        if (entity != null) {

            requirementEntity = new StakeholderRequirementEntity(entity.getWebSession());

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
                        case BASISREQCODE :
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
                    }
                }
            }

        }
        return requirementEntity;
    }
}
