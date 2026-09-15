package com.bepa.eis.server.dataprovider.fields.lookups.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class FunctionResponsibleDomain extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(FunctionResponsibleDomain.class);

    public static String FIELD_NAME = "FunctionResponsibleDomainId";

    public FunctionResponsibleDomain() {
    }

    public FunctionResponsibleDomain(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "FunctionResponsibleDomain";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Function Responsible Domain";
    }

    @Override
    public String getFieldHeaderName() {
        return "Responsible Domain";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select function responsible domain ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getFunctionResponsibleDomainLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer responsibleDomainId) {
        LookupValue lookupValue = CustomerLookupCache.getFunctionResponsibleDomainLookupValue(getWebSession(), responsibleDomainId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
