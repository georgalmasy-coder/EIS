package dk.eis.tech.timesheet.api;

import dk.eis.tech.timesheet.data.ActivityRepository;
import dk.eis.tech.timesheet.data.AccountingRepository;
import dk.eis.tech.timesheet.config.CompanyFooterConfig;
import dk.eis.tech.timesheet.data.CustomerRepository;
import dk.eis.tech.timesheet.data.MaterialRepository;
import dk.eis.tech.timesheet.data.TimeEntryRepository;
import dk.eis.tech.timesheet.model.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.*;

@WebServlet(urlPatterns = "/api/*")
public class ApiServlet extends HttpServlet {

    private static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("25.00");

    private final CustomerRepository customerRepository = new CustomerRepository();
    private final ActivityRepository activityRepository = new ActivityRepository();
    private final TimeEntryRepository timeEntryRepository = new TimeEntryRepository();
    private final MaterialRepository materialRepository = new MaterialRepository();
    private final AccountingRepository accountingRepository = new AccountingRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            dispatchGet(request, response);
        } catch (Exception ex) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            dispatchPost(request, response);
        } catch (Exception ex) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            dispatchPut(request, response);
        } catch (Exception ex) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            dispatchDelete(request, response);
        } catch (Exception ex) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
    }

    private void dispatchGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = normalizedPath(request);
        if (path.equals("/bootstrap")) {
            writeJson(response, new BootstrapResponse(customerRepository.findAll(), selectedCustomerId(request), CompanyFooterConfig.toRecord()));
            return;
        }
        if (path.equals("/customers")) {
            writeJson(response, Map.of("customers", customerRepository.findAll()));
            return;
        }
        if (path.matches("/customers/\\d+/activities")) {
            long customerId = idFromPath(path, "/customers/");
            writeJson(response, Map.of("activities", activityRepository.findByCustomerId(customerId)));
            return;
        }
        if (path.equals("/calendar")) {
            long customerId = requiredLong(request, "customerId");
            int year = requiredInt(request, "year");
            int month = requiredInt(request, "month");
            writeJson(response, buildCalendarResponse(customerId, year, month));
            return;
        }
        if (path.equals("/invoice")) {
            long customerId = requiredLong(request, "customerId");
            int year = requiredInt(request, "year");
            int month = requiredInt(request, "month");
            writeJson(response, buildInvoiceResponse(customerId, year, month));
            return;
        }
        if (path.equals("/invoice/posting-preview")) {
            long customerId = requiredLong(request, "customerId");
            int year = requiredInt(request, "year");
            int month = requiredInt(request, "month");
            InvoiceResponse invoice = buildInvoiceResponse(customerId, year, month);
            writeJson(response, accountingRepository.invoicePreview(invoiceNumber(customerId, year, month),
                    invoiceDate(year, month), invoice.subtotal(), invoice.vatAmount(), invoice.total()));
            return;
        }
        if (path.equals("/accounts")) {
            boolean activeOnly = Boolean.parseBoolean(Optional.ofNullable(request.getParameter("activeOnly")).orElse("false"));
            writeJson(response, Map.of("accounts", accountingRepository.findAccounts(activeOnly)));
            return;
        }
        if (path.equals("/accounting/entries")) {
            int year = requiredInt(request, "year");
            Integer month = optionalInt(request, "month");
            writeJson(response, Map.of("entries", accountingRepository.findEntries(year, month)));
            return;
        }
        if (path.equals("/accounting/statement")) {
            int year = requiredInt(request, "year");
            Integer fromMonth = optionalInt(request, "fromMonth");
            Integer toMonth = optionalInt(request, "toMonth");
            writeJson(response, buildAccountingStatement(year, fromMonth, toMonth));
            return;
        }
        if (path.matches("/customers/\\d+")) {
            long customerId = idFromPath(path, "/customers/");
            CustomerRecord customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            writeJson(response, customer);
            return;
        }
        if (path.matches("/activities/\\d+")) {
            long activityId = idFromPath(path, "/activities/");
            ActivityRecord activity = activityRepository.findById(activityId)
                    .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
            writeJson(response, activity);
            return;
        }
        if (path.matches("/time-entries/\\d+")) {
            long timeEntryId = idFromPath(path, "/time-entries/");
            TimeEntryRecord entry = timeEntryRepository.findById(timeEntryId)
                    .orElseThrow(() -> new IllegalArgumentException("Time entry not found"));
            writeJson(response, entry);
            return;
        }
        if (path.matches("/materials/\\d+")) {
            long materialId = idFromPath(path, "/materials/");
            MaterialEntryRecord entry = materialRepository.findById(materialId)
                    .orElseThrow(() -> new IllegalArgumentException("Material not found"));
            writeJson(response, entry);
            return;
        }
        sendNotFound(response);
    }

    private void dispatchPost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = normalizedPath(request);
        if (path.equals("/selection")) {
            SelectionRequest selection = readJson(request, SelectionRequest.class);
            if (selection.customerId() == null) {
                request.getSession(true).removeAttribute("selectedCustomerId");
            } else {
                CustomerRecord customer = customerRepository.findById(selection.customerId())
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
                if (customer.inactive()) {
                    throw new IllegalArgumentException("The selected customer is inactive");
                }
                request.getSession(true).setAttribute("selectedCustomerId", selection.customerId());
            }
            writeJson(response, Map.of("selectedCustomerId", selection.customerId()));
            return;
        }
        if (path.equals("/customers")) {
            CustomerUpsertRequest body = readJson(request, CustomerUpsertRequest.class);
            long id = customerRepository.insert(normalizeCustomer(body, null));
            writeJson(response, Map.of("id", id));
            return;
        }
        if (path.matches("/customers/\\d+/activities")) {
            long customerId = idFromPath(path, "/customers/");
            ActivityUpsertRequest body = readJson(request, ActivityUpsertRequest.class);
            long id = activityRepository.insert(customerId, normalizeActivity(body, null));
            writeJson(response, Map.of("id", id));
            return;
        }
        if (path.matches("/customers/\\d+/time-entries")) {
            long customerId = idFromPath(path, "/customers/");
            TimeEntryUpsertRequest body = readJson(request, TimeEntryUpsertRequest.class);
            validateHalfHour(body.hours());
            validateActivityForCustomer(customerId, body.activityId());
            long id = timeEntryRepository.insert(customerId, normalizeTimeEntry(body, customerId, null));
            writeJson(response, Map.of("id", id));
            return;
        }
        if (path.matches("/customers/\\d+/materials")) {
            long customerId = idFromPath(path, "/customers/");
            MaterialEntryUpsertRequest body = readJson(request, MaterialEntryUpsertRequest.class);
            long id = materialRepository.insert(customerId, normalizeMaterial(body, customerId, null));
            writeJson(response, Map.of("id", id));
            return;
        }
        if (path.equals("/invoice/approve")) {
            Map<?, ?> body = readJson(request, Map.class);
            long customerId = ((Number) body.get("customerId")).longValue();
            int year = ((Number) body.get("year")).intValue();
            int month = ((Number) body.get("month")).intValue();
            InvoiceResponse invoice = buildInvoiceResponse(customerId, year, month);
            InvoiceApprovalRecord approval = accountingRepository.approveInvoice(customerId, year, month,
                    invoiceNumber(customerId, year, month), invoiceDate(year, month),
                    invoice.subtotal(), invoice.vatAmount(), invoice.total());
            writeJson(response, approval);
            return;
        }
        if (path.equals("/accounts")) {
            AccountUpsertRequest body = readJson(request, AccountUpsertRequest.class);
            writeJson(response, Map.of("id", accountingRepository.insertAccount(body)));
            return;
        }
        if (path.equals("/accounting/entries")) {
            AccountingEntryRequest body = readJson(request, AccountingEntryRequest.class);
            writeJson(response, Map.of("id", accountingRepository.insertEntry(body)));
            return;
        }
        if (path.matches("/accounting/entries/\\d+/corrections")) {
            long entryId = idFromPath(path, "/accounting/entries/");
            AccountingEntryRequest body = readJson(request, AccountingEntryRequest.class);
            writeJson(response, Map.of("id", accountingRepository.correctEntry(entryId, body)));
            return;
        }
        sendNotFound(response);
    }

    private void dispatchPut(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = normalizedPath(request);
        if (path.matches("/customers/\\d+")) {
            long customerId = idFromPath(path, "/customers/");
            CustomerRecord existing = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            CustomerUpsertRequest body = readJson(request, CustomerUpsertRequest.class);
            customerRepository.update(customerId, normalizeCustomer(body, existing));
            if (body.inactive()) {
                activityRepository.softDelete(customerId);
            }
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        if (path.matches("/activities/\\d+")) {
            long activityId = idFromPath(path, "/activities/");
            ActivityRecord existing = activityRepository.findById(activityId)
                    .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
            ActivityUpsertRequest body = readJson(request, ActivityUpsertRequest.class);
            activityRepository.update(activityId, normalizeActivity(body, existing));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        if (path.matches("/time-entries/\\d+")) {
            long id = idFromPath(path, "/time-entries/");
            TimeEntryRecord existing = timeEntryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Time entry not found"));
            TimeEntryUpsertRequest body = readJson(request, TimeEntryUpsertRequest.class);
            validateHalfHour(body.hours());
            validateActivityForCustomer(existing.customerId(), body.activityId(), existing.activityId());
            timeEntryRepository.update(id, normalizeTimeEntry(body, existing.customerId(), existing));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        if (path.matches("/materials/\\d+")) {
            long id = idFromPath(path, "/materials/");
            MaterialEntryRecord existing = materialRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Material not found"));
            MaterialEntryUpsertRequest body = readJson(request, MaterialEntryUpsertRequest.class);
            materialRepository.update(id, normalizeMaterial(body, existing.customerId(), existing));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        if (path.matches("/accounts/\\d+")) {
            long id = idFromPath(path, "/accounts/");
            AccountUpsertRequest body = readJson(request, AccountUpsertRequest.class);
            accountingRepository.updateAccount(id, body);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        sendNotFound(response);
    }

    private void dispatchDelete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = normalizedPath(request);
        if (path.matches("/customers/\\d+")) {
            customerRepository.softDelete(idFromPath(path, "/customers/"));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        if (path.matches("/activities/\\d+")) {
            activityRepository.softDelete(idFromPath(path, "/activities/"));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        if (path.matches("/time-entries/\\d+")) {
            timeEntryRepository.softDelete(idFromPath(path, "/time-entries/"));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        if (path.matches("/materials/\\d+")) {
            materialRepository.softDelete(idFromPath(path, "/materials/"));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        sendNotFound(response);
    }

    private CalendarResponse buildCalendarResponse(long customerId, int year, int month) throws Exception {
        YearMonth yearMonth = YearMonth.of(year, month);
        CustomerRecord customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        List<TimeEntryRecord> timeEntries = timeEntryRepository.findByCustomerAndMonth(customerId, year, month);
        List<MaterialEntryRecord> materials = materialRepository.findByCustomerAndMonth(customerId, year, month);

        Map<LocalDate, List<TimeEntryRecord>> entriesByDate = new LinkedHashMap<>();
        for (TimeEntryRecord entry : timeEntries) {
            entriesByDate.computeIfAbsent(entry.entryDate(), key -> new ArrayList<>()).add(entry);
        }

        List<CalendarDayRecord> days = new ArrayList<>();
        BigDecimal monthHours = BigDecimal.ZERO;
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            List<TimeEntryRecord> entries = entriesByDate.getOrDefault(date, List.of());
            BigDecimal hours = entries.stream()
                    .map(TimeEntryRecord::hours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthHours = monthHours.add(hours);
            days.add(new CalendarDayRecord(date, day, scaleOne(hours), entries));
        }

        WeekFields weekFields = WeekFields.ISO;
        Map<Integer, BigDecimal> weekTotals = new LinkedHashMap<>();
        for (CalendarDayRecord day : days) {
            int weekNumber = day.date().get(weekFields.weekOfWeekBasedYear());
            weekTotals.merge(weekNumber, day.hours(), BigDecimal::add);
        }

        List<CalendarWeekRecord> weeks = weekTotals.entrySet().stream()
                .map(entry -> new CalendarWeekRecord(entry.getKey(), scaleOne(entry.getValue())))
                .toList();

        return new CalendarResponse(
                year,
                month,
                yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("da-DK")),
                scaleOne(monthHours),
                days,
                weeks,
                materials
        );
    }

    private InvoiceResponse buildInvoiceResponse(long customerId, int year, int month) throws Exception {
        YearMonth yearMonth = YearMonth.of(year, month);
        CustomerRecord customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        BigDecimal rate = customer.hourlyRate() == null ? BigDecimal.ZERO : customer.hourlyRate();
        BigDecimal vatRate = customer.vatRate() == null ? DEFAULT_VAT_RATE : customer.vatRate();

        List<TimeEntryRecord> timeEntries = timeEntryRepository.findByCustomerAndMonth(customerId, year, month);
        List<MaterialEntryRecord> materials = materialRepository.findByCustomerAndMonth(customerId, year, month);

        Map<Long, List<TimeEntryRecord>> byActivity = new LinkedHashMap<>();
        for (TimeEntryRecord entry : timeEntries) {
            byActivity.computeIfAbsent(entry.activityId(), key -> new ArrayList<>()).add(entry);
        }

        List<InvoiceTimeRow> timeRows = new ArrayList<>();
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal laborAmount = BigDecimal.ZERO;
        for (Map.Entry<Long, List<TimeEntryRecord>> entry : byActivity.entrySet()) {
            BigDecimal hours = entry.getValue().stream().map(TimeEntryRecord::hours).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal amount = hours.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            totalHours = totalHours.add(hours);
            laborAmount = laborAmount.add(amount);
            String shortDescription = entry.getValue().getFirst().activityShortDescription();
            timeRows.add(new InvoiceTimeRow(entry.getKey(), shortDescription, scaleOne(hours), moneyScale(rate), amount));
        }

        List<InvoiceMaterialRow> materialRows = new ArrayList<>();
        BigDecimal materialAmount = BigDecimal.ZERO;
        for (MaterialEntryRecord material : materials) {
            BigDecimal amount = material.quantity().multiply(material.unitPrice()).setScale(2, RoundingMode.HALF_UP);
            materialAmount = materialAmount.add(amount);
            materialRows.add(new InvoiceMaterialRow(
                    material.id(),
                    material.entryDate(),
                    material.quantity(),
                    material.unit(),
                    material.shortDescription(),
                    material.unitPrice(),
                    amount
            ));
        }

        BigDecimal subtotal = laborAmount.add(materialAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = subtotal.multiply(vatRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        return new InvoiceResponse(
                year,
                month,
                yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("da-DK")),
                vatRate,
                scaleOne(totalHours),
                subtotal,
                vatAmount,
                total,
                accountingRepository.findInvoiceApproval(customerId, year, month).orElse(null),
                timeRows,
                materialRows
        );
    }

    private Map<String, Object> buildAccountingStatement(int year, Integer fromMonth, Integer toMonth) throws Exception {
        if (fromMonth != null && (fromMonth < 1 || fromMonth > 12)) {
            throw new IllegalArgumentException("fromMonth must be between 1 and 12");
        }
        if (toMonth != null && (toMonth < 1 || toMonth > 12)) {
            throw new IllegalArgumentException("toMonth must be between 1 and 12");
        }
        List<AccountRecord> accounts = accountingRepository.findAccounts(false);
        List<AccountingEntryRecord> entries = accountingRepository.findEntries(year, fromMonth, toMonth);
        Map<Long, BigDecimal> debitByAccount = new HashMap<>();
        Map<Long, BigDecimal> creditByAccount = new HashMap<>();
        for (AccountingEntryRecord entry : entries) {
            for (AccountingLineRecord line : entry.lines()) {
                debitByAccount.merge(line.accountId(), line.debitAmount(), BigDecimal::add);
                creditByAccount.merge(line.accountId(), line.creditAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> rows = accounts.stream().map(account -> {
            BigDecimal debit = debitByAccount.getOrDefault(account.id(), BigDecimal.ZERO);
            BigDecimal credit = creditByAccount.getOrDefault(account.id(), BigDecimal.ZERO);
            BigDecimal balance = switch (account.accountType()) {
                case "ASSET", "EXPENSE" -> debit.subtract(credit);
                default -> credit.subtract(debit);
            };
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("accountId", account.id());
            row.put("accountNumber", account.accountNumber());
            row.put("accountName", account.accountName());
            row.put("accountType", account.accountType());
            row.put("debit", moneyScale(debit));
            row.put("credit", moneyScale(credit));
            row.put("balance", moneyScale(balance));
            return row;
        }).filter(row -> ((BigDecimal) row.get("debit")).compareTo(BigDecimal.ZERO) != 0
                || ((BigDecimal) row.get("credit")).compareTo(BigDecimal.ZERO) != 0
                || ((BigDecimal) row.get("balance")).compareTo(BigDecimal.ZERO) != 0).toList();
        Map<String, Object> statement = new LinkedHashMap<>();
        statement.put("year", year);
        statement.put("fromMonth", fromMonth);
        statement.put("toMonth", toMonth);
        statement.put("rows", rows);
        return statement;
    }

    private String invoiceNumber(long customerId, int year, int month) {
        return customerId + String.format("%04d%02d", year, month);
    }

    private LocalDate invoiceDate(int year, int month) {
        return YearMonth.of(year, month).plusMonths(1).atDay(1);
    }

    private CustomerRecord normalizeCustomer(CustomerUpsertRequest body, CustomerRecord existing) {
        BigDecimal hourlyRate = body.hourlyRate() == null ? BigDecimal.ZERO : body.hourlyRate();
        BigDecimal vatRate = existing == null ? DEFAULT_VAT_RATE : existing.vatRate();
        return new CustomerRecord(
                existing == null ? 0 : existing.id(),
                requiredText(body.companyName(), "Firmanavn"),
                requiredText(body.contactName(), "Kontakt navn"),
                requiredText(body.contactEmail(), "Kontakt e-mail"),
                optionalText(body.phoneNumber()),
                requiredText(body.addressLine(), "Adresse"),
                requiredText(body.postalCode(), "Postnr."),
                requiredText(body.city(), "By"),
                moneyScale(hourlyRate),
                vatRate,
                body.inactive(),
                existing == null ? null : existing.createdAt(),
                existing == null ? null : existing.updatedAt()
        );
    }

    private ActivityRecord normalizeActivity(ActivityUpsertRequest body, ActivityRecord existing) {
        return new ActivityRecord(
                existing == null ? 0 : existing.id(),
                existing == null ? 0 : existing.customerId(),
                requiredText(body.shortDescription(), "Kort aktivitetsbeskrivelse"),
                optionalText(body.longDescription()),
                body.inactive(),
                existing == null ? null : existing.createdAt(),
                existing == null ? null : existing.updatedAt()
        );
    }

    private TimeEntryRecord normalizeTimeEntry(TimeEntryUpsertRequest body, long customerId, TimeEntryRecord existing) {
        return new TimeEntryRecord(
                existing == null ? 0 : existing.id(),
                customerId,
                requireId(body.activityId(), "Aktivitet"),
                requireDate(body.entryDate(), "Dato"),
                scaleOne(body.hours()),
                optionalText(body.note()),
                false,
                existing == null ? null : existing.createdAt(),
                existing == null ? null : existing.updatedAt(),
                existing == null ? null : existing.activityShortDescription(),
                existing == null ? null : existing.activityLongDescription()
        );
    }

    private MaterialEntryRecord normalizeMaterial(MaterialEntryUpsertRequest body, long customerId, MaterialEntryRecord existing) {
        return new MaterialEntryRecord(
                existing == null ? 0 : existing.id(),
                customerId,
                requireDate(body.entryDate(), "Dato"),
                body.quantity() == null ? BigDecimal.ZERO : body.quantity().setScale(2, RoundingMode.HALF_UP),
                requiredText(body.unit(), "Enhed"),
                requiredText(body.shortDescription(), "Beskrivelse"),
                moneyScale(body.unitPrice() == null ? BigDecimal.ZERO : body.unitPrice()),
                false,
                existing == null ? null : existing.createdAt(),
                existing == null ? null : existing.updatedAt()
        );
    }

    private long requireId(Long value, String label) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value;
    }

    private LocalDate requireDate(LocalDate value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value;
    }

    private String requiredText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private void validateHalfHour(BigDecimal hours) {
        if (hours == null) {
            throw new IllegalArgumentException("Hours are required");
        }
        BigDecimal scaled = hours.multiply(new BigDecimal("2"));
        if (scaled.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Hours must be in 0.5 hour increments");
        }
    }

    private void validateActivityForCustomer(long customerId, Long activityId) throws Exception {
        validateActivityForCustomer(customerId, activityId, null);
    }

    private void validateActivityForCustomer(long customerId, Long activityId, Long allowInactiveActivityId) throws Exception {
        if (activityId == null) {
            throw new IllegalArgumentException("Activity is required");
        }
        ActivityRecord activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        if (activity.customerId() != customerId) {
            throw new IllegalArgumentException("The activity does not belong to the selected customer");
        }
        if (activity.inactive() && (allowInactiveActivityId == null || allowInactiveActivityId.longValue() != activityId.longValue())) {
            throw new IllegalArgumentException("The selected activity is inactive");
        }
    }

    private BigDecimal scaleOne(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyScale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    private long idFromPath(String path, String prefix) {
        String tail = path.substring(prefix.length());
        if (tail.contains("/")) {
            tail = tail.substring(0, tail.indexOf('/'));
        }
        return Long.parseLong(tail);
    }

    private long requiredLong(HttpServletRequest request, String parameter) {
        String value = request.getParameter(parameter);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parameter is required: " + parameter);
        }
        return Long.parseLong(value);
    }

    private int requiredInt(HttpServletRequest request, String parameter) {
        String value = request.getParameter(parameter);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parameter is required: " + parameter);
        }
        return Integer.parseInt(value);
    }

    private Integer optionalInt(HttpServletRequest request, String parameter) {
        String value = request.getParameter(parameter);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private Long selectedCustomerId(HttpServletRequest request) {
        Object value = request.getSession(true).getAttribute("selectedCustomerId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private <T> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        return JsonSupport.mapper().readValue(request.getInputStream(), type);
    }

    private void writeJson(HttpServletResponse response, Object body) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        JsonSupport.mapper().writeValue(response.getOutputStream(), body);
    }

    private void sendNotFound(HttpServletResponse response) throws IOException {
        sendError(response, HttpServletResponse.SC_NOT_FOUND, "Not found");
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        JsonSupport.mapper().writeValue(response.getOutputStream(), Map.of("error", message == null ? "Unknown error" : message));
    }

}
