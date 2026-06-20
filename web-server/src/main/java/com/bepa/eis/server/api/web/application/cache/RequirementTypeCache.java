package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class RequirementTypeCache extends GenericLookup {

    public RequirementTypeCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(9);
        reloadCache();
    }

}
