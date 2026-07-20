package com.bepa.eis.server.api.web.application.cache;

public class IrlCache extends GenericLookup {

    private static final String LOOKUP_SQL =
            "SELECT CustomerId, ProjectId, IrlId as LookupId, concat(concat(IRLCode, ' - ' ), IRLName) as LookupCode, IRLDescription as LookupDescription, Color AS Color, Active " +
            "FROM IRL " +
            "WHERE CustomerId=? " +
            "AND   ProjectId=? " +
            "ORDER BY IRLCode";

    public IrlCache(Integer customerId, Integer projectId) {
        setLookupSql(LOOKUP_SQL, customerId, projectId);
        reloadCache();
    }

}
