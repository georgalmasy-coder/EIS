package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;
import com.bepa.eis.common.enums.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER_REQUIREMENT;

public class SystemRequirementWrapper {

    private static final Logger log = LoggerFactory.getLogger(SystemRequirementWrapper.class);

    private final SystemRequirementEntity systemRequirementEntity;
    private final List<EntityRelationRecord> listOfRelationsToStakeholderRequirements = new ArrayList<>();
    private List<StakeholderRequirementWrapper> listOfStakeholderRequirementWrappers = new ArrayList<>();

    protected SystemRequirementWrapper(SystemRequirementEntity systemRequirementEntity) {
        this.systemRequirementEntity = systemRequirementEntity;
    }

    protected void addRelationToStakeholderRequirement(EntityRelationRecord relation) {
        listOfRelationsToStakeholderRequirements.add(relation);
    }

    protected boolean hasRelationToStakeholderRequirement(StakeholderRequirementWrapper stakeholderRequirementWrapper) {

        for (EntityRelationRecord relation : listOfRelationsToStakeholderRequirements) {
            if (Objects.equals(relation.getRelatedEntityId(), stakeholderRequirementWrapper.getEntityId())
                    && relation.getRelatedEntityType() == STAKEHOLDER_REQUIREMENT) {
                return true;
            }
            if (Objects.equals(relation.getEntityId(), stakeholderRequirementWrapper.getEntityId())
                    && relation.getEntityType() == STAKEHOLDER_REQUIREMENT) {
                return true;
            }
        }
        return false;
    }

    protected EntityId getEntityId() {
        return systemRequirementEntity.getEntityId();
    }

    protected EntityType getEntityType() {
        return systemRequirementEntity.getEntityType();
    }

    protected String getSortKey() {
        return systemRequirementEntity.getSortKey();
    }

    protected String getRequirementCode() {
        return systemRequirementEntity.getRequirementCode().getValue();
    }

    protected String getRequirementName() {
        return systemRequirementEntity.getRequirementName().getValue();
    }

    protected String getRequirementDescription() {
        return systemRequirementEntity.getRequirementDescription().getValue();
    }

    protected void setListOfStakeholderRequirements(List<StakeholderRequirementWrapper> listOfStakeholderRequirementWrappers) {
        this.listOfStakeholderRequirementWrappers = listOfStakeholderRequirementWrappers;
    }

    protected List<StakeholderRequirementWrapper> getListOfStakeholderRequirements() {
        return listOfStakeholderRequirementWrappers;
    }
}

