package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalAllocationStatus extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalAllocationStatus.class);

    public static String FIELD_NAME = "LogicalAllocationStatusId";

    public LogicalAllocationStatus() {
    }

    public LogicalAllocationStatus(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalAllocationStatus";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Logical Allocation Status";
    }

    @Override
    public String getFieldHeaderName() {
        return "Allocation Status";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select logical allocation status ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalAllocationStatusLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer allocationStatusId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalAllocationStatusLookupValue(getWebSession(), allocationStatusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
