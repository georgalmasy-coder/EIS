package com.bepa.eis.server.api.web.application.cache;


public class LogicalApplicabilityCache extends GenericLookup {

    public LogicalApplicabilityCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(30);
        reloadCache();
    }

}
