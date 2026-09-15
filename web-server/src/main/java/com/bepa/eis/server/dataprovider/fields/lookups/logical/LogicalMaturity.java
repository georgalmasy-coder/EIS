package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalMaturity extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalMaturity.class);

    public static String FIELD_NAME = "LogicalMaturityId";

    public LogicalMaturity() {
    }

    public LogicalMaturity(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalMaturity";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Logical Maturity";
    }

    @Override
    public String getFieldHeaderName() {
        return "Maturity";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select logical maturity ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalMaturityLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer maturityId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalMaturityLookupValue(getWebSession(), maturityId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
