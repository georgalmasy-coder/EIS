package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalCriticality extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalCriticality.class);

    public static String FIELD_NAME = "LogicalCriticalityId";

    public LogicalCriticality() {
    }

    public LogicalCriticality(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalCriticality";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Logical Criticality";
    }

    @Override
    public String getFieldHeaderName() {
        return "Criticality";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select logical criticality ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalCriticalityLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer bahaviorTypeId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalCriticalityLookupValue(getWebSession(), bahaviorTypeId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
