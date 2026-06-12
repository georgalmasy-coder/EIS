package com.bepa.eis.server.dataprovider.fields.strings;

import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;

public class ProjectName extends AbstractString {

    public static String FIELD_NAME = "ProjectName";

    public ProjectName(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Project Name";
    }

    @Override
    public String getFieldHeaderName() {
        return "Project Name";
    }

    @Override
    public Integer getFieldMinLength() {
        return 5;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 100;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
