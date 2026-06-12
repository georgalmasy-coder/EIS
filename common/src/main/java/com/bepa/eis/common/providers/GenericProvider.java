package com.bepa.eis.common.providers;

import com.bepa.eis.common.GlobalConfiguration;import com.bepa.eis.common.dto.WebSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

public class GenericProvider {

    private WebSession webSession;

    private GenericProvider() {
    }

    public GenericProvider(WebSession webSession) {
        this.webSession = webSession;
    }

    protected static DataSource getDataSource() {
        try {
            InitialContext ctx = new InitialContext();
            return (DataSource) ctx.lookup(GlobalConfiguration.getJndiName());
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to lookup DataSource via JNDI name: " + GlobalConfiguration.getJndiName(), e);
        }
    }

    protected WebSession getWebSession() {
        return webSession;
    }

    protected Timestamp getTimestamp(Date date) {
        return (date == null) ? null : new Timestamp(date.getTime());
    }

    protected void setString(PreparedStatement ps, String value, int index ) throws SQLException {
        ps.setString(index, value);
    }

    protected void setInt(PreparedStatement ps, Integer value, int index ) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    protected void setTimestamp(PreparedStatement ps, Date value, int index ) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, getTimestamp(value));
        }
    }

}
