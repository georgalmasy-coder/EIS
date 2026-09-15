package com.bepa.eis.server.api.web.application.cache;


public class RequirementVerificationCache extends GenericLookup {

    public RequirementVerificationCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(10);
        reloadCache();
    }

}
