/*
    Adds customer-level MFA policy to [dbo].[CUSTOMER].

    Values:
      OPTIONAL = Customer does not require MFA by default.
      REQUIRED = Customer requires MFA.
      DISABLED = Customer disables MFA unless global MFA setting overrides it.

    Login evaluation order:
      Global setting -> Customer -> User

    Default for new and existing customers:
      OPTIONAL
*/

IF COL_LENGTH('dbo.CUSTOMER', 'CustomerMfaPolicy') IS NULL
BEGIN
ALTER TABLE [dbo].[CUSTOMER]
    ADD [CustomerMfaPolicy] [nvarchar](20) NULL;
END
GO

EXEC('
    UPDATE [dbo].[CUSTOMER]
    SET [CustomerMfaPolicy] = ''OPTIONAL''
    WHERE [CustomerMfaPolicy] IS NULL
');
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c
        ON c.default_object_id = dc.object_id
    INNER JOIN sys.tables t
        ON t.object_id = c.object_id
    INNER JOIN sys.schemas s
        ON s.schema_id = t.schema_id
    WHERE dc.name = 'DF_CUSTOMER_CustomerMfaPolicy'
      AND s.name = 'dbo'
      AND t.name = 'CUSTOMER'
      AND c.name = 'CustomerMfaPolicy'
)
BEGIN
ALTER TABLE [dbo].[CUSTOMER]
    ADD CONSTRAINT [DF_CUSTOMER_CustomerMfaPolicy]
    DEFAULT ('OPTIONAL') FOR [CustomerMfaPolicy];
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    INNER JOIN sys.tables t
        ON t.object_id = c.object_id
    INNER JOIN sys.schemas s
        ON s.schema_id = t.schema_id
    WHERE s.name = 'dbo'
      AND t.name = 'CUSTOMER'
      AND c.name = 'CustomerMfaPolicy'
      AND c.is_nullable = 1
)
BEGIN
ALTER TABLE [dbo].[CUSTOMER]
ALTER COLUMN [CustomerMfaPolicy] [nvarchar](20) NOT NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_CUSTOMER_CustomerMfaPolicy'
)
BEGIN
ALTER TABLE [dbo].[CUSTOMER]
    ADD CONSTRAINT [CK_CUSTOMER_CustomerMfaPolicy]
    CHECK ([CustomerMfaPolicy] IN ('OPTIONAL', 'REQUIRED', 'DISABLED'));
END
GO