package com.bepa.eis.server.api.web.application.views.pro.move;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.dataprovider.entities.FunctionalStructureProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.functional.FunctionalStructureEntity;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FunctionalStructureMoveEntity extends AbstractMoveEntity {

    private FunctionalStructureProvider functionalStructureProvider;
    private List<FunctionalStructureEntity> listOfFunctionalStructureEntities;

    public FunctionalStructureMoveEntity(WebSession webSession, MoveRequest moveRequest) {
        super(webSession, moveRequest);
        functionalStructureProvider = new FunctionalStructureProvider(getWebSession());
    }

    @Override
    public void getAllEntities() {
        listOfFunctionalStructureEntities = functionalStructureProvider.getAllFunctionalStructure(true);
    }

    @Override
    void getEntitiesToMove() {
        for (FunctionalStructureEntity functionalStructureEntity : listOfFunctionalStructureEntities) {
            if (functionalStructureEntity.getCode().startsWith(getMoveRequest().fromCode())) {
                addEntityToMove(functionalStructureEntity);
            }
        }
    }

    @Override
    public EntityProvider getEntityProvider() {
        if (functionalStructureProvider == null) {
            functionalStructureProvider = new FunctionalStructureProvider(getWebSession());
        }
        return functionalStructureProvider;
    }

}
