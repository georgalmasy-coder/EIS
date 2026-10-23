# Admin Dashboard

This page provides an overview of administrative status and operational information for BEPA EIS.

Use the dashboard sections to monitor customers, users, integrations, subscriptions, system status, audit/security information, and other administration areas.

The available information depends on your permissions and the enabled administration modules.
# System Health

System Health summarizes the latest recorded executions of the mail queue job, customer workflow job,
SMTP delivery, CVR lookup, VIES VAT validation and Geo-IP lookup. Database and API connectivity are not scored.

Each measured component contributes equally: OK = 100 points, Warning = 50, Critical = 0.
The displayed percentage is the rounded average. Unknown components are excluded from the score;
the dashboard shows measurement coverage and each component's last observation. With no observations, the score is unknown.

Enabled jobs become Critical five minutes after their next cron slot is missed.
A failed run is Critical; individual errors in a completed
customer workflow batch produce Warning. Disabled jobs and disabled VIES validation are excluded.
Integration status reflects real calls, not synthetic probes, and expires to Unknown after 24 hours.
An unsuccessful company/VAT lookup is a successful service response if the service itself responded correctly.

Observations are stored as COMPONENT_HEALTH entries in AUDIT_EVENT so both servers share the same data.
Deploy the web server and integration server together to enable job observations. Existing records are not backfilled.
Add future jobs or integrations to ComponentHealthProvider's definitions and record their execution results.

## Shared cron scheduling

All integration-server jobs use the same five-field cron scheduler and configuration reload behavior.
The mail queue defaults to mail.queue.job.cron=* * * * * (every minute), and customer workflow
to customer.workflow.job.cron=*/10 * * * * (every ten minutes).
Each supports job.enabled and job.timezone under its respective prefix; default timezone is Europe/Copenhagen.
The old mail.queue.job.interval.seconds and customer.workflow.job.interval.seconds keys are no longer used.
Existing custom intervals must be replaced with the desired cron expression. Missing cron keys are added automatically.
Jobs wait for the next cron slot after startup or configuration changes; they do not run immediately at startup.
Long executions and failures do not replay missed slots or cause immediate retries.

## Data cleanup jobs

The integration server runs three independent cleanup jobs, by default daily at 02:00 in Europe/Copenhagen:
SESSION records older than 1 day (Created), INCIDENTS older than 120 days (LogCreated),
and PERFORMANCE older than 120 days (Created). Age means elapsed days, with a strict cutoff.
Deletion runs in batches of 1,000 rows. No cleanup is triggered just by starting the server.

Configure each job in the shared eis-global.properties using cleanup.session.job.*, cleanup.incidents.job.*,
and cleanup.performance.job.*. Each prefix supports enabled, cron, timezone and retention.days.
Cron has five fields: minute hour day-of-month month day-of-week, and supports *, numeric lists, ranges and steps.
For example, cron=0 2 * * * runs daily at 02:00, and cron=30 3 * * 0 runs Sundays at 03:30.
Retention must be 1–36500 days; invalid settings stop that cleanup job and show Critical.
Settings are reloaded through the existing global configuration listener.

Cron slots missed while the server was stopped are not replayed. A repeated autumn clock hour only runs once;
a scheduled time in the missing spring clock hour is skipped. Cleanup jobs become Critical five minutes after
their next cron slot is missed. They are Unknown until the first recorded execution.
Service Status displays each job's last completed execution (including failures).

## Archived log cleanup

The integration server also deletes .gz log archives older than 30 days, by last-modified time,
daily at 02:00 Europe/Copenhagen. It scans only the files directly in the two configured directories;
active .log files, subdirectories and symbolic links are excluded. Missing directories are skipped.

The settings are cleanup.logs.job.enabled, cron, timezone and retention.days,
plus cleanup.logs.job.web-server.directory and cleanup.logs.job.integration-server.directory.
Empty directory settings default to web-server and integration-server beneath eis.logs.dir.
The directories must match the actual Log4j archive locations and be accessible to the integration-server account.
Missing keys are automatically appended to the shared eis-global.properties during startup or reload.
The job's status and last execution are included in System Health and Service Status.
