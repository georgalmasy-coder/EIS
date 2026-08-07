package com.bepa.eis.server.api.web.application.views.basis.systemsbreakdown;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.*;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.common.enums.entity.EntityType;

public class SystemBreakdownInfo extends GenericXmlDocument {

    private ListOfElements rootElement;
    private TopPanel topPanel;

    public SystemBreakdownInfo(WebSession webSession, EntityRequestType type, Integer parentEntityId) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, -1, null, parentEntityId);
    }

    public SystemBreakdownInfo(WebSession webSession, EntityRequestType type, Integer entityId, Integer version) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, entityId, version, null);
    }

    private void buildXmlDocument(WebSession webSession, EntityRequestType type, Integer entityId, Integer version, Integer parentEntityId) throws Exception{

        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.SYSTEMS_BREAKDOWN_EDIT_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        Entities systemBreakdown;
        if (type == EntityRequestType.EDIT_ENTITY) {
            systemBreakdown = systemBreakdownProvider.getSystemBreakdownInfo(entityId, version);
        } else if (type == EntityRequestType.CREATE_ENTITY) {
            systemBreakdown = systemBreakdownProvider.getSystemBreakdownInfo(parentEntityId);
        } else {
            throw new IllegalArgumentException("Invalid entity request type");
        }

        rootElement.addElement(systemBreakdown.getListOfEntities());

        Entities systemBreakdownHistory = systemBreakdownProvider.getSystemBreakdownHistory(entityId);
        rootElement.addElement(systemBreakdownHistory.getListOfEntities());

        EntityNoteProvider entityNoteProvider = new EntityNoteProvider(webSession);
        EntityNotes entityNotes = entityNoteProvider.getEntityNotesByEntityId(EntityType.SYSTEMS_BREAKDOWN, entityId, version);
        rootElement.addElement(entityNotes.getEntityNoteElements());

        EntityLinkProvider entityLinkProvider = new EntityLinkProvider(webSession);
        EntityLinks entityLinks = entityLinkProvider.getEntityLinkByEntityId(EntityType.SYSTEMS_BREAKDOWN, entityId, version);
        rootElement.addElement(entityLinks.getEntityLinkElements());

        EntityAttachmentProvider entityAttachmentProvider = new EntityAttachmentProvider(webSession);
        EntityAttachments entityAttachments = entityAttachmentProvider.getEntityAttachmentsByEntityId(EntityType.SYSTEMS_BREAKDOWN, entityId);
        rootElement.addElement(entityAttachments.getEntityAttachmentElements());

        EntityRelationProvider entityRelationProvider = new EntityRelationProvider(webSession);
        EntityRelations entityRelations = entityRelationProvider.getEntityRelationsByEntityId(EntityType.SYSTEMS_BREAKDOWN, entityId);
        rootElement.addElement(entityRelations.getEntityRelationElements());
    }

}
