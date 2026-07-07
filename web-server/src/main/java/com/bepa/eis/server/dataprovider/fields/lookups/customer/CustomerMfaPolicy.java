package com.bepa.eis.server.dataprovider.fields.lookups.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CustomerMfaPolicy extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(CustomerMfaPolicy.class);

    public static String FIELD_NAME = "CustomerMfaPolicy";

    public CustomerMfaPolicy(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Mfa Policy";
    }

    @Override
    public String getFieldHeaderName() {
        return "Customer Mfa Policy";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return new ArrayList<>(); //CustomerLookupCache.getProjectPriorityLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer priorityId) {
        LookupValue lookupValue = null; //CustomerLookupCache.getProjectPriorityLookupValue(getWebSession(), priorityId);
        setLookupValue(lookupValue);
    }

    @Override
    public String getDropdownSelectText() {
        return "Select Mfa Policy ...";
    }
}
