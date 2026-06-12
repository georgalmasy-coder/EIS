package com.bepa.eis.server.dataprovider.fields.strings;

import com.bepa.eis.common.dto.WebSession;

public class RequirementPerformance extends AbstractString {

    public static String FIELD_NAME = "RequirementPerformance";

    public RequirementPerformance() { }

    public RequirementPerformance(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Requirement Performance";
    }

    @Override
    public String getFieldHeaderName() {
        return "Requirement Performance";
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
