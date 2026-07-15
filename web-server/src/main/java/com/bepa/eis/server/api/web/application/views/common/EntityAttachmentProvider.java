package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.common.AttachmentRecord;
import com.bepa.eis.server.dataprovider.fields.binary.FileData;
import com.bepa.eis.server.dataprovider.fields.integers.FileSize;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityAttachmentId;
import com.bepa.eis.server.dataprovider.fields.lookups.common.CreatedBy;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.timestamp.CreatedDateTime;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class EntityAttachmentProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(EntityAttachmentProvider.class);

    private static final String GET_ENTITY_ATTACHMENTS_BY_ENTITY_ID_SQL =
            "SELECT EA.CustomerId, EA.ProjectId, EA.EntityType, EA.EntityId, EA.Version, EA.CreatedById, EA.CreatedTime, " +
            "       EAB.EntityAttachmentBlobPK AS EntityAttachmentPK, EAB.FileName, EAB.ContentType, EAB.FileSize, EAB.FileData, EAB.Description  " +
            "FROM ENTITY E, ENTITY_ATTACHMENTS EA, ENTITY_ATTACHMENTS_BLOB EAB " +
            "WHERE E.CustomerId = EA.CustomerId " +
            "AND E.ProjectId = EA.ProjectId " +
            "AND E.EntityType = EA.EntityType " +
            "AND E.EntityId = EA.EntityId " +
            "AND E.Version = EA.Version " +
            "AND E.CustomerId = ? " +
            "AND E.ProjectId = ? " +
            "AND E.EntityType =  ? " +
            "AND E.EntityId = ? " +
            "AND EA.EntityAttachmentBlobPK = EAB.EntityAttachmentBlobPK " +
            "AND E.Latest = 1 " +
            "ORDER BY EA.CreatedTime DESC";

    private static final String INSERT_ENTITY_ATTACHMENT_SQL =
            "INSERT INTO ENTITY_ATTACHMENTS (" +
                    "  CustomerId, " +
                    "  ProjectId, " +
                    "  EntityId, " +
                    "  EntityType, " +
                    "  Version, " +
                    "  CreatedById, " +
                    "  CreatedTime, " +
                    "  EntityAttachmentBlobPK " +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ENTITY_ATTACHMENT_BLOB_SQL =
            "INSERT INTO ENTITY_ATTACHMENTS_BLOB (" +
                    "  FileName, ContentType, FileSize, FileData, Description" +
                    ") VALUES (?, ?, ?, ?, ?)";

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

    public void insertEntityAttachment(Connection con, AbstractEntity entity) throws SQLException {

        log.debug("Inserting entity attachments for entityId : {} count : {}", entity.getEntityId(), entity.getListOfEntityAttachments().size());

        for (AttachmentRecord attachmentRecord : entity.getListOfEntityAttachments()) {

            log.debug("Inserting entity attachment isFileDeleted : {} - PK {}", attachmentRecord.isFileDeleted(), attachmentRecord.getEntityAttachmentBlobPK());


            if (! attachmentRecord.isFileDeleted()) {
                if (attachmentRecord.getEntityAttachmentBlobPK() == null) {
                    int blobId = insertAttachmentBlob(con, attachmentRecord);
                    attachmentRecord.setEntityAttachmentBlobPK(blobId);

                    log.debug("Insert attachment BLOB - New PK : {}", attachmentRecord.getEntityAttachmentBlobPK());
               }

                try (PreparedStatement ps = con.prepareStatement(INSERT_ENTITY_ATTACHMENT_SQL)) {

                    ps.setInt(1, entity.getCustomerId().getValue());
                    ps.setInt(2, entity.getProjectId().getValue());
                    ps.setInt(3, entity.getEntityId().getValue());
                    ps.setInt(4, entity.getEntityType().getId());
                    ps.setInt(5, entity.getVersion().getValue());

                    Integer createdById = attachmentRecord.getChangedByUserId() != null ? attachmentRecord.getChangedByUserId().getValue() : getWebSession().getUserId();
                    ps.setInt(6, createdById);
                    ps.setTimestamp(7, Timestamp.valueOf(attachmentRecord.getChangedDate().getValue()));

                    ps.setInt(8, attachmentRecord.getEntityAttachmentBlobPK());

                    int rows = ps.executeUpdate();

                    if (rows == 0) {
                        throw new SQLException("Insert entity attachment failed for entityId = " + entity.getEntityId());
                    }

                }

            }

        }
    }

    private int insertAttachmentBlob(Connection con, AttachmentRecord attachmentRecord) throws SQLException {

        try (PreparedStatement ps = con.prepareStatement(INSERT_ENTITY_ATTACHMENT_BLOB_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, attachmentRecord.getFileName());
            ps.setString(2, attachmentRecord.getContentType());
            ps.setLong(3, attachmentRecord.getFileSize());
            ps.setBytes(4, attachmentRecord.getFileDataAsBinary());
            ps.setString(5, attachmentRecord.getDescription());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert blob failed");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Insert blob failed: no generated key returned");
    }
}
