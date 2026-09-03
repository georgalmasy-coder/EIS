package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.fields.integers.AbstractInteger;
import com.bepa.eis.server.dataprovider.fields.strings.AbstractString;
import com.bepa.eis.server.dataprovider.fields.timestamp.AbstractDate;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

abstract class InterfaceMatrixDocument extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(InterfaceMatrixDocument.class);

    public static final String ENTITIES_ELEMENT_NAME = "entities";
    public static final String ENTITY_ELEMENT_NAME = "entity";


    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    private InterfaceMatrixMetaData interfaceMatrixMetaData;
    private MatrixLookupMetaData matrixLookupMetaData;
    private MatrixInterfaceCellsData matrixCellsData;

    abstract public PageType getPageType();
    abstract public EntityType getEntityType();
    abstract public String getTitle();
    abstract public String getColumnGroupLabel();
    abstract public String getRowGroupLabel();
    abstract public Element getEntityElements();
    abstract public String getEntityCount();

    protected InterfaceMatrixDocument(WebSession webSession) throws Exception {
        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(getPageType());
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
        matrixElement.appendChild(getEntityElements());

        matrixElement.appendChild(matrixCellsData.getCellElement(getDoc()));
    }

    private void buildInterfaceMatrixDocument() throws Exception {
        interfaceMatrixMetaData = createInterfaceMatrixMetaData();
        matrixLookupMetaData = createMatrixLookupData();
        matrixCellsData = getInterfaceMatrixCellsData(getWebSession());
    }

    public  <T extends AbstractEntity> void sortBySortKey(List<T> entities) {
        entities.sort(
                Comparator.comparing(
                        AbstractEntity::getSortKey,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                )
        );
    }

    private InterfaceMatrixMetaData createInterfaceMatrixMetaData() {
        InterfaceMatrixMetaData matrixMetaData = new InterfaceMatrixMetaData();
        matrixMetaData.setTitle(getTitle());
        matrixMetaData.setColumnGroupLabel(getColumnGroupLabel());
        matrixMetaData.setRowGroupLabel(getRowGroupLabel());
        matrixMetaData.setGeneratedAt();
        return matrixMetaData;
    }

    private MatrixLookupMetaData createMatrixLookupData() {
        return new MatrixLookupMetaData(getWebSession());
    }

    private List<SystemBreakdownEntity> getPhysicalStructures() {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
        List<SystemBreakdownEntity> listOfSystemBreakdownEntities = systemBreakdownProvider.getAllSystemBreakdown(false);
        sortBySortKey(listOfSystemBreakdownEntities);
        return listOfSystemBreakdownEntities;
    }

    private MatrixInterfaceCellsData getInterfaceMatrixCellsData(WebSession webSession) throws SQLException {
        return new MatrixInterfaceCellsData(webSession, getEntityType());
    }

    protected void addEntityElement(Element parentElement, String elementName, AbstractField field) {
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
        matrixElement.setAttribute("entityCount", getEntityCount());
        matrixElement.setAttribute("version", "1");
        return matrixElement;
    }
}
