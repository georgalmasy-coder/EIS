package com.bepa.eis.server.dataprovider.fields.lookups.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class FunctionCategory extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(FunctionCategory.class);

    public static String FIELD_NAME = "FunctionCategoryId";

    public FunctionCategory() {
    }

    public FunctionCategory(WebSession webSession) {
        super(webSession);
    }

    @Override
    public String getLookupName() {
        return "FunctionCategory";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Function Category";
    }

    @Override
    public String getFieldHeaderName() {
        return "Category";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select function category ...";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return CustomerLookupCache.getFunctionCategoryLookupValues(getWebSession());
    }

    @Override
    public void setValue(Integer bahaviorTypeId) {
        LookupValue lookupValue = CustomerLookupCache.getFunctionCategoryLookupValue(getWebSession(), bahaviorTypeId);
        setLookupValue(lookupValue);
    }

    @Override
    public String toString() {
        return getLookupCode();
    }

}
