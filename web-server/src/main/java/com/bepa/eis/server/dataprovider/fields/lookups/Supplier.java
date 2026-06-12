package com.bepa.eis.server.dataprovider.fields.lookups;

import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Supplier extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(Supplier.class);

    public static String FIELD_NAME = "SupplierId";

    @Override
    public void setValue(Integer supplierId) {
//        setLookupId(supplierId);

    }

    @Override
    public String getLookupName() {
        return "Supplier";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return List.of();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Supplier";
    }

    @Override
    public String getFieldHeaderName() {
        return "Supplier";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select supplier ...";
    }
}
