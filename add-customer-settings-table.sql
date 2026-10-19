-- Run against the web server database before deploying the settings editor.
IF OBJECT_ID(N'dbo.CUSTOMER_SETTINGS', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CUSTOMER_SETTINGS (
        CustomerId INT NOT NULL CONSTRAINT PK_CUSTOMER_SETTINGS PRIMARY KEY,
        Settings XML NOT NULL,
        ChangedAt DATETIME2(7) NOT NULL CONSTRAINT DF_CUSTOMER_SETTINGS_ChangedAt DEFAULT SYSUTCDATETIME(),
        ChangedByUserId INT NULL
    );
END;
