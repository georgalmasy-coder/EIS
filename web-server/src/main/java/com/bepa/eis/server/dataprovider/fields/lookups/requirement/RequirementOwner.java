package com.bepa.eis.server.dataprovider.fields.lookups.requirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractUserLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RequirementOwner extends AbstractUserLookup {

    private static final Logger log = LoggerFactory.getLogger(RequirementOwner.class);

    public static String FIELD_NAME = "RequirementOwnerId";

    public RequirementOwner() {
        super();
    }

    public RequirementOwner(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "RequirementOwner";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Owner";
    }

    @Override
    public String getFieldHeaderName() {
        return "Requirement Owner";
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 40;
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getUserLookupValues(getWebSession());
    }

    @Override
    public String getDropdownSelectText() {
        return "Select requirement owner ...";
    }

}
