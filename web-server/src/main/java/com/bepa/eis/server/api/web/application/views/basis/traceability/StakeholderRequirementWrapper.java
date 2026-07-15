package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.common.enums.entity.EntityType;

import java.util.List;

public class StakeholderRequirementWrapper {

    private final StakeholderRequirementEntity stakeholderRequirementEntity;
    private final List<SystemRequirementWrapper> listOfSystemRequirementWrappers;

    protected StakeholderRequirementWrapper(StakeholderRequirementEntity stakeholderRequirementEntity, List<SystemRequirementWrapper> listOfSystemRequirementWrappers) {
        this.stakeholderRequirementEntity = stakeholderRequirementEntity;
        this.listOfSystemRequirementWrappers = listOfSystemRequirementWrappers;
    }

    protected EntityId getEntityId() {
        return stakeholderRequirementEntity.getEntityId();
    }

    protected EntityType getEntityType() {
        return stakeholderRequirementEntity.getEntityType();
    }

    protected String getRequirementCode() {
        return stakeholderRequirementEntity.getRequirementCode().getValue();
    }

    protected String getRequirementName() {
        return stakeholderRequirementEntity.getRequirementName().getValue();
    }

    protected String getRequirementDescription() {
        return stakeholderRequirementEntity.getRequirementDescription().getValue();
    }

    protected List<SystemRequirementWrapper> getListOfSystemRequirements() {
        return listOfSystemRequirementWrappers;
    }

    protected boolean hasRelationToAnySystemRequirement() {
        for (SystemRequirementWrapper systemRequirementWrapper : listOfSystemRequirementWrappers) {
            if (systemRequirementWrapper.hasRelationToStakeholderRequirement(this)) {
                return true;
            }
        }
        return false;
    }
}
