package com.bepa.eis.server.dataprovider.fields.lookups.codeselector;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;

public class StakeholderRequirementParentCodeSelector extends AbstractParentCodeSelector {

    public static String FIELD_NAME = "BasisStakeholderRequirementParentCode";

    public StakeholderRequirementParentCodeSelector(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Parent Stakeholder Requirement ID";
    }

    @Override
    public String getFieldHeaderName() {
        return "Parent Stakeholder Requirement ID";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.STAKEHOLDER_REQUIREMENT;
    }

    @Override
    public String getDropdownSelectText() {
        return "Select stakeholder requirement ...";
    }
}
