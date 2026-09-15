package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalSecurityClassification extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalSecurityClassification.class);

    public static String FIELD_NAME = "LogicalSecurityClassificationId";

    public LogicalSecurityClassification() {
    }

    public LogicalSecurityClassification(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalSecurityClassification";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Logical Security Classification";
    }

    @Override
    public String getFieldHeaderName() {
        return "Security Classification";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select logical security classification ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalSecurityClassificationLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer securityClassificationId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalSecurityClassificationLookupValue(getWebSession(), securityClassificationId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
