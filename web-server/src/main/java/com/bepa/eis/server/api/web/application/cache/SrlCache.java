package com.bepa.eis.server.api.web.application.cache;

public class SrlCache extends GenericLookup {

    private static final String LOOKUP_SQL =
            "SELECT CustomerId, ProjectId, SRLLevel as LookupId, concat(concat(SRLLevel, ' - ' ), SRLName) as LookupCode, SRLDescription as LookupDescription, Color AS Color, Active " +
            "FROM SRL " +
            "WHERE CustomerId=? " +
            "AND   ProjectId=? " +
            "ORDER BY SRLLevel";

    public SrlCache(Integer customerId, Integer projectId) {
        setLookupSql(LOOKUP_SQL, customerId, projectId);
        reloadCache();
    }

}
