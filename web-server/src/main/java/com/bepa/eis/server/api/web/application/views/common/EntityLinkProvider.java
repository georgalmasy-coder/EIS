package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.dataprovider.entities.common.LinkRecord;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityLinkId;
import com.bepa.eis.server.dataprovider.fields.lookups.common.CreatedBy;
import com.bepa.eis.server.dataprovider.fields.strings.EntityLinkDescription;
import com.bepa.eis.server.dataprovider.fields.strings.EntityLinkUrl;
import com.bepa.eis.server.dataprovider.fields.timestamp.CreatedDateTime;
import com.bepa.eis.server.entites.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class EntityLinkProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(EntityLinkProvider.class);

    private static final String GET_ENTITY_LINKS_BY_ENTITY_ID_SQL =
            "SELECT EL.* " +
            "FROM ENTITY E, ENTITY_LINKS EL " +
            "WHERE E.CustomerId = EL.CustomerId " +
            "AND E.ProjectId = EL.ProjectId " +
            "AND E.EntityType = EL.EntityType " +
            "AND E.EntityId = EL.EntityId " +
            "AND E.Version = EL.Version " +
            "AND E.CustomerId = ? " +
            "AND E.ProjectId = ? " +
            "AND E.EntityType =  ? " +
            "AND E.EntityId = ? " +
            " #VERSION_CONDITION# " +
            "ORDER BY EL.CreatedTime DESC";

    private static final String INSERT_ENTITY_LINK_SQL =
            "INSERT INTO ENTITY_LINKS (" +
                    "  CustomerId, " +
                    "  ProjectId, " +
                    "  EntityId, " +
                    "  Version, " +
                    "  EntityType, " +
                    "  Description, " +
                    "  LinkUrl, " +
                    "  CreatedById, " +
                    "  CreatedTime" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";



    public EntityLinkProvider(WebSession webSession) {
        super(webSession);
    }

    public EntityLinks getEntityLinkByEntityId(EntityType entityType, Integer entityId, Integer historyVersion) throws SQLException {

        EntityLinks entityLinks = new EntityLinks(getWebSession());

        if (getWebSession() != null && getWebSession().getProjectId() != null) {

            String noteSql = GET_ENTITY_LINKS_BY_ENTITY_ID_SQL;
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

                        EntityLink entityLink = entityLinks.getNewEntityLink();
                        entityLink.addElement(new EntityLinkId(rs.getInt(EntityLinkId.FIELD_NAME)));
                        entityLink.addElement(new EntityLinkDescription(rs.getString(EntityLinkDescription.FIELD_NAME)));
                        entityLink.addElement(new EntityLinkUrl(rs.getString(EntityLinkUrl.FIELD_NAME)));

                        CreatedBy createdBy = new CreatedBy(getWebSession(), rs.getInt(CreatedBy.FIELD_NAME));
                        entityLink.addElement(createdBy);
                        entityLink.addElement(new CreatedDateTime(rs.getTimestamp(CreatedDateTime.FIELD_NAME)));

                        entityLinks.addEntityLink(entityLink);
                    }

                }
            }
        }
        return entityLinks;
    }

    public void insertEntityLinks(Connection con, AbstractEntity entity) throws SQLException {

        for (LinkRecord linkRecord : entity.getListOfEntityLinks()) {

            try (PreparedStatement ps = con.prepareStatement(INSERT_ENTITY_LINK_SQL)) {

                ps.setInt(1, entity.getCustomerId().getValue());
                ps.setInt(2, entity.getProjectId().getValue());
                ps.setInt(3, entity.getEntityId().getValue());
                ps.setInt(4, entity.getVersion().getValue());
                ps.setInt(5, entity.getEntityType().getId());
                ps.setString(6, linkRecord.getDescription());
                ps.setString(7, linkRecord.getUrl());
                ps.setInt(8, linkRecord.getChangedByUserId().getValue());
                ps.setTimestamp(9, Timestamp.valueOf(linkRecord.getChangedDate().getValue()));

                int rows = ps.executeUpdate();

                if (rows == 0) {
                    throw new SQLException("Insert entity link failed for entityId=" + entity.getEntityId());
                }
            }
        }
    }


}
