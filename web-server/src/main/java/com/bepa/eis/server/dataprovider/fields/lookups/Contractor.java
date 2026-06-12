package com.bepa.eis.server.dataprovider.fields.lookups;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Contractor extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(Contractor.class);

    public static String FIELD_NAME = "ContractorId";

    public void setValue(Integer contractorId) {
//        setLookupId(contractorId);
    }

    @Override
    public String getLookupName() {
        return "Contractor";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Contractor";
    }

    @Override
    public String getFieldHeaderName() {
        return "Contractor";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return List.of();
    }

    @Override
    public String getDropdownSelectText() {
        return "Select contractor ...";
    }
}
