package com.bepa.eis.server.api.web.application.views.myprojects;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.project.IrlType;
import com.bepa.eis.common.enums.project.SrlType;
import com.bepa.eis.common.enums.project.TrlType;
import com.bepa.eis.common.providers.GenericProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InstallDefaultConfiguration extends GenericProvider {

    private static final int SQL_SERVER_DUPLICATE_KEY = 2601;
    private static final int SQL_SERVER_PRIMARY_KEY_VIOLATION = 2627;

    private static final String INSERT_IRL_SQL =
            "INSERT INTO IRL (" +
                    "CustomerId, " +
                    "ProjectId, " +
                    "IrlId, " +
                    "IRLCode, " +
                    "IRLLevel, " +
                    "IRLName, " +
                    "IRLDescription, " +
                    "Active" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_SRL_SQL =
            "INSERT INTO SRL (" +
                    "CustomerId, " +
                    "ProjectId, " +
                    "SRLLevel, " +
                    "SRLName, " +
                    "color, " +
                    "Active" +
                    ") VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_TRL_SQL =
            "INSERT INTO TRL (" +
                    "CustomerId, " +
                    "ProjectId, " +
                    "TRLLevel, " +
                    "TRLName, " +
                    "TRLDescription, " +
                    "Active" +
                    ") VALUES (?, ?, ?, ?, ?, ?)";

    public InstallDefaultConfiguration(WebSession webSession) {
        super(webSession);
    }

    public void installDefaultIrlConfiguration(Integer customerId, Integer projectId) throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_IRL_SQL)) {

            for (IrlType irlType : IrlType.values()) {
                if (irlType == IrlType.INVALID_IRL_LEVEL) {
                    continue;
                }

                setInt(ps, customerId, 1);
                setInt(ps, projectId, 2);
                ps.setInt(3, irlType.getIrlId());
                ps.setString(4, irlType.getIrlCode());
                ps.setInt(5, irlType.getIrlLevel());
                ps.setString(6, irlType.getIrlName());
                ps.setString(7, irlType.getIrlDescription());
                ps.setBoolean(8, irlType.isActive());

                executeInsertIgnoringDuplicateKey(ps);
            }
        }
    }

    public void installDefaultSrlConfiguration(Integer customerId, Integer projectId) throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_SRL_SQL)) {

            for (SrlType srlType : SrlType.values()) {
                if (srlType == SrlType.INVALID_SRL_LEVEL) {
                    continue;
                }

                setInt(ps, customerId, 1);
                setInt(ps, projectId, 2);
                ps.setInt(3, srlType.getTrlLevel());
                ps.setString(4, srlType.getSrlName());
                ps.setString(5, srlType.getSrlColor());
                ps.setBoolean(6, srlType.isActive());

                executeInsertIgnoringDuplicateKey(ps);
            }
        }
    }

    public void installDefaultTrlConfiguration(Integer customerId, Integer projectId) throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_TRL_SQL)) {

            for (TrlType trlType : TrlType.values()) {
                if (trlType == TrlType.INVALID_TRL_LEVEL) {
                    continue;
                }

                setInt(ps, customerId, 1);
                setInt(ps, projectId, 2);
                ps.setInt(3, trlType.getTrlLevel());
                ps.setString(4, trlType.getTrlName());
                ps.setString(5, trlType.getTrlDescription());
                ps.setBoolean(6, trlType.isActive());

                executeInsertIgnoringDuplicateKey(ps);
            }
        }
    }

    private void executeInsertIgnoringDuplicateKey(PreparedStatement ps) throws SQLException {
        try {
            ps.executeUpdate();
        } catch (SQLException e) {
            if (!isDuplicateKeyException(e)) {
                throw e;
            }
        }
    }

    private boolean isDuplicateKeyException(SQLException e) {
        return e.getErrorCode() == SQL_SERVER_DUPLICATE_KEY ||
                e.getErrorCode() == SQL_SERVER_PRIMARY_KEY_VIOLATION;
    }
}