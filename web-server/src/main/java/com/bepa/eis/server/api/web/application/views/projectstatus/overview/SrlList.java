package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class SrlList extends ListOfElements {

    public SrlList(WebSession webSession) {
        super(webSession);
        setElementName(this.getClass().getSimpleName());
    }

    public SrlList(WebSession webSession, String elementName) {
        super(webSession, elementName);
    }

    public SrlList getSrlElements() {
        return this;
    }

    public Srl getNewSrl() {
        Srl srl = new Srl();
        return srl;
    }

    public void addSrl(Srl srl) {
        this.addElement(srl);
    }

}
