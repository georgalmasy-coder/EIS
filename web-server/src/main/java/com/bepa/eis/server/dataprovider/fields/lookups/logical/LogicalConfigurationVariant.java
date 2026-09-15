package com.bepa.eis.server.dataprovider.fields.lookups.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LogicalConfigurationVariant extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(LogicalConfigurationVariant.class);

    public static String FIELD_NAME = "LogicalConfigurationVariantId";

    public LogicalConfigurationVariant() {
    }

    public LogicalConfigurationVariant(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "LogicalConfigurationVariant";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Logical Configuration Variant";
    }

    @Override
    public String getFieldHeaderName() {
        return "Configuration Variant";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select logical configuration variant ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getLogicalConfigurationVariantLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer configurationVariantId) {
        LookupValue lookupValue = CustomerLookupCache.getLogicalConfigurationVariantLookupValue(getWebSession(), configurationVariantId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
