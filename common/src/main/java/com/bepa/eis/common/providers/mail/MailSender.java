package com.bepa.eis.common.providers.mail;

import com.bepa.eis.common.dto.mail.MailQueueItem;
import com.bepa.eis.common.dto.mail.MailSendResult;

public interface MailSender {

    MailSendResult send(MailQueueItem mail);
}