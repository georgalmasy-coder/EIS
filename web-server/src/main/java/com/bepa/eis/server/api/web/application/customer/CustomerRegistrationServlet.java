package com.bepa.eis.server.api.web.application.customer;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.providers.customer.CustomerRegistrationProvider;
import com.bepa.eis.common.providers.customer.CustomerRegistrationProvider.CustomerRegistrationData;
import com.bepa.eis.common.providers.customer.CustomerRegistrationProvider.CustomerRegistrationResult;
import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.dto.customer.SubscriptionPlanBillingPeriod;
import com.bepa.eis.common.enums.customer.BillingPeriod;
import com.bepa.eis.common.enums.customer.Subscription;
import com.bepa.eis.common.providers.customer.SubscriptionPlanBillingPeriodProvider;
import com.bepa.eis.common.providers.customer.SubscriptionPlanProvider;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import com.bepa.eis.server.api.external.virk.cvr.CvrCompanyDto;
import com.bepa.eis.server.api.external.virk.cvr.CvrLookupService;
import com.bepa.eis.server.api.external.virk.cvr.CvrapiDkLookupService;
import com.bepa.eis.server.api.external.vies.ViesVatLookupService;
import com.bepa.eis.server.api.external.vies.ViesVatLookupService.ViesVatValidationResult;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@WebServlet(name = "CustomerRegistrationServlet", urlPatterns = {
        "/api/customers/cvr",
        "/api/customers/vat-validate",
        "/api/customers/phone-country-codes",
        "/api/customers/subscription-options",
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

    private static final Set<String> EU_COUNTRY_CODES = Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR",
            "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK",
            "SI", "ES", "SE"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CvrLookupService cvrLookupService;
    private ViesVatLookupService viesVatLookupService;

    @Override
    public void init() throws ServletException {
        this.cvrLookupService = new CvrapiDkLookupService();
        this.viesVatLookupService = new ViesVatLookupService();
    }

    @Override
    public void processGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if ("/api/customers/config".equals(request.getServletPath())) {
            sendJson(
                    response,
                    HttpServletResponse.SC_OK,
                    new CustomerRegistrationConfigResponse(
                            GlobalConfiguration.isCustomerRegistrationCvrLookupEnabled()
                    )
            );
            return;
        }

        if ("/api/customers/subscription-options".equals(request.getServletPath())) {
            sendJson(
                    response,
                    HttpServletResponse.SC_OK,
                    buildSubscriptionOptions()
            );
            return;
        }

        if ("/api/customers/vat-validate".equals(request.getServletPath())) {
            handleVatValidation(request, response);
            return;
        }

        if (!"/api/customers/cvr".equals(request.getServletPath())) {
            if ("/api/customers/phone-country-codes".equals(request.getServletPath())) {
                sendJson(
                        response,
                        HttpServletResponse.SC_OK,
                        CustomerLookupCache.getPhoneCountryRules().stream()
                                .map(rule -> new PhoneCountryCodeResponse(
                                        rule.country(),
                                        rule.code(),
                                        rule.minDigits(),
                                        rule.maxDigits(),
                                        rule.example()
                                ))
                                .toList()
                );
                return;
            }

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

        EhcacheProvider.clearCacheEntry(result.getCustomerId());

                sendJson(
                resp,
                HttpServletResponse.SC_CREATED,
                new CustomerCreateResponse(
                        normalize(request.cvrNumber()),
                        trimToEmpty(request.name()),
                        trimToEmpty(request.administratorEmail()),
                        trimToEmpty(firstNonBlank(request.subscriptionCode(), request.moduleCode())),
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
        SubscriptionPlanBillingPeriod selectedBillingPeriod = resolveBillingPeriod(
                request.subscriptionPlanId(),
                request.billingPeriodCode()
        );

        data.setSubscriptionPlanId(request.subscriptionPlanId());
        data.setBillingPeriodCode(request.billingPeriodCode());
        data.setSubscriptionPlanBillingPeriodId(
                selectedBillingPeriod == null ? null : selectedBillingPeriod.getSubscriptionPlanBillingPeriodId()
        );
        data.setModuleCode(firstNonBlank(request.subscriptionCode(), request.moduleCode()));
        data.setCustomerName(request.name());
        data.setCvrNumber(normalize(request.cvrNumber()));
        data.setVatNumber(normalize(request.vatNumber()));
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

        String moduleValidationError = validateSelection(request);

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

        String vatValidationError = validateVatNumber(
                firstNonBlank(request.countryCode(), request.country()),
                request.vatNumber(),
                request.name()
        );

        if (vatValidationError != null) {
            return vatValidationError;
        }

        if (isEuCountryCode(firstNonBlank(request.countryCode(), request.country()))) {
            String viesValidationError = validateVatWithVies(
                    firstNonBlank(request.countryCode(), request.country()),
                    request.vatNumber()
            );

            if (viesValidationError != null) {
                return viesValidationError;
            }
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

    private String validateSelection(CustomerCreateRequest request) {
        SubscriptionPlan selectedPlan = resolveSubscriptionPlan(request);

        if (selectedPlan == null) {
            return "Subscription is required.";
        }

        if (isBlank(request.billingPeriodCode())) {
            return "Billing period is required.";
        }

        SubscriptionPlanBillingPeriod selectedBillingPeriod = resolveBillingPeriod(
                selectedPlan.getSubscriptionPlanId(),
                request.billingPeriodCode()
        );

        if (selectedBillingPeriod == null) {
            return "Selected billing period is not available for the chosen subscription.";
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

    private void handleVatValidation(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String countryCode = normalizeCountryCode(request.getParameter("countryCode"));
        String vatNumber = normalizeVatNumber(request.getParameter("vatNumber"));
        String companyName = trimToEmpty(request.getParameter("companyName"));

        log.info(
                "VAT validation request received. countryCode={}, vatNumber={}, companyName={}",
                countryCode,
                vatNumber,
                companyName
        );

        String validationError = validateVatNumber(countryCode, vatNumber, companyName);

        if (validationError != null) {
            log.info(
                    "VAT validation request rejected before VIES. countryCode={}, vatNumber={}, companyName={}, reason={}",
                    countryCode,
                    vatNumber,
                    companyName,
                    validationError
            );
            sendJsonError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    validationError
            );
            return;
        }

        if (!isEuCountryCode(countryCode)) {
            log.info(
                    "VAT validation skipped for non-EU country. countryCode={}, vatNumber={}, companyName={}",
                    countryCode,
                    vatNumber,
                    companyName
            );
            sendJson(
                    response,
                    HttpServletResponse.SC_OK,
                    new VatValidationResponse(true, countryCode, vatNumber, companyName, "", "", "Validation skipped for non-EU country.")
            );
            return;
        }

        if (!isViesValidationEnabled()) {
            log.info(
                    "VAT validation skipped because VIES validation is disabled. countryCode={}, vatNumber={}, companyName={}",
                    countryCode,
                    vatNumber,
                    companyName
            );
            sendJson(
                    response,
                    HttpServletResponse.SC_OK,
                    new VatValidationResponse(true, countryCode, vatNumber, companyName, "", "", "VIES validation is disabled.")
            );
            return;
        }

        try {
            ViesVatValidationResult viesResult = viesVatLookupService.validateVat(countryCode, vatNumber);

            if (!viesResult.valid()) {
                log.info(
                        "VAT validation failed in VIES. countryCode={}, vatNumber={}, companyName={}, viesName={}, viesAddress={}, valid={}",
                        countryCode,
                        vatNumber,
                        companyName,
                        viesResult.name(),
                        viesResult.address(),
                        false
                );
                sendJsonError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "VAT number could not be validated with VIES."
                );
                return;
            }

            log.info(
                    "VAT validation succeeded in VIES. countryCode={}, vatNumber={}, companyName={}, viesName={}, viesAddress={}, valid={}",
                    countryCode,
                    vatNumber,
                    companyName,
                    viesResult.name(),
                    viesResult.address(),
                    true
            );

            sendJson(
                    response,
                    HttpServletResponse.SC_OK,
                    new VatValidationResponse(
                            true,
                            countryCode,
                            vatNumber,
                            companyName,
                            viesResult.name(),
                            viesResult.address(),
                            "VAT number was validated successfully."
                    )
            );
        } catch (IllegalArgumentException e) {
            log.info(
                    "VAT validation rejected with IllegalArgumentException. countryCode={}, vatNumber={}, companyName={}, message={}",
                    countryCode,
                    vatNumber,
                    companyName,
                    e.getMessage()
            );
            sendJsonError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    safeMessage(e, "Invalid VAT validation request.")
            );
        } catch (Exception e) {
            log.info(
                    "VAT validation failed with exception. countryCode={}, vatNumber={}, companyName={}, message={}",
                    countryCode,
                    vatNumber,
                    companyName,
                    e.getMessage(),
                    e
            );
            sendJsonError(
                    response,
                    HttpServletResponse.SC_BAD_GATEWAY,
                    "Could not validate VAT number through VIES."
            );
        }
    }

    private String validateVatNumber(
            String countryCode,
            String vatNumber,
            String companyName
    ) {
        if (!isEuCountryCode(countryCode)) {
            return null;
        }

        if (isBlank(vatNumber)) {
            log.info(
                    "VAT validation rejected because VAT number is blank for EU country. countryCode={}, companyName={}",
                    countryCode,
                    companyName
            );
            return "VAT number is required for EU countries.";
        }

        if (!isValidVatFormat(vatNumber)) {
            log.info(
                    "VAT validation rejected because VAT number format is invalid. countryCode={}, vatNumber={}, companyName={}",
                    countryCode,
                    vatNumber,
                    companyName
            );
            return "VAT number format is invalid.";
        }

        return null;
    }

    private String validateVatWithVies(
            String countryCode,
            String vatNumber
    ) {
        if (!isEuCountryCode(countryCode)) {
            return null;
        }

        if (!isViesValidationEnabled()) {
            log.info(
                    "VAT validation skipped during registration because VIES validation is disabled. countryCode={}, vatNumber={}",
                    countryCode,
                    vatNumber
            );
            return null;
        }

        if (isBlank(vatNumber) || !isValidVatFormat(vatNumber)) {
            log.info(
                    "VAT validation rejected before VIES during registration. countryCode={}, vatNumber={}",
                    countryCode,
                    vatNumber
            );
            return "VAT number format is invalid.";
        }

        try {
            ViesVatValidationResult viesResult = viesVatLookupService.validateVat(countryCode, vatNumber);

            if (!viesResult.valid()) {
                log.info(
                        "VAT validation failed in VIES during registration. countryCode={}, vatNumber={}, viesName={}, viesAddress={}, valid={}",
                        countryCode,
                        vatNumber,
                        viesResult.name(),
                        viesResult.address(),
                        false
                );
                return "VAT number could not be validated with VIES.";
            }

            log.info(
                    "VAT validation succeeded in VIES during registration. countryCode={}, vatNumber={}, viesName={}, viesAddress={}, valid={}",
                    countryCode,
                    vatNumber,
                    viesResult.name(),
                    viesResult.address(),
                    true
            );

            return null;
        } catch (IllegalArgumentException e) {
            log.info(
                    "VAT validation rejected with IllegalArgumentException during registration. countryCode={}, vatNumber={}, message={}",
                    countryCode,
                    vatNumber,
                    e.getMessage()
            );
            return safeMessage(e, "Invalid VAT validation request.");
        } catch (Exception e) {
            log.info(
                    "VAT validation failed with exception during registration. countryCode={}, vatNumber={}, message={}",
                    countryCode,
                    vatNumber,
                    e.getMessage(),
                    e
            );
            return "Could not validate VAT number through VIES.";
        }
    }

    private boolean isViesValidationEnabled() {
        return GlobalConfiguration.isViesValidationEnabled();
    }

    private boolean isValidCvr(String cvrNumber) {
        return cvrNumber != null && CVR_PATTERN.matcher(cvrNumber).matches();
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private String normalizeVatNumber(String value) {
        return value == null ? "" : value.replaceAll("[\\s.-]+", "").toUpperCase();
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

    private String normalizeCountryCode(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean isEuCountryCode(String countryCode) {
        return EU_COUNTRY_CODES.contains(normalizeCountryCode(countryCode));
    }

    private boolean isValidVatFormat(String vatNumber) {
        String normalizedVatNumber = normalizeVatNumber(vatNumber);
        return normalizedVatNumber.matches("[A-Z0-9]{2,20}");
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }

        if (!isBlank(second)) {
            return second.trim();
        }

        return "";
    }

    private SubscriptionPlan resolveSubscriptionPlan(CustomerCreateRequest request) {
        SubscriptionPlanProvider planProvider = new SubscriptionPlanProvider(null);

        if (request == null) {
            return null;
        }

        Integer subscriptionPlanId = request.subscriptionPlanId();

        if (subscriptionPlanId != null && subscriptionPlanId > 0) {
            SubscriptionPlan planById = planProvider.getPlanById(subscriptionPlanId);
            if (isSelectableSubscriptionPlan(planById)) {
                return planById;
            }
        }

        String moduleCode = firstNonBlank(request.subscriptionCode(), request.moduleCode());

        if (isBlank(moduleCode)) {
            return null;
        }

        SubscriptionPlan planByModuleCode = planProvider.getActivePlanByModuleCode(moduleCode);
        if (isSelectableSubscriptionPlan(planByModuleCode)) {
            return planByModuleCode;
        }

        return null;
    }

    private boolean isSelectableSubscriptionPlan(SubscriptionPlan plan) {
        if (plan == null || plan.getSubscriptionPlanId() == null) {
            return false;
        }

        Subscription subscription = Subscription.fromModuleCode(plan.getModuleCode());
        return subscription != null && subscription.isActive();
    }

    private SubscriptionPlanBillingPeriod resolveBillingPeriod(
            Integer subscriptionPlanId,
            String billingPeriodCode
    ) {
        if (subscriptionPlanId == null || isBlank(billingPeriodCode)) {
            return null;
        }

        SubscriptionPlanBillingPeriodProvider billingPeriodProvider = new SubscriptionPlanBillingPeriodProvider(null);
        List<SubscriptionPlanBillingPeriod> billingPeriods = billingPeriodProvider.getBillingPeriodsByPlanId(subscriptionPlanId);

        for (SubscriptionPlanBillingPeriod billingPeriod : billingPeriods) {
            BillingPeriod enumValue = BillingPeriod.fromCode(billingPeriod.getBillingPeriodCode());

            if (enumValue == null || !enumValue.isActive()) {
                continue;
            }

            if (billingPeriodCode.trim().equalsIgnoreCase(billingPeriod.getBillingPeriodCode())) {
                return billingPeriod;
            }
        }

        return null;
    }

    private SubscriptionOptionsResponse buildSubscriptionOptions() {
        SubscriptionPlanProvider planProvider = new SubscriptionPlanProvider(null);
        SubscriptionPlanBillingPeriodProvider billingPeriodProvider = new SubscriptionPlanBillingPeriodProvider(null);

        List<SubscriptionPlan> plans = planProvider.getActivePlans();
        List<SubscriptionOptionResponse> subscriptionOptions = new ArrayList<>();
        LinkedHashMap<String, BillingPeriodResponse> billingPeriodMap = new LinkedHashMap<>();

        for (SubscriptionPlan plan : plans) {
            Subscription subscription = Subscription.fromModuleCode(plan.getModuleCode());

            if (subscription == null || !subscription.isActive()) {
                continue;
            }

            List<SubscriptionBillingPeriodResponse> billingPeriods = billingPeriodProvider
                    .getBillingPeriodsByPlanId(plan.getSubscriptionPlanId())
                    .stream()
                    .map(this::toBillingPeriodResponse)
                    .filter(response -> response != null && response.active())
                    .sorted(Comparator
                            .comparingInt(SubscriptionBillingPeriodResponse::displayOrder)
                            .thenComparing(SubscriptionBillingPeriodResponse::label, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            if (billingPeriods.isEmpty()) {
                continue;
            }

            for (SubscriptionBillingPeriodResponse billingPeriod : billingPeriods) {
                billingPeriodMap.putIfAbsent(
                        billingPeriod.code(),
                        new BillingPeriodResponse(
                                billingPeriod.code(),
                                billingPeriod.label(),
                                billingPeriod.description(),
                                billingPeriod.months(),
                                billingPeriod.displayOrder(),
                                billingPeriod.active()
                        )
                );
            }

            subscriptionOptions.add(new SubscriptionOptionResponse(
                    plan.getSubscriptionPlanId(),
                    subscription.getModuleCode(),
                    subscription.getLabel(),
                    plan.getDescription(),
                    plan.getValidFrom() == null ? "" : plan.getValidFrom().toString(),
                    plan.getValidTo() == null ? "" : plan.getValidTo().toString(),
                    subscription.getDisplayOrder(),
                    plan.getActive(),
                    billingPeriods
            ));
        }

        subscriptionOptions.sort(Comparator
                .comparingInt(SubscriptionOptionResponse::displayOrder)
                .thenComparing(SubscriptionOptionResponse::subscriptionLabel, String.CASE_INSENSITIVE_ORDER));

        List<BillingPeriodResponse> billingPeriods = new ArrayList<>(billingPeriodMap.values());
        billingPeriods.sort(Comparator
                .comparingInt(BillingPeriodResponse::displayOrder)
                .thenComparing(BillingPeriodResponse::label, String.CASE_INSENSITIVE_ORDER));

        return new SubscriptionOptionsResponse(billingPeriods, subscriptionOptions);
    }

    private SubscriptionBillingPeriodResponse toBillingPeriodResponse(SubscriptionPlanBillingPeriod billingPeriod) {
        if (billingPeriod == null) {
            return null;
        }

        BillingPeriod enumValue = BillingPeriod.fromCode(billingPeriod.getBillingPeriodCode());

        if (enumValue == null) {
            return null;
        }

        return new SubscriptionBillingPeriodResponse(
                enumValue.getCode(),
                enumValue.getLabel(),
                enumValue.getDescription(),
                enumValue.getMonths(),
                billingPeriod.getPriceAmount(),
                billingPeriod.getCurrency(),
                enumValue.getDisplayOrder(),
                billingPeriod.getActive()
        );
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
            Integer subscriptionPlanId,
            String subscriptionCode,
            String billingPeriodCode,
            String moduleCode,
            String moduleName,
            String modulePrice,
            String countryCode,
            String vatNumber,
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

    public record VatValidationResponse(
            boolean valid,
            String countryCode,
            String vatNumber,
            String companyName,
            String viesName,
            String viesAddress,
            String message
    ) {
    }

    public record ErrorResponse(
            String message
    ) {
    }

    public record PhoneCountryCodeResponse(
            String country,
            String code,
            int min,
            int max,
            String example
    ) {
    }

    public record CustomerRegistrationConfigResponse(
            boolean cvrLookupEnabled
    ) {
    }

    public record SubscriptionOptionsResponse(
            List<BillingPeriodResponse> billingPeriods,
            List<SubscriptionOptionResponse> subscriptions
    ) {
    }

    public record BillingPeriodResponse(
            String code,
            String label,
            String description,
            int months,
            int displayOrder,
            boolean active
    ) {
    }

    public record SubscriptionOptionResponse(
            Integer subscriptionPlanId,
            String subscriptionCode,
            String subscriptionLabel,
            String description,
            String validFrom,
            String validTo,
            int displayOrder,
            boolean active,
            List<SubscriptionBillingPeriodResponse> billingPeriods
    ) {
    }

    public record SubscriptionBillingPeriodResponse(
            String code,
            String label,
            String description,
            int months,
            java.math.BigDecimal priceAmount,
            String currency,
            int displayOrder,
            boolean active
    ) {
    }
}
