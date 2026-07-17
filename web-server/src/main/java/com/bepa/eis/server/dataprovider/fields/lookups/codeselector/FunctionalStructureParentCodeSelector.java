package com.bepa.eis.server.dataprovider.fields.lookups.codeselector;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;

public class FunctionalStructureParentCodeSelector extends AbstractParentCodeSelector {

    public static String FIELD_NAME = "FunctionalStructureParentCode";

    public FunctionalStructureParentCodeSelector(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Parent Functional Structure ID";
    }

    @Override
    public String getFieldHeaderName() {
        return "Parent Functional Structure ID";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.FUNCTIONAL_STRUCTURE;
    }

    @Override
    public String getDropdownSelectText() {
        return "Select Functional Structure ...";
    }
}
