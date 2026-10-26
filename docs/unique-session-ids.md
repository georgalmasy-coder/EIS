# Unique login sessions

Each completed login (including MFA) generates a cryptographically random 256-bit
Base64URL identifier. The browser continues to use Tomcat's JSESSIONID cookie;
the new identifier is stored in HttpSession and the database. Identity and
preferences use UserId, not email or the session identifier.

Two logins for one user have independent customer/project selections and logout.
The remembered project and other preferences remain shared by UserId. The most
recent preference write wins. Inactive accounts cannot refresh sessions; email
lookups used by login select the active account. No unique email constraint is added.

Development mode still bypasses login and uses the configured udv.customerid and
udv.projectid. Session cleanup continues to use LastAccessed and the shared
security.session.timeout.minutes setting. Dashboard counts distinct UserId values.

## Deployment

Run `sql/unique-session-ids.sql` against EISDB before deploying the changed modules.
It preserves all data and adds a binary comparison and unique index for SessionId.
If duplicate session ids already exist, index creation fails and the transaction
rolls back. Review those rows before attempting the migration again.

After deployment, old email-based HttpSession attributes are rejected and affected
users must log in again. Legacy database session rows remain until normal cleanup.
Login history and preferences are preserved. The migration has not been executed
automatically against a database.
