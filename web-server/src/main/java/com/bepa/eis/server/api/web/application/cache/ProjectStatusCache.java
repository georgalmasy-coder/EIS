package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.project.ProjectStatus;

public class ProjectStatusCache extends GenericLookup {

    public ProjectStatusCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(3);
        reloadCache();
    }

    @Override
    public void reloadCache() {

        for (ProjectStatus projectStatus : ProjectStatus.values()) {
            LookupValue lookupValue = new LookupValue(
                    null,
                    null,
                    projectStatus.getId(),
                    projectStatus.getLabel(),
                    projectStatus.getDescription(),
                    true);

            addLookupValue(lookupValue);
        }

    }
}
