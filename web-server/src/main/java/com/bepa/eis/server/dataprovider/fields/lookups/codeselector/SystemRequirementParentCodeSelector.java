package com.bepa.eis.server.dataprovider.fields.lookups.codeselector;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;

public class SystemRequirementParentCodeSelector extends AbstractParentCodeSelector {

    public static String FIELD_NAME = "BasisSystemRequirementParentCode";

    public SystemRequirementParentCodeSelector(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Parent Systems Requirement ID";
    }

    @Override
    public String getFieldHeaderName() {
        return "Parent Systems Requirement ID";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SYSTEM_REQUIREMENT;
    }

    @Override
    public String getDropdownSelectText() {
        return "Select system requirement ...";
    }


}
