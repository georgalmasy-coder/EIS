package com.bepa.eis.server.api.web.application.views.basis.stakeholderrequirement;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import com.bepa.eis.server.api.web.application.views.common.*;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.common.enums.entity.EntityType;

public class StakeholderRequirementInfo extends GenericXmlDocument {

    private ListOfElements rootElement;
    private TopPanel topPanel;
    private final static EntityType entityType = EntityType.STAKEHOLDER_REQUIREMENT;

    public StakeholderRequirementInfo(WebSession webSession, EntityRequestType type, Integer parentEntityId) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, -1, null, parentEntityId);
    }

    public StakeholderRequirementInfo(WebSession webSession, EntityRequestType type, Integer entityId, Integer version) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, entityId, version, null);
    }

    private void buildXmlDocument(WebSession webSession, EntityRequestType type, Integer entityId, Integer version, Integer parentEntityId) throws Exception{

        StakeholderRequirementProvider stakeholderRequirementProvider = new StakeholderRequirementProvider(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());

        Entities basisRequirements;
        if (type == EntityRequestType.EDIT_ENTITY) {
            basisRequirements = stakeholderRequirementProvider.getBasisRequirementInfo(entityId, version);
        } else if (type == EntityRequestType.CREATE_ENTITY) {
            basisRequirements = stakeholderRequirementProvider.getBasisRequirementInfo(parentEntityId);
        } else {
            throw new IllegalArgumentException("Invalid entity request type");
        }

        rootElement.addElement(basisRequirements.getListOfEntities());

        Entities systemBreakdownHistory = stakeholderRequirementProvider.getBasisRequirementHistory(entityId);
        rootElement.addElement(systemBreakdownHistory.getListOfEntities());

        EntityNoteProvider entityNoteProvider = new EntityNoteProvider(webSession);
        EntityNotes entityNotes = entityNoteProvider.getEntityNotesByEntityId(entityType, entityId, version);
        rootElement.addElement(entityNotes.getEntityNoteElements());

        EntityAttachmentProvider entityAttachmentProvider = new EntityAttachmentProvider(webSession);
        EntityAttachments entityAttachments = entityAttachmentProvider.getEntityAttachmentsByEntityId(entityType, entityId);
        rootElement.addElement(entityAttachments.getEntityAttachmentElements());

        EntityRelationProvider entityRelationProvider = new EntityRelationProvider(webSession);
        EntityRelations entityRelations = entityRelationProvider.getEntityRelationsByEntityId(entityType, entityId);
        rootElement.addElement(entityRelations.getEntityRelationElements());

    }

}
