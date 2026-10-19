-- Run after add-customer-settings-table.sql and before deploying XML-based MFA.
-- Existing XML policies (including values saved in the settings editor) take precedence.
-- Restart the web server afterwards to discard any cached settings snapshots.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @CustomerId INT, @Policy NVARCHAR(16);
DECLARE customerPolicies CURSOR LOCAL FAST_FORWARD FOR
    SELECT CustomerId, CASE WHEN UPPER(CustomerMfaPolicy) IN ('REQUIRED', 'DISABLED', 'OPTIONAL')
                           THEN UPPER(CustomerMfaPolicy) ELSE 'OPTIONAL' END
    FROM dbo.CUSTOMER WHERE Latest = 1;

OPEN customerPolicies;
FETCH NEXT FROM customerPolicies INTO @CustomerId, @Policy;
WHILE @@FETCH_STATUS = 0
BEGIN
    MERGE dbo.CUSTOMER_SETTINGS WITH (HOLDLOCK) AS target
    USING (SELECT @CustomerId AS CustomerId) AS source ON target.CustomerId = source.CustomerId
    WHEN NOT MATCHED THEN INSERT (CustomerId, Settings, ChangedAt)
        VALUES (source.CustomerId, CONVERT(XML, N'<settings/>'), SYSUTCDATETIME());

    IF NOT EXISTS (SELECT 1 FROM dbo.CUSTOMER_SETTINGS WHERE CustomerId = @CustomerId
                   AND Settings.exist('/settings/customer[@id=sql:variable("@CustomerId")]') = 1)
        UPDATE dbo.CUSTOMER_SETTINGS
        SET Settings.modify('insert <customer id="{sql:variable("@CustomerId")}"/> as first into (/settings)[1]')
        WHERE CustomerId = @CustomerId;

    IF NOT EXISTS (SELECT 1 FROM dbo.CUSTOMER_SETTINGS WHERE CustomerId = @CustomerId
                   AND Settings.exist('/settings/customer[@id=sql:variable("@CustomerId")]/security') = 1)
        UPDATE dbo.CUSTOMER_SETTINGS
        SET Settings.modify('insert <security/> into (/settings/customer[@id=sql:variable("@CustomerId")])[1]')
        WHERE CustomerId = @CustomerId;

    IF NOT EXISTS (SELECT 1 FROM dbo.CUSTOMER_SETTINGS WHERE CustomerId = @CustomerId
                   AND Settings.exist('/settings/customer[@id=sql:variable("@CustomerId")]/security/mfaPolicy') = 1)
        UPDATE dbo.CUSTOMER_SETTINGS
        SET Settings.modify('insert <mfaPolicy>{sql:variable("@Policy")}</mfaPolicy>
            into (/settings/customer[@id=sql:variable("@CustomerId")]/security)[1]'),
            ChangedAt = SYSUTCDATETIME()
        WHERE CustomerId = @CustomerId;

    FETCH NEXT FROM customerPolicies INTO @CustomerId, @Policy;
END;
CLOSE customerPolicies;
DEALLOCATE customerPolicies;
COMMIT;
