package com.bepa.eis.server.api.web.application.admin.subscriptioneditor;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.dto.customer.SubscriptionPlanBillingPeriod;
import com.bepa.eis.common.enums.customer.BillingPeriod;
import com.bepa.eis.common.enums.customer.Subscription;
import com.bepa.eis.common.providers.EisDataSourceProvider;
import com.bepa.eis.common.providers.customer.SubscriptionPlanBillingPeriodProvider;
import com.bepa.eis.common.providers.customer.SubscriptionPlanProvider;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@WebServlet(name = "SubscriptionEditorServlet", urlPatterns = {"/api/admin/subscription-editor"})
public class SubscriptionEditorServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEditorServlet.class);

    private static final String INSERT_PLAN_SQL =
            "INSERT INTO dbo.SUBSCRIPTION_PLAN (ModuleCode, ModuleName, PlanName, Description, ValidFrom, ValidTo, PriceAmount, Currency, BillingPeriodMonths, TrialDays, Active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_PLAN_SQL =
            "UPDATE dbo.SUBSCRIPTION_PLAN " +
                    "SET ModuleCode = ?, ModuleName = ?, PlanName = ?, Description = ?, ValidFrom = ?, ValidTo = ?, PriceAmount = ?, Currency = ?, BillingPeriodMonths = ?, TrialDays = ?, Active = ?, UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE SubscriptionPlanId = ?";

    private static final String INSERT_BILLING_PERIOD_SQL =
            "INSERT INTO dbo.SUBSCRIPTION_PLAN_BILLING_PERIOD (SubscriptionPlanId, BillingPeriodCode, BillingPeriodName, Description, BillingPeriodMonths, PriceAmount, Currency, Active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DELETE_BILLING_PERIODS_SQL =
            "DELETE FROM dbo.SUBSCRIPTION_PLAN_BILLING_PERIOD WHERE SubscriptionPlanId = ?";

    private static final String SELECT_CUSTOMERS_SQL =
            """
            SELECT
                C.CustomerId,
                C.CustomerName,
                CS.SubscriptionId,
                CS.SubscriptionStatus,
                COALESCE(CS.SubscriptionPlanId, CM.SubscriptionPlanId) AS SubscriptionPlanId,
                COALESCE(SP.ModuleName, CM.ModuleName, CS.SubscriptionPlanName) AS SubscriptionPlanName,
                CS.PeriodEndAt,
                CS.TrialEndAt,
                CS.GracePeriodEndsAt
            FROM dbo.CUSTOMER C
            INNER JOIN dbo.CUSTOMER_MODULE CM
                ON CM.CustomerId = C.CustomerId
               AND CM.Latest = 1
            LEFT JOIN dbo.SUBSCRIPTION_PLAN SP
                ON SP.SubscriptionPlanId = CM.SubscriptionPlanId
            OUTER APPLY (
                SELECT TOP (1)
                    S.SubscriptionId,
                    S.SubscriptionStatus,
                    S.SubscriptionPlanId,
                    S.SubscriptionPlanName,
                    S.PeriodEndAt,
                    S.TrialEndAt,
                    S.GracePeriodEndsAt
                FROM dbo.CUSTOMER_SUBSCRIPTION S
                WHERE S.CustomerId = C.CustomerId
                ORDER BY S.SubscriptionId DESC
            ) CS
            WHERE C.Latest = 1
              AND CM.ModuleCode = ?
            ORDER BY C.CustomerName ASC, C.CustomerId ASC
            """;

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Subscription editor import is not supported.");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) throws Exception {
        SubscriptionEditorSaveRequest saveRequest = parseSaveRequest(rootElement);
        saveSubscriptionPlan(webSession, saveRequest);
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        return buildDocument(webSession, null, null);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) {
        return buildDocument(webSession, entityId, null);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) {
        return buildDocument(webSession, null, request.getParameter("moduleCode"));
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Subscription editor export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        return handleListOfEntities(webSession, request, response);
    }

    private void saveSubscriptionPlan(
            WebSession webSession,
            SubscriptionEditorSaveRequest saveRequest
    ) throws SQLException {
        if (saveRequest == null || saveRequest.plan() == null) {
            throw new IllegalArgumentException("Subscription plan data is required.");
        }

        SubscriptionPlanProvider planProvider = new SubscriptionPlanProvider(webSession);
        SubscriptionPlanBillingPeriodProvider billingPeriodProvider = new SubscriptionPlanBillingPeriodProvider(webSession);

        SubscriptionPlan plan = saveRequest.plan();
        String moduleCode = normalizeModuleCode(plan.getModuleCode());

        if (!isKnownSubscriptionModule(moduleCode)) {
            throw new IllegalArgumentException("Invalid subscription module.");
        }

        if (plan.getValidFrom() == null) {
            throw new IllegalArgumentException("Valid from is required.");
        }

        if (plan.getValidTo() != null && plan.getValidTo().isBefore(plan.getValidFrom())) {
            throw new IllegalArgumentException("Valid to cannot be earlier than valid from.");
        }

        if (planProvider.hasOverlappingPlan(moduleCode, plan.getSubscriptionPlanId(), plan.getValidFrom(), plan.getValidTo())) {
            throw new IllegalArgumentException("The validity period overlaps with another subscription plan of the same type.");
        }

        applyHiddenPlanDefaults(planProvider, plan);

        List<SubscriptionPlanBillingPeriod> billingPeriods = normalizeBillingPeriods(
                plan.getSubscriptionPlanId(),
                saveRequest.billingPeriods()
        );

        plan.setModuleCode(moduleCode);
        plan.setModuleName(resolveModuleName(moduleCode));

        Connection connection = EisDataSourceProvider.getDataSource().getConnection();

        try {
            connection.setAutoCommit(false);

            Integer planId = plan.getSubscriptionPlanId();
            if (planId == null) {
                planId = insertPlan(connection, plan);
            } else {
                updatePlan(connection, plan);
            }

            if (planId == null) {
                throw new IllegalStateException("Subscription plan could not be saved.");
            }

            deleteBillingPeriods(connection, planId);
            insertBillingPeriods(connection, planId, billingPeriods);

            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    private Integer insertPlan(Connection connection, SubscriptionPlan plan) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_PLAN_SQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            setPlanParameters(statement, plan);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    plan.setSubscriptionPlanId(id);
                    return id;
                }
            }
        }

        throw new SQLException("Could not read generated subscription plan id.");
    }

    private void updatePlan(Connection connection, SubscriptionPlan plan) throws SQLException {
        if (plan.getSubscriptionPlanId() == null) {
            throw new IllegalArgumentException("SubscriptionPlanId is required for update.");
        }

        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PLAN_SQL)) {
            setPlanParameters(statement, plan);
            statement.setInt(12, plan.getSubscriptionPlanId());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Subscription plan not found.");
            }
        }
    }

    private void setPlanParameters(PreparedStatement statement, SubscriptionPlan plan) throws SQLException {
        statement.setString(1, safeText(plan.getModuleCode(), ""));
        statement.setString(2, safeText(plan.getModuleName(), ""));
        statement.setString(3, safeText(plan.getPlanName(), ""));
        statement.setString(4, safeText(plan.getDescription(), ""));
        statement.setDate(5, java.sql.Date.valueOf(plan.getValidFrom()));

        if (plan.getValidTo() == null) {
            statement.setNull(6, java.sql.Types.DATE);
        } else {
            statement.setDate(6, java.sql.Date.valueOf(plan.getValidTo()));
        }

        statement.setBigDecimal(7, plan.getPriceAmount());
        statement.setString(8, safeText(plan.getCurrency(), "EUR"));
        statement.setInt(9, plan.getBillingPeriodMonths());
        statement.setInt(10, plan.getTrialDays());
        statement.setBoolean(11, plan.getActive());
    }

    private void deleteBillingPeriods(Connection connection, Integer subscriptionPlanId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BILLING_PERIODS_SQL)) {
            statement.setInt(1, subscriptionPlanId);
            statement.executeUpdate();
        }
    }

    private void insertBillingPeriods(
            Connection connection,
            Integer subscriptionPlanId,
            List<SubscriptionPlanBillingPeriod> billingPeriods
    ) throws SQLException {
        for (SubscriptionPlanBillingPeriod billingPeriod : billingPeriods) {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_BILLING_PERIOD_SQL)) {
                statement.setInt(1, subscriptionPlanId);
                statement.setString(2, safeText(billingPeriod.getBillingPeriodCode(), ""));
                statement.setString(3, safeText(billingPeriod.getBillingPeriodName(), ""));
                statement.setString(4, safeText(billingPeriod.getDescription(), ""));
                statement.setInt(5, billingPeriod.getBillingPeriodMonths());
                statement.setBigDecimal(6, billingPeriod.getPriceAmount());
                statement.setString(7, safeText(billingPeriod.getCurrency(), "EUR"));
                statement.setBoolean(8, billingPeriod.getActive());
                statement.executeUpdate();
            }
        }
    }

    private SubscriptionEditorSaveRequest parseSaveRequest(Element rootElement) {
        Element planElement = firstChild(rootElement, "subscriptionPlan");
        if (planElement == null && "subscriptionPlan".equalsIgnoreCase(rootElement.getTagName())) {
            planElement = rootElement;
        }

        if (planElement == null) {
            throw new IllegalArgumentException("Subscription plan data is required.");
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        Integer planId = intValue(planElement, "SubscriptionPlanId");
        if (planId != null) {
            plan.setSubscriptionPlanId(planId);
        }

        plan.setModuleCode(textValue(planElement, "ModuleCode"));
        plan.setModuleName(textValue(planElement, "ModuleName"));
        plan.setPlanName(textValue(planElement, "PlanName"));
        plan.setDescription(textValue(planElement, "Description"));
        plan.setValidFrom(parseLocalDate(textValue(planElement, "ValidFrom")));
        plan.setValidTo(parseLocalDate(textValue(planElement, "ValidTo")));
        plan.setActive(boolValue(planElement, "Active"));

        List<SubscriptionPlanBillingPeriod> billingPeriods = new ArrayList<>();
        Element billingPeriodsElement = firstChild(rootElement, "billingPeriods");
        if (billingPeriodsElement != null) {
            for (Element billingPeriodElement : children(billingPeriodsElement, "billingPeriod")) {
                SubscriptionPlanBillingPeriod billingPeriod = new SubscriptionPlanBillingPeriod();
                Integer billingPeriodId = intValue(billingPeriodElement, "SubscriptionPlanBillingPeriodId");
                if (billingPeriodId != null) {
                    billingPeriod.setSubscriptionPlanBillingPeriodId(billingPeriodId);
                }
                billingPeriod.setBillingPeriodCode(textValue(billingPeriodElement, "BillingPeriodCode"));
                billingPeriod.setBillingPeriodName(textValue(billingPeriodElement, "BillingPeriodName"));
                billingPeriod.setDescription(textValue(billingPeriodElement, "Description"));
                billingPeriod.setPriceAmount(parseBigDecimal(textValue(billingPeriodElement, "PriceAmount")));
                billingPeriod.setCurrency(textValue(billingPeriodElement, "Currency"));
                billingPeriod.setActive(boolValue(billingPeriodElement, "Active"));
                billingPeriods.add(billingPeriod);
            }
        }

        return new SubscriptionEditorSaveRequest(plan, billingPeriods);
    }

    private GenericXmlDocument buildDocument(
            WebSession webSession,
            Integer subscriptionPlanId,
            String moduleCode
    ) {
        SubscriptionPlanProvider planProvider = new SubscriptionPlanProvider(webSession);
        SubscriptionPlanBillingPeriodProvider billingPeriodProvider = new SubscriptionPlanBillingPeriodProvider(webSession);

        SubscriptionEditorXmlDocument xmlDocument = new SubscriptionEditorXmlDocument(webSession, "subscriptionEditor");
        Element root = xmlDocument.root();

        appendTopPanel(xmlDocument, root, webSession);
        appendLookups(xmlDocument, root);
        appendPlanList(xmlDocument, root, planProvider.getAllPlans());

        SubscriptionPlan selectedPlan = null;
        List<SubscriptionPlanBillingPeriod> billingPeriods = List.of();
        List<SubscriptionCustomerRow> customers = List.of();

        if (subscriptionPlanId != null) {
            selectedPlan = planProvider.getPlanById(subscriptionPlanId);
            if (selectedPlan != null) {
                billingPeriods = billingPeriodProvider.getBillingPeriodsByPlanId(subscriptionPlanId);
                customers = loadCustomersForSubscription(moduleCodeFor(selectedPlan));
            }
        } else if (moduleCode != null && !moduleCode.isBlank()) {
            selectedPlan = buildBlankPlan(moduleCode);
            billingPeriods = buildBlankBillingPeriods();
            customers = loadCustomersForSubscription(normalizeModuleCode(moduleCode));
        }

        if (selectedPlan != null) {
            appendPlanDetail(xmlDocument, root, selectedPlan, billingPeriods, customers);
        }

        return xmlDocument;
    }

    private void appendTopPanel(
            SubscriptionEditorXmlDocument xmlDocument,
            Element parent,
            WebSession webSession
    ) {
        Element topPanelElement = xmlDocument.appendElement(parent, "TopPanel");

        if (webSession == null) {
            return;
        }

        try {
            TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
            TopPanel topPanel = topPanelProvider.getTopPanelBySession();

            if (topPanel != null && topPanel.getTopPanelElements() != null) {
                for (com.bepa.eis.server.dataprovider.fields.AbstractField field : topPanel.getTopPanelElements().getElements()) {
                    if (field != null && field.getFieldName() != null && !field.getFieldName().isBlank()) {
                        xmlDocument.appendTextElement(topPanelElement, field.getFieldName(), field.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to append top panel for subscription editor page", e);
        }
    }

    private void appendLookups(SubscriptionEditorXmlDocument xmlDocument, Element parent) {
        Element lookupsElement = xmlDocument.appendElement(parent, "lookups");

        Element subscriptionsLookup = xmlDocument.appendElement(lookupsElement, "lookup");
        subscriptionsLookup.setAttribute("name", "subscriptions");
        for (Subscription subscription : Subscription.values()) {
            Element option = xmlDocument.appendElement(subscriptionsLookup, "option");
            option.setAttribute("code", subscription.getModuleCode());
            option.setAttribute("label", subscription.getLabel());
            option.setAttribute("subscriptionCode", subscription.getCode());
            option.setAttribute("moduleCode", subscription.getModuleCode());
            option.setAttribute("moduleName", subscription.getLabel());
            option.setAttribute("active", String.valueOf(subscription.isActive()));
        }

        Element billingPeriodsLookup = xmlDocument.appendElement(lookupsElement, "lookup");
        billingPeriodsLookup.setAttribute("name", "billingPeriods");
        for (BillingPeriod billingPeriod : BillingPeriod.values()) {
            Element option = xmlDocument.appendElement(billingPeriodsLookup, "option");
            option.setAttribute("code", billingPeriod.getCode());
            option.setAttribute("label", billingPeriod.getLabel());
            option.setAttribute("description", billingPeriod.getDescription());
            option.setAttribute("months", String.valueOf(billingPeriod.getMonths()));
            option.setAttribute("active", String.valueOf(billingPeriod.isActive()));
        }
    }

    private void appendPlanList(
            SubscriptionEditorXmlDocument xmlDocument,
            Element parent,
            List<SubscriptionPlan> plans
    ) {
        Element plansElement = xmlDocument.appendElement(parent, "subscriptionPlans");

        for (SubscriptionPlan plan : plans) {
            appendPlanRow(xmlDocument, plansElement, plan);
        }
    }

    private void appendPlanRow(
            SubscriptionEditorXmlDocument xmlDocument,
            Element parent,
            SubscriptionPlan plan
    ) {
        if (plan == null) {
            return;
        }

        Element planElement = xmlDocument.appendElement(parent, "subscriptionPlan");
        xmlDocument.appendTextElement(planElement, "SubscriptionPlanId", plan.getSubscriptionPlanId());
        xmlDocument.appendTextElement(planElement, "ModuleCode", plan.getModuleCode());
        xmlDocument.appendTextElement(planElement, "ModuleName", plan.getModuleName());
        xmlDocument.appendTextElement(planElement, "Description", plan.getDescription());
        xmlDocument.appendTextElement(planElement, "ValidFrom", plan.getValidFrom());
        xmlDocument.appendTextElement(planElement, "ValidTo", plan.getValidTo());
        xmlDocument.appendTextElement(planElement, "BillingPeriodMonths", plan.getBillingPeriodMonths());
        xmlDocument.appendTextElement(planElement, "TrialDays", plan.getTrialDays());
        xmlDocument.appendTextElement(planElement, "Active", plan.getActive());
        xmlDocument.appendTextElement(planElement, "CreatedAt", plan.getCreatedAt());
        xmlDocument.appendTextElement(planElement, "UpdatedAt", plan.getUpdatedAt());
        xmlDocument.appendTextElement(planElement, "DisplayName", plan.getDisplayName());
        xmlDocument.appendTextElement(planElement, "IsCurrent", isCurrent(plan));
    }

    private void appendPlanDetail(
            SubscriptionEditorXmlDocument xmlDocument,
            Element parent,
            SubscriptionPlan plan,
            List<SubscriptionPlanBillingPeriod> billingPeriods,
            List<SubscriptionCustomerRow> customers
    ) {
        Element detailElement = xmlDocument.appendElement(parent, "subscriptionPlanDetail");
        appendPlanRow(xmlDocument, detailElement, plan);

        Element billingPeriodsElement = xmlDocument.appendElement(detailElement, "billingPeriods");
        for (SubscriptionPlanBillingPeriod billingPeriod : billingPeriods) {
            appendBillingPeriod(xmlDocument, billingPeriodsElement, billingPeriod);
        }

        Element customersElement = xmlDocument.appendElement(detailElement, "customers");
        for (SubscriptionCustomerRow customer : customers) {
            appendCustomerRow(xmlDocument, customersElement, customer);
        }
    }

    private void appendBillingPeriod(
            SubscriptionEditorXmlDocument xmlDocument,
            Element parent,
            SubscriptionPlanBillingPeriod billingPeriod
    ) {
        if (billingPeriod == null) {
            return;
        }

        Element billingPeriodElement = xmlDocument.appendElement(parent, "billingPeriod");
        xmlDocument.appendTextElement(billingPeriodElement, "SubscriptionPlanBillingPeriodId", billingPeriod.getSubscriptionPlanBillingPeriodId());
        xmlDocument.appendTextElement(billingPeriodElement, "SubscriptionPlanId", billingPeriod.getSubscriptionPlanId());
        xmlDocument.appendTextElement(billingPeriodElement, "BillingPeriodCode", billingPeriod.getBillingPeriodCode());
        xmlDocument.appendTextElement(billingPeriodElement, "BillingPeriodName", billingPeriod.getBillingPeriodName());
        xmlDocument.appendTextElement(billingPeriodElement, "Description", billingPeriod.getDescription());
        xmlDocument.appendTextElement(billingPeriodElement, "BillingPeriodMonths", billingPeriod.getBillingPeriodMonths());
        xmlDocument.appendTextElement(billingPeriodElement, "PriceAmount", billingPeriod.getPriceAmount());
        xmlDocument.appendTextElement(billingPeriodElement, "Currency", billingPeriod.getCurrency());
        xmlDocument.appendTextElement(billingPeriodElement, "Active", billingPeriod.getActive());
        xmlDocument.appendTextElement(billingPeriodElement, "CreatedAt", billingPeriod.getCreatedAt());
        xmlDocument.appendTextElement(billingPeriodElement, "UpdatedAt", billingPeriod.getUpdatedAt());
    }

    private void appendCustomerRow(
            SubscriptionEditorXmlDocument xmlDocument,
            Element parent,
            SubscriptionCustomerRow customer
    ) {
        if (customer == null) {
            return;
        }

        Element customerElement = xmlDocument.appendElement(parent, "customer");
        xmlDocument.appendTextElement(customerElement, "CustomerId", customer.customerId());
        xmlDocument.appendTextElement(customerElement, "CustomerName", customer.customerName());
        xmlDocument.appendTextElement(customerElement, "SubscriptionId", customer.subscriptionId());
        xmlDocument.appendTextElement(customerElement, "SubscriptionStatus", customer.subscriptionStatus());
        xmlDocument.appendTextElement(customerElement, "SubscriptionPlanId", customer.subscriptionPlanId());
        xmlDocument.appendTextElement(customerElement, "SubscriptionPlanName", customer.subscriptionPlanName());
        xmlDocument.appendTextElement(customerElement, "RenewalAt", customer.renewalAt());
        xmlDocument.appendTextElement(customerElement, "PeriodEndAt", customer.periodEndAt());
        xmlDocument.appendTextElement(customerElement, "TrialEndAt", customer.trialEndAt());
        xmlDocument.appendTextElement(customerElement, "GracePeriodEndsAt", customer.gracePeriodEndsAt());
    }

    private SubscriptionPlan buildBlankPlan(String moduleCode) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setModuleCode(moduleCode);
        plan.setModuleName(resolveModuleName(moduleCode));
        plan.setDescription("");
        plan.setValidFrom(LocalDate.now());
        plan.setValidTo(null);
        plan.setActive(true);
        return plan;
    }

    private List<SubscriptionPlanBillingPeriod> buildBlankBillingPeriods() {
        List<SubscriptionPlanBillingPeriod> periods = new ArrayList<>();
        for (BillingPeriod billingPeriod : BillingPeriod.values()) {
            SubscriptionPlanBillingPeriod row = new SubscriptionPlanBillingPeriod();
            row.setBillingPeriodCode(billingPeriod.getCode());
            row.setBillingPeriodName(billingPeriod.getLabel());
            row.setDescription(billingPeriod.getDescription());
            row.setBillingPeriodMonths(billingPeriod.getMonths());
            row.setPriceAmount(BigDecimal.ZERO);
            row.setCurrency("EUR");
            row.setActive(billingPeriod.isActive());
            periods.add(row);
        }
        return periods;
    }

    private List<SubscriptionPlanBillingPeriod> normalizeBillingPeriods(
            Integer subscriptionPlanId,
            List<SubscriptionPlanBillingPeriod> billingPeriods
    ) {
        Map<String, SubscriptionPlanBillingPeriod> byCode = new HashMap<>();
        if (billingPeriods != null) {
            for (SubscriptionPlanBillingPeriod billingPeriod : billingPeriods) {
                if (billingPeriod == null || billingPeriod.getBillingPeriodCode() == null) {
                    continue;
                }
                byCode.put(normalizeModuleCode(billingPeriod.getBillingPeriodCode()), billingPeriod);
            }
        }

        List<SubscriptionPlanBillingPeriod> normalized = new ArrayList<>();
        for (BillingPeriod billingPeriod : BillingPeriod.values()) {
            SubscriptionPlanBillingPeriod row = byCode.get(billingPeriod.getCode());
            if (row == null) {
                row = new SubscriptionPlanBillingPeriod();
                row.setPriceAmount(BigDecimal.ZERO);
                row.setCurrency("EUR");
                row.setActive(billingPeriod.isActive());
            }

            row.setSubscriptionPlanId(subscriptionPlanId);
            row.setBillingPeriodCode(billingPeriod.getCode());
            row.setBillingPeriodName(resolveText(row.getBillingPeriodName(), billingPeriod.getLabel()));
            row.setDescription(resolveText(row.getDescription(), billingPeriod.getDescription()));
            row.setBillingPeriodMonths(billingPeriod.getMonths());
            row.setCurrency(resolveText(row.getCurrency(), "EUR"));
            normalized.add(row);
        }

        return normalized;
    }

    private boolean isKnownSubscriptionModule(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return false;
        }

        for (Subscription subscription : Subscription.values()) {
            if (subscription.getModuleCode().equalsIgnoreCase(moduleCode)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeModuleCode(String moduleCode) {
        return moduleCode == null ? "" : moduleCode.trim().toUpperCase(Locale.ROOT);
    }

    private String moduleCodeFor(SubscriptionPlan plan) {
        return plan == null ? "" : normalizeModuleCode(plan.getModuleCode());
    }

    private String resolveModuleName(String moduleCode) {
        if (moduleCode == null) {
            return "";
        }

        for (Subscription subscription : Subscription.values()) {
            if (subscription.getModuleCode().equalsIgnoreCase(moduleCode)) {
                return subscription.getLabel();
            }
        }

        return moduleCode;
    }

    private void applyHiddenPlanDefaults(
            SubscriptionPlanProvider planProvider,
            SubscriptionPlan plan
    ) {
        if (plan == null) {
            return;
        }

        SubscriptionPlan existingPlan = null;
        if (plan.getSubscriptionPlanId() != null) {
            existingPlan = planProvider.getPlanById(plan.getSubscriptionPlanId());
        }

        if (existingPlan != null) {
            if (plan.getPlanName() == null || plan.getPlanName().isBlank()) {
                plan.setPlanName(existingPlan.getPlanName());
            }

            plan.setPriceAmount(existingPlan.getPriceAmount());
            plan.setCurrency(existingPlan.getCurrency());
            plan.setBillingPeriodMonths(existingPlan.getBillingPeriodMonths());
            plan.setTrialDays(existingPlan.getTrialDays());
            return;
        }

        if (plan.getPlanName() == null || plan.getPlanName().isBlank()) {
            plan.setPlanName("Standard");
        }

        if (plan.getCurrency() == null || plan.getCurrency().isBlank()) {
            plan.setCurrency("EUR");
        }
    }

    private boolean isCurrent(SubscriptionPlan plan) {
        if (plan == null || plan.getValidFrom() == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (plan.getValidFrom().isAfter(today)) {
            return false;
        }

        return plan.getValidTo() == null || !plan.getValidTo().isBefore(today);
    }

    private String resolveText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private List<SubscriptionCustomerRow> loadCustomersForSubscription(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return List.of();
        }

        List<SubscriptionCustomerRow> rows = new ArrayList<>();

        try (Connection connection = EisDataSourceProvider.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_CUSTOMERS_SQL)) {

            statement.setString(1, moduleCode.trim().toUpperCase(Locale.ROOT));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new SubscriptionCustomerRow(
                            resultSet.getInt("CustomerId"),
                            resultSet.getString("CustomerName"),
                            nullableInt(resultSet, "SubscriptionId"),
                            resultSet.getString("SubscriptionStatus"),
                            nullableInt(resultSet, "SubscriptionPlanId"),
                            resultSet.getString("SubscriptionPlanName"),
                            timestampToString(resultSet, "PeriodEndAt"),
                            timestampToString(resultSet, "TrialEndAt"),
                            timestampToString(resultSet, "GracePeriodEndsAt"),
                            resolveRenewalAt(resultSet)
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customers for subscription moduleCode={}", moduleCode, e);
            return List.of();
        }

        return rows;
    }

    private Integer nullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private String timestampToString(ResultSet resultSet, String columnName) throws SQLException {
        java.sql.Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString();
    }

    private String resolveRenewalAt(ResultSet resultSet) throws SQLException {
        java.sql.Timestamp periodEnd = resultSet.getTimestamp("PeriodEndAt");
        java.sql.Timestamp trialEnd = resultSet.getTimestamp("TrialEndAt");
        java.sql.Timestamp graceEnd = resultSet.getTimestamp("GracePeriodEndsAt");

        java.sql.Timestamp candidate = periodEnd != null ? periodEnd : trialEnd != null ? trialEnd : graceEnd;
        return candidate == null ? "" : candidate.toLocalDateTime().toString();
    }

    private void writeError(String message) {
        throw new IllegalArgumentException(message);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private record SubscriptionEditorSaveRequest(
            SubscriptionPlan plan,
            List<SubscriptionPlanBillingPeriod> billingPeriods
    ) {
    }

    private record SubscriptionCustomerRow(
            Integer customerId,
            String customerName,
            Integer subscriptionId,
            String subscriptionStatus,
            Integer subscriptionPlanId,
            String subscriptionPlanName,
            String periodEndAt,
            String trialEndAt,
            String gracePeriodEndsAt,
            String renewalAt
    ) {
    }
}
