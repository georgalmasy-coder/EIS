package com.bepa.eis.server.api.web.application.cache;


public class FunctionOperatingModeCache extends GenericLookup {

    public FunctionOperatingModeCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(34);
        reloadCache();
    }

}
