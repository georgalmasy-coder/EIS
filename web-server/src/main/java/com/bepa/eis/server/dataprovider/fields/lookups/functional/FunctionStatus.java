package com.bepa.eis.server.dataprovider.fields.lookups.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class FunctionStatus extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(FunctionStatus.class);

    public static String FIELD_NAME = "FunctionStatusId";

    public FunctionStatus() {
    }

    public FunctionStatus(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "FunctionalStatus";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Function Status";
    }

    @Override
    public String getFieldHeaderName() {
        return "Status";
    }


    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getFunctionStatusLookupValues(getWebSession());
    }

    @Override
    public String getDropdownSelectText() {
        return "Select function status ...";
    }

    @Override
    public void setValue(Integer statusId) {
        LookupValue lookupValue = CustomerLookupCache.getFunctionStatusLookupValue(getWebSession(), statusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }
}
