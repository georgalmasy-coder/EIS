package com.bepa.eis.server.api.web.application.views.basis.systemrequirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericImporters;
import com.bepa.eis.server.api.web.application.views.basis.systemrequirement.SystemRequirementExportRow;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.common.enums.entity.EntityType;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SystemRequirementImporters extends GenericImporters {

    private static final int COL_ID = 0;
    private static final int COL_LEVEL = 1;
    private static final int COL_NAME = 2;
    private static final int COL_DESCRIPTION = 3;

    private static final EntityType entityType = EntityType.SYSTEM_REQUIREMENT;

    public SystemRequirementImporters(WebSession webSession, HttpServletRequest request) throws Exception{
        super(webSession, request);
    }

    @Override
    public EntityProvider getProvider() {
        return new SystemRequirementProvider(getWebSession());
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    @Override
    public Object loadFromXml(InputStream inputStream) throws Exception {
        return fromXml(inputStream);
    }

    @Override
    public Object loadFromCsv(InputStream inputStream) throws Exception {
        return fromCsv(inputStream);
    }

    @Override
    public Object loadFromXlsx(InputStream inputStream) throws Exception {
        return fromXlsx(inputStream);
    }

    @Override
    protected List<String> getPreviewFieldNames() {
        return List.of("id", "level", "name", "description");
    }

    @Override
    protected List<String> getRequiredFieldNames() {
        return List.of("id", "name");
    }

    @Override
    protected List<String> getValidPrefixes() {
        return List.of("", entityType.getIdPrefix());
    }

    @Override
    protected String getImportEntitiesButtonText() {
        return "Import System Requirements";
    }

    private List<SystemRequirementExportRow> fromXml(InputStream inputStream) throws Exception {
        var document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(inputStream);

        document.getDocumentElement().normalize();

        List<SystemRequirementExportRow> rows = new ArrayList<>();
        var nodes = document.getElementsByTagName("systemRequirement");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (!(node instanceof Element element)) {
                continue;
            }

            rows.add(new SystemRequirementExportRow(
                    text(element, "ID"),
                    intValue(text(element, "Level")),
                    text(element, "Name"),
                    text(element, "Description"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Boolean.TRUE)
            );
        }

        return rows;
    }

    private List<SystemRequirementExportRow> fromCsv(InputStream inputStream) throws Exception {
        List<SystemRequirementExportRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                List<String> values = parseCsvLine(line);

                if (firstLine) {
                    firstLine = false;

                    if (isHeader(values)) {
                        continue;
                    }
                }

                rows.add(rowFromValues(values));
            }
        }

        return rows;
    }

    private List<SystemRequirementExportRow> fromXlsx(InputStream inputStream) throws Exception {
        List<SystemRequirementExportRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            var sheet = workbook.getSheetAt(0);

            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row sheetRow = sheet.getRow(rowIndex);

                if (sheetRow == null) {
                    continue;
                }

                List<String> values = new ArrayList<>();

                for (int col = 0; col <= COL_DESCRIPTION; col++) {
                    values.add(formatter.formatCellValue(sheetRow.getCell(col)));
                }

                if (rowIndex == 0 && isHeader(values)) {
                    continue;
                }

                rows.add(rowFromValues(values));
            }
        }

        return rows;
    }

    private SystemRequirementExportRow rowFromValues(List<String> values) {
        return new SystemRequirementExportRow(
                valueAt(values, COL_ID),
                intValue(valueAt(values, COL_LEVEL)),
                valueAt(values, COL_NAME),
                valueAt(values, COL_DESCRIPTION),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Boolean.TRUE);
    }

    private boolean isHeader(List<String> values) {
        return "ID".equalsIgnoreCase(valueAt(values, COL_ID))
                && "Level".equalsIgnoreCase(valueAt(values, COL_LEVEL))
                && "Name".equalsIgnoreCase(valueAt(values, COL_NAME));
    }

}
