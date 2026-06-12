package com.bepa.eis.server.dataprovider.fields.lookups.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProjectPriority extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(ProjectPriority.class);

    public static String FIELD_NAME = "PriorityId";

    public ProjectPriority(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Priority";
    }

    @Override
    public String getFieldHeaderName() {
        return "Priority";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getProjectPriorityLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer priorityId) {
        LookupValue lookupValue = CustomerLookupCache.getProjectPriorityLookupValue(getWebSession(), priorityId);
        setLookupValue(lookupValue);
    }

    @Override
    public String getDropdownSelectText() {
        return "Select project priority ...";
    }
}
