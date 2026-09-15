package com.bepa.eis.server.api.web.application.cache;


public class FunctionVerificationCache extends GenericLookup {

    public FunctionVerificationCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(14);
        reloadCache();
    }

}
