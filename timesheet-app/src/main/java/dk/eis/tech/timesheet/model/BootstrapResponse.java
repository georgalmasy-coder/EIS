package dk.eis.tech.timesheet.model;

import java.util.List;

public record BootstrapResponse(
        List<CustomerRecord> customers,
        Long selectedCustomerId,
        CompanyFooterRecord companyFooter
) {
}
