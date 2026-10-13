package com.bepa.eis.common.providers.mail;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Database-backed mail templates for administration and delivery. */
public class MailTemplateCatalogProvider extends GenericProvider {
    public record Template(String key, String name, String subject, String bodyHtml, String contentType,
                           String sourceFileName, String placeholdersJson, int revision, String updatedAt) {}

    public MailTemplateCatalogProvider(WebSession session) { super(session); }

    public List<Template> list() throws SQLException {
        try (Connection connection = getDataSource().getConnection(); PreparedStatement query = connection.prepareStatement(
                "SELECT TemplateKey, Name, Subject, ContentType, SourceFileName, PlaceholdersJson, Revision, UpdatedAt FROM dbo.MAIL_TEMPLATE ORDER BY Name");
             ResultSet result = query.executeQuery()) {
            List<Template> rows = new ArrayList<>();
            while (result.next()) rows.add(read(result, false));
            return rows;
        }
    }

    public Template get(String key) throws SQLException {
        try (Connection connection = getDataSource().getConnection(); PreparedStatement query = connection.prepareStatement(
                "SELECT * FROM dbo.MAIL_TEMPLATE WHERE TemplateKey = ?")) {
            query.setString(1, key);
            try (ResultSet result = query.executeQuery()) { return result.next() ? read(result, true) : null; }
        }
    }

    public List<Template> exportAll() throws SQLException {
        try (Connection connection = getDataSource().getConnection();
             var query = connection.prepareStatement("SELECT * FROM dbo.MAIL_TEMPLATE ORDER BY TemplateKey"); var result = query.executeQuery()) {
            List<Template> rows = new ArrayList<>();
            while (result.next()) rows.add(read(result, true));
            return rows;
        }
    }

    /** All rows are validated while locked, then applied in one transaction. No partial imports. */
    public int importAll(List<Template> templates, java.util.function.BiConsumer<Template, Template> validator) throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                var existing = new java.util.HashMap<String, Template>();
                try (var query = connection.prepareStatement("SELECT * FROM dbo.MAIL_TEMPLATE WITH (UPDLOCK, HOLDLOCK)"); var result = query.executeQuery()) {
                    while (result.next()) { var row = read(result, true); existing.put(row.key(), row); }
                }
                for (Template template : templates) validator.accept(template, existing.get(template.key()));
                try (var update = connection.prepareStatement("UPDATE dbo.MAIL_TEMPLATE SET Name=?, Subject=?, BodyHtml=?, ContentType=?, SourceFileName=?, PlaceholdersJson=?, Revision=Revision+1, UpdatedAt=SYSUTCDATETIME(), UpdatedByUserId=? WHERE TemplateKey=?");
                     var insert = connection.prepareStatement("INSERT INTO dbo.MAIL_TEMPLATE (Name,Subject,BodyHtml,ContentType,SourceFileName,PlaceholdersJson,UpdatedByUserId,TemplateKey) VALUES (?,?,?,?,?,?,?,?)")) {
                    for (Template template : templates) {
                        var statement = existing.containsKey(template.key()) ? update : insert;
                        statement.setString(1, template.name()); statement.setString(2, template.subject());
                        statement.setString(3, template.bodyHtml()); statement.setString(4, template.contentType());
                        statement.setString(5, template.sourceFileName()); statement.setString(6, template.placeholdersJson());
                        setInt(statement, getWebSession() == null ? null : getWebSession().getUserId(), 7);
                        statement.setString(8, template.key()); statement.executeUpdate();
                    }
                }
                connection.commit(); return templates.size();
            } catch (SQLException | RuntimeException e) { connection.rollback(); throw e; }
        }
    }

    /** Optimistic concurrency: another administrator's edit cannot be overwritten silently. */
    public boolean save(String key, String subject, String bodyHtml, int revision) throws SQLException {
        try (Connection connection = getDataSource().getConnection(); PreparedStatement update = connection.prepareStatement(
                "UPDATE dbo.MAIL_TEMPLATE SET Subject=?, BodyHtml=?, Revision=Revision+1, UpdatedAt=SYSUTCDATETIME(), UpdatedByUserId=? WHERE TemplateKey=? AND Revision=?")) {
            update.setString(1, subject);
            update.setString(2, bodyHtml);
            update.setInt(3, getWebSession().getUserId());
            update.setString(4, key);
            update.setInt(5, revision);
            return update.executeUpdate() == 1;
        }
    }

    private Template read(ResultSet result, boolean includeBody) throws SQLException {
        return new Template(result.getString("TemplateKey"), result.getString("Name"), result.getString("Subject"),
                includeBody ? result.getString("BodyHtml") : null, result.getString("ContentType"),
                result.getString("SourceFileName"), result.getString("PlaceholdersJson"), result.getInt("Revision"),
                result.getTimestamp("UpdatedAt").toInstant().toString());
    }
}
