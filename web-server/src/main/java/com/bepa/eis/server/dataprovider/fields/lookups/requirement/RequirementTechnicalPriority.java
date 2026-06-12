package com.bepa.eis.server.dataprovider.fields.lookups.requirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class RequirementTechnicalPriority extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(RequirementTechnicalPriority.class);

    public static String FIELD_NAME = "RequirementTechnicalPriorityId";

    public RequirementTechnicalPriority() {
    }

    public RequirementTechnicalPriority(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "RequirementTechnicalPriority";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Technical Priority";
    }

    @Override
    public String getFieldHeaderName() {
        return "Technical Priority";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getRequirementTechnicalPriorityLookupValues(getWebSession());
    }

    @Override
    public String getDropdownSelectText() {
        return "Select technical priority ...";
    }

    @Override
    public void setValue(Integer priorityId) {
        LookupValue lookupValue = CustomerLookupCache.getRequirementTechnicalPriorityLookupValue(getWebSession(), priorityId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }
}
