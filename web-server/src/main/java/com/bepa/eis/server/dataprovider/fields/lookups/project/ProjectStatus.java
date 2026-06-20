package com.bepa.eis.server.dataprovider.fields.lookups.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;

import java.util.List;

public class ProjectStatus extends AbstractLookup {

    public static String FIELD_NAME = "ProjectStatus";

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
        com.bepa.eis.common.enums.project.ProjectStatus status =
                com.bepa.eis.common.enums.project.ProjectStatus.fromIdOrDefault(
                        statusId,
                        com.bepa.eis.common.enums.project.ProjectStatus.CREATED
                );

        setLookupValue(toLookupValue(status));
    }

    @Override
    public String getDropdownSelectText() {
        return "Select project status ...";
    }

    private LookupValue toLookupValue(com.bepa.eis.common.enums.project.ProjectStatus status) {
        return new LookupValue(
                null,
                null,
                status.getId(),
                status.getCode(),
                status.getLabel(),
                true
        );
    }
}