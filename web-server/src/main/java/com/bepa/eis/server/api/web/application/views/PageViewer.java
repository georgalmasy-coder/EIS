package com.bepa.eis.server.api.web.application.views;

import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.server.api.generic.BuildInfo;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@WebServlet(name = "PageViewer", urlPatterns = "/web/view")
public class PageViewer extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(PageViewer.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        WebSession webSession = getSession(request);
        String page = request.getParameter("page");

        try {
            PageType pageType = PageType.mapToType(page);
            if (pageType != PageType.NONE) {

                String html = loadHtmlPage(pageType.getPath());
                html = mergeTitle(pageType, webSession, html);

                html = mergeBuildNumber(html);

                setContextInResponse(response);
                response.getWriter().write(html);

            } else {
                log.error("Page not found : {}", page);
                throw new IllegalArgumentException("Page not found : " + page);
            }
        } catch (Throwable throwable) {
            String module = request.getServletPath() +  "." + page;
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createWebServiceIncident(SeverityType.HIGH, module, throwable);
        }

    }

    private String mergeTitle(PageType pageType, WebSession webSession, String html) {
        String title = pageType.getTitle();

        String projectName = "";
        try {
            TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
            projectName = topPanelProvider.getProjectName();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        title = title + " - " + projectName;

        return html.replace("{{title}}", title);
    }

    private String mergeBuildNumber(String html) {
        String version = BuildInfo.buildNumber();

        html = html.replace(".js\"></script>", ".js?v=" + version + "\"></script>");
        html = html.replace("src=\"../js/", "src=\"/js/");

        html = html.replace(".css\"", ".css?v=" + version + "\"");
        html = html.replace("<link rel=\"stylesheet\" href=\"../css", "<link rel=\"stylesheet\" href=\"/css");

        return html;
    }

    private void setContextInResponse(HttpServletResponse response) {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private String loadHtmlPage(String resourcePath) throws IOException {
        try (InputStream inputStream = PageViewer.class
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (inputStream == null) {
                throw new IOException("HTML resource not found: " + resourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
