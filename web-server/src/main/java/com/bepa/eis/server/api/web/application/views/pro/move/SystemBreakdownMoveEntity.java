package com.bepa.eis.server.api.web.application.views.pro.move;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;

import java.util.List;

public class SystemBreakdownMoveEntity extends AbstractMoveEntity {

    private SystemBreakdownProvider systemBreakdownProvider;
    private List<SystemBreakdownEntity> listOfSystemBreakdownEntities;

    public SystemBreakdownMoveEntity(WebSession webSession, MoveRequest moveRequest) {
        super(webSession, moveRequest);
        systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
    }

    @Override
    public void getAllEntities() {
        listOfSystemBreakdownEntities = systemBreakdownProvider.getAllSystemBreakdown(true);
    }

    @Override
    void getEntitiesToMove() {
        for (SystemBreakdownEntity systemBreakdownEntity : listOfSystemBreakdownEntities) {
            if (systemBreakdownEntity.getCode().startsWith(getMoveRequest().fromCode())) {
                addEntityToMove(systemBreakdownEntity);
            }
        }
    }

    @Override
    public EntityProvider getEntityProvider() {
        if (systemBreakdownProvider == null) {
            systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
        }
        return systemBreakdownProvider;
    }
}
