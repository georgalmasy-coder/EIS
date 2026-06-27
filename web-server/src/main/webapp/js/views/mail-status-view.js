import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawMultiLineChart,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { postForm } from "/js/core/http.js";
import { formatDateTimeForDisplay } from "/js/core/format.js";
import { escapeHtml } from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/mail-status";
const RESEND_URL = "/admin/api/dashboard/mail-status-action";
const REFRESH_MS = 20000;
const RESENDABLE_STATUSES = new Set(["FAILED", "UNDELIVERED"]);
const EMPTY_PLACEHOLDER = "-";

export function createViewController(context) {
    const root = context.root;
    let latestMailRowsById = new Map();
    let currentOpenMailId = null;
    let isResendingMail = false;

    function fallbackData() {
        return {
            sentLast7Days: 0,
            errorsLast7Days: 3,
            queuedCount: 0,
            retryingCount: 0,
            undeliveredCount: 3,
            cancelledCount: 0,
            hourlySeries: [
                { hour: "00:00", sent: 0, error: 0 },
                { hour: "01:00", sent: 0, error: 0 },
                { hour: "02:00", sent: 0, error: 0 },
                { hour: "03:00", sent: 0, error: 0 },
                { hour: "04:00", sent: 0, error: 0 },
                { hour: "05:00", sent: 0, error: 3 },
                { hour: "06:00", sent: 0, error: 0 },
                { hour: "07:00", sent: 0, error: 0 },
                { hour: "08:00", sent: 0, error: 0 },
                { hour: "09:00", sent: 0, error: 0 },
                { hour: "10:00", sent: 0, error: 0 },
                { hour: "11:00", sent: 0, error: 0 },
                { hour: "12:00", sent: 0, error: 0 },
                { hour: "13:00", sent: 0, error: 0 },
                { hour: "14:00", sent: 0, error: 0 },
                { hour: "15:00", sent: 0, error: 0 },
                { hour: "16:00", sent: 0, error: 0 },
                { hour: "17:00", sent: 0, error: 0 },
                { hour: "18:00", sent: 0, error: 0 },
                { hour: "19:00", sent: 0, error: 0 },
                { hour: "20:00", sent: 0, error: 0 },
                { hour: "21:00", sent: 0, error: 0 },
                { hour: "22:00", sent: 0, error: 0 },
                { hour: "23:00", sent: 0, error: 0 }
            ],
            latestSentEmails: [],
            latestQueuedEmails: [],
            latestErrorEmails: [
                {
                    mailId: 1,
                    templateType: "CUSTOMER_CONFIRMATION",
                    templateTypeLabel: "Customer confirmation",
                    fromName: "BEPA EIS",
                    fromEmail: "no-reply@example.dk",
                    toName: "",
                    toEmail: "ole@max.dk",
                    subject: "Customer confirmation",
                    bodyText: "",
                    bodyHtml: "<p>Customer confirmation email preview is not available in fallback data.</p>",
                    status: "UNDELIVERED",
                    statusLabel: "Undelivered",
                    attemptCount: 5,
                    maxAttempts: 5,
                    createdAt: "",
                    lastAttemptAt: "2026-06-13T05:26:59",
                    nextAttemptAt: "",
                    sentAt: "",
                    smtpMessageId: "",
                    lastError: "SMTP error while sending mail: Couldn't connect to host, port: localhost, 25; timeout 10000"
                },
                {
                    mailId: 2,
                    templateType: "CUSTOMER_CONFIRMATION",
                    templateTypeLabel: "Customer confirmation",
                    fromName: "BEPA EIS",
                    fromEmail: "no-reply@example.dk",
                    toName: "",
                    toEmail: "georg.almasy@gmail.it",
                    subject: "Customer confirmation",
                    bodyText: "",
                    bodyHtml: "<p>Customer confirmation email preview is not available in fallback data.</p>",
                    status: "UNDELIVERED",
                    statusLabel: "Undelivered",
                    attemptCount: 5,
                    maxAttempts: 5,
                    createdAt: "",
                    lastAttemptAt: "2026-06-13T05:26:59",
                    nextAttemptAt: "",
                    sentAt: "",
                    smtpMessageId: "",
                    lastError: "SMTP error while sending mail: Couldn't connect to host, port: localhost, 25; timeout 10000"
                },
                {
                    mailId: 3,
                    templateType: "CUSTOMER_CONFIRMATION",
                    templateTypeLabel: "Customer confirmation",
                    fromName: "BEPA EIS",
                    fromEmail: "no-reply@example.dk",
                    toName: "",
                    toEmail: "georg.almasy@gmail.ru",
                    subject: "Customer confirmation",
                    bodyText: "",
                    bodyHtml: "<p>Customer confirmation email preview is not available in fallback data.</p>",
                    status: "UNDELIVERED",
                    statusLabel: "Undelivered",
                    attemptCount: 5,
                    maxAttempts: 5,
                    createdAt: "",
                    lastAttemptAt: "2026-06-13T05:27:00",
                    nextAttemptAt: "",
                    sentAt: "",
                    smtpMessageId: "",
                    lastError: "SMTP error while sending mail: Couldn't connect to host, port: localhost, 25; timeout 10000"
                }
            ]
        };
    }

    function render(data) {
        const safeData = data || fallbackData();

        latestMailRowsById = buildMailRowMap(
            safeData.latestSentEmails,
            safeData.latestQueuedEmails,
            safeData.latestErrorEmails
        );

        setText(root, '[data-field="sentLast7Days"]', safeData.sentLast7Days ?? EMPTY_PLACEHOLDER);
        setText(root, '[data-field="errorsLast7Days"]', safeData.errorsLast7Days ?? EMPTY_PLACEHOLDER);
        setText(root, '[data-field="queuedCount"]', safeData.queuedCount ?? EMPTY_PLACEHOLDER);
        setText(root, '[data-field="retryingCount"]', safeData.retryingCount ?? EMPTY_PLACEHOLDER);
        setText(root, '[data-field="undeliveredCount"]', safeData.undeliveredCount ?? EMPTY_PLACEHOLDER);

        setBar(root, '[data-bar="sentLast7Days"]', safeData.sentLast7Days, Math.max(1, safeData.sentLast7Days, safeData.errorsLast7Days));
        setBar(root, '[data-bar="errorsLast7Days"]', safeData.errorsLast7Days, Math.max(1, safeData.sentLast7Days, safeData.errorsLast7Days));
        setBar(root, '[data-bar="queuedCount"]', safeData.queuedCount, 100);
        setBar(root, '[data-bar="retryingCount"]', safeData.retryingCount, 100);
        setBar(root, '[data-bar="undeliveredCount"]', safeData.undeliveredCount, 100);

        renderHourlyChart(safeData.hourlySeries);
        renderMailTable('[data-table="latestSentEmails"]', safeData.latestSentEmails);
        renderMailTable('[data-table="latestQueuedEmails"]', safeData.latestQueuedEmails);
        renderMailTable('[data-table="latestErrorEmails"]', safeData.latestErrorEmails);
        bindModalEvents();
    }

    function buildMailRowMap(sentRows, queuedRows, errorRows) {
        const result = new Map();

        []
            .concat(Array.isArray(sentRows) ? sentRows : [])
            .concat(Array.isArray(queuedRows) ? queuedRows : [])
            .concat(Array.isArray(errorRows) ? errorRows : [])
            .forEach(function (row) {
                if (!row || row.mailId === null || row.mailId === undefined) {
                    return;
                }

                result.set(String(row.mailId), row);
            });

        return result;
    }

    function renderHourlyChart(rows) {
        const safeRows = Array.isArray(rows) ? rows : [];
        const sentValues = safeRows.map(function (row) {
            return toNumber(row.sent);
        });
        const errorValues = safeRows.map(function (row) {
            return toNumber(row.error);
        });

        drawMultiLineChart(
            root.querySelector('[data-chart="mailHourlyChart"]'),
            [
                {
                    label: "Sent",
                    values: sentValues,
                    color: "#84d64b",
                    lineWidth: 2
                },
                {
                    label: "Error",
                    values: errorValues,
                    color: "#ef4444",
                    lineWidth: 2
                }
            ],
            {
                padding: 44,
                startLabel: safeRows.length > 0 ? String(safeRows[0].hour || "Oldest") : "Oldest",
                endLabel: safeRows.length > 0 ? String(safeRows[safeRows.length - 1].hour || "Now") : "Now",
                legend: true
            }
        );
    }

    function renderMailTable(selector, rows) {
        const body = root.querySelector(selector);

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            const status = row.statusLabel || row.status || "Unknown";
            const statusClass = buildStatusClass(row.status || status);

            return `
                <tr data-mail-id="${escapeHtml(String(row.mailId ?? ""))}" tabindex="0">
                    <td>${escapeHtml(row.toEmail || EMPTY_PLACEHOLDER)}</td>
                    <td>${escapeHtml(row.toName || EMPTY_PLACEHOLDER)}</td>
                    <td>${escapeHtml(row.fromName || EMPTY_PLACEHOLDER)}</td>
                    <td>${escapeHtml(row.templateTypeLabel || row.templateType || EMPTY_PLACEHOLDER)}</td>
                    <td>${escapeHtml(row.subject || EMPTY_PLACEHOLDER)}</td>
                    <td><span class="dashboard-pill ${statusClass}">${escapeHtml(status)}</span></td>
                    <td>${escapeHtml(String(row.attemptCount ?? EMPTY_PLACEHOLDER))}</td>
                </tr>
            `;
        }).join("");

        body.querySelectorAll("tr[data-mail-id]").forEach(function (rowElement) {
            rowElement.addEventListener("dblclick", function () {
                openMailDetails(rowElement.dataset.mailId);
            });

            rowElement.addEventListener("keydown", function (event) {
                if (event.key === "Enter") {
                    openMailDetails(rowElement.dataset.mailId);
                }
            });
        });
    }

    function buildStatusClass(status) {
        const normalizedStatus = String(status || "").toUpperCase();

        if (normalizedStatus === "SENT") {
            return "ok";
        }

        if (normalizedStatus === "FAILED" || normalizedStatus === "SENDING" || normalizedStatus === "QUEUED") {
            return "warning";
        }

        return "error";
    }

    function bindModalEvents() {
        const closeButton = root.querySelector('[data-action="closeMailDetails"]');
        const resendButton = root.querySelector('[data-action="resendMail"]');
        const resendCloseButton = root.querySelector('[data-action="closeMailResend"]');
        const resendCancelButton = root.querySelector('[data-action="cancelMailResend"]');
        const resendForm = root.querySelector('[data-form="mailResend"]');
        const modalBackdrop = root.querySelector('[data-modal="mailDetails"]');
        const resendBackdrop = root.querySelector('[data-modal="mailResend"]');

        if (closeButton && !closeButton.dataset.bound) {
            closeButton.dataset.bound = "true";
            closeButton.addEventListener("click", closeMailDetails);
        }

        if (resendButton && !resendButton.dataset.bound) {
            resendButton.dataset.bound = "true";
            resendButton.addEventListener("click", openMailResendDialog);
        }

        if (resendCloseButton && !resendCloseButton.dataset.bound) {
            resendCloseButton.dataset.bound = "true";
            resendCloseButton.addEventListener("click", closeMailResendDialog);
        }

        if (resendCancelButton && !resendCancelButton.dataset.bound) {
            resendCancelButton.dataset.bound = "true";
            resendCancelButton.addEventListener("click", closeMailResendDialog);
        }

        if (resendForm && !resendForm.dataset.bound) {
            resendForm.dataset.bound = "true";
            resendForm.addEventListener("submit", submitMailResend);
        }

        if (modalBackdrop && !modalBackdrop.dataset.bound) {
            modalBackdrop.dataset.bound = "true";

            modalBackdrop.addEventListener("click", function (event) {
                if (event.target === modalBackdrop) {
                    closeMailDetails();
                }
            });
        }

        if (resendBackdrop && !resendBackdrop.dataset.bound) {
            resendBackdrop.dataset.bound = "true";

            resendBackdrop.addEventListener("click", function (event) {
                if (event.target === resendBackdrop) {
                    closeMailResendDialog();
                }
            });
        }
    }

    function openMailDetails(mailId) {
        const mail = latestMailRowsById.get(String(mailId || ""));

        if (!mail) {
            return;
        }

        currentOpenMailId = String(mail.mailId ?? "");

        setText(root, '[data-field="modalMailSubtitle"]', `${mail.toEmail || "Unknown recipient"} - ${mail.statusLabel || mail.status || "Unknown"}`);
        setText(root, '[data-field="modalSubject"]', mail.subject || EMPTY_PLACEHOLDER);
        setText(root, '[data-field="modalMailId"]', mail.mailId ?? EMPTY_PLACEHOLDER);
        setText(root, '[data-field="modalTemplate"]', mail.templateTypeLabel || mail.templateType || EMPTY_PLACEHOLDER);
        setText(root, '[data-field="modalStatus"]', mail.statusLabel || mail.status || EMPTY_PLACEHOLDER);
        setText(root, '[data-field="modalAttempts"]', `${mail.attemptCount ?? EMPTY_PLACEHOLDER} / ${mail.maxAttempts ?? EMPTY_PLACEHOLDER}`);
        setText(root, '[data-field="modalCreatedAt"]', formatDisplayDateTime(mail.createdAt));
        setText(root, '[data-field="modalLastAttemptAt"]', formatDisplayDateTime(mail.lastAttemptAt));
        setText(root, '[data-field="modalNextAttemptAt"]', formatDisplayDateTime(mail.nextAttemptAt));
        setText(root, '[data-field="modalSentAt"]', formatDisplayDateTime(mail.sentAt));
        setText(root, '[data-field="modalSmtpMessageId"]', mail.smtpMessageId || EMPTY_PLACEHOLDER);
        setText(root, '[data-field="modalToName"]', mail.toName || EMPTY_PLACEHOLDER);
        setText(root, '[data-field="modalToEmail"]', mail.toEmail || EMPTY_PLACEHOLDER);
        setText(root, '[data-field="modalFromName"]', mail.fromName || EMPTY_PLACEHOLDER);
        setText(root, '[data-field="modalFromEmail"]', mail.fromEmail || EMPTY_PLACEHOLDER);
        setText(root, '[data-field="modalLastError"]', mail.lastError || EMPTY_PLACEHOLDER);

        renderBodyPreview(mail);
        updateMailDetailsVisibility(mail);

        const modalBackdrop = root.querySelector('[data-modal="mailDetails"]');

        if (modalBackdrop) {
            modalBackdrop.hidden = false;
        }
    }

    function closeMailDetails() {
        const modalBackdrop = root.querySelector('[data-modal="mailDetails"]');

        if (modalBackdrop) {
            modalBackdrop.hidden = true;
        }

        closeMailResendDialog();
        currentOpenMailId = null;
    }

    function updateMailDetailsVisibility(mail) {
        const status = String(mail?.status || "").toUpperCase();
        const errorSection = root.querySelector('[data-section="mailErrorDetails"]');
        const resendButton = root.querySelector('[data-action="resendMail"]');
        const resendable = RESENDABLE_STATUSES.has(status);

        if (errorSection) {
            errorSection.innerHTML = resendable
                ? `
                    <section class="dashboard-modal-section">
                        <h3 class="dashboard-modal-section-title">Error details</h3>
                        <pre class="dashboard-error-details" data-field="modalLastError">${escapeHtml(mail?.lastError || EMPTY_PLACEHOLDER)}</pre>
                    </section>
                `
                : "";
        }

        if (resendButton) {
            resendButton.hidden = !resendable;
            resendButton.disabled = isResendingMail || !resendable;
        }
    }

    function openMailResendDialog() {
        if (!currentOpenMailId) {
            return;
        }

        const mail = latestMailRowsById.get(String(currentOpenMailId));

        if (!mail || !RESENDABLE_STATUSES.has(String(mail.status || "").toUpperCase())) {
            return;
        }

        const input = root.querySelector('[name="recipientEmail"]');
        const subtitle = root.querySelector('[data-field="resendMailSubtitle"]');
        const resendBackdrop = root.querySelector('[data-modal="mailResend"]');

        if (input) {
            input.value = mail.toEmail || "";
        }

        if (subtitle) {
            subtitle.textContent = `${mail.toEmail || "Unknown recipient"} - ${mail.statusLabel || mail.status || "Unknown"}`;
        }

        if (resendBackdrop) {
            resendBackdrop.hidden = false;
        }

        if (input) {
            input.focus();
            input.select();
        }
    }

    function closeMailResendDialog() {
        const resendBackdrop = root.querySelector('[data-modal="mailResend"]');

        if (resendBackdrop) {
            resendBackdrop.hidden = true;
        }

        isResendingMail = false;

        const mail = currentOpenMailId ? latestMailRowsById.get(String(currentOpenMailId)) : null;
        if (mail) {
            updateMailDetailsVisibility(mail);
        }
    }

    async function submitMailResend(event) {
        if (event) {
            event.preventDefault();
        }

        if (!currentOpenMailId || isResendingMail) {
            return;
        }

        const mail = latestMailRowsById.get(String(currentOpenMailId));

        if (!mail || !RESENDABLE_STATUSES.has(String(mail.status || "").toUpperCase())) {
            return;
        }

        const form = root.querySelector('[data-form="mailResend"]');
        const formData = form ? new FormData(form) : new FormData();
        const recipientEmail = String(formData.get("recipientEmail") || "").trim() || String(mail.toEmail || "").trim();

        if (!recipientEmail) {
            return;
        }

        const submitButton = root.querySelector('[data-action="submitMailResend"]');
        isResendingMail = true;

        if (submitButton) {
            submitButton.disabled = true;
        }

        try {
            await postForm(
                RESEND_URL,
                new URLSearchParams({
                    action: "resend",
                    mailId: String(currentOpenMailId),
                    recipientEmail: recipientEmail
                })
            );

            closeMailResendDialog();
            closeMailDetails();
            await load();
        } catch (error) {
            console.warn("Failed to queue resend mail.", error);
        } finally {
            isResendingMail = false;

            if (submitButton) {
                submitButton.disabled = false;
            }
        }
    }

    function renderBodyPreview(mail) {
        const preview = root.querySelector('[data-field="modalBodyPreview"]');

        if (!preview) {
            return;
        }

        if (mail.bodyHtml && String(mail.bodyHtml).trim()) {
            preview.innerHTML = String(mail.bodyHtml);
            return;
        }

        if (mail.bodyText && String(mail.bodyText).trim()) {
            preview.innerHTML = `<pre>${escapeHtml(mail.bodyText)}</pre>`;
            return;
        }

        preview.textContent = EMPTY_PLACEHOLDER;
    }

    function formatDisplayDateTime(value) {
        if (!value) {
            return EMPTY_PLACEHOLDER;
        }

        return formatDateTimeForDisplay(value, "en-GB", String(value));
    }

    function toNumber(value) {
        const numberValue = Number(value);

        if (Number.isFinite(numberValue)) {
            return numberValue;
        }

        return 0;
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback mail status dashboard data.", error);
            data = fallbackData();
        }

        render(data);
    }

    return createAutoRefreshController({
        refreshMs: REFRESH_MS,
        load,
        setLoadStatus: context.setLoadStatus,
        setLastRefreshNow: context.setLastRefreshNow,
        refreshButton: context.refreshButton
    });
}
