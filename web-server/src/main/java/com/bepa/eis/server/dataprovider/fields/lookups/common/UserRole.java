package com.bepa.eis.server.dataprovider.fields.lookups.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserRole extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(UserRole.class);

    public static String FIELD_NAME = "UserRole";

    private UserRole(WebSession webSession) {
        super(webSession);
    }

    private UserRole(Integer roleId) {
        setValue(roleId);
    }

    public UserRole(WebSession webSession, Integer roleId) {
        super(webSession);
        setValue(roleId);
    }

    @Override
    public String getLookupName() {
        return "User Type";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "User Role";
    }

    @Override
    public String getFieldHeaderName() {
        return "User Role";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getCustomerAdminUserRolesLookupValue();
    }

    @Override
    public void setValue(Integer roleId) {
        LookupValue lookupValue = CustomerLookupCache.getCustomerAdminUserRolesLookupValue(getWebSession(), roleId);
        setLookupValue(lookupValue);
    }

    @Override
    public String getDropdownSelectText() {
        return "Select user role ...";
    }
}
