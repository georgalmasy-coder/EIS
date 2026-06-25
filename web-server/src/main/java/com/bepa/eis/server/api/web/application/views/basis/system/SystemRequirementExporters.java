package com.bepa.eis.server.api.web.application.views.basis.system;

import com.bepa.eis.server.api.generic.GenericExporters;
import org.apache.poi.ss.usermodel.Row;

public final class SystemRequirementExporters extends GenericExporters {

    private static final String[] HEADERS = {
            "ID",
            "Level",
            "Name",
            "Description",
            "VerificationStatus",
            "RationalStatement",
            "RequirementCaptured",
            "Status",
            "BusinessPriority",
            "Owner",
            "ChangedBy",
            "Changed",
            "Active"
    };

    private static final float[] PDF_COL_WIDTH = {
            30f,   // ID
            28f,   // Level
            105f,  // Name
            165f,  // Description
            70f,   // VerificationStatus
            80f,   // RationalStatement
            65f,   // RequirementCaptured
            48f,   // Status
            62f,   // BusinessPriority
            48f,   // Owner
            48f,   // ChangedBy
            55f,   // Changed
            32f    // Active
    };

    public SystemRequirementExporters() {
    }

    @Override
    public String[] getHeaders() {
        return HEADERS;
    }

    @Override
    public String getFileName() {
        return "systemrequirements";
    }

    @Override
    public String getWorksheetName() {
        return "System Requirements";
    }

    @Override
    public String getXmlRootNodeName() {
        return "systemRequirementsExport";
    }

    @Override
    public float[] getPdfColWidth() {
        return PDF_COL_WIDTH;
    }

    @Override
    public String getPdfTitle() {
        return "System Requirements Export";
    }

    /*
    @Override
    public boolean isPdfLandscape() {
        return true;
    }

    @Override
    public int getPdfFontSize() {
        return 8;
    }

    @Override
    public float getPdfMinRowHeight() {
        return 18f;
    }

     */

    @Override
    public void buildCsvRow(StringBuilder csv, Object rowData) {
        SystemRequirementExportRow row = (SystemRequirementExportRow) rowData;

        csv.append(csv(row.id())).append(",");
        csv.append(csv(row.level())).append(",");
        csv.append(csv(row.name())).append(",");
        csv.append(csv(row.description())).append(",");
        csv.append(csv(row.verificationStatus())).append(",");
        csv.append(csv(row.rationalStatement())).append(",");
        csv.append(csv(row.requirementCaptured())).append(",");
        csv.append(csv(row.status())).append(",");
        csv.append(csv(row.businessPriority())).append(",");
        csv.append(csv(row.owner())).append(",");
        csv.append(csv(row.changedBy())).append(",");
        csv.append(csv(row.changed())).append(",");
        csv.append(csv(row.active())).append(NEW_LINE);
    }

    @Override
    public void buildXmlRow(StringBuilder xml, Object rowData) {
        SystemRequirementExportRow row = (SystemRequirementExportRow) rowData;

        xml.append("  <systemRequirement>").append(NEW_LINE);
        xml.append(tag(getHeaders()[0], row.id()));
        xml.append(tag(getHeaders()[1], row.level()));
        xml.append(tag(getHeaders()[2], row.name()));
        xml.append(tag(getHeaders()[3], row.description()));
        xml.append(tag(getHeaders()[4], row.verificationStatus()));
        xml.append(tag(getHeaders()[5], row.rationalStatement()));
        xml.append(tag(getHeaders()[6], row.requirementCaptured()));
        xml.append(tag(getHeaders()[7], row.status()));
        xml.append(tag(getHeaders()[8], row.businessPriority()));
        xml.append(tag(getHeaders()[9], row.owner()));
        xml.append(tag(getHeaders()[10], row.changedBy()));
        xml.append(tag(getHeaders()[11], row.changed()));
        xml.append(tag(getHeaders()[12], row.active()));
        xml.append("  </systemRequirement>").append(NEW_LINE);
    }

    @Override
    public void buildWorksheetRow(Row sheetRow, Object rowData) {
        SystemRequirementExportRow row = (SystemRequirementExportRow) rowData;

        sheetRow.createCell(0).setCellValue(nvl(row.id()));
        sheetRow.createCell(1).setCellValue(nvl(row.level()));
        sheetRow.createCell(2).setCellValue(nvl(row.name()));
        sheetRow.createCell(3).setCellValue(nvl(row.description()));
        sheetRow.createCell(4).setCellValue(nvl(row.verificationStatus()));
        sheetRow.createCell(5).setCellValue(nvl(row.rationalStatement()));
        sheetRow.createCell(6).setCellValue(nvl(row.requirementCaptured()));
        sheetRow.createCell(7).setCellValue(nvl(row.status()));
        sheetRow.createCell(8).setCellValue(nvl(row.businessPriority()));
        sheetRow.createCell(9).setCellValue(nvl(row.owner()));
        sheetRow.createCell(10).setCellValue(nvl(row.changedBy()));
        sheetRow.createCell(11).setCellValue(nvl(row.changed()));
        sheetRow.createCell(12).setCellValue(nvl(row.active()));
    }

    @Override
    public String[] getPdfRowValues(Object rowData) {
        return rowStrings((SystemRequirementExportRow) rowData);
    }

    private static String[] rowStrings(SystemRequirementExportRow row) {
        return new String[] {
                nvl(row.id()),
                nvl(row.level()),
                nvl(row.name()),
                nvl(row.description()),
                nvl(row.verificationStatus()),
                nvl(row.rationalStatement()),
                nvl(row.requirementCaptured()),
                nvl(row.status()),
                nvl(row.businessPriority()),
                nvl(row.owner()),
                nvl(row.changedBy()),
                nvl(row.changed()),
                nvl(row.active())
        };
    }
}