package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(
        name = "PsysInterfaceMatrixServlet",
        urlPatterns = {
                "/pro/psys/interfacematrix",
                "/pro/psys/interfacematrix/*"
        }
)
@MultipartConfig
public class PsysInterfaceMatrixServlet extends InterfaceMatrixServlet {

    private static final Logger log = LoggerFactory.getLogger(PsysInterfaceMatrixServlet.class);

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {

        return buildInterfaceMatrix(webSession, resolveEntityType(request));
    }

    @Override
    public GenericXmlDocument buildInterfaceMatrix (WebSession webSession, EntityType entityType) {
        try {
            return new PsysInterfaceMatrixDocument(webSession, entityType);
        } catch (Exception e) {
            try {
                logIncidentError("P-SYS-InterfaceMatrixServlet", e);
            } catch (Throwable throwable) {
                // Ignore error
            }

            log.error("Error getting p-sys interface management matrix document: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }
}
