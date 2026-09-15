package com.bepa.eis.server.api.web.application.cache;


public class FunctionCategoryCache extends GenericLookup {

    public FunctionCategoryCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(32);
        reloadCache();
    }

}
