package com.bepa.eis.server.api.web.application.cache;

public class RequirementTypeCache extends GenericLookup {

    public RequirementTypeCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(9);
        reloadCache();
    }

}
