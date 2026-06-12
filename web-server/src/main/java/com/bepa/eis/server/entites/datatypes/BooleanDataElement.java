package com.bepa.eis.server.entites.datatypes;

import com.bepa.eis.common.enums.entity.EntityElementType;

import static com.bepa.eis.common.enums.entity.EntityElementType.BOOLEAN;

public class BooleanDataElement extends AbstractDataElement {

    @Override
    public EntityElementType getElementType() {
        return BOOLEAN;
    }

    public BooleanDataElement(String name, Boolean booleanValue) {
        super(name);
        setBooleanValue(booleanValue);
    }

    public BooleanDataElement(String name, String value) {
        super(name);

        if (value != null) {
            Boolean booleanValue = Boolean.parseBoolean(value);
            setBooleanValue(booleanValue);
        }
    }
}
