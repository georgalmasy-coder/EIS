package com.bepa.eis.server.api.web.application.views.pro.move;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.dataprovider.entities.FunctionalStructureProvider;
import com.bepa.eis.server.dataprovider.entities.LogicalStructureProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.functional.FunctionalStructureEntity;
import com.bepa.eis.server.entites.logical.LogicalStructureEntity;

import java.util.List;

public class LogicalStructureMoveEntity extends AbstractMoveEntity {

    private LogicalStructureProvider logicalStructureProvider;
    private List<LogicalStructureEntity> listOfLogicalStructureEntities;

    public LogicalStructureMoveEntity(WebSession webSession, MoveRequest moveRequest) {
        super(webSession, moveRequest);
        logicalStructureProvider = new LogicalStructureProvider(getWebSession());
    }

    @Override
    public void getAllEntities() {
        listOfLogicalStructureEntities = logicalStructureProvider.getAllLogicalStructures(true);
    }

    @Override
    void getEntitiesToMove() {
        for (LogicalStructureEntity logicalStructureEntity : listOfLogicalStructureEntities) {
            if (logicalStructureEntity.getCode().startsWith(getMoveRequest().fromCode())) {
                addEntityToMove(logicalStructureEntity);
            }
        }
    }

    @Override
    public EntityProvider getEntityProvider() {
        if (logicalStructureProvider == null) {
            logicalStructureProvider = new LogicalStructureProvider(getWebSession());
        }
        return logicalStructureProvider;
    }
}
