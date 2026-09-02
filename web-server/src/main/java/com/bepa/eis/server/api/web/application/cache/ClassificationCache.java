package com.bepa.eis.server.api.web.application.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClassificationCache extends GenericLookup {

    private static final Logger log = LoggerFactory.getLogger(ClassificationCache.class);

    private static final String LOOKUP_SQL =
            "SELECT CustomerId, ProjectId, ClassId as LookupId, Code as LookupCode, Description as LookupDescription, null AS Color, Example, UsageExample, Active " +
            "FROM CLASSIFICATION " +
            "WHERE CustomerId=? " +
            "AND   ProjectId=? " +
            "ORDER BY Code ";

    public ClassificationCache(Integer customerId, Integer projectId) {
        setLookupSql(LOOKUP_SQL, customerId, projectId);
        reloadCache();
    }

    public void reloadCache() {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(getLookupSql());
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {


                ClassLookupValue lookupValue = new ClassLookupValue(
                        rs.getInt("CustomerId"),
                        rs.getInt("ProjectId"),
                        rs.getInt("LookupId"),
                        rs.getString("LookupCode"),
                        rs.getString("LookupDescription"),
                        rs.getString("Example"),
                        rs.getString("UsageExample"),
                        rs.getBoolean("Active"));

                addLookupValue(lookupValue);
            }

            log.debug("Bulk-loaded {} {} rows into Ehcache", this.getClass().getSimpleName(), nbrOfValues());
        } catch (SQLException e) {
            log.error("Error bulk-loading status lookup data: {}", e.getMessage());
        }
    }
}
