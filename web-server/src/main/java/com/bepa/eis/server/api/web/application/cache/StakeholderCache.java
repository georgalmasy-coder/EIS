package com.bepa.eis.server.api.web.application.cache;

public class StakeholderCache extends GenericLookup {

    private static final String LOOKUP_SQL =

        "SELECT CustomerId, ProjectId, EntityId as LookupId, " +
        "  (SELECT StringValue FROM ENTITY_ELEMENT EE " +
        "    WHERE E.CustomerId = EE.CustomerId " +
        "    AND E.ProjectId = EE.ProjectId " +
        "    AND E.EntityId = EE.EntityId " +
        "    AND E.EntityType = EE.EntityType \n" +
        "    AND E.Version = EE.Version " +
        "    AND EntityDataElementType = 31) as LookupCode," +
        "  (SELECT StringValue FROM ENTITY_ELEMENT EE " +
        "    WHERE E.CustomerId = EE.CustomerId " +
        "    AND E.ProjectId = EE.ProjectId " +
        "    AND E.EntityId = EE.EntityId " +
        "    AND E.EntityType = EE.EntityType " +
        "    AND E.Version = EE.Version " +
        "    AND EntityDataElementType = 32) as LookupDescription," +
        "  null AS Color, " +
        "  Active " +
        "FROM ENTITY E " +
        "WHERE CustomerId=? " +
        "AND ProjectId=? " +
        "AND EntityType = 1 " +
        "AND Latest = 1";

    public StakeholderCache(Integer customerId, Integer projectId) {
        setLookupSql(LOOKUP_SQL, customerId, projectId);
        reloadCache();
    }

}
