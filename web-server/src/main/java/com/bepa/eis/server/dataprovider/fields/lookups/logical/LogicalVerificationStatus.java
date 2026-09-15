package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalVerificationStatus extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalVerificationStatus.class);

    public static String FIELD_NAME = "LogicalVerificationStatusId";

    public LogicalVerificationStatus() {
    }

    public LogicalVerificationStatus(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalVerificationStatus";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Verification Status";
    }

    @Override
    public String getFieldHeaderName() {
        return "Ver. Status";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select verification status ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalVerificationLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer statusId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalVerificationLookupValue(getWebSession(), statusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
