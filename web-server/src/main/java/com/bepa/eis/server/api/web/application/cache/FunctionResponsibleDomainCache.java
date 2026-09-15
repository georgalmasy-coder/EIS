package com.bepa.eis.server.api.web.application.cache;


public class FunctionResponsibleDomainCache extends GenericLookup {

    public FunctionResponsibleDomainCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(35);
        reloadCache();
    }

}
