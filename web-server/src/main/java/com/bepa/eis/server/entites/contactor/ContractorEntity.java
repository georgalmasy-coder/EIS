package com.bepa.eis.server.entites.contactor;

import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.entites.AbstractEntity;

import static com.bepa.eis.common.enums.entity.EntityType.CONTRACTOR;

public class ContractorEntity extends AbstractEntity {

    @Override
    public EntityType getEntityType() {
        return CONTRACTOR;
    }

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getEntityOwner() {
        return "";
    }

    @Override
    public String getEntityDepartment() {
        return "";
    }

    @Override
    public String getEntityTrl() {
        return "";
    }

    @Override
    public void initializeFields() {
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
    }

    @Override
    public void addAllFieldElementsForEdit(Entity entityElement) {
    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {
    }
}
