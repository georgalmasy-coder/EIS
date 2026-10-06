package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class ProjectId extends AbstractId {

    public static String FIELD_NAME = "ProjectId";

    public ProjectId() {
    }

    public ProjectId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Project ID";
    }

}
