package com.bepa.eis.server.api.web.application.cache;


public class LogicalSafetyClassificationCache extends GenericLookup {

    public LogicalSafetyClassificationCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(26);
        reloadCache();
    }

}
