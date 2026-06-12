package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class DepartmentId extends AbstractId {

    @Override
    public String getFieldName() {
        return "DepartmentId";
    }

    @Override
    public String getFieldHeaderName() {
        return "Department ID";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

}
