USE [EIS-TECH];
GO

IF COL_LENGTH('dbo.customers', 'is_inactive') IS NULL
BEGIN
    ALTER TABLE dbo.customers
    ADD is_inactive BIT NOT NULL
        CONSTRAINT df_customers_is_inactive DEFAULT (0);
END
GO

IF COL_LENGTH('dbo.activities', 'is_inactive') IS NULL
BEGIN
    ALTER TABLE dbo.activities
    ADD is_inactive BIT NOT NULL
        CONSTRAINT df_activities_is_inactive DEFAULT (0);
END
GO

UPDATE dbo.customers
SET is_inactive = 0;

UPDATE dbo.activities
SET is_inactive = 0;
GO

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_activities_customer' AND object_id = OBJECT_ID(N'dbo.activities'))
BEGIN
    DROP INDEX ix_activities_customer ON dbo.activities;
END
GO

CREATE INDEX ix_activities_customer ON dbo.activities(customer_id, is_inactive);
GO
