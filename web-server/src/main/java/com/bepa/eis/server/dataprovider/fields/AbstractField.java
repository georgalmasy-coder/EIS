package com.bepa.eis.server.dataprovider.fields;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.api.web.application.enums.FieldEditable;
import com.bepa.eis.server.api.web.application.enums.FieldRequired;
import com.bepa.eis.server.api.web.application.enums.FieldVisible;
import com.bepa.eis.server.dataprovider.generic.Attribute;

import java.util.ArrayList;
import java.util.List;

import static com.bepa.eis.server.api.web.application.enums.FieldControl.HIDDEN;
import static com.bepa.eis.server.api.web.application.enums.FieldControl.NONE;
import static com.bepa.eis.server.api.web.application.enums.FieldVisible.FIELD_VISIBLE;

abstract public class AbstractField implements Cloneable {

    private final List<Attribute> attributes = new ArrayList<>();

    private FieldEditable fieldEditable = FieldEditable.FIELD_NOT_EDITABLE;
    private FieldRequired fieldRequired = FieldRequired.FIELD_NOT_REQUIRED;
    private FieldVisible fieldVisible = FIELD_VISIBLE;
    private String tableWidth = null;

    abstract public String getFieldName();
    abstract public String getFieldLabelName();
    abstract public String getFieldHeaderName();

    public void setFieldEditable() {
        fieldEditable = FieldEditable.FIELD_EDITABLE;
    }
    public  void setFieldNotEditable() {
        fieldEditable = FieldEditable.FIELD_NOT_EDITABLE;
    }
    public boolean isFieldEditable() {
        return fieldEditable == FieldEditable.FIELD_EDITABLE;
    }
    public String getFieldEditableAsString() {
        return Boolean.toString(isFieldEditable());
    };

    public void setFieldVisible() {
        fieldVisible = FIELD_VISIBLE;
    }
    public void setFieldNotVisible() {
        fieldVisible = FieldVisible.FIELD_NOT_VISIBLE;
    }
    public String getFieldVisibleAsString() {
        return fieldVisible.getDescription();
    }
    public boolean isFieldVisible() {
        return Boolean.parseBoolean(fieldVisible.getDescription());
    }

    public void setFieldRequired() {
        fieldRequired = FieldRequired.FIELD_REQUIRED;
    }
    public void setFieldNotRequired() {
        fieldRequired = FieldRequired.FIELD_NOT_REQUIRED;
    }

    public boolean isFieldRequired() {
        return fieldRequired == FieldRequired.FIELD_REQUIRED;
    }

    public String getFieldRequiredAsString() {
        return Boolean.toString(isFieldRequired());
    };

    public void addAttribute(String name, String value) {
        Attribute attribute = new Attribute(name, value);
        attributes.add(attribute);
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }


    public abstract FieldControl getFieldControl();
    public abstract Integer getFieldMinLength();
    public abstract Integer getFieldMaxLength();
    public abstract Integer getFieldDisplayLength();

    public abstract Integer getFieldRow();
    public abstract Integer getFieldCol();

    abstract public String toString();

    public void setTableWidth(String tableWidth) {
        this.tableWidth = tableWidth;
    }

    public String getTableWidth() {
        return tableWidth;
    }

    @Override
    public AbstractField clone()  {
        try {
           return (AbstractField) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unable to close field object " + e.getMessage(), e);
        }
    }

    public String getSortKey() {
        return null;
    }
}
