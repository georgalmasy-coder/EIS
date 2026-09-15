package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalResponsibleDomain extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalResponsibleDomain.class);

    public static String FIELD_NAME = "LogicalResponsibleDomainId";

    public LogicalResponsibleDomain() {
    }

    public LogicalResponsibleDomain(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalResponsibleDomain";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Logical Responsible Domain";
    }

    @Override
    public String getFieldHeaderName() {
        return "Responsible Domain";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select logical responsible domain ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalResponsibleDomainLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer responsibleDomainId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalResponsibleDomainLookupValue(getWebSession(), responsibleDomainId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
