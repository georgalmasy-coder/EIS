package com.bepa.eis.server.dataprovider.fields.lookups.requirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class RequirementStatus extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(RequirementStatus.class);

    public static String FIELD_NAME = "RequirementStatusId";

    public RequirementStatus() {
    }

    public RequirementStatus(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "RequirementStatus";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Requirement Status";
    }

    @Override
    public String getFieldHeaderName() {
        return "Status";
    }


    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getRequirementStatusLookupValues(getWebSession());
    }

    @Override
    public String getDropdownSelectText() {
        return "Select status ...";
    }

    @Override
    public void setValue(Integer statusId) {
        LookupValue lookupValue = CustomerLookupCache.getRequirementStatusLookupValue(getWebSession(), statusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }
}
