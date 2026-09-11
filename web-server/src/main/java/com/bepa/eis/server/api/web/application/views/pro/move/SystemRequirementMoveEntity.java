package com.bepa.eis.server.api.web.application.views.pro.move;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;

import java.util.List;

public class SystemRequirementMoveEntity extends AbstractMoveEntity {

    private SystemRequirementProvider systemRequirementProvider;
    private List<SystemRequirementEntity> listOfSystemRequirementEntities;

    public SystemRequirementMoveEntity(WebSession webSession, MoveRequest moveRequest) {
        super(webSession, moveRequest);
        systemRequirementProvider = new SystemRequirementProvider(getWebSession());
    }

    @Override
    public void getAllEntities() {
        listOfSystemRequirementEntities = systemRequirementProvider.getAllSystemRequirement(true);
    }

    @Override
    void getEntitiesToMove() {
        for (SystemRequirementEntity systemRequirementEntity : listOfSystemRequirementEntities) {
            if (systemRequirementEntity.getCode().startsWith(getMoveRequest().fromCode())) {
                addEntityToMove(systemRequirementEntity);
            }
        }
    }

    @Override
    public EntityProvider getEntityProvider() {
        if (systemRequirementProvider == null) {
            systemRequirementProvider = new SystemRequirementProvider(getWebSession());
        }
        return systemRequirementProvider;
    }
}
