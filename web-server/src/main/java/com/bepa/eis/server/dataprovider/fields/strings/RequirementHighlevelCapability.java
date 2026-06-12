package com.bepa.eis.server.dataprovider.fields.strings;

import com.bepa.eis.common.dto.WebSession;

public class RequirementHighlevelCapability extends AbstractString {

    public static String FIELD_NAME = "RequirementHighlevelCapability";

    public RequirementHighlevelCapability() { }

    public RequirementHighlevelCapability(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "High Level Capability";
    }

    @Override
    public String getFieldHeaderName() {
        return "High Level Capability";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 255;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 10;
    }

}
