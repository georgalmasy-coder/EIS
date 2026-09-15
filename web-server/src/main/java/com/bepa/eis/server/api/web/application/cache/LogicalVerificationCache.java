package com.bepa.eis.server.api.web.application.cache;


public class LogicalVerificationCache extends GenericLookup {

    public LogicalVerificationCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(16);
        reloadCache();
    }

}
