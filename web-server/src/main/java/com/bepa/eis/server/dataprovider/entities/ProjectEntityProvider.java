package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.entites.AbstractEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.bepa.eis.common.enums.entity.EntityDataElement.*;

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
    public EntityDataElement[] getEntityDataElementForList() {
        return new EntityDataElement[]{
                PROJECTNAME};
    }

    @Override
    public EntityDataElement[] getEntityDataElementForEdit() {
        return new EntityDataElement[]{
                PROJECTNAME};
    }

    @Override
    public EntityDataElement[] getEntityDataElementForCreate() {
        return new EntityDataElement[]{
                PROJECTNAME};
    }

    @Override
    public void addAllFieldElementsForList(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity) {
    }

    @Override
    public void addAllFieldElementsForEdit(ConcurrentHashMap<Integer, AbstractField> mapOfLoadedFields, Entity entity) {
    }

    @Override
    public void addAllFieldElementsForCreate(WebSession webSession, Entity entity, Integer parentEntityId) {
    }

    @Override
    public List<AbstractEntity> toEntities(WebSession webSession, Object rows)  {
        return new ArrayList<>();
    }

}
