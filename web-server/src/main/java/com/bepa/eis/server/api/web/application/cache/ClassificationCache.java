package com.bepa.eis.server.api.web.application.cache;

public class ClassificationCache extends GenericLookup {

    private static final String LOOKUP_SQL =
            "SELECT CustomerId, ProjectId, ClassId as LookupId, Code as LookupCode, Description as LookupDescription, null AS Color, Active " +
            "FROM CLASSIFICATION " +
            "WHERE CustomerId=? " +
            "AND   ProjectId=? " +
            "ORDER BY Code ";

    public ClassificationCache(Integer customerId, Integer projectId) {
        setLookupSql(LOOKUP_SQL, customerId, projectId);
        reloadCache();
    }

}
