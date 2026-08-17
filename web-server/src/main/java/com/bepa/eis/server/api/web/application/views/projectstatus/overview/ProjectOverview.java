package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.*;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class ProjectOverview extends GenericXmlDocument {

    private ListOfElements rootElement;
    private TopPanel topPanel;
    private Project project;
    private Notifications notifications;
    private SrlList srlList;

    public ProjectOverview(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.PROJECT_OVERVIEW_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        ProjectOverviewProvider projectOverviewProvider = new ProjectOverviewProvider(webSession);
        project = projectOverviewProvider.getProjectByProjectId();
        rootElement.addElement(project.getProjectElements());

        NotificationProvider notificationProvider = new NotificationProvider(webSession);
        notifications = notificationProvider.getNotificationsByUserAndProjectId();
        rootElement.addElement(notifications.getNotificationElements());

        SrlProvider srlProvider = new SrlProvider(webSession);
        srlList = srlProvider.getSrlsByProjectId();
        rootElement.addElement(srlList.getSrlElements());
    }

}
