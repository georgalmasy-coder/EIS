package com.bepa.eis.server.dataprovider.fields.lookups.system;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TRL extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(TRL.class);

    public static String FIELD_NAME = "TrlId";

    public TRL() {
        super();
    }

    public TRL(WebSession webSession) {
        super(webSession);
    }

    @Override
    public void setValue(Integer trlId) {
        LookupValue lookupValue = CustomerLookupCache.getTrlLookupValue(getWebSession(), trlId);
        setLookupValue(lookupValue);
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getTrlLookupValues(getWebSession());
    }

    @Override
    public String getLookupName() {
        return "TRL";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "TRL";
    }

    @Override
    public String getFieldHeaderName() {
        return "TRL";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select TRL ...";
    }
}
