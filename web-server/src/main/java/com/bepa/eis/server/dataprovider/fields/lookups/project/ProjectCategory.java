package com.bepa.eis.server.dataprovider.fields.lookups.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProjectCategory extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(ProjectCategory.class);

    public static String FIELD_NAME = "CategoryId";

    public ProjectCategory(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Category";
    }

    @Override
    public String getFieldHeaderName() {
        return "Category";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getProjectCategoryLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer categoryId) {
        LookupValue lookupValue = CustomerLookupCache.getProjectCategoryLookupValue(getWebSession(), categoryId);
        setLookupValue(lookupValue);
    }

    @Override
    public String getDropdownSelectText() {
        return "Select project category ...";
    }
}
