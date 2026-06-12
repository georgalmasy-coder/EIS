package com.bepa.eis.server.dataprovider.fields.lookups.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CreatedBy extends AbstractUserLookup {

    private static final Logger log = LoggerFactory.getLogger(CreatedBy.class);

    public static String FIELD_NAME = "CreatedById";

    private CreatedBy(WebSession webSession) {
        super(webSession);
    }

    private CreatedBy(Integer createdById) {
        super(createdById);
    }

    public CreatedBy(WebSession webSession, Integer createdById) {
        super(webSession);
        setValue(createdById);
    }

    @Override
    public String getLookupName() {
        return "Created By";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Created By";
    }

    @Override
    public String getFieldHeaderName() {
        return "Created By";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getUserLookupValues(getWebSession());
    }

    @Override
    public String getDropdownSelectText() {
        return null;
    }
}
