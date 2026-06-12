package com.bepa.eis.server.entites.datatypes;

import com.bepa.eis.common.enums.entity.EntityElementType;

import static com.bepa.eis.common.enums.entity.EntityElementType.STRING;

public class StringDataElement extends AbstractDataElement {

    @Override
    public EntityElementType getElementType() {
        return STRING;
    }

    public StringDataElement(String name, String value) {
        super(name);
        setStringValue(value);
    }
}
