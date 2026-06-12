package com.bepa.eis.server.dataprovider.fields.lookups.requirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class RequirementBusinessPriority extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(RequirementBusinessPriority.class);

    public static String FIELD_NAME = "RequirementBusinessPriorityId";

    public RequirementBusinessPriority() {
    }

    public RequirementBusinessPriority(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "RequirementBusinessPriority";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Business Priority";
    }

    @Override
    public String getFieldHeaderName() {
        return "Priority";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getRequirementBusinessPriorityLookupValues(getWebSession());
    }

    @Override
    public String getDropdownSelectText() {
        return "Select business priority ...";
    }

    public void setValue(Integer statusId) {
        LookupValue lookupValue = CustomerLookupCache.getRequirementBusinessPriorityLookupValue(getWebSession(), statusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }
}
