package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalRealizationStatus extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalRealizationStatus.class);

    public static String FIELD_NAME = "LogicalRealizationStatusId";

    public LogicalRealizationStatus() {
    }

    public LogicalRealizationStatus(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalRealizationStatus";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Logical Realization Status";
    }

    @Override
    public String getFieldHeaderName() {
        return "Realization Status";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select logical realization status ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalRealizationStatusLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer realizationStatusId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalRealizationStatusLookupValue(getWebSession(), realizationStatusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
