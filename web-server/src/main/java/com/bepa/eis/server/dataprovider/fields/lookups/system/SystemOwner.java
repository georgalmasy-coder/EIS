package com.bepa.eis.server.dataprovider.fields.lookups.system;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractUserLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SystemOwner extends AbstractUserLookup {

    private static final Logger log = LoggerFactory.getLogger(SystemOwner.class);

    public static String FIELD_NAME = "SystemOwnerId";

    public SystemOwner() {
        super();
    }

    public SystemOwner(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "SystemOwner";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getUserLookupValues(getWebSession());
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "System Owner";
    }

    @Override
    public String getFieldHeaderName() {
        return "System Owner";
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 40;
    }

    @Override
    public String getDropdownSelectText() {
        return "Select system owner ...";
    }
}

