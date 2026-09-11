package com.bepa.eis.server.api.web.application.views.pro.move;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.dataprovider.entities.LogicalStructureProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.logical.LogicalStructureEntity;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;

import java.util.List;

public class StakeholderRequirementMoveEntity extends AbstractMoveEntity {

    private StakeholderRequirementProvider stakeholderRequirementProvider;
    private List<StakeholderRequirementEntity> listOfStakeholderRequirementEntities;

    public StakeholderRequirementMoveEntity(WebSession webSession, MoveRequest moveRequest) {
        super(webSession, moveRequest);
        stakeholderRequirementProvider = new StakeholderRequirementProvider(getWebSession());
    }

    @Override
    public void getAllEntities() {
        listOfStakeholderRequirementEntities = stakeholderRequirementProvider.getAllStakeholderRequirement(true);
    }

    @Override
    void getEntitiesToMove() {
        for (StakeholderRequirementEntity stakeholderRequirementEntity : listOfStakeholderRequirementEntities) {
            if (stakeholderRequirementEntity.getCode().startsWith(getMoveRequest().fromCode())) {
                addEntityToMove(stakeholderRequirementEntity);
            }
        }
    }

    @Override
    public EntityProvider getEntityProvider() {
        if (stakeholderRequirementProvider == null) {
            stakeholderRequirementProvider = new StakeholderRequirementProvider(getWebSession());
        }
        return stakeholderRequirementProvider;
    }
}
