package com.bepa.eis.server.dataprovider.fields.integers.ids;

import com.bepa.eis.server.api.web.application.enums.FieldVisible;

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
