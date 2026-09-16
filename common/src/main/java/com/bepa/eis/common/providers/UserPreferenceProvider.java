package com.bepa.eis.common.providers;

import com.bepa.eis.common.dto.WebSession;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserPreferenceProvider extends GenericProvider {

    private static final String SELECT_SELECTED_PROJECT_ID_SQL = """
            SELECT Preferences.value(
                '(/UserPreferences/SelectedProjectId/text())[1]',
                'int'
            ) AS SelectedProjectId
            FROM [dbo].[USER_PREFERENCE]
            WHERE UserId = ?
            """;

    private static final String UPDATE_SELECTED_PROJECT_ID_SQL = """
            DECLARE @UserId INT = ?;
            DECLARE @SelectedProjectId INT = ?;

            MERGE INTO [dbo].[USER_PREFERENCE] WITH (HOLDLOCK) AS target
            USING (SELECT @UserId AS UserId) AS source
                ON target.UserId = source.UserId
            WHEN NOT MATCHED THEN
                INSERT (UserId, Preferences, ChangedAt)
                VALUES (source.UserId, CONVERT(XML, N'<UserPreferences />'), SYSUTCDATETIME());

            IF @SelectedProjectId IS NULL
            BEGIN
                UPDATE [dbo].[USER_PREFERENCE]
                SET Preferences.modify('delete (/UserPreferences/SelectedProjectId)[1]'),
                    ChangedAt = SYSUTCDATETIME()
                WHERE UserId = @UserId;
            END
            ELSE IF EXISTS (
                SELECT 1
                FROM [dbo].[USER_PREFERENCE]
                WHERE UserId = @UserId
                  AND Preferences.exist('/UserPreferences/SelectedProjectId') = 1
            )
            BEGIN
                UPDATE [dbo].[USER_PREFERENCE]
                SET Preferences.modify(
                        'replace value of (/UserPreferences/SelectedProjectId/text())[1]
                         with sql:variable("@SelectedProjectId")'
                    ),
                    ChangedAt = SYSUTCDATETIME()
                WHERE UserId = @UserId;
            END
            ELSE
            BEGIN
                UPDATE [dbo].[USER_PREFERENCE]
                SET Preferences.modify(
                        'insert <SelectedProjectId>{sql:variable("@SelectedProjectId")}</SelectedProjectId>
                         as last into (/UserPreferences)[1]'
                    ),
                    ChangedAt = SYSUTCDATETIME()
                WHERE UserId = @UserId;
            END;
            """;

    private static final String SELECT_PREFERENCES_SQL = """
            SELECT CONVERT(NVARCHAR(MAX), Preferences) AS Preferences
            FROM [dbo].[USER_PREFERENCE]
            WHERE UserId = ?
            """;

    private static final String UPSERT_PREFERENCES_SQL = """
            DECLARE @UserId INT = ?;
            DECLARE @Preferences XML = CONVERT(XML, ?);

            MERGE INTO [dbo].[USER_PREFERENCE] WITH (HOLDLOCK) AS target
            USING (SELECT @UserId AS UserId, @Preferences AS Preferences) AS source
                ON target.UserId = source.UserId
            WHEN MATCHED THEN
                UPDATE SET Preferences = source.Preferences, ChangedAt = SYSUTCDATETIME()
            WHEN NOT MATCHED THEN
                INSERT (UserId, Preferences, ChangedAt)
                VALUES (source.UserId, source.Preferences, SYSUTCDATETIME());
            """;

    public UserPreferenceProvider(WebSession webSession) {
        super(webSession);
    }

    public Integer getSelectedProjectId(Integer userId) throws SQLException {
        if (userId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SELECTED_PROJECT_ID_SQL)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                int projectId = resultSet.getInt("SelectedProjectId");
                return resultSet.wasNull() ? null : projectId;
            }
        }
    }

    public void setSelectedProjectId(Integer userId, Integer projectId) throws SQLException {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SELECTED_PROJECT_ID_SQL)) {
            statement.setInt(1, userId);
            if (projectId == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, projectId);
            }
            statement.executeUpdate();
        }
    }

    public Map<String, String> getSettings(Integer userId) throws SQLException {
        if (userId == null) {
            return Map.of();
        }

        Document document = readPreferencesDocument(userId);
        NodeList settings = document.getElementsByTagName("Setting");
        Map<String, String> values = new LinkedHashMap<>();

        for (int index = 0; index < settings.getLength(); index++) {
            Node node = settings.item(index);
            if (node instanceof Element setting) {
                String key = setting.getAttribute("Key");
                if (key != null && !key.isBlank()) {
                    values.put(key, setting.getTextContent());
                }
            }
        }

        return values;
    }

    public String getSetting(Integer userId, String key) throws SQLException {
        if (key == null || key.isBlank()) {
            return null;
        }

        return getSettings(userId).get(key);
    }

    public void setSetting(Integer userId, String key, String value) throws SQLException {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is required");
        }

        Document document = readPreferencesDocument(userId);
        Element root = document.getDocumentElement();
        Element settings = getOrCreateChild(document, root, "Settings");
        Element setting = findSetting(settings, key);

        if (value == null) {
            if (setting != null) {
                settings.removeChild(setting);
            }
        } else {
            if (setting == null) {
                setting = document.createElement("Setting");
                setting.setAttribute("Key", key.trim());
                settings.appendChild(setting);
            }
            setting.setTextContent(value);
        }

        if (!settings.hasChildNodes()) {
            root.removeChild(settings);
        }

        writePreferencesDocument(userId, document);
    }

    private Document readPreferencesDocument(Integer userId) throws SQLException {
        String xml = null;

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PREFERENCES_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    xml = resultSet.getString("Preferences");
                }
            }
        }

        return parsePreferencesXml(xml);
    }

    private void writePreferencesDocument(Integer userId, Document document) throws SQLException {
        String xml = toXml(document);

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_PREFERENCES_SQL)) {
            statement.setInt(1, userId);
            statement.setString(2, xml);
            statement.executeUpdate();
        }
    }

    private Document parsePreferencesXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            if (xml == null || xml.isBlank()) {
                return factory.newDocumentBuilder().parse(new InputSource(new StringReader("<UserPreferences />")));
            }

            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse user preferences XML", e);
        }
    }

    private String toXml(Document document) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize user preferences XML", e);
        }
    }

    private Element getOrCreateChild(Document document, Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        if (children.getLength() > 0 && children.item(0) instanceof Element element) {
            return element;
        }

        Element child = document.createElement(tagName);
        parent.appendChild(child);
        return child;
    }

    private Element findSetting(Element settings, String key) {
        NodeList children = settings.getElementsByTagName("Setting");
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element element && key.trim().equals(element.getAttribute("Key"))) {
                return element;
            }
        }
        return null;
    }
}
