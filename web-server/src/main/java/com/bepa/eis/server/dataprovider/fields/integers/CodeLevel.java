package com.bepa.eis.server.dataprovider.fields.integers;

public class CodeLevel extends AbstractInteger {

    public static String FIELD_NAME = "CodeLevel";

    public CodeLevel() {
        setFieldRequired();
    }

    public CodeLevel(Integer value) {
        setValue(value);
        setFieldRequired();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Level";
    }

    @Override
    public String getFieldHeaderName() {
        return "Level";
    }

}
