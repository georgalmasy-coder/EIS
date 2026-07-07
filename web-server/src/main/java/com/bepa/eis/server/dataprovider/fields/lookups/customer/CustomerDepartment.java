package com.bepa.eis.server.dataprovider.fields.lookups.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractDepartmentLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerDepartment extends AbstractDepartmentLookup {

    private static final Logger log = LoggerFactory.getLogger(CustomerDepartment.class);

    public static String FIELD_NAME = "DepartmentId";

    public CustomerDepartment(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "Department";
    }

    @Override
    public String getFieldName() {
        return "DepartmentId";
    }

    @Override
    public String getFieldLabelName() {
        return "Department";
    }

    @Override
    public String getFieldHeaderName() {
        return "Department";
    }

}
