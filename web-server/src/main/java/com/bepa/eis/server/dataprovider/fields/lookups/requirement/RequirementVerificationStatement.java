package com.bepa.eis.server.dataprovider.fields.lookups.requirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class RequirementVerificationStatement extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(RequirementVerificationStatement.class);

    public static String FIELD_NAME = "RequirementVerificationStatementId";

    public RequirementVerificationStatement() {
    }

    public RequirementVerificationStatement(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "RequirementVerificationStatement";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Verification Statement";
    }

    @Override
    public String getFieldHeaderName() {
        return "Verification Statement";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getRequirementVerificationStatementLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer statementId) {
        LookupValue lookupValue = CustomerLookupCache.getRequirementVerificationStatementLookupValue(getWebSession(), statementId);
        setLookupValue(lookupValue);
    }

    @Override
    public String getDropdownSelectText() {
        return "Select verification statement ...";
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
