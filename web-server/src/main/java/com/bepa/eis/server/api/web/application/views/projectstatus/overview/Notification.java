package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class Notification extends ListOfElements {

    ListOfElements notificationsElements = new ListOfElements(getWebSession(), this.getClass().getSimpleName());

    public Notification(WebSession webSession) {
        super(webSession);
        super.setElementName(this.getClass().getSimpleName());
    }

    public ListOfElements getNotificationsElements() {
        return notificationsElements;
    }

}
