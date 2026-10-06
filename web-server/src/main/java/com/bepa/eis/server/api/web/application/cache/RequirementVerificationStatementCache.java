package com.bepa.eis.server.api.web.application.cache;

public class RequirementVerificationStatementCache extends GenericLookup {

    public RequirementVerificationStatementCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(7);
        reloadCache();
    }

}
