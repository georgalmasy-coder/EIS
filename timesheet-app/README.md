# EIS Time Registration

Standalone WAR for Tomcat 11 and Java 21.

## Build

```bash
mvn -f timesheet-app/pom.xml clean package
```

Run the bookkeeping report UI regression tests from the repository root:

```bash
node --test timesheet-app/src/test/js/accounting-reports.test.mjs
```

## Database

Run `install-database.sql` against MS SQL Server before deploying.
The same schema is also available at `src/main/resources/db/schema.sql` for packaging inside the WAR.

## JNDI

Configure the Tomcat resource `jdbc/TimesheetDB` in `src/main/webapp/META-INF/context.xml`.

## Frontend

The application is a single-page UI with:

- customer administration
- activity administration per customer
- daily time registration in half-hour steps
- material registration
- monthly invoice summary with VAT
- bookkeeping reports: quarterly VAT (input, output and net payable, excluding VAT settlements) and monthly bank reconciliation with month-end balances carried forward from all earlier postings

Selected customer is persisted in the HTTP session.
