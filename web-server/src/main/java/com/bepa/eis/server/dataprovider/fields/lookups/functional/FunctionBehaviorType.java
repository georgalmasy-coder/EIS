package com.bepa.eis.server.dataprovider.fields.lookups.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class FunctionBehaviorType extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(FunctionBehaviorType.class);

    public static String FIELD_NAME = "FunctionBehaviorTypeId";

    public FunctionBehaviorType() {
    }

    public FunctionBehaviorType(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "FunctionBehaviorType";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Behavior Type";
    }

    @Override
    public String getFieldHeaderName() {
        return "Behavior Type";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select behavior type ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getFunctionBehaviorTypeLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer bahaviorTypeId) {
        LookupValue lookupValue = CustomerLookupCache.getFunctionBehaviorTypeLookupValue(getWebSession(), bahaviorTypeId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
