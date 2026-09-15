package com.bepa.eis.server.api.web.application.cache;


public class LogicalResponsibleDomainCache extends GenericLookup {

    public LogicalResponsibleDomainCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(21);
        reloadCache();
    }

}
