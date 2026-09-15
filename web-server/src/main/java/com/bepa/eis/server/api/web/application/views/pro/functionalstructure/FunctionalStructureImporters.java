package com.bepa.eis.server.api.web.application.views.pro.functionalstructure;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.generic.GenericImporters;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.dataprovider.entities.FunctionalStructureProvider;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import com.bepa.eis.server.dataprovider.fields.lookups.functional.*;
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

public final class FunctionalStructureImporters extends GenericImporters {

    private static final int COL_ID = 0;
    private static final int COL_LEVEL = 1;
    private static final int COL_NAME = 2;
    private static final int COL_DESCRIPTION = 3;
    private static final int COL_OWNER_ID = 4;
    private static final int COL_STATUS_ID = 5;
    private static final int COL_BEHAVIOR_TYPE_ID = 6;
    private static final int COL_CATEGORY_ID = 7;
    private static final int COL_CRITICALITY_ID = 8;
    private static final int COL_VERIFICATION_STATUS_ID = 9;
    private static final int COL_FUNCTION_LEVEL_ID = 10;
    private static final int COL_OPERATING_MODE_ID = 11;
    private static final int COL_RESPONSIBLE_DOMAIN_ID = 12;
    private static final int COL_CONFIGURATION_VARIANT_ID = 13;
    private static final int COL_APPLICABILITY_ID = 14;

    private static final EntityType entityType = EntityType.FUNCTIONAL_STRUCTURE;

    public FunctionalStructureImporters(WebSession webSession, HttpServletRequest request) throws Exception{
        super(webSession, request);
    }

    @Override
    public EntityProvider getProvider() {
        return new FunctionalStructureProvider(getWebSession());
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
        return "Import Functional Structures";
    }

    private List<FunctionalStructureExportRow> fromXml(InputStream inputStream) throws Exception {
        var document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(inputStream);

        document.getDocumentElement().normalize();

        List<FunctionalStructureExportRow> rows = new ArrayList<>();
        var nodes = document.getElementsByTagName("functionStructures");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (!(node instanceof Element element)) {
                continue;
            }

            rows.add(new FunctionalStructureExportRow(
                    text(element, "ID"),
                    intValue(text(element, "Level")),
                    text(element, "Name"),
                    text(element, "Description"),
                    lookup(new FunctionOwner(getWebSession()), intValue(text(element, "OwnerId"))),
                    lookup(new FunctionStatus(getWebSession()), intValue(text(element, "StatusId"))),
                    lookup(new FunctionBehaviorType(getWebSession()), intValue(text(element, "BehaviorTypeId"))),
                    lookup(new FunctionCategory(getWebSession()), intValue(text(element, "CategoryId"))),
                    lookup(new FunctionCriticality(getWebSession()), intValue(text(element, "CriticalityId"))),
                    lookup(new FunctionVerificationStatus(getWebSession()), intValue(text(element, "VerificationStatusId"))),
                    lookup(new FunctionLevel(getWebSession()), intValue(text(element, "FunctionLevelId"))),
                    lookup(new FunctionOperatingMode(getWebSession()), intValue(text(element, "OperatingModeId"))),
                    lookup(new FunctionResponsibleDomain(getWebSession()), intValue(text(element, "ResponsibleDomainId"))),
                    lookup(new FunctionConfigurationVariant(getWebSession()), intValue(text(element, "ConfigurationVariantId"))),
                    lookup(new FunctionApplicability(getWebSession()), intValue(text(element, "ApplicabilityId"))),
                    null,
                    null,
                    Boolean.TRUE) // Active
            );
        }

        return rows;
    }

    private List<FunctionalStructureExportRow> fromCsv(InputStream inputStream) throws Exception {
        List<FunctionalStructureExportRow> rows = new ArrayList<>();

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

    private List<FunctionalStructureExportRow> fromXlsx(InputStream inputStream) throws Exception {
        List<FunctionalStructureExportRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            var sheet = workbook.getSheetAt(0);

            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row sheetRow = sheet.getRow(rowIndex);

                if (sheetRow == null) {
                    continue;
                }

                List<String> values = new ArrayList<>();

                for (int col = 0; col <= COL_APPLICABILITY_ID; col++) {
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

    private FunctionalStructureExportRow rowFromValues(List<String> values) {
        return new FunctionalStructureExportRow(
                valueAt(values, COL_ID),
                intValue(valueAt(values, COL_LEVEL)),
                valueAt(values, COL_NAME),
                valueAt(values, COL_DESCRIPTION),
                lookup(new FunctionOwner(getWebSession()), intValue(valueAt(values, COL_OWNER_ID))),
                lookup(new FunctionStatus(getWebSession()), intValue(valueAt(values, COL_STATUS_ID))),
                lookup(new FunctionBehaviorType(getWebSession()), intValue(valueAt(values, COL_BEHAVIOR_TYPE_ID))),
                lookup(new FunctionCategory(getWebSession()), intValue(valueAt(values, COL_CATEGORY_ID))),
                lookup(new FunctionCriticality(getWebSession()), intValue(valueAt(values, COL_CRITICALITY_ID))),
                lookup(new FunctionVerificationStatus(getWebSession()), intValue(valueAt(values, COL_VERIFICATION_STATUS_ID))),
                lookup(new FunctionLevel(getWebSession()), intValue(valueAt(values, COL_FUNCTION_LEVEL_ID))),
                lookup(new FunctionOperatingMode(getWebSession()), intValue(valueAt(values, COL_OPERATING_MODE_ID))),
                lookup(new FunctionResponsibleDomain(getWebSession()), intValue(valueAt(values, COL_RESPONSIBLE_DOMAIN_ID))),
                lookup(new FunctionConfigurationVariant(getWebSession()), intValue(valueAt(values, COL_CONFIGURATION_VARIANT_ID))),
                lookup(new FunctionApplicability(getWebSession()), intValue(valueAt(values, COL_APPLICABILITY_ID))),
                null,
                null,
                Boolean.TRUE
        );
    }
    private boolean isHeader(List<String> values) {
        return "ID".equalsIgnoreCase(valueAt(values, COL_ID))
                && "Level".equalsIgnoreCase(valueAt(values, COL_LEVEL))
                && "Name".equalsIgnoreCase(valueAt(values, COL_NAME));
    }

    private static AbstractLookup lookup(AbstractLookup lookup, Integer value) {
        lookup.setValue(value);
        return lookup;
    }

}

