package com.bepa.eis.server.api.web.application.views.project.systembreakdown;

import com.bepa.eis.server.api.generic.GenericExporters;
import org.apache.poi.ss.usermodel.Row;

public final class SystemBreakdownExporters extends GenericExporters {

    private static final String[] HEADERS = {
            "SBS",
            "Level",
            "Name",
            "SystemOwner",
            "Department",
            "TRL",
            "DeadlineNextTRL",
            "DeadlineFinalized",
            "ChangedBy",
            "Changed",
            "Active"
    };

    private static final float[] PDF_COL_WIDTH = {
            30f,   // SBS
            28f,   // Level
            105f,  // Name
            48f,   // System Owner
            48f,   // Department
            150f,   // TRL
            48f,   // Deadline Next TRL
            48f,   // Deadline Finalized
            100f,   // ChangedBy
            55f,   // Changed
            32f    // Active
    };

    public SystemBreakdownExporters() {
    }

    @Override
    public String[] getHeaders() {
        return HEADERS;
    }

    @Override
    public String getFileName() {
        return "systemsbreakdown";
    }

    @Override
    public String getWorksheetName() {
        return "Systems Breakdown";
    }

    @Override
    public String getXmlRootNodeName() {
        return "systemsBreakdownExport";
    }

    @Override
    public float[] getPdfColWidth() {
        return PDF_COL_WIDTH;
    }

    @Override
    public String getPdfTitle() {
        return "System Breakdown Export";
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
        SystemBreakdownExportRow row = (SystemBreakdownExportRow) rowData;

        csv.append(csv(row.id())).append(",");
        csv.append(csv(row.level())).append(",");
        csv.append(csv(row.name())).append(",");

        csv.append(csv(row.name())).append(",");
        csv.append(csv(row.systemOwner())).append(",");
        csv.append(csv(row.systemDepartment())).append(",");
        csv.append(csv(row.trl())).append(",");
        csv.append(csv(row.deadlineNextTRL())).append(",");
        csv.append(csv(row.deadlineFinalized())).append(",");

        csv.append(csv(row.changedBy())).append(",");
        csv.append(csv(row.changed())).append(",");
        csv.append(csv(row.active())).append(NEW_LINE);
    }

    @Override
    public void buildXmlRow(StringBuilder xml, Object rowData) {
        SystemBreakdownExportRow row = (SystemBreakdownExportRow) rowData;

        xml.append("  <systemsBreakdown>").append(NEW_LINE);
        xml.append(tag(getHeaders()[0], row.id()));
        xml.append(tag(getHeaders()[1], row.level()));
        xml.append(tag(getHeaders()[2], row.name()));
        xml.append(tag(getHeaders()[3], row.systemOwner()));
        xml.append(tag(getHeaders()[4], row.systemDepartment()));
        xml.append(tag(getHeaders()[5], row.trl()));
        xml.append(tag(getHeaders()[6], row.deadlineNextTRL()));
        xml.append(tag(getHeaders()[7], row.deadlineFinalized()));
        xml.append(tag(getHeaders()[8], row.changedBy()));
        xml.append(tag(getHeaders()[9], row.changed()));
        xml.append(tag(getHeaders()[10], row.active()));
        xml.append("  </systemsBreakdown>").append(NEW_LINE);
    }

    @Override
    public void buildWorksheetRow(Row sheetRow, Object rowData) {
        SystemBreakdownExportRow row = (SystemBreakdownExportRow) rowData;

        sheetRow.createCell(0).setCellValue(nvl(row.id()));
        sheetRow.createCell(1).setCellValue(nvl(row.level()));
        sheetRow.createCell(2).setCellValue(nvl(row.name()));
        sheetRow.createCell(3).setCellValue(nvl(row.systemOwner()));
        sheetRow.createCell(4).setCellValue(nvl(row.systemDepartment()));
        sheetRow.createCell(5).setCellValue(nvl(row.trl()));
        sheetRow.createCell(6).setCellValue(nvl(row.deadlineNextTRL()));
        sheetRow.createCell(7).setCellValue(nvl(row.deadlineFinalized()));
        sheetRow.createCell(8).setCellValue(nvl(row.changedBy()));
        sheetRow.createCell(9).setCellValue(nvl(row.changed()));
        sheetRow.createCell(10).setCellValue(nvl(row.active()));
    }

    @Override
    public String[] getPdfRowValues(Object rowData) {
        return rowStrings((SystemBreakdownExportRow) rowData);
    }

    private static String[] rowStrings(SystemBreakdownExportRow row) {
        return new String[] {
                nvl(row.id()),
                nvl(row.level()),
                nvl(row.name()),
                nvl(row.systemOwner()),
                nvl(row.systemDepartment()),
                nvl(row.trl()),
                nvl(row.deadlineNextTRL()),
                nvl(row.deadlineFinalized()),
                nvl(row.changedBy()),
                nvl(row.changed()),
                nvl(row.active())
        };
    }
}