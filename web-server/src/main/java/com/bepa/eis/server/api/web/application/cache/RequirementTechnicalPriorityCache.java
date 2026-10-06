package com.bepa.eis.server.api.web.application.cache;

public class RequirementTechnicalPriorityCache extends GenericLookup {

    public RequirementTechnicalPriorityCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(5);
        reloadCache();
    }

}
