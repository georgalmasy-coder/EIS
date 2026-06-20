package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class RequirementStatusCache extends GenericLookup {

    public RequirementStatusCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(4);
        reloadCache();
    }

}
