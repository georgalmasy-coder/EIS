package com.bepa.eis.server.api.web.application.cache;

public class RequirementBusinessPriorityCache extends GenericLookup {

    public RequirementBusinessPriorityCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(5);
        reloadCache();
    }

}
