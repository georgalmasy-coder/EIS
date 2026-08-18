package com.bepa.eis.server.dataprovider.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import static com.bepa.eis.server.api.web.application.enums.EntityRequestType.EDIT_ENTITY;

@WebServlet(name = "CustomerServlet", urlPatterns = {
        "/customer"
})
@MultipartConfig
public class CustomerServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(CustomerServlet.class);

    @Override
    public void handleImport(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new UnsupportedOperationException("Customer import is not supported.");
    }

    @Override
    public void handleSave(
            WebSession webSession,
            HttpServletRequest request,
            Element rootElement
    ) {
        log.debug("Customer save is not implemented yet.");
    }

    @Override
    public GenericXmlDocument handleListOfEntities(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        throw new UnsupportedOperationException("Customer list is not supported.");
    }

    @Override
    public GenericXmlDocument handleEditEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer customerId,
            Integer version
    ) throws Throwable {
        return editCustomerById(webSession, customerId, version);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer parentEntityId
    ) throws Throwable {

        GenericXmlDocument xmlDocument = handleEditEntity(webSession, request, response, webSession.getCustomerId(), null);
        log.debug("Customer xml : {}", xmlDocument.toXmlString());
        return xmlDocument;
//        throw new UnsupportedOperationException("Create new customer is not supported.");
    }

    @Override
    public void handleExport(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new UnsupportedOperationException("Customer export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        throw new UnsupportedOperationException("Customer overview is not supported.");
    }

    private GenericXmlDocument editCustomerById(
            WebSession webSession,
            Integer customerId,
            Integer version
    ) {
        try {
            return new CustomerInfo(webSession, EDIT_ENTITY, customerId, version);
        } catch (Exception exception) {
            log.error("Error getting customer info document: {}", exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }
}
