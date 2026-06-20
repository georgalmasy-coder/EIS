package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class ProjectPriorityCache extends GenericLookup {

    public ProjectPriorityCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(2);
        reloadCache();
    }

}
