package com.bepa.eis.server.dataprovider.fields.lookups.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChangedBy extends AbstractUserLookup {

    private static final Logger log = LoggerFactory.getLogger(ChangedBy.class);

    public static String FIELD_NAME = "ChangedByUserId";

    public ChangedBy(Integer userId) {
        super(userId);
        setFieldRequired();
    }

    public ChangedBy() {
        super();
        setFieldRequired();
    }

    public ChangedBy(WebSession webSession) {
        super(webSession);
        setFieldRequired();
    }

    @Override
    public String getLookupName() {
        return "Changed By";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Changed By";
    }

    @Override
    public String getFieldHeaderName() {
        return "Changed By";
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
        return null;
    }
}
