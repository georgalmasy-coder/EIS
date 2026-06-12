package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class DepartmentCache extends GenericLookup {

    private static final String LOOKUP_SQL =

            "SELECT DEPARTMENTID AS LookupId, " +
            "CONCAT( CONCAT( DEPARTMENTNAME,' - '), DEPARTMENTDESCRIPTION) AS LookupCode, " +
            "DEPARTMENTDESCRIPTION AS LookupDescription, " +
            "ACTIVE "  +
            "FROM DEPARTMENT " +
            "WHERE CustomerId=? " +
            "ORDER BY DepartmentName";

    public DepartmentCache(WebSession webSession) {
        setLookupSql(LOOKUP_SQL, webSession);
        reloadCache();
    }

}
