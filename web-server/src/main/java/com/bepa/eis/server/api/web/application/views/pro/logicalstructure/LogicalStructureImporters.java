package com.bepa.eis.server.api.web.application.views.pro.logicalstructure;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.generic.GenericImporters;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.dataprovider.entities.LogicalStructureProvider;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import com.bepa.eis.server.dataprovider.fields.lookups.logical.*;
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

public final class LogicalStructureImporters extends GenericImporters {

    private static final int COL_ID = 0;
    private static final int COL_LEVEL = 1;
    private static final int COL_NAME = 2;
    private static final int COL_DESCRIPTION = 3;
    private static final int COL_OWNER_ID = 4;
    private static final int COL_VERIFICATION_STATUS_ID = 5;
    private static final int COL_CRITICALITY_ID = 6;
    private static final int COL_ELEMENT_CATEGORY_ID = 7;
    private static final int COL_LOGICAL_LEVEL_ID = 8;
    private static final int COL_TYPE_ID = 9;
    private static final int COL_RESPONSIBLE_DOMAIN_ID = 10;
    private static final int COL_LIFECYCLE_STATUS_ID = 11;
    private static final int COL_MATURITY_ID = 12;
    private static final int COL_ALLOCATION_STATUS_ID = 13;
    private static final int COL_REALIZATION_STATUS_ID = 14;
    private static final int COL_SAFETY_CLASSIFICATION_ID = 15;
    private static final int COL_SECURITY_CLASSIFICATION_ID = 16;
    private static final int COL_REDUNDANCY_TYPE_ID = 17;
    private static final int COL_CONFIGURATION_VARIANT_ID = 18;
    private static final int COL_APPLICABILITY_ID = 19;

    private static final EntityType entityType = EntityType.LOGICAL_STRUCTURE;

    public LogicalStructureImporters(WebSession webSession, HttpServletRequest request) throws Exception{
        super(webSession, request);
    }

    @Override
    public EntityProvider getProvider() {
        return new LogicalStructureProvider(getWebSession());
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
        return "Import Logical Structures";
    }

    private List<LogicalStructureExportRow> fromXml(InputStream inputStream) throws Exception {
        var document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(inputStream);

        document.getDocumentElement().normalize();

        List<LogicalStructureExportRow> rows = new ArrayList<>();
        var nodes = document.getElementsByTagName("logicalStructures");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (!(node instanceof Element element)) {
                continue;
            }

            rows.add(new LogicalStructureExportRow(
                    text(element, "ID"),
                    intValue(text(element, "Level")),
                    text(element, "Name"),
                    text(element, "Description"),
                    lookup(new LogicalOwner(getWebSession()), intValue(text(element, "OwnerId"))),
                    lookup(new LogicalVerificationStatus(getWebSession()), intValue(text(element, "VerificationStatusId"))),
                    lookup(new LogicalCriticality(getWebSession()), intValue(text(element, "CriticalityId"))),
                    lookup(new LogicalElementCategory(getWebSession()), intValue(text(element, "ElementCategoryId"))),
                    lookup(new LogicalLevel(getWebSession()), intValue(text(element, "LogicalLevelId"))),
                    lookup(new LogicalType(getWebSession()), intValue(text(element, "TypeId"))),
                    lookup(new LogicalResponsibleDomain(getWebSession()), intValue(text(element, "ResponsibleDomainId"))),
                    lookup(new LogicalLifecycleStatus(getWebSession()), intValue(text(element, "LifecycleStatusId"))),
                    lookup(new LogicalMaturity(getWebSession()), intValue(text(element, "MaturityId"))),
                    lookup(new LogicalAllocationStatus(getWebSession()), intValue(text(element, "AllocationStatusId"))),
                    lookup(new LogicalRealizationStatus(getWebSession()), intValue(text(element, "RealizationStatusId"))),
                    lookup(new LogicalSafetyClassification(getWebSession()), intValue(text(element, "SafetyClassificationId"))),
                    lookup(new LogicalSecurityClassification(getWebSession()), intValue(text(element, "SecurityClassificationId"))),
                    lookup(new LogicalRedundancyType(getWebSession()), intValue(text(element, "RedundancyTypeId"))),
                    lookup(new LogicalConfigurationVariant(getWebSession()), intValue(text(element, "ConfigurationVariantId"))),
                    lookup(new LogicalApplicability(getWebSession()), intValue(text(element, "ApplicabilityId"))),
                    null,
                    null,
                    Boolean.TRUE) // Active
            );
        }

        return rows;
    }

    private List<LogicalStructureExportRow> fromCsv(InputStream inputStream) throws Exception {
        List<LogicalStructureExportRow> rows = new ArrayList<>();

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

    private List<LogicalStructureExportRow> fromXlsx(InputStream inputStream) throws Exception {
        List<LogicalStructureExportRow> rows = new ArrayList<>();
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

    private LogicalStructureExportRow rowFromValues(List<String> values) {
        return new LogicalStructureExportRow(
                valueAt(values, COL_ID),
                intValue(valueAt(values, COL_LEVEL)),
                valueAt(values, COL_NAME),
                valueAt(values, COL_DESCRIPTION),
                lookup(new LogicalOwner(getWebSession()), intValue(valueAt(values, COL_OWNER_ID))),
                lookup(new LogicalVerificationStatus(getWebSession()), intValue(valueAt(values, COL_VERIFICATION_STATUS_ID))),
                lookup(new LogicalCriticality(getWebSession()), intValue(valueAt(values, COL_CRITICALITY_ID))),
                lookup(new LogicalElementCategory(getWebSession()), intValue(valueAt(values, COL_ELEMENT_CATEGORY_ID))),
                lookup(new LogicalLevel(getWebSession()), intValue(valueAt(values, COL_LOGICAL_LEVEL_ID))),
                lookup(new LogicalType(getWebSession()), intValue(valueAt(values, COL_TYPE_ID))),
                lookup(new LogicalResponsibleDomain(getWebSession()), intValue(valueAt(values, COL_RESPONSIBLE_DOMAIN_ID))),
                lookup(new LogicalLifecycleStatus(getWebSession()), intValue(valueAt(values, COL_LIFECYCLE_STATUS_ID))),
                lookup(new LogicalMaturity(getWebSession()), intValue(valueAt(values, COL_MATURITY_ID))),
                lookup(new LogicalAllocationStatus(getWebSession()), intValue(valueAt(values, COL_ALLOCATION_STATUS_ID))),
                lookup(new LogicalRealizationStatus(getWebSession()), intValue(valueAt(values, COL_REALIZATION_STATUS_ID))),
                lookup(new LogicalSafetyClassification(getWebSession()), intValue(valueAt(values, COL_SAFETY_CLASSIFICATION_ID))),
                lookup(new LogicalSecurityClassification(getWebSession()), intValue(valueAt(values, COL_SECURITY_CLASSIFICATION_ID))),
                lookup(new LogicalRedundancyType(getWebSession()), intValue(valueAt(values, COL_REDUNDANCY_TYPE_ID))),
                lookup(new LogicalConfigurationVariant(getWebSession()), intValue(valueAt(values, COL_CONFIGURATION_VARIANT_ID))),
                lookup(new LogicalApplicability(getWebSession()), intValue(valueAt(values, COL_APPLICABILITY_ID))),
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
