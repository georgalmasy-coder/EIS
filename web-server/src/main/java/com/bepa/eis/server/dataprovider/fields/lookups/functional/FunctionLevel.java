package com.bepa.eis.server.dataprovider.fields.lookups.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class FunctionLevel extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(FunctionLevel.class);

    public static String FIELD_NAME = "FunctionLevelId";

    public FunctionLevel() {
    }

    public FunctionLevel(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "FunctionLevel";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Function Level";
    }

    @Override
    public String getFieldHeaderName() {
        return "Level";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select function level ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getFunctionLevelLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer levelId) {
        LookupValue lookupValue = CustomerLookupCache.getFunctionLevelLookupValue(getWebSession(), levelId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
