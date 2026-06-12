package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.mail.MailRecipient;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.CustomerWorkflowMailType;
import com.bepa.eis.common.providers.mail.MailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomerWorkflowMailProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerWorkflowMailProvider.class);

    private MailProvider mailProvider;

    /* GFA
    public CustomerWorkflowMailProvider() {
        this(null);
    }

     */

    public CustomerWorkflowMailProvider(WebSession webSession) {
        this(new MailProvider(webSession));
    }

    public CustomerWorkflowMailProvider(MailProvider mailProvider) {
        this.mailProvider = mailProvider == null ? new MailProvider(null) : mailProvider;
    }

    public Integer createWorkflowMail(
            CustomerWorkflowMailType mailType,
            String recipientName,
            String recipientEmail,
            Map<String, Object> parameters
    ) {
        if (mailType == null) {
            log.warn("Customer workflow mail could not be created because mail type is missing.");
            return null;
        }

        MailRecipient recipient = new MailRecipient(
                safeText(recipientName),
                safeText(recipientEmail)
        );

        if (!recipient.isValid()) {
            log.warn(
                    "Customer workflow mail could not be created because recipient is invalid. mailType={}, recipientEmail={}",
                    mailType.getCode(),
                    recipientEmail
            );
            return null;
        }

        Map<String, Object> safeParameters = normalizeParameters(
                mailType,
                parameters
        );

        Integer mailId = mailProvider.createMail(
                recipient,
                mailType.getMailTemplateType(),
                safeParameters
        );

        if (mailId == null) {
            log.warn(
                    "Customer workflow mail could not be queued. mailType={}, recipientEmail={}",
                    mailType.getCode(),
                    recipient.getEmail()
            );
            return null;
        }

        log.info(
                "Customer workflow mail queued. mailId={}, mailType={}, recipientEmail={}",
                mailId,
                mailType.getCode(),
                recipient.getEmail()
        );

        return mailId;
    }

    private Map<String, Object> normalizeParameters(
            CustomerWorkflowMailType mailType,
            Map<String, Object> parameters
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (parameters != null) {
            result.putAll(parameters);
        }

        putIfMissing(result, "mailType", mailType.getCode());
        putIfMissing(result, "mailTypeLabel", mailType.getLabel());
        putIfMissing(result, "mailTypeDescription", mailType.getDescription());
        putIfMissing(result, "templateName", mailType.getTemplateName());
        putIfMissing(result, "applicationName", "BEPA EIS");

        return result;
    }

    private void putIfMissing(
            Map<String, Object> parameters,
            String key,
            Object value
    ) {
        if (!parameters.containsKey(key)) {
            parameters.put(key, value == null ? "" : value);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}