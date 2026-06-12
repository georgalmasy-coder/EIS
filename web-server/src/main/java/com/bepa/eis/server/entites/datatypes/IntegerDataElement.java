package com.bepa.eis.server.entites.datatypes;

import com.bepa.eis.common.enums.entity.EntityElementType;

import static com.bepa.eis.common.enums.entity.EntityElementType.INTEGER;

public class IntegerDataElement extends AbstractDataElement {

    @Override
    public EntityElementType getElementType() {
        return INTEGER;
    }

    public IntegerDataElement(String name, String value) {
        super(name);

        if (value != null) {
            Integer integerValue = Integer.parseInt(value);
            setIntegerValue(integerValue);
        }
    }
    public IntegerDataElement(String name, Integer value) {
        super(name);
        setIntegerValue(value);
    }
}
