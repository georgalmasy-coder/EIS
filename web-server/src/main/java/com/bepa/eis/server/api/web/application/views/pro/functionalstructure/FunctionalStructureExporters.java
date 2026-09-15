package com.bepa.eis.server.api.web.application.views.pro.functionalstructure;

import com.bepa.eis.server.api.generic.GenericExporters;
import com.bepa.eis.server.dataprovider.fields.lookups.functional.*;
import org.apache.poi.ss.usermodel.Row;

public final class FunctionalStructureExporters extends GenericExporters {

    private static final String[] HEADERS = {
            "ID",
            "Level",
            "Name",
            "Description",
            new FunctionOwner().getFieldHeaderName(),
            new FunctionStatus().getFieldHeaderName(),
            new FunctionBehaviorType().getFieldHeaderName(),
            new FunctionCategory().getFieldHeaderName(),
            new FunctionCriticality().getFieldHeaderName(),
            new FunctionVerificationStatus().getFieldHeaderName(),
            new FunctionLevel().getFieldHeaderName(),
            new FunctionOperatingMode().getFieldHeaderName(),
            new FunctionResponsibleDomain().getFieldHeaderName(),
            new FunctionConfigurationVariant().getFieldHeaderName(),
            new FunctionApplicability().getFieldHeaderName(),
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
            "Status",
            "BehaviorType",
            "Category",
            "Criticality",
            "VerificationStatus",
            "FunctionLevel",
            "OperatingMode",
            "ResponsibleDomain",
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
            35f,   // StatusId
            35f,   // BehaviorTypeId
            35f,   // CategoryId
            35f,   // CriticalityId
            35f,   // VerificationStatusId
            35f,   // FunctionLevelId
            35f,   // OperatingModeId
            35f,   // ResponsibleDomainId
            35f,   // ConfigurationVariantId
            35f,   // ApplicabilityId
            48f,   // ChangedBy
            55f,   // Changed
            32f    // Active
    };

    public FunctionalStructureExporters() {
    }

    @Override
    public String[] getHeaders() {
        return HEADERS;
    }

    @Override
    public String getFileName() {
        return "FunctionalArchitecture";
    }

    @Override
    public String getWorksheetName() {
        return "Functional Architecture";
    }

    @Override
    public String getXmlRootNodeName() {
        return "functionalArchitectureExport";
    }

    @Override
    public float[] getPdfColWidth() {
        return PDF_COL_WIDTH;
    }

    @Override
    public String getPdfTitle() {
        return "Functional Architecture Export";
    }

    @Override
    public void buildCsvRow(StringBuilder csv, Object rowData) {
        String[] values = rowStrings((FunctionalStructureExportRow) rowData);

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
        FunctionalStructureExportRow row = (FunctionalStructureExportRow) rowData;

        xml.append("  <functionStructures>").append(NEW_LINE);
        String[] values = rowStrings(row);
        for (int i = 0; i < values.length; i++) {
            xml.append(tag(XML_TAGS[i], values[i]));
        }
        xml.append("  </functionStructures>").append(NEW_LINE);
    }

    @Override
    public void buildWorksheetRow(Row sheetRow, Object rowData) {
        FunctionalStructureExportRow row = (FunctionalStructureExportRow) rowData;

        String[] values = rowStrings(row);
        for (int i = 0; i < values.length; i++) {
            sheetRow.createCell(i).setCellValue(values[i]);
        }
    }

    @Override
    public String[] getPdfRowValues(Object rowData) {
        return rowStrings((FunctionalStructureExportRow) rowData);
    }

    private static String[] rowStrings(FunctionalStructureExportRow row) {
        return new String[] {
                nvl(row.id()),
                nvl(row.level()),
                nvl(row.name()),
                nvl(row.description()),
                nvl(row.owner()),
                nvl(row.status()),
                nvl(row.behaviorType()),
                nvl(row.category()),
                nvl(row.criticality()),
                nvl(row.verificationStatus()),
                nvl(row.functionLevel()),
                nvl(row.operatingMode()),
                nvl(row.responsibleDomain()),
                nvl(row.configurationVariant()),
                nvl(row.applicability()),
                nvl(row.changedBy()),
                nvl(row.changed()),
                nvl(row.active())
        };
    }
}

