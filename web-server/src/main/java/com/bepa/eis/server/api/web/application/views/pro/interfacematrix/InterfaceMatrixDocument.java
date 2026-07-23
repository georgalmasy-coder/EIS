package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.fields.integers.AbstractInteger;
import com.bepa.eis.server.dataprovider.fields.strings.AbstractString;
import com.bepa.eis.server.dataprovider.fields.timestamp.AbstractDate;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.List;

public class InterfaceMatrixDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(InterfaceMatrixDocument.class);

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    private InterfaceMatrixMetaData interfaceMatrixMetaData;
    private MatrixLookupMetaData matrixLookupMetaData;
    private List<SystemBreakdownEntity> listOfPhysicalStructuresEntities;
    private MatrixInterfaceCellsData matrixCellsData;

    protected InterfaceMatrixDocument(WebSession webSession) throws Exception {
        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());

        buildInterfaceMatrixDocument();

        Element matrixElement = buildMatrixElement();
        getRoot().appendChild(matrixElement);
        matrixElement.appendChild(interfaceMatrixMetaData.getMetaElement(getDoc()));
        matrixElement.appendChild(matrixLookupMetaData.getTrlElement(getDoc()));
        matrixElement.appendChild(matrixLookupMetaData.getIrlElement(getDoc()));
        matrixElement.appendChild(matrixLookupMetaData.getClassificationElement(getDoc()));
        matrixElement.appendChild(matrixLookupMetaData.getUserElement(getDoc()));
        matrixElement.appendChild(matrixLookupMetaData.getDepartmentElement(getDoc()));
        matrixElement.appendChild(getPhysicalStructureElements());
        matrixElement.appendChild(matrixCellsData.getCellElement(getDoc()));
    }

    private void buildInterfaceMatrixDocument() throws Exception {
        interfaceMatrixMetaData = createInterfaceMatrixMetaData();
        matrixLookupMetaData = createMatrixLookupData();
        listOfPhysicalStructuresEntities = getPhysicalStructures();
        matrixCellsData = getInterfaceMatrixCellsData(getWebSession());
    }

    private InterfaceMatrixMetaData createInterfaceMatrixMetaData() {
        InterfaceMatrixMetaData matrixMetaData = new InterfaceMatrixMetaData();
        matrixMetaData.setTitle("Interface Management");
        matrixMetaData.setColumnGroupLabel("To Physical Structure");
        matrixMetaData.setRowGroupLabel("From Physical Structure");
        matrixMetaData.setGeneratedAt();
        return matrixMetaData;
    }

    private MatrixLookupMetaData createMatrixLookupData() {
        return new MatrixLookupMetaData(getWebSession());
    }

    private List<SystemBreakdownEntity> getPhysicalStructures() {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
        return systemBreakdownProvider.getAllSystemBreakdown(false);
    }

    private MatrixInterfaceCellsData getInterfaceMatrixCellsData(WebSession webSession) throws SQLException {
        return new MatrixInterfaceCellsData(webSession);
    }

    private Element getPhysicalStructureElements() {
        Element physicalStructuresElement = getDoc().createElement("physicalStructures");

        for (SystemBreakdownEntity entity : listOfPhysicalStructuresEntities) {
            Element entityElement = getDoc().createElement("physicalStructure");
            addEntityElement(entityElement, "entityId", entity.getEntityId());
            addEntityElement(entityElement, "id", entity.getSbsCode());
            addEntityElement(entityElement, "name", entity.getSystemName());
            addEntityElement(entityElement, "systemOwner", entity.getSystemOwner());
            addEntityElement(entityElement, "trlId", entity.getTrl());
            addEntityElement(entityElement, "departmentId", entity.getSystemDepartment());
            addEntityElement(entityElement, "deadlineNextTrl", entity.getDeadlineNextTrlField());
            physicalStructuresElement.appendChild(entityElement);
        }

        return physicalStructuresElement;
    }

    private void addEntityElement(Element parentElement, String elementName, AbstractField field) {
        if (field == null) {
            Element element = getDoc().createElement(elementName);
            parentElement.appendChild(element);
            return;
        }

        String value = null;
        switch (field) {
            case AbstractInteger integer -> value = integer.getValue() != null ? integer.getValue().toString() : null;
            case AbstractString string -> value = string.getValue() != null ? string.getValue() : null;
            case AbstractDate date -> value = date.getValue() != null ? date.getValue().toString() : null;
            default -> {
            }
        }

        Element element = getDoc().createElement(elementName);
        element.setTextContent(value);
        parentElement.appendChild(element);
    }

    private Element buildMatrixElement() {
        Element matrixElement = getDoc().createElement("interfaceMatrix");
        matrixElement.setAttribute("entityCount", String.valueOf(listOfPhysicalStructuresEntities.size()));
        matrixElement.setAttribute("version", "1");
        return matrixElement;
    }
}
