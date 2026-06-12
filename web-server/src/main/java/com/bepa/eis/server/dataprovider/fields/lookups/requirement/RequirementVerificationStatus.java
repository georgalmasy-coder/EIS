package com.bepa.eis.server.dataprovider.fields.lookups.requirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class RequirementVerificationStatus extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(RequirementVerificationStatus.class);

    public static String FIELD_NAME = "RequirementVerificationStatusId";

    public RequirementVerificationStatus() {
    }

    public RequirementVerificationStatus(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "RequirementVerificationStatus";
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
        return CustomerLookupCache.getRequirementVerificationLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer statusId) {
        LookupValue lookupValue = CustomerLookupCache.getRequirementVerificationLookupValue(getWebSession(), statusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
