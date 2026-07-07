package dk.eis.tech.timesheet.model;

import java.util.List;

public record CompanyFooterRecord(
        String leftCompanyName,
        List<String> leftAddressLines,
        List<String> middleInfoLines,
        List<String> rightBankLines,
        int footerLineY,
        String footerLineColor,
        int footerLineWidth,
        int dueDateDays
) {
}
