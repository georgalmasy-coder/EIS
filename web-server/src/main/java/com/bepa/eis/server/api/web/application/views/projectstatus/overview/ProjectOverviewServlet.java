package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


@WebServlet(name = "ProjectOverviewServlet", urlPatterns = "/project/overview")
public class ProjectOverviewServlet extends GenericDataProviderServlet  {

    private static final Logger log = LoggerFactory.getLogger(ProjectOverviewServlet.class);

    public GenericXmlDocument processGet(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ProjectOverview projectOverview;

        try {
            projectOverview = new ProjectOverview(webSession);
        } catch (Exception e) {
            log.error("Error getting project basic info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return projectOverview;
    }

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {

    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) throws Exception {
        throw new RuntimeException("Invalid save request");
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
       throw new RuntimeException("Invalid list of entities request");
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable {
        throw new RuntimeException("Invalid edit entity request");
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable {
        throw new RuntimeException("Invalid create entity request");
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        throw new RuntimeException("Invalid export request");
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return getProjectOverview(webSession);
    }

    public GenericXmlDocument getProjectOverview(WebSession webSession) throws Throwable {
        ProjectOverview projectOverview;

        try {
            projectOverview = new ProjectOverview(webSession);
        } catch (Exception e) {
            log.error("Error getting project basic info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return projectOverview;
    }


}
