package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class UserCache extends GenericLookup {

    private static final String LOOKUP_SQL =

            "SELECT null as CustomerId, null as ProjectId, U.UserId AS LookupId, Name as LookupCode, CONCAT(Initials, ' - ', Name) as LookupDescription,  null AS Color, Active " +
            "FROM USERS U, USER_CUSTOMER UC " +
            "WHERE U.UserId = UC.UserId " +
            "AND   UC.CustomerId=? " +
            "ORDER BY U.Name";

    public UserCache(Integer customerId, Integer projectId) {
        setLookupSql(LOOKUP_SQL, customerId, projectId);
        reloadCache();
    }

}
