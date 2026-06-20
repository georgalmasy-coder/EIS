package com.bepa.eis.server.dataprovider.fields.lookups.system;

import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SBSCodeType extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(SBSCodeType.class);

    public static String FIELD_NAME = "SBSCodeTypeId";

    private static final List<LookupValue> lookupValues = new ArrayList<>();

    public void setValue(Integer sbsCodeTypeId) {
    }

    public Integer getValue() {
        return getLookupId();
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        if (lookupValues.isEmpty()) {

            lookupValues.add(new LookupValue(getWebSession().getCustomerId(), getWebSession().getProjectId(), 1, "= Function", "Function", true));
            lookupValues.add(new LookupValue(getWebSession().getCustomerId(), getWebSession().getProjectId(), 2, "+ Location", "Location", true));
            lookupValues.add(new LookupValue(getWebSession().getCustomerId(), getWebSession().getProjectId(), 3, "- Product", "Product", true));
            lookupValues.add(new LookupValue(getWebSession().getCustomerId(), getWebSession().getProjectId(), 4, "% Type or classification", "Type or classification", true));
            lookupValues.add(new LookupValue(getWebSession().getCustomerId(), getWebSession().getProjectId(), 5, "# Other", "Other", true));
        }
        return lookupValues;
    }

    @Override
    public String getLookupName() {
        return "SBSCodeType";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "SBS Code Type";
    }

    @Override
    public String getFieldHeaderName() {
        return "SBS Code Type";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select SBS Code Type ...";
    }
}
