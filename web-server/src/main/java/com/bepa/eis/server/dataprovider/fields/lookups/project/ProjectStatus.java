package com.bepa.eis.server.dataprovider.fields.lookups.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProjectStatus extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(ProjectStatus.class);

    public static String FIELD_NAME = "StatusId";

    public ProjectStatus(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "ProjectStatus";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Status";
    }

    @Override
    public String getFieldHeaderName() {
        return "Status";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getProjectStatusLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer statusId) {
        LookupValue lookupValue = CustomerLookupCache.getProjectStatusLookupValue(getWebSession(), statusId);
        setLookupValue(lookupValue);
    }

    @Override
    public String getDropdownSelectText() {
        return "Select project status ...";
    }
}
