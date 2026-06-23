package com.bepa.eis.server.dataprovider.fields.lookups.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EntityRelationType extends AbstractUserLookup {

    private static final Logger log = LoggerFactory.getLogger(EntityRelationType.class);

    public static String FIELD_NAME = "RelationType";

    private EntityRelationType(WebSession webSession) {
        super(webSession);
    }

    private EntityRelationType(Integer relationTypeId) {
        super(relationTypeId);
    }

    public EntityRelationType(WebSession webSession, Integer relationTypeId) {
        super(webSession);
        setValue(relationTypeId);
    }

    @Override
    public String getLookupName() {
        return "Relation Type";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Relation Type";
    }

    @Override
    public String getFieldHeaderName() {
        return "Relation Type";
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return List.of();
    }

    @Override
    public String getDropdownSelectText() {
        return null;
    }
}
