package com.bepa.eis.server.api.web.application.views.pro.logicalstructure;

import com.bepa.eis.server.api.generic.GenericExporters;
import com.bepa.eis.server.dataprovider.fields.lookups.logical.*;
import org.apache.poi.ss.usermodel.Row;

public final class LogicalStructureExporters extends GenericExporters {

    private static final String[] HEADERS = {
            "ID",
            "Level",
            "Name",
            "Description",
            new LogicalOwner().getFieldHeaderName(),
            new LogicalVerificationStatus().getFieldHeaderName(),
            new LogicalCriticality().getFieldHeaderName(),
            new LogicalElementCategory().getFieldHeaderName(),
            new LogicalLevel().getFieldHeaderName(),
            new LogicalType().getFieldHeaderName(),
            new LogicalResponsibleDomain().getFieldHeaderName(),
            new LogicalLifecycleStatus().getFieldHeaderName(),
            new LogicalMaturity().getFieldHeaderName(),
            new LogicalAllocationStatus().getFieldHeaderName(),
            new LogicalRealizationStatus().getFieldHeaderName(),
            new LogicalSafetyClassification().getFieldHeaderName(),
            new LogicalSecurityClassification().getFieldHeaderName(),
            new LogicalRedundancyType().getFieldHeaderName(),
            new LogicalConfigurationVariant().getFieldHeaderName(),
            new LogicalApplicability().getFieldHeaderName(),
            "ChangedBy",
            "Changed",
            "Active"
    };

    private static final String[] XML_TAGS = {
            "ID",
            "Level",
            "Name",
            "Description",
            "Owner",
            "VerificationStatus",
            "Criticality",
            "ElementCategory",
            "LogicalLevel",
            "Type",
            "ResponsibleDomain",
            "LifecycleStatus",
            "Maturity",
            "AllocationStatus",
            "RealizationStatus",
            "SafetyClassification",
            "SecurityClassification",
            "RedundancyType",
            "ConfigurationVariant",
            "Applicability",
            "ChangedBy",
            "Changed",
            "Active"
    };

    private static final float[] PDF_COL_WIDTH = {
            30f,   // ID
            28f,   // Level
            65f,   // Name
            90f,   // Description
            35f,   // OwnerId
            35f,   // VerificationStatusId
            35f,   // CriticalityId
            35f,   // ElementCategoryId
            35f,   // LogicalLevelId
            35f,   // TypeId
            35f,   // ResponsibleDomainId
            35f,   // LifecycleStatusId
            35f,   // MaturityId
            35f,   // AllocationStatusId
            35f,   // RealizationStatusId
            35f,   // SafetyClassificationId
            35f,   // SecurityClassificationId
            35f,   // RedundancyTypeId
            35f,   // ConfigurationVariantId
            35f,   // ApplicabilityId
            48f,   // ChangedBy
            55f,   // Changed
            32f    // Active
    };

    public LogicalStructureExporters() {
    }

    @Override
    public String[] getHeaders() {
        return HEADERS;
    }

    @Override
    public String getFileName() {
        return "LogicalArchitecture";
    }

    @Override
    public String getWorksheetName() {
        return "Logical Architecture";
    }

    @Override
    public String getXmlRootNodeName() {
        return "logicalArchitectureExport";
    }

    @Override
    public float[] getPdfColWidth() {
        return PDF_COL_WIDTH;
    }

    @Override
    public String getPdfTitle() {
        return "Logical Architecture Export";
    }

    @Override
    public void buildCsvRow(StringBuilder csv, Object rowData) {
        String[] values = rowStrings((LogicalStructureExportRow) rowData);

        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(",");
            }
            csv.append(csv(values[i]));
        }
        csv.append(NEW_LINE);
    }

    @Override
    public void buildXmlRow(StringBuilder xml, Object rowData) {
        LogicalStructureExportRow row = (LogicalStructureExportRow) rowData;

        xml.append("  <logicalStructures>").append(NEW_LINE);
        String[] values = rowStrings(row);
        for (int i = 0; i < values.length; i++) {
            xml.append(tag(XML_TAGS[i], values[i]));
        }
        xml.append("  </logicalStructures>").append(NEW_LINE);
    }

    @Override
    public void buildWorksheetRow(Row sheetRow, Object rowData) {
        LogicalStructureExportRow row = (LogicalStructureExportRow) rowData;

        String[] values = rowStrings(row);
        for (int i = 0; i < values.length; i++) {
            sheetRow.createCell(i).setCellValue(values[i]);
        }
    }

    @Override
    public String[] getPdfRowValues(Object rowData) {
        return rowStrings((LogicalStructureExportRow) rowData);
    }

    private static String[] rowStrings(LogicalStructureExportRow row) {
        return new String[] {
                nvl(row.id()),
                nvl(row.level()),
                nvl(row.name()),
                nvl(row.description()),
                nvl(row.owner()),
                nvl(row.verificationStatus()),
                nvl(row.criticality()),
                nvl(row.elementCategory()),
                nvl(row.logicalLevel()),
                nvl(row.type()),
                nvl(row.responsibleDomain()),
                nvl(row.lifecycleStatus()),
                nvl(row.maturity()),
                nvl(row.allocationStatus()),
                nvl(row.realizationStatus()),
                nvl(row.safetyClassification()),
                nvl(row.securityClassification()),
                nvl(row.redundancyType()),
                nvl(row.configurationVariant()),
                nvl(row.applicability()),
                nvl(row.changedBy()),
                nvl(row.changed()),
                nvl(row.active())
        };
    }
}
