package com.bepa.eis.server.api.web.application.views.users;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.common.providers.customer.CustomerRecordProvider;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.fields.booleans.AbstractBoolean;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.lookups.CustomerDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.UserId;
import com.bepa.eis.server.dataprovider.fields.lookups.common.UserRole;
import com.bepa.eis.server.dataprovider.fields.strings.AbstractString;
import com.bepa.eis.server.dataprovider.fields.strings.UserInitials;
import com.bepa.eis.server.dataprovider.fields.strings.UserName;
import com.bepa.eis.server.dataprovider.fields.strings.email.UserEmail;
import com.bepa.eis.server.dataprovider.fields.strings.phone.UserPhone;
import com.bepa.eis.server.dataprovider.fields.timestamp.AbstractDateTime;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static com.bepa.eis.server.api.web.application.enums.EntityRequestType.CREATE_ENTITY;

public class UserMainInfo extends GenericXmlDocument {

    private final Integer userId;
    private final EntityRequestType requestType;
    private final ListOfElements rootElement;

    public UserMainInfo(
            WebSession webSession,
            EntityRequestType requestType,
            Integer userId
    ) throws Exception {
        super(webSession);

        this.requestType = requestType;
        this.userId = userId;

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        appendTopPanel(webSession);
        appendUserDocument(resolveUserRecord(webSession));
        appendLookups();
    }

    private void appendTopPanel(WebSession webSession) throws Exception {
        if (webSession == null) {
            return;
        }

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        TopPanel topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());
    }

    private UserProvider.UserAdministrationRow resolveUserRecord(WebSession webSession) throws SQLException {
        if (requestType == CREATE_ENTITY) {
            String customerName = resolveCurrentCustomerName(webSession);

            return new UserProvider.UserAdministrationRow(
                    null,
                    "",
                    "",
                    "",
                    "",
                    null,
                    true,
                    UserRoles.CUSTOMER_ADMINISTRATOR,
                    null,
                    false,
                    false,
                    "",
                    "DEFAULT",
                    false,
                    null,
                    null,
                    "",
                    null,
                    "",
                    "",
                    customerName
            );
        }

        UserProvider userProvider = new UserProvider(webSession);
        return userProvider.getUserAdministrationRow(userId);
    }

    private String resolveCurrentCustomerName(WebSession webSession) {
        if (webSession == null || webSession.getCustomerId() == null) {
            return "";
        }

        try {
            CustomerRecordProvider customerRecordProvider = new CustomerRecordProvider(webSession);
            CustomerRecord customer = customerRecordProvider.getLatestCustomerByCustomerId(webSession.getCustomerId());

            return customer == null ? "" : safeText(customer.getCustomerName(), "");
        } catch (Exception ignored) {
            return "";
        }
    }

    private void appendUserDocument(UserProvider.UserAdministrationRow userDetail) throws SQLException {
        WebSession webSession = getWebSession();

        ListOfElements userDocument = new ListOfElements(webSession, "userDocument");
        ListOfElements userElements = new ListOfElements(webSession, "user");

        addUserFields(webSession, userElements, userDetail);

        userDocument.addElement(userElements);
        rootElement.addElement(userDocument);
    }

    private void appendLookups() {
        WebSession webSession = getWebSession();
        ListOfElements lookups = new ListOfElements(webSession, "lookups");
        ListOfElements countryCodeLookup = new ListOfElements(webSession, "lookup");
        countryCodeLookup.addAttribute("name", "countryCode");

        for (CustomerLookupCache.PhoneCountryRule rule : CustomerLookupCache.getPhoneCountryRules()) {
            countryCodeLookup.addElement(phoneCountryOption(webSession, rule));
        }

        lookups.addElement(countryCodeLookup);
        rootElement.addElement(lookups);
    }

    private ListOfElements phoneCountryOption(
            WebSession webSession,
            CustomerLookupCache.PhoneCountryRule rule
    ) {
        ListOfElements option = new ListOfElements(webSession, "option");
        option.addAttribute("code", safeText(rule.code(), ""));
        option.addAttribute("country", safeText(rule.country(), ""));
        option.addAttribute("label", safeText(rule.country(), ""));
        option.addAttribute("min", String.valueOf(rule.minDigits()));
        option.addAttribute("max", String.valueOf(rule.maxDigits()));
        option.addAttribute("example", safeText(rule.example(), ""));
        return option;
    }

    private void addUserFields(
            WebSession webSession,
            ListOfElements userElements,
            UserProvider.UserAdministrationRow userDetail
    )  {
        UserProvider.UserAdministrationRow safeUser = userDetail == null
                ? createEmptyUser(webSession)
                : userDetail;


        UserId userId = new UserId(safeUser.userId());
        userId.setFieldNotVisible();
        userElements.addElement(userId);

        CustomerId customerId = new CustomerId(webSession.getCustomerId());
        customerId.setFieldNotVisible();
        userElements.addElement(customerId);

        UserName userName = new UserName(safeUser.name());
        userName.setFieldEditable();
        userName.setFieldRequired();
        userElements.addElement(userName);

        UserInitials userInitials = new UserInitials(safeUser.initials());
        userInitials.setFieldEditable();
        userInitials.setFieldRequired();
        userElements.addElement(userInitials);

        UserEmail userEmail = new UserEmail();
        userEmail.setValue(safeUser.email());
        userEmail.setFieldEditable();
        userEmail.setFieldRequired();
        userElements.addElement(userEmail);

        UserPhone userPhone = new UserPhone();
        userPhone.setValue(safeUser.phone());
        userPhone.setFieldEditable();
        userPhone.setFieldRequired();
        userElements.addElement(userPhone);

        CustomerDepartment department = new CustomerDepartment(webSession);
        department.setValue(safeUser.departmentId());
        department.setFieldEditable();
        department.setFieldRequired();
        userElements.addElement(department);

        Integer roleId = safeUser.userRole() == null ? UserRoles.CUSTOMER_ADMINISTRATOR.getId() : safeUser.userRole().getId();
        UserRole userRole = new UserRole(webSession, roleId);
        userRole.setFieldEditable();
        userRole.setFieldRequired();
        userElements.addElement(userRole);

        Active active = new Active(safeUser.active());
        active.setFieldEditable();
        userElements.addElement(active);
    }

    private UserProvider.UserAdministrationRow createEmptyUser(WebSession webSession) {
        return new UserProvider.UserAdministrationRow(
                null,
                "",
                "",
                "",
                "",
                null,
                true,
                UserRoles.CUSTOMER_ADMINISTRATOR,
                null,
                false,
                false,
                "",
                "DEFAULT",
                false,
                null,
                null,
                "",
                null,
                "",
                "",
                resolveCurrentCustomerName(webSession)
        );
    }

    private AbstractField textField(
            String fieldName,
            String label,
            String header,
            Object value,
            Integer minLength,
            Integer maxLength,
            Integer displayLength,
            boolean required,
            boolean editable,
            boolean visible
    ) {
        return new NamedTextField(
                fieldName,
                label,
                header,
                value == null ? "" : String.valueOf(value),
                minLength,
                maxLength,
                displayLength,
                required,
                editable,
                visible,
                false
        );
    }

    private AbstractField booleanField(
            String fieldName,
            String label,
            String header,
            boolean value,
            boolean editable,
            boolean visible
    ) {
        return new NamedBooleanField(fieldName, label, header, value, editable, visible);
    }

    private AbstractField dateTimeField(
            String fieldName,
            String label,
            String header,
            Timestamp value,
            boolean editable,
            boolean visible
    ) {
        return new NamedDateTimeField(fieldName, label, header, value, editable, visible);
    }

    private static final class NamedTextField extends AbstractString {
        private final String fieldName;
        private final String label;
        private final String header;
        private final Integer minLength;
        private final Integer maxLength;
        private final Integer displayLength;

        private NamedTextField(
                String fieldName,
                String label,
                String header,
                String value,
                Integer minLength,
                Integer maxLength,
                Integer displayLength,
                boolean required,
                boolean editable,
                boolean visible,
                boolean hidden
        ) {
            this.fieldName = fieldName;
            this.label = label;
            this.header = header;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.displayLength = displayLength;
            setValue(value);

            if (required) {
                setFieldRequired();
            } else {
                setFieldNotRequired();
            }

            if (editable) {
                setFieldEditable();
            } else {
                setFieldNotEditable();
            }

            if (!visible || hidden) {
                setFieldNotVisible();
            }
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }

        @Override
        public String getFieldLabelName() {
            return label;
        }

        @Override
        public String getFieldHeaderName() {
            return header;
        }

        @Override
        public Integer getFieldMinLength() {
            return minLength;
        }

        @Override
        public Integer getFieldMaxLength() {
            return maxLength;
        }

        @Override
        public Integer getFieldDisplayLength() {
            return displayLength;
        }

        @Override
        public Integer getFieldRow() {
            return null;
        }

        @Override
        public Integer getFieldCol() {
            return null;
        }
    }

    private static final class NamedBooleanField extends AbstractBoolean {
        private final String fieldName;
        private final String label;
        private final String header;

        private NamedBooleanField(
                String fieldName,
                String label,
                String header,
                boolean value,
                boolean editable,
                boolean visible
        ) {
            this.fieldName = fieldName;
            this.label = label;
            this.header = header;
            setValue(value);

            if (editable) {
                setFieldEditable();
            } else {
                setFieldNotEditable();
            }

            if (!visible) {
                setFieldNotVisible();
            }
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }

        @Override
        public String getFieldLabelName() {
            return label;
        }

        @Override
        public String getFieldHeaderName() {
            return header;
        }

        @Override
        public String toString() {
            Boolean value = getValue();
            return value == null ? "" : value.toString();
        }
    }

    private static final class NamedDateTimeField extends AbstractDateTime {
        private final String fieldName;
        private final String label;
        private final String header;

        private NamedDateTimeField(
                String fieldName,
                String label,
                String header,
                Timestamp value,
                boolean editable,
                boolean visible
        ) {
            super(value);
            this.fieldName = fieldName;
            this.label = label;
            this.header = header;

            if (editable) {
                setFieldEditable();
            } else {
                setFieldNotEditable();
            }

            if (!visible) {
                setFieldNotVisible();
            }
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }

        @Override
        public String getFieldLabelName() {
            return label;
        }

        @Override
        public String getFieldHeaderName() {
            return header;
        }
    }

    private static final class UserRoleLookupField extends AbstractLookup {
        @Override
        public String getLookupName() {
            return "UserRole";
        }

        @Override
        public String getDropdownSelectText() {
            return "Select role ...";
        }

        @Override
        public List<LookupValue> getListOfActiveLookupValues() {
            return Arrays.stream(UserRoles.values())
                    .filter(role -> role != UserRoles.INVASIVE_USER_ROLE)
                    .map(role -> new LookupValue(null, null, role.getId(), role.getLabel(), role.getDescription(), true))
                    .toList();
        }

        @Override
        public void setValue(Integer value) {
            UserRoles role = UserRoles.fromIdOrDefault(value, UserRoles.CUSTOMER_ADMINISTRATOR);
            setLookupValue(new LookupValue(null, null, role.getId(), role.getLabel(), role.getDescription(), true));
        }

        @Override
        public String getFieldName() {
            return "UserRole";
        }

        @Override
        public String getFieldLabelName() {
            return "Role";
        }

        @Override
        public String getFieldHeaderName() {
            return "Role";
        }

        @Override
        public String toString() {
            return getLookupCode() != null ? getLookupCode() : "";
        }
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}
