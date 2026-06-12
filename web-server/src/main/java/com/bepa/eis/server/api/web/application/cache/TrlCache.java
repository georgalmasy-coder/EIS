package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class TrlCache extends GenericLookup {

    private static final String LOOKUP_SQL =
            "SELECT TRLLevel as LookupId, concat(concat(TRLLevel, ' - ' ), TRLName) as LookupCode, TRLName as LookupDescription, Active " +
            "FROM TRL " +
            "WHERE CustomerId=? " +
            "AND   ProjectId=? " +
            "ORDER BY TRLLevel";


    public TrlCache(WebSession webSession) {
        setLookupSql(LOOKUP_SQL, webSession);
        reloadCache();
    }

}
