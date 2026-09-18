package dk.eis.tech.timesheet.model;

import java.util.List;

public record InvoicePostingPreview(
        String invoiceNumber,
        InvoiceApprovalRecord approval,
        List<AccountingLineRecord> lines
) {
}
