package com.bepa.eis.server.dataprovider.fields.booleans;

public class RelevantToStakeholderRequirement extends AbstractBoolean {

    public static final String FIELD_NAME = "RelevantToStakeholderRequirement";

    public RelevantToStakeholderRequirement() {
    }

    public RelevantToStakeholderRequirement(Boolean value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Is System Relevant to Stakeholder Requirement?";
    }

    @Override
    public String getFieldHeaderName() {
        return "Is Relevant to Stakeholder Requirement?";
    }

    @Override
    public String toString() {
        return getValue() != null ? getValue().toString() : "";
    }

}
