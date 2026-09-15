package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalLifecycleStatus extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalLifecycleStatus.class);

    public static String FIELD_NAME = "LogicalLifecycleStatusId";

    public LogicalLifecycleStatus() {
    }

    public LogicalLifecycleStatus(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalLifecycleStatus";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Logical Lifecycle Status";
    }

    @Override
    public String getFieldHeaderName() {
        return "Lifecycle Status";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select logical lifecycle status ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalLifecycleStatusLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer lifecycleStatusId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalLifecycleStatusLookupValue(getWebSession(), lifecycleStatusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
