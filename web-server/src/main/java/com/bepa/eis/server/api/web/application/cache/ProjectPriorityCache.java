package com.bepa.eis.server.api.web.application.cache;

public class ProjectPriorityCache extends GenericLookup {

    public ProjectPriorityCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(2);
        reloadCache();
    }

}
