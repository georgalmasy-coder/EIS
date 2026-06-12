package com.bepa.eis.server.dataprovider.fields.strings;

public class EntityNoteText extends AbstractString {

    public static String FIELD_NAME = "NoteText";

    public EntityNoteText(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Note";
    }

    @Override
    public String getFieldHeaderName() {
        return "Note";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
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
