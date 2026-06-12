package com.bepa.eis.server.dataprovider.generic;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.AbstractField;

public class AttributeElement extends AbstractField {

    @Override
    public String getFieldName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getFieldLabelName() {
        return "";
    }

    @Override
    public String getFieldHeaderName() {
        return "";
    }

    @Override
    public FieldControl getFieldControl() {
        return null;
    }

    @Override
    public Integer getFieldMinLength() {
        return 0;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 0;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 0;
    }

    @Override
    public Integer getFieldRow() {
        return 0;
    }

    @Override
    public Integer getFieldCol() {
        return 0;
    }

    @Override
    public String toString() {
        return "";
    }

}
