package com.bepa.eis.server.api.web.application.customer;

import com.bepa.eis.common.providers.customer.CustomerRegistrationProvider;
import com.bepa.eis.common.providers.customer.CustomerRegistrationProvider.CustomerRegistrationData;
import com.bepa.eis.common.providers.customer.CustomerRegistrationProvider.CustomerRegistrationResult;
import com.bepa.eis.server.api.external.virk.cvr.CvrCompanyDto;
import com.bepa.eis.server.api.external.virk.cvr.CvrLookupService;
import com.bepa.eis.server.api.external.virk.cvr.CvrapiDkLookupService;
import com.bepa.eis.server.api.web.application.admin.AbstractAdminServlet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@WebServlet(name = "CustomerRegistrationServlet", urlPatterns = {
        "/api/customers/cvr",
        "/api/customers"
})

public class CustomerRegistrationServlet extends AbstractAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(CustomerRegistrationServlet.class);

    private static final Pattern CVR_PATTERN = Pattern.compile("\\d{8}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Set<String> AVAILABLE_MODULE_CODES = Set.of(
            "BASIS-MODULE"
    );

    private static final Set<String> KNOWN_MODULE_CODES = Set.of(
            "BASIS-MODULE",
            "PRO-MODULE",
            "MASTER-MODULE"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CvrLookupService cvrLookupService;

    @Override
    public void init() throws ServletException {
        this.cvrLookupService = new CvrapiDkLookupService();
    }

    @Override
    public void processGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if (!"/api/customers/cvr".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String cvrNumber = normalize(request.getParameter("cvrNumber"));

        if (!isValidCvr(cvrNumber)) {
            sendJsonError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "CVR number must contain exactly 8 digits."
            );
            return;
        }

        try {
            Optional<CvrCompanyDto> company = cvrLookupService.findCompanyByCvrNumber(cvrNumber);

            if (company.isEmpty()) {
                sendJsonError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "No company was found for the entered CVR number."
                );
                return;
            }

            sendJson(
                    response,
                    HttpServletResponse.SC_OK,
                    company.get()
            );
        } catch (IllegalArgumentException e) {
            sendJsonError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    safeMessage(e, "Invalid CVR lookup request.")
            );
        } catch (Exception e) {
            sendJsonError(
                    response,
                    HttpServletResponse.SC_BAD_GATEWAY,
                    "Could not lookup company by CVR number."
            );
        }
    }

    @Override
    public void processPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {
        if (!"/api/customers".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.setCharacterEncoding(StandardCharsets.UTF_8.name());

        CustomerCreateRequest request;

        try {
            request = objectMapper.readValue(
                    req.getInputStream(),
                    CustomerCreateRequest.class
            );
        } catch (Exception e) {
            sendJsonError(
                    resp,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON request body."
            );
            return;
        }

        String validationError = validateCreateRequest(request);

        if (validationError != null) {
            log.error("Invalid customer registration request: {}", validationError);
            sendJsonError(
                    resp,
                    HttpServletResponse.SC_BAD_REQUEST,
                    validationError
            );
            return;
        }

        CustomerRegistrationData registrationData = toRegistrationData(request);

        CustomerRegistrationProvider registrationProvider = new CustomerRegistrationProvider(null);
        CustomerRegistrationResult result = registrationProvider.registerCustomer(registrationData);

        if (!result.isSuccess()) {
            sendJsonError(
                    resp,
                    HttpServletResponse.SC_BAD_REQUEST,
                    result.getMessage()
            );
            return;
        }

        sendJson(
                resp,
                HttpServletResponse.SC_CREATED,
                new CustomerCreateResponse(
                        normalize(request.cvrNumber()),
                        trimToEmpty(request.name()),
                        trimToEmpty(request.administratorEmail()),
                        trimToEmpty(request.moduleCode()),
                        result.getCustomerId(),
                        result.getCustomerPK(),
                        result.getCustomerModuleId(),
                        result.getCustomerPaymentMethodId(),
                        result.getWorkflowId(),
                        result.getUserId(),
                        result.getMessage()
                )
        );
    }

    private CustomerRegistrationData toRegistrationData(CustomerCreateRequest request) {
        CustomerRegistrationData data = new CustomerRegistrationData();

        data.setModuleCode(request.moduleCode());
        data.setCustomerName(request.name());
        data.setCvrNumber(normalize(request.cvrNumber()));
        data.setPhone(request.phone());

        data.setAddress(request.address());
        data.setZipCode(request.zipCode());
        data.setCity(request.city());
        data.setCountry(request.country());

        /*
         * New Customer currently has administratorEmail.
         * This is intentionally stored as ContactEmail.
         * Until a separate contact name exists, customer name is used as contact name.
         */
        data.setContactName(request.name());
        data.setContactEmail(request.administratorEmail());

        if (request.payment() != null) {
            data.setCardholderName(request.payment().cardholderName());
            data.setCardNumber(request.payment().cardNumber());
            data.setExpiryMonth(request.payment().expiryMonth());
            data.setExpiryYear(request.payment().expiryYear());
            data.setBillingZipCode(request.payment().billingZipCode());
        }

        return data;
    }

    private String validateCreateRequest(CustomerCreateRequest request) {
        if (request == null) {
            return "Request body is required.";
        }

        String moduleValidationError = validateModule(request);

        if (moduleValidationError != null) {
            return moduleValidationError;
        }

        String normalizedCvrNumber = normalize(request.cvrNumber());

        if (!isBlank(normalizedCvrNumber) && !isValidCvr(normalizedCvrNumber)) {
            return "CVR number must contain exactly 8 digits when entered.";
        }

        if (isBlank(request.name())) {
            return "Name is required.";
        }

        if (isBlank(request.address())) {
            return "Address is required.";
        }

        if (isBlank(request.zipCode())) {
            return "Zip Code is required.";
        }

        if (isBlank(request.city())) {
            return "City is required.";
        }

        if (isBlank(request.country())) {
            return "Country is required.";
        }

        if (isBlank(request.phone())) {
            return "Phone is required.";
        }

        if (isBlank(request.administratorEmail())) {
            return "Contact email is required.";
        }

        if (!EMAIL_PATTERN.matcher(request.administratorEmail().trim()).matches()) {
            return "Contact email must be valid.";
        }

        if (request.payment() == null) {
            return "Payment information is required.";
        }

        String paymentValidationError = validatePayment(request.payment());

        if (paymentValidationError != null) {
            return paymentValidationError;
        }

        if (!request.termsAccepted()) {
            return "Terms and conditions must be accepted.";
        }

        return null;
    }

    private String validateModule(CustomerCreateRequest request) {
        if (isBlank(request.moduleCode())) {
            return "Module is required.";
        }

        String moduleCode = request.moduleCode().trim();

        if (!KNOWN_MODULE_CODES.contains(moduleCode)) {
            return "Selected module is unknown.";
        }

        if (!AVAILABLE_MODULE_CODES.contains(moduleCode)) {
            return "Selected module is not available.";
        }

        return null;
    }

    private String validatePayment(PaymentInformation payment) {
        if (isBlank(payment.cardholderName())) {
            return "Cardholder name is required.";
        }

        String cardNumber = onlyDigits(payment.cardNumber());

        if (cardNumber.length() < 13 || cardNumber.length() > 19) {
            return "Card number must contain between 13 and 19 digits.";
        }

        if (isBlank(payment.expiryMonth())) {
            return "Expiry month is required.";
        }

        if (isBlank(payment.expiryYear())) {
            return "Expiry year is required.";
        }

        int month;
        int year;

        try {
            month = Integer.parseInt(payment.expiryMonth().trim());
            year = Integer.parseInt(payment.expiryYear().trim());
        } catch (NumberFormatException e) {
            return "Expiry month and year must be numeric.";
        }

        if (month < 1 || month > 12) {
            return "Expiry month must be between 01 and 12.";
        }

        YearMonth expiry = YearMonth.of(
                year,
                month
        );

        YearMonth currentMonth = YearMonth.now();

        if (expiry.isBefore(currentMonth)) {
            return "Payment card has expired.";
        }

        String cvc = onlyDigits(payment.cvc());

        if (cvc.length() < 3 || cvc.length() > 4) {
            return "CVC must contain 3 or 4 digits.";
        }

        if (isBlank(payment.billingZipCode())) {
            return "Billing zip code is required.";
        }

        return null;
    }

    private boolean isValidCvr(String cvrNumber) {
        return cvrNumber != null && CVR_PATTERN.matcher(cvrNumber).matches();
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeMessage(
            Exception exception,
            String fallbackMessage
    ) {
        if (exception == null || isBlank(exception.getMessage())) {
            return fallbackMessage;
        }

        return exception.getMessage();
    }

    private void sendJson(
            HttpServletResponse resp,
            int status,
            Object body
    ) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        objectMapper.writeValue(
                resp.getOutputStream(),
                body
        );
    }

    private void sendJsonError(
            HttpServletResponse resp,
            int status,
            String message
    ) throws IOException {
        sendJson(
                resp,
                status,
                new ErrorResponse(message)
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerCreateRequest(
            String moduleCode,
            String moduleName,
            String modulePrice,
            String cvrNumber,
            String name,
            String address,
            String zipCode,
            String city,
            String country,
            String phone,
            String administratorEmail,
            PaymentInformation payment,
            boolean termsAccepted
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentInformation(
            String cardholderName,
            String cardNumber,
            String expiryMonth,
            String expiryYear,
            String cvc,
            String billingZipCode
    ) {
    }

    public record CustomerCreateResponse(
            String cvrNumber,
            String name,
            String administratorEmail,
            String moduleCode,
            Integer customerId,
            Integer customerPK,
            Integer customerModuleId,
            Integer customerPaymentMethodId,
            Integer workflowId,
            Integer userId,
            String message
    ) {
    }

    public record ErrorResponse(
            String message
    ) {
    }
}