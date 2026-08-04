package com.bepa.eis.server.dataprovider.fields.lookups.codeselector;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;

public class SystemsBreakdownParentCodeSelector extends AbstractParentCodeSelector {

    public static String FIELD_NAME = "BasisSystemsBreakdownParentCode";

    public SystemsBreakdownParentCodeSelector(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Parent Physical Structure ID";
    }

    @Override
    public String getFieldHeaderName() {
        return "Parent Physical Structure ID";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SYSTEMS_BREAKDOWN;
    }

    @Override
    public String getDropdownSelectText() {
        return "Select Physical Structure ...";
    }

}
