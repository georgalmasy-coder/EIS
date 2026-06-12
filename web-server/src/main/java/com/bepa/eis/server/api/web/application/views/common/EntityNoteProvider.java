package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.integers.ids.*;
import com.bepa.eis.server.dataprovider.fields.lookups.common.CreatedBy;
import com.bepa.eis.server.dataprovider.fields.strings.EntityNoteText;
import com.bepa.eis.server.dataprovider.fields.timestamp.CreatedDateTime;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.common.enums.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EntityNoteProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(EntityNoteProvider.class);

    private static final String GET_ENTITY_NOTES_BY_ENTITY_ID_SQL =
            "SELECT EN.* " +
            "FROM ENTITY E, ENTITY_NOTES EN " +
            "WHERE E.CustomerId = EN.CustomerId " +
            "AND E.ProjectId = EN.ProjectId " +
            "AND E.EntityType = EN.EntityType " +
            "AND E.EntityId = EN.EntityId " +
            "AND E.Version = EN.Version " +
            "AND E.CustomerId = ? " +
            "AND E.ProjectId = ? " +
            "AND E.EntityType =  ? " +
            "AND E.EntityId = ? " +
            " #VERSION_CONDITION# " +
            "ORDER BY EN.CreatedTime DESC";

    public EntityNoteProvider(WebSession webSession) {
        super(webSession);
    }

    public EntityNotes getEntityNotesByEntityId(EntityType entityType, Integer entityId, Integer historyVersion) throws SQLException {

        EntityNotes entityNotes = new EntityNotes(getWebSession());

        if (getWebSession() != null && getWebSession().getProjectId() != null) {

            String noteSql = GET_ENTITY_NOTES_BY_ENTITY_ID_SQL;
            String versionCondition = historyVersion != null ? " AND E.VERSION = " + historyVersion : " AND E.LATEST = 1 ";
            noteSql = noteSql.replace("#VERSION_CONDITION#", versionCondition);

            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(noteSql)) {

                setInt(ps, getWebSession().getCustomerId(), 1);
                setInt(ps, getWebSession().getProjectId(), 2);
                setInt(ps, entityType.getId(), 3);
                setInt(ps, entityId, 4);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {

                        EntityNote entityNote = entityNotes.getNewEntityNote();
                        entityNote.addElement(new EntityNoteId(rs.getInt(EntityNoteId.FIELD_NAME)));
                        entityNote.addElement(new EntityNoteText(rs.getString(EntityNoteText.FIELD_NAME)));

                        CreatedBy createdBy = new CreatedBy(getWebSession(), rs.getInt(CreatedBy.FIELD_NAME));
                        entityNote.addElement(createdBy);
                        entityNote.addElement(new CreatedDateTime(rs.getTimestamp(CreatedDateTime.FIELD_NAME)));

                        entityNotes.addEntityNote(entityNote);
                    }

                }
            }
        }
        return entityNotes;
    }
}
