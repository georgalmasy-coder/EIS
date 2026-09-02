package dk.eis.tech.timesheet.config;

import dk.eis.tech.timesheet.model.CompanyFooterRecord;

import java.util.List;

public final class CompanyFooterConfig {

    public static final int FOOTER_LINE_Y = 736;
    public static final String FOOTER_LINE_COLOR = "#000000";
    public static final int FOOTER_LINE_WIDTH = 2;
    public static final int DUE_DATE_DAYS = 14;

    public static final String LEFT_COMPANY_NAME = "EIS Technology";
    public static final List<String> LEFT_ADDRESS_LINES = List.of(
            "Haslehøjvej 15",
            "8210 Aarhus V"
    );

    public static final List<String> MIDDLE_INFO_LINES = List.of(
            "Phone: 2171 7344",
            "E-mail: georg.almasy@gmail.com",
            "CVR no.: 29453268"
    );

    public static final List<String> RIGHT_BANK_LINES = List.of(
            "Jyske Bank",
            "Silkeborg afd.",
            "7170-2606477"
    );

    private CompanyFooterConfig() {
    }

    public static CompanyFooterRecord toRecord() {
        return new CompanyFooterRecord(
                LEFT_COMPANY_NAME,
                LEFT_ADDRESS_LINES,
                MIDDLE_INFO_LINES,
                RIGHT_BANK_LINES,
                FOOTER_LINE_Y,
                FOOTER_LINE_COLOR,
                FOOTER_LINE_WIDTH,
                DUE_DATE_DAYS
        );
    }
}
