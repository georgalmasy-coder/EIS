package com.bepa.eis.server.dataprovider.fields.lookups.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class FunctionApplicability extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(FunctionApplicability.class);

    public static String FIELD_NAME = "FunctionApplicabilityId";

    public FunctionApplicability() {
    }

    public FunctionApplicability(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "FunctionApplicability";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Function Applicability";
    }

    @Override
    public String getFieldHeaderName() {
        return "Applicability";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select function applicability ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getFunctionApplicabilityLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer applicabilityId) {
        LookupValue lookupValue = CustomerLookupCache.getFunctionApplicabilityLookupValue(getWebSession(), applicabilityId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
