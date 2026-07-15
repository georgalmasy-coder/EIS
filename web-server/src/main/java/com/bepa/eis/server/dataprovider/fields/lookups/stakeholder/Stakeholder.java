package com.bepa.eis.server.dataprovider.fields.lookups.stakeholder;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Stakeholder extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(Stakeholder.class);

    public static String FIELD_NAME = "Stakeholder";

    public Stakeholder() {
        super();
    }

    public Stakeholder(WebSession webSession) {
        super(webSession);
    }

    @Override
    public void setValue(Integer stakeholderId) {
        LookupValue lookupValue = CustomerLookupCache.getStakeholderLookupValue(getWebSession(), stakeholderId);
        setLookupValue(lookupValue);
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getStakeholderLookupValues(getWebSession());
    }

    @Override
    public String getLookupName() {
        return "Stakeholder";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Stakeholder";
    }

    @Override
    public String getFieldHeaderName() {
        return "Stakeholder";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select Stakeholder ...";
    }
}
