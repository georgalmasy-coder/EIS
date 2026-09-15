package com.bepa.eis.server.dataprovider.fields.lookups.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractUserLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FunctionOwner extends AbstractUserLookup {

    private static final Logger log = LoggerFactory.getLogger(FunctionOwner.class);

    public static String FIELD_NAME = "FunctionOwnerId";

    public FunctionOwner() {
        super();
    }

    public FunctionOwner(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "FunctionOwner";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Owner";
    }

    @Override
    public String getFieldHeaderName() {
        return "Function Owner";
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 40;
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getUserLookupValues(getWebSession());
    }

    @Override
    public String getDropdownSelectText() {
        return "Select function owner ...";
    }

}
