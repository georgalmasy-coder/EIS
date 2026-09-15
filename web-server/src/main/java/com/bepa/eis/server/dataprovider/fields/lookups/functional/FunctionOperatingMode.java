package com.bepa.eis.server.dataprovider.fields.lookups.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class FunctionOperatingMode extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(FunctionOperatingMode.class);

    public static String FIELD_NAME = "FunctionOperatingModeId";

    public FunctionOperatingMode() {
    }

    public FunctionOperatingMode(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "FunctionOperatingMode";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Function Operating Mode";
    }

    @Override
    public String getFieldHeaderName() {
        return "Operating Mode";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select function operating mode ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getFunctionOperatingModeLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer operatingModeId) {
        LookupValue lookupValue = CustomerLookupCache.getFunctionOperatingModeLookupValue(getWebSession(), operatingModeId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
