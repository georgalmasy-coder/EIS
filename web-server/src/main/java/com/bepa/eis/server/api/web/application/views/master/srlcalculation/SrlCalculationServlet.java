package com.bepa.eis.server.api.web.application.views.master.srlcalculation;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.w3c.dom.Element;

@WebServlet(name = "SrlCalculationServlet", urlPatterns = {"/master/srlcalculation", "/master/srlcalculation/*"})
public class SrlCalculationServlet extends GenericDataProviderServlet {

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request,
                                             HttpServletResponse response) throws Exception {
        return new SrlCalculationDocument(webSession);
    }

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new IllegalArgumentException("Invalid import request");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        throw new IllegalArgumentException("Invalid save request");
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request,
                                                   HttpServletResponse response) {
        throw new IllegalArgumentException("Invalid list request");
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request,
                                               HttpServletResponse response, Integer entityId, Integer version) {
        throw new IllegalArgumentException("Invalid edit request");
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request,
                                                 HttpServletResponse response, Integer parentEntityId) {
        throw new IllegalArgumentException("Invalid create request");
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new IllegalArgumentException("Invalid export request");
    }
}
