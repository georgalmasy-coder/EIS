package com.bepa.eis.server.api.external.virk.cvr;

/**
 * Intern DTO til resten af backend'en.
 * Denne DTO skjuler detaljer om den eksterne cvrapi.dk response.
 */
public class CvrCompanyDto {

    private final String cvrNumber;
    private final String name;
    private final String address;
    private final String postalCode;
    private final String city;
    private final String country;
    private final String phone;
    private final String email;
    private final String website;
    private final String startDate;
    private final String endDate;
    private final String industryCode;
    private final String industryDescription;
    private final String companyCode;
    private final String companyDescription;
    private final String employees;

    public CvrCompanyDto(
            String cvrNumber,
            String name,
            String address,
            String postalCode,
            String city,
            String country,
            String phone,
            String email,
            String website,
            String startDate,
            String endDate,
            String industryCode,
            String industryDescription,
            String companyCode,
            String companyDescription,
            String employees
    ) {
        this.cvrNumber = cvrNumber;
        this.name = name;
        this.address = address;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.startDate = startDate;
        this.endDate = endDate;
        this.industryCode = industryCode;
        this.industryDescription = industryDescription;
        this.companyCode = companyCode;
        this.companyDescription = companyDescription;
        this.employees = employees;
    }

    public String getCvrNumber() {
        return cvrNumber;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getWebsite() {
        return website;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public String getIndustryDescription() {
        return industryDescription;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public String getEmployees() {
        return employees;
    }

    public static CvrCompanyDto fromApiResponse(CvrApiResponseDto response) {
        if (response == null) {
            return null;
        }

        return new CvrCompanyDto(
                response.getVat(),
                response.getName(),
                response.getAddress(),
                response.getZipcode(),
                response.getCity(),
                response.getCountry(),
                response.getPhone(),
                response.getEmail(),
                response.getWebsite(),
                response.getStartDate(),
                response.getEndDate(),
                response.getIndustryCode(),
                response.getIndustryDescription(),
                response.getCompanyCode(),
                response.getCompanyDescription(),
                response.getEmployees()
        );
    }
}