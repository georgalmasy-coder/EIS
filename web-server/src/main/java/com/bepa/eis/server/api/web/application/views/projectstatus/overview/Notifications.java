package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class Notifications extends ListOfElements {

    public Notifications(WebSession webSession) {
        super(webSession);
        setElementName(this.getClass().getSimpleName());
    }

    public Notifications(WebSession webSession, String elementName) {
        super(webSession, elementName);
    }

    public Notifications getNotificationElements() {
        return this;
    }

    public Notification getNewNotification() {
        Notification notification = new Notification(getWebSession());
        return notification;
    }

    public void addNotification(Notification notification) {
        this.addElement(notification);
    }

}
