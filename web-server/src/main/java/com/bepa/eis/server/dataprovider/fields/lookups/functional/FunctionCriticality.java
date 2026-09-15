package com.bepa.eis.server.dataprovider.fields.lookups.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class FunctionCriticality extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(FunctionCriticality.class);

    public static String FIELD_NAME = "FunctionCriticalityId";

    public FunctionCriticality() {
    }

    public FunctionCriticality(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "FunctionCriticality";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Function Criticality";
    }

    @Override
    public String getFieldHeaderName() {
        return "Criticality";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select function criticality ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getFunctionCriticalityLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer bahaviorTypeId) {
        LookupValue lookupValue = CustomerLookupCache.getFunctionCriticalityLookupValue(getWebSession(), bahaviorTypeId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
