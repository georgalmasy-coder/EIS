package com.bepa.eis.server.api.web.application.views.basis.stakeholder;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import com.bepa.eis.server.api.web.application.views.common.*;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.entities.StakeholderProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class StakeholderInfo extends GenericXmlDocument {

    private ListOfElements rootElement;
    private TopPanel topPanel;
    private final static EntityType entityType = EntityType.STAKEHOLDER;

    public StakeholderInfo(WebSession webSession, EntityRequestType type) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, -1, null);
    }

    public StakeholderInfo(WebSession webSession, EntityRequestType type, Integer entityId, Integer version) throws Exception {
        super(webSession);
        buildXmlDocument(webSession, type, entityId, version);
    }

    private void buildXmlDocument(WebSession webSession, EntityRequestType type, Integer entityId, Integer version) throws Exception{

        StakeholderProvider stakeholderProvider = new StakeholderProvider(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.STAKEHOLDER_EDIT_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        Entities stakeholders;
        if (type == EntityRequestType.EDIT_ENTITY) {
            stakeholders = stakeholderProvider.getStakeholderInfo(entityId, version);
        } else if (type == EntityRequestType.CREATE_ENTITY) {
            stakeholders = stakeholderProvider.getStakeholderInfo();
        } else {
            throw new IllegalArgumentException("Invalid entity request type");
        }

        rootElement.addElement(stakeholders.getListOfEntities());

        Entities systemBreakdownHistory = stakeholderProvider.getStakeholderHistory(entityId);
        rootElement.addElement(systemBreakdownHistory.getListOfEntities());

        EntityNoteProvider entityNoteProvider = new EntityNoteProvider(webSession);
        EntityNotes entityNotes = entityNoteProvider.getEntityNotesByEntityId(entityType, entityId, version);
        rootElement.addElement(entityNotes.getEntityNoteElements());

        EntityAttachmentProvider entityAttachmentProvider = new EntityAttachmentProvider(webSession);
        EntityAttachments entityAttachments = entityAttachmentProvider.getEntityAttachmentsByEntityId(entityType, entityId);
        rootElement.addElement(entityAttachments.getEntityAttachmentElements());

    }

}
