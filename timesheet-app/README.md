# EIS Time Registration

Standalone WAR for Tomcat 11 and Java 21.

## Build

```bash
mvn -f timesheet-app/pom.xml clean package
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

Selected customer is persisted in `localStorage` and mirrored in the HTTP session.
