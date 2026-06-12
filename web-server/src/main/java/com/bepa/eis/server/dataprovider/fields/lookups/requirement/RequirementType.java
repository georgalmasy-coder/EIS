package com.bepa.eis.server.dataprovider.fields.lookups.requirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class RequirementType extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(RequirementType.class);

    public static String FIELD_NAME = "RequirementTypeId";

    public RequirementType() {
    }

    public RequirementType(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "RequirementType";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Requirement Type";
    }

    @Override
    public String getFieldHeaderName() {
        return "Requirement Type";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getRequirementTypeLookupValues(getWebSession());
    }

    @Override
    public String getDropdownSelectText() {
        return "Select requirement type ...";
    }

    public void setValue(Integer statusId) {
        LookupValue lookupValue = CustomerLookupCache.getRequirementTypeLookupValue(getWebSession(), statusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }
}
