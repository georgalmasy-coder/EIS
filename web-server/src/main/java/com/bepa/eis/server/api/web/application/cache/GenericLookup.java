package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.WebSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GenericLookup {

    private static final Logger log = LoggerFactory.getLogger(GenericLookup.class);

    private List<LookupValue> listOfAllLookupValues = new ArrayList();
    private List<LookupValue> listOfActiveLookupValues = new ArrayList();
    private ConcurrentMap<Integer, LookupValue> mapOfValues = new ConcurrentHashMap<>();

    private String lookupSql = null;

    public void setLookupSqlByType(Integer lookupType) {
        lookupSql = "SELECT null AS CustomerId, null AS ProjectId, LookupId, LookupCode, LookupDescription, Color, Active FROM LOOKUP_TABLE WHERE LookupType = " + lookupType + " ORDER BY LookupCode";
    }

    public void setLookupSql(String sql, Integer customerId, Integer projectId) {
        lookupSql = sql != null ? sql.toUpperCase() : null;

        if (GlobalConfiguration.isUdvMode()) {
            customerId = GlobalConfiguration.isUdvMode() ? 1 : customerId;
            projectId = GlobalConfiguration.isUdvMode() ? 1 : projectId;

            lookupSql = lookupSql.replace("CUSTOMERID=?", "CUSTOMERID=" +customerId);
            lookupSql = lookupSql.replace("PROJECTID=?", "PROJECTID=" + projectId);
        } else {
            lookupSql = lookupSql.replace("CUSTOMERID=?", "CUSTOMERID=" + customerId);
            lookupSql = lookupSql.replace("PROJECTID=?", "PROJECTID=" + projectId);
        }
    }

    protected static DataSource getDataSource() {
        try {
            InitialContext ctx = new InitialContext();
            return (DataSource) ctx.lookup(GlobalConfiguration.getJndiName());
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to lookup DataSource via JNDI name: " + GlobalConfiguration.getJndiName(), e);
        }
    }

    public GenericLookup() {

    }

    public String getLookupSql() {
        if (lookupSql == null) {
            throw new IllegalStateException("No sql defined ");
        }
        return lookupSql;
    }

    public LookupValue getLookupValueById(Integer lookupId) {
        return lookupId != null ? mapOfValues.get(lookupId) : null  ;
    }

    public LookupValue getLookupValue(Integer lookupId) {
        return lookupId != null ? mapOfValues.get(lookupId) : null  ;
    }

    public List<LookupValue> getListOfActiveLookupValues() {
        return listOfActiveLookupValues;
    }

    public List<LookupValue> getListOfAllLookupValues() {
        return listOfAllLookupValues;
    }

    public void reloadCache() {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(getLookupSql());
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                LookupValue lookupValue = new LookupValue(
                        rs.getInt("CustomerId"),
                        rs.getInt("ProjectId"),
                        rs.getInt("LookupId"),
                        rs.getString("LookupCode"),
                        rs.getString("LookupDescription"),
                        rs.getString("Color"),
                        rs.getBoolean("Active"));

                addLookupValue(lookupValue);
            }

            log.debug("Bulk-loaded {} {} rows into Ehcache", this.getClass().getSimpleName(), mapOfValues.size());
        } catch (SQLException e) {
            log.error("Error bulk-loading {} lookup data: {}", this.getClass().getSimpleName(), e.getMessage());
        }
    }

    public void addLookupValue(LookupValue lookupValue) {
        listOfAllLookupValues.add(lookupValue);
        if (lookupValue.isActive()) {
            listOfActiveLookupValues.add(lookupValue);
        }
        mapOfValues.put(lookupValue.getLookupId(), lookupValue);
    }


}
