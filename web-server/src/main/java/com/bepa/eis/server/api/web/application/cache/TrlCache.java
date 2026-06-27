package com.bepa.eis.server.api.web.application.cache;

public class TrlCache extends GenericLookup {

    private static final String LOOKUP_SQL =
            "SELECT CustomerId, ProjectId, TRLLevel as LookupId, concat(concat(TRLLevel, ' - ' ), TRLName) as LookupCode, TRLName as LookupDescription, null AS Color, Active " +
            "FROM TRL " +
            "WHERE CustomerId=? " +
//            "AND   ProjectId=? " +
            "ORDER BY TRLLevel";

    public TrlCache(Integer customerId, Integer projectId) {
        setLookupSql(LOOKUP_SQL, customerId, projectId);
        reloadCache();
    }

}
