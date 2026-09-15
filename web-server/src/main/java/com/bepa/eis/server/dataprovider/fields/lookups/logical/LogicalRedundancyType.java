package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalRedundancyType extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalRedundancyType.class);

    public static String FIELD_NAME = "LogicalRedundancyTypeId";

    public LogicalRedundancyType() {
    }

    public LogicalRedundancyType(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalRedundancyType";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Logical Redundancy Type";
    }

    @Override
    public String getFieldHeaderName() {
        return "Redundancy Type";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select logical redundancy type ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalRedundancyTypeLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer redundancyTypeId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalRedundancyTypeLookupValue(getWebSession(), redundancyTypeId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
