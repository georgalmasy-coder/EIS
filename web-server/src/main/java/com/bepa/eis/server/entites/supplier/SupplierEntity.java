package com.bepa.eis.server.entites.supplier;

import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityType;

import static com.bepa.eis.common.enums.entity.EntityType.SUPPLIER;

public class SupplierEntity extends AbstractEntity {

    /* GFA
    public AbstractEntity convertXmlRowToElements(SupplierRow row) {
        setProjectId(row.projectId());
        setCustomerId((Integer) null);
        setEntityId(row.supplierId());
        setVersion(1);

        setDateOfChange(row.dateOfChange());
        setChangedByUserId(row.changedByUserId() != null ? row.changedByUserId() : "1");

        addDataElement(new StringDataElement("SupplierName", row.supplierName()));
        addDataElement(new BooleanDataElement("Active", row.active()));

        return this;
    }

     */

    @Override
    public EntityType getEntityType() {
        return SUPPLIER;
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
}
