package com.bepa.eis.server.dataprovider.fields.lookups.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AbstractDepartmentLookup extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(AbstractDepartmentLookup.class);

    public static String FIELD_NAME = "DepartmentId";

    @Override
    public String getLookupName() {
        return "Department";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select department ...";
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

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getDepartmentLookupValues(getWebSession());
    }

    public AbstractDepartmentLookup() { }

    public AbstractDepartmentLookup(WebSession webSession) {
        setWebSession(webSession);
    }

    public void setValue(Integer departmentId) {
        LookupValue lookupValue = CustomerLookupCache.getDepartmentLookupValue(getWebSession(), departmentId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
