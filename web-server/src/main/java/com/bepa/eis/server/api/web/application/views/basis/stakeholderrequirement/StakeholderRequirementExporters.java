package com.bepa.eis.server.api.web.application.views.basis.stakeholderrequirement;

import com.bepa.eis.server.api.generic.GenericExporters;
import org.apache.poi.ss.usermodel.Row;

public final class StakeholderRequirementExporters extends GenericExporters {

    private static final String[] HEADERS = {
            "ID",
            "Level",
            "Name",
            "Description",
            "ChangedBy",
            "Changed",
            "Active"
    };

    private static final float[] PDF_COL_WIDTH = {
            30f,   // ID
            28f,   // Level
            105f,  // Name
            165f,  // Description
            48f,   // ChangedBy
            55f,   // Changed
            32f    // Active
    };

    public StakeholderRequirementExporters() {
    }

    @Override
    public String[] getHeaders() {
        return HEADERS;
    }

    @Override
    public String getFileName() {
        return "stakeholderrequirements";
    }

    @Override
    public String getWorksheetName() {
        return "Stakeholder Requirements";
    }

    @Override
    public String getXmlRootNodeName() {
        return "stakeholderRequirementsExport";
    }

    @Override
    public float[] getPdfColWidth() {
        return PDF_COL_WIDTH;
    }

    @Override
    public String getPdfTitle() {
        return "Stakeholder Requirements Export";
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
        StakeholderRequirementExportRow row = (StakeholderRequirementExportRow) rowData;

        csv.append(csv(row.id())).append(",");
        csv.append(csv(row.level())).append(",");
        csv.append(csv(row.name())).append(",");
        csv.append(csv(row.description())).append(",");
        csv.append(csv(row.changedBy())).append(",");
        csv.append(csv(row.changed())).append(",");
        csv.append(csv(row.active())).append(NEW_LINE);
    }

    @Override
    public void buildXmlRow(StringBuilder xml, Object rowData) {
        StakeholderRequirementExportRow row = (StakeholderRequirementExportRow) rowData;

        xml.append("  <stakeholderRequirement>").append(NEW_LINE);
        xml.append(tag(getHeaders()[0], row.id()));
        xml.append(tag(getHeaders()[1], row.level()));
        xml.append(tag(getHeaders()[2], row.name()));
        xml.append(tag(getHeaders()[3], row.description()));
        xml.append(tag(getHeaders()[4], row.changedBy()));
        xml.append(tag(getHeaders()[5], row.changed()));
        xml.append(tag(getHeaders()[6], row.active()));
        xml.append("  </stakeholderRequirement>").append(NEW_LINE);
    }

    @Override
    public void buildWorksheetRow(Row sheetRow, Object rowData) {
        StakeholderRequirementExportRow row = (StakeholderRequirementExportRow) rowData;

        sheetRow.createCell(0).setCellValue(nvl(row.id()));
        sheetRow.createCell(1).setCellValue(nvl(row.level()));
        sheetRow.createCell(2).setCellValue(nvl(row.name()));
        sheetRow.createCell(3).setCellValue(nvl(row.description()));
        sheetRow.createCell(4).setCellValue(nvl(row.changedBy()));
        sheetRow.createCell(5).setCellValue(nvl(row.changed()));
        sheetRow.createCell(6).setCellValue(nvl(row.active()));
    }

    @Override
    public String[] getPdfRowValues(Object rowData) {
        return rowStrings((StakeholderRequirementExportRow) rowData);
    }

    private static String[] rowStrings(StakeholderRequirementExportRow row) {
        return new String[] {
                nvl(row.id()),
                nvl(row.level()),
                nvl(row.name()),
                nvl(row.description()),
                nvl(row.changedBy()),
                nvl(row.changed()),
                nvl(row.active())
        };
    }
}