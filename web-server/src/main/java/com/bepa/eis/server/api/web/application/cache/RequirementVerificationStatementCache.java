package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class RequirementVerificationStatementCache extends GenericLookup {

    public RequirementVerificationStatementCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(7);
        reloadCache();
    }

}
