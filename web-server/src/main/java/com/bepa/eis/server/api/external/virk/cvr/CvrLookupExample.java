package com.bepa.eis.server.api.external.virk.cvr;

import java.util.Optional;

public class CvrLookupExample {

    public static void main(String[] args) {
        CvrLookupService service = new CvrapiDkLookupService();

        Optional<CvrCompanyDto> company = service.findCompanyByCvrNumber("12345678");

        company.ifPresent(c -> {
            System.out.println(c.getName());
            System.out.println(c.getAddress());
            System.out.println(c.getPostalCode() + " " + c.getCity());
        });
    }
}