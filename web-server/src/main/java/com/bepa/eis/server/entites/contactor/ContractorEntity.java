package com.bepa.eis.server.entites.contactor;

import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.strings.RequirementDescription;
import com.bepa.eis.server.dataprovider.fields.strings.RequirementName;
import com.bepa.eis.server.dataprovider.fields.strings.StakeholderRequirementCode;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityType;

import static com.bepa.eis.common.enums.entity.EntityType.CONTRACTOR;

public class ContractorEntity extends AbstractEntity {

/* GFA
    private ContractorRow x;

    public AbstractEntity convertXmlRowToElements(ContractorRow row) {
        setProjectId(row.projectId());
        setCustomerId((Integer) null);
        setEntityId(row.supplierId());
        setVersion(1);

        setDateOfChange(row.dateOfChange());
        setChangedByUserId(row.changedByUserId() != null ? row.changedByUserId() : "1");

        addDataElement(new StringDataElement("ContractorName", row.supplierName()));
        addDataElement(new BooleanDataElement("Active", row.active()));

        return this;
    }
*/
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
