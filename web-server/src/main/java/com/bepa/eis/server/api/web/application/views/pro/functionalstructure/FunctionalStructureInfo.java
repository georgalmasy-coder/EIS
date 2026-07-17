package com.bepa.eis.server.api.web.application.views.pro.functionalstructure;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import com.bepa.eis.server.api.web.application.views.common.*;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.entities.FunctionalStructureProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class FunctionalStructureInfo extends GenericXmlDocument {

    private ListOfElements rootElement;
    private TopPanel topPanel;
    private final static EntityType entityType = EntityType.FUNCTIONAL_STRUCTURE;

    public FunctionalStructureInfo(WebSession webSession, EntityRequestType type, Integer parentEntityId) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, -1, null, parentEntityId);
    }

    public FunctionalStructureInfo(WebSession webSession, EntityRequestType type, Integer entityId, Integer version) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, entityId, version, null);
    }

    private void buildXmlDocument(WebSession webSession, EntityRequestType type, Integer entityId, Integer version, Integer parentEntityId) throws Exception{

        FunctionalStructureProvider functionalStructureProvider = new FunctionalStructureProvider(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());

        Entities functionalStructures;
        if (type == EntityRequestType.EDIT_ENTITY) {
            functionalStructures = functionalStructureProvider.getFunctionalStructureInfo(entityId, version);
        } else if (type == EntityRequestType.CREATE_ENTITY) {
            functionalStructures = functionalStructureProvider.getFunctionalStructureInfo(parentEntityId);
        } else {
            throw new IllegalArgumentException("Invalid entity request type");
        }

        rootElement.addElement(functionalStructures.getListOfEntities());

        Entities functionalStructureHistory = functionalStructureProvider.getFunctionalStructureHistory(entityId);
        rootElement.addElement(functionalStructureHistory.getListOfEntities());

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

