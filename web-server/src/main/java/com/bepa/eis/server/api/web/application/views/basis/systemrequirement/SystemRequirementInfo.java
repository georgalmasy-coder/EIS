package com.bepa.eis.server.api.web.application.views.basis.systemrequirement;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.*;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.common.enums.entity.EntityType;

public class SystemRequirementInfo extends GenericXmlDocument {

    private final static EntityType entityType = EntityType.SYSTEM_REQUIREMENT;

    public SystemRequirementInfo(WebSession webSession, EntityRequestType type, Integer parentEntityId) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, null, null, parentEntityId);
    }

    public SystemRequirementInfo(WebSession webSession, EntityRequestType type, Integer entityId, Integer version) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, entityId, version, null);
    }

    private void buildXmlDocument(WebSession webSession, EntityRequestType type, Integer entityId, Integer version, Integer parentEntityId) throws Exception{

        SystemRequirementProvider systemRequirementProvider = new SystemRequirementProvider(webSession);

        ListOfElements rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        TopPanel topPanel = topPanelProvider.getTopPanelBySession(PageType.SYSTEM_REQUIREMENT_EDIT_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        Entities basisRequirements;
        if (type == EntityRequestType.EDIT_ENTITY) {
            basisRequirements = systemRequirementProvider.getSystemRequirementInfo(entityId, version);
        } else if (type == EntityRequestType.CREATE_ENTITY) {
            basisRequirements = systemRequirementProvider.getSystemRequirementInfo(parentEntityId);
        } else {
            throw new IllegalArgumentException("Invalid entity request type");
        }

        rootElement.addElement(basisRequirements.getListOfEntities());

        Entities systemRequirementHistory = systemRequirementProvider.getSystemRequirementHistory(entityId);
        rootElement.addElement(systemRequirementHistory.getListOfEntities());

        EntityNoteProvider entityNoteProvider = new EntityNoteProvider(webSession);
        EntityNotes entityNotes = entityNoteProvider.getEntityNotesByEntityId(entityType, entityId, version);
        rootElement.addElement(entityNotes.getEntityNoteElements());

        EntityLinkProvider entityLinkProvider = new EntityLinkProvider(webSession);
        EntityLinks entityLinks = entityLinkProvider.getEntityLinkByEntityId(entityType, entityId, version);
        rootElement.addElement(entityLinks.getEntityLinkElements());

        EntityAttachmentProvider entityAttachmentProvider = new EntityAttachmentProvider(webSession);
        EntityAttachments entityAttachments = entityAttachmentProvider.getEntityAttachmentsByEntityId(entityType, entityId);
        rootElement.addElement(entityAttachments.getEntityAttachmentElements());

        EntityRelationProvider entityRelationProvider = new EntityRelationProvider(webSession);
        EntityRelations entityRelations = entityRelationProvider.getEntityRelationsByEntityId(entityType, entityId);
        rootElement.addElement(entityRelations.getEntityRelationElements());

    }

}
