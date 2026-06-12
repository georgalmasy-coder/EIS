package com.bepa.eis.server.dataprovider.fields.lookups.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractUserLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProjectOwner extends AbstractUserLookup {

    private static final Logger log = LoggerFactory.getLogger(ProjectOwner.class);

    public static String FIELD_NAME = "OwnerId";

    public ProjectOwner() {
        super();
    }

    public ProjectOwner(WebSession webSession) {
        super(webSession);
    }

    public ProjectOwner(Integer userId) {
        super(userId);
    }

    @Override
    public String getLookupName() {
        return "ProjectOwner";
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
        return "Owner";
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
        return "Select project owner ...";
    }
}
