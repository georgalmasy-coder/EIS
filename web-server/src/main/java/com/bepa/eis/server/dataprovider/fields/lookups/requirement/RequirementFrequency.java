package com.bepa.eis.server.dataprovider.fields.lookups.requirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class RequirementFrequency extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(RequirementFrequency.class);

    public static String FIELD_NAME = "RequirementFrequencyId";

    public RequirementFrequency() {
    }

    public RequirementFrequency(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "RequirementFrequency";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Requirement Frequency";
    }

    @Override
    public String getFieldHeaderName() {
        return "Requirement Frequency";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getRequirementFrequencyLookupValues(getWebSession());
    }

    @Override
    public String getDropdownSelectText() {
        return "Select frequency ...";
    }

    public void setValue(Integer frequencyId) {
        LookupValue lookupValue = CustomerLookupCache.getRequirementFrequencyLookupValue(getWebSession(), frequencyId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }
}
