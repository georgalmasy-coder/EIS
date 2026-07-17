package com.bepa.eis.server.dataprovider.fields.lookups.codeselector;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;

public class LogicalStructureParentCodeSelector extends AbstractParentCodeSelector {

    public static String FIELD_NAME = "LogicalStructureParentCode";

    public LogicalStructureParentCodeSelector(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Parent Logical Structure ID";
    }

    @Override
    public String getFieldHeaderName() {
        return "Parent Logical Structure ID";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.LOGICAL_STRUCTURE;
    }

    @Override
    public String getDropdownSelectText() {
        return "Select Logical Structure ...";
    }
}
