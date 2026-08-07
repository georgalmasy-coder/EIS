package com.bepa.eis.server.api.web.application.views.admin.department;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.DTO.Department;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.List;

@WebServlet(name = "DepartmentMaintenanceServlet", urlPatterns = {"/api/admin/departments"})
public class DepartmentMaintenanceServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(DepartmentMaintenanceServlet.class);

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Department import is not supported.");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        DepartmentMaintenanceProvider provider = new DepartmentMaintenanceProvider(webSession);
        Element departmentElement = firstChild(rootElement, "department");

        if (departmentElement == null && "department".equalsIgnoreCase(rootElement.getTagName())) {
            departmentElement = rootElement;
        }

        if (departmentElement == null) {
            throw new IllegalArgumentException("Department data is required.");
        }

        Department department = new Department();
        Integer departmentId = intValue(departmentElement, "DepartmentId");
        if (departmentId != null) {
            department.setDepartmentId(departmentId);
        }
        department.setDepartmentName(textValue(departmentElement, "DepartmentName"));
        department.setDepartmentDescription(textValue(departmentElement, "DepartmentDescription"));
        department.setActive(boolValue(departmentElement, "Active"));
        department.setCustomerId(webSession.getCustomerId());

        provider.saveDepartment(department);
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        DepartmentMaintenanceProvider provider = new DepartmentMaintenanceProvider(webSession);
        return buildDocument(webSession, provider, null);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) {
        DepartmentMaintenanceProvider provider = new DepartmentMaintenanceProvider(webSession);
        Department department = provider.getDepartmentById(entityId);

        if (department == null) {
            throw new IllegalArgumentException("Department was not found.");
        }

        return buildDocument(webSession, provider, department);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) {
        DepartmentMaintenanceProvider provider = new DepartmentMaintenanceProvider(webSession);
        Department department = new Department();
        department.setCustomerId(webSession.getCustomerId());
        department.setDepartmentName("");
        department.setDepartmentDescription("");
        department.setActive(true);
        return buildDocument(webSession, provider, department);
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Department export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        return handleListOfEntities(webSession, request, response);
    }

    private DepartmentMaintenanceXmlDocument buildDocument(
            WebSession webSession,
            DepartmentMaintenanceProvider provider,
            Department department
    ) {
        DepartmentMaintenanceXmlDocument xmlDocument = new DepartmentMaintenanceXmlDocument(webSession, "departmentMaintenance");
        Element root = xmlDocument.root();

        appendTopPanel(xmlDocument, root, webSession);
        appendDepartments(xmlDocument, root, provider.getDepartments());

        if (department != null) {
            appendDepartment(xmlDocument, root, department);
        }

        return xmlDocument;
    }

    private void appendTopPanel(DepartmentMaintenanceXmlDocument xmlDocument, Element parent, WebSession webSession) {
        Element topPanelElement = xmlDocument.appendElement(parent, "TopPanel");

        if (webSession == null) {
            return;
        }

        try {
            TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
            TopPanel topPanel = topPanelProvider.getTopPanelBySession(PageType.DEPARTMENT_MAIN_PAGE);

            if (topPanel != null && topPanel.getTopPanelElements() != null) {
                for (com.bepa.eis.server.dataprovider.fields.AbstractField field : topPanel.getTopPanelElements().getElements()) {
                    if (field != null && field.getFieldName() != null && !field.getFieldName().isBlank()) {
                        xmlDocument.appendTextElement(topPanelElement, field.getFieldName(), field.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to append top panel", e);
        }
    }

    private void appendDepartments(
            DepartmentMaintenanceXmlDocument xmlDocument,
            Element parent,
            List<Department> departments
    ) {
        Element departmentsElement = xmlDocument.appendElement(parent, "departments");

        for (Department department : departments) {
            appendDepartment(xmlDocument, departmentsElement, department);
        }
    }

    private void appendDepartment(
            DepartmentMaintenanceXmlDocument xmlDocument,
            Element parent,
            Department department
    ) {
        if (department == null) {
            return;
        }

        Element departmentElement = xmlDocument.appendElement(parent, "department");
        xmlDocument.appendTextElement(departmentElement, "DepartmentId", department.getDepartmentId());
        xmlDocument.appendTextElement(departmentElement, "CustomerId", department.getCustomerId());
        xmlDocument.appendTextElement(departmentElement, "DepartmentName", department.getDepartmentName());
        xmlDocument.appendTextElement(departmentElement, "DepartmentDescription", department.getDepartmentDescription());
        xmlDocument.appendTextElement(departmentElement, "Active", department.isActive());
    }
}
