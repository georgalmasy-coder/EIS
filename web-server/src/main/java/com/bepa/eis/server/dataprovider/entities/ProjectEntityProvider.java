package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.AbstractEntity;

import java.util.ArrayList;
import java.util.List;

public class ProjectEntityProvider extends EntityProvider {

    private final static EntityType entityType = EntityType.PROJECT;

    public ProjectEntityProvider(WebSession webSession) {
        super(webSession);
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows)  {
        return new ArrayList<>();
    }

}
