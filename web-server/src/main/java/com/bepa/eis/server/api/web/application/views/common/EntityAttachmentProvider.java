package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.binary.FileData;
import com.bepa.eis.server.dataprovider.fields.booleans.FileDeleted;
import com.bepa.eis.server.dataprovider.fields.integers.FileSize;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityAttachmentId;
import com.bepa.eis.server.dataprovider.fields.lookups.common.CreatedBy;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.timestamp.CreatedDateTime;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.common.enums.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EntityAttachmentProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(EntityAttachmentProvider.class);

    private static final String GET_ENTITY_ATTACHMENTS_BY_ENTITY_ID_SQL =
            "SELECT EA.* " +
            "FROM ENTITY E, ENTITY_ATTACHMENTS EA " +
            "WHERE E.CustomerId = EA.CustomerId " +
            "AND E.ProjectId = EA.ProjectId " +
            "AND E.EntityType = EA.EntityType " +
            "AND E.EntityId = EA.EntityId " +
//            "AND E.Version = EN.Version " +
            "AND E.CustomerId = ? " +
            "AND E.ProjectId = ? " +
            "AND E.EntityType =  ? " +
            "AND E.EntityId = ? " +
            "AND E.Latest = 1 " +
            "ORDER BY EA.CreatedTime DESC";

    public EntityAttachmentProvider(WebSession webSession) {
        super(webSession);
    }

    public EntityAttachments getEntityAttachmentsByEntityId(EntityType entityType, Integer entityId) throws SQLException {

        EntityAttachments entityAttachments = new EntityAttachments(getWebSession());

        if (getWebSession() != null && getWebSession().getProjectId() != null) {

            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(GET_ENTITY_ATTACHMENTS_BY_ENTITY_ID_SQL)) {

                setInt(ps, getWebSession().getCustomerId(), 1);
                setInt(ps, getWebSession().getProjectId(), 2);
                setInt(ps, entityType.getId(), 3);
                setInt(ps, entityId, 4);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {

                        EntityAttachment entityAttachment = entityAttachments.getNewEntityAttachment();

                        EntityAttachmentId entityAttachmentId = new EntityAttachmentId(rs.getInt(EntityAttachmentId.FIELD_NAME));
                        entityAttachment.addElement(entityAttachmentId);

                        FileName fileName = new FileName(rs.getString(FileName.FIELD_NAME));
                        entityAttachment.addElement(fileName);

                        ContentType contentType = new ContentType(rs.getString(ContentType.FIELD_NAME));
                        entityAttachment.addElement(contentType);

                        FileSize fileSize = new FileSize(rs.getInt(FileSize.FIELD_NAME));
                        entityAttachment.addElement(fileSize);

                        FileDescription  fileDescription = new FileDescription(rs.getString(FileDescription.FIELD_NAME));
                        entityAttachment.addElement(fileDescription);

                        FileDeleted fileDeleted = new FileDeleted(rs.getBoolean(FileDeleted.FIELD_NAME));
                        fileDeleted.setFieldNotVisible();
                        entityAttachment.addElement(fileDeleted);

                        FileData fileData = new FileData(rs.getBytes(FileData.FIELD_NAME));
                        entityAttachment.addElement(fileData);

                        entityAttachment.addElement(new CreatedBy(getWebSession(), rs.getInt(CreatedBy.FIELD_NAME)));
                        entityAttachment.addElement(new CreatedDateTime(rs.getTimestamp(CreatedDateTime.FIELD_NAME)));

                        entityAttachments.addEntityAttachment(entityAttachment);
                    }

                }
            }
        }
        return entityAttachments;
    }
}
