/*
    Adds [UserRole] to [dbo].[USERS].

    Migration steps:
      1. Add column as NULL.
      2. Populate all existing rows with role id 1.
      3. Make the column NOT NULL.

    Role mapping:
      1 = BEPA_SYSTEM_ADMINISTRATOR
*/

IF COL_LENGTH('dbo.USERS', 'UserRole') IS NULL
BEGIN
ALTER TABLE [dbo].[USERS]
    ADD [UserRole] [int] NULL;
END
GO

EXEC('
    UPDATE [dbo].[USERS]
    SET [UserRole] = 1
    WHERE [UserRole] IS NULL
');
GO

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    INNER JOIN sys.tables t
        ON t.object_id = c.object_id
    INNER JOIN sys.schemas s
        ON s.schema_id = t.schema_id
    WHERE s.name = 'dbo'
      AND t.name = 'USERS'
      AND c.name = 'UserRole'
      AND c.is_nullable = 1
)
BEGIN
ALTER TABLE [dbo].[USERS]
ALTER COLUMN [UserRole] [int] NOT NULL;
END
GO
