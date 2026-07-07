IF DB_ID('EIS-TECH') IS NULL
BEGIN
    CREATE DATABASE [EIS-TECH];
END
GO

USE [EIS-TECH];
GO

IF OBJECT_ID('dbo.material_entries', 'U') IS NOT NULL DROP TABLE dbo.material_entries;
IF OBJECT_ID('dbo.time_entries', 'U') IS NOT NULL DROP TABLE dbo.time_entries;
IF OBJECT_ID('dbo.activities', 'U') IS NOT NULL DROP TABLE dbo.activities;
IF OBJECT_ID('dbo.customers', 'U') IS NOT NULL DROP TABLE dbo.customers;
GO

CREATE TABLE dbo.customers (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    company_name NVARCHAR(200) NOT NULL,
    contact_name NVARCHAR(200) NOT NULL,
    contact_email NVARCHAR(320) NOT NULL,
    phone_number NVARCHAR(50) NULL,
    address_line NVARCHAR(250) NOT NULL,
    postal_code NVARCHAR(20) NOT NULL,
    city NVARCHAR(120) NOT NULL,
    hourly_rate DECIMAL(12,2) NOT NULL CONSTRAINT df_customers_hourly_rate DEFAULT (0),
    vat_rate DECIMAL(5,2) NOT NULL CONSTRAINT df_customers_vat_rate DEFAULT (25.00),
    is_inactive BIT NOT NULL CONSTRAINT df_customers_is_inactive DEFAULT (0),
    created_at DATETIME2(0) NOT NULL CONSTRAINT df_customers_created_at DEFAULT (SYSDATETIME()),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT df_customers_updated_at DEFAULT (SYSDATETIME())
);

CREATE TABLE dbo.activities (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    customer_id INT NOT NULL,
    short_description NVARCHAR(200) NOT NULL,
    long_description NVARCHAR(2000) NULL,
    is_inactive BIT NOT NULL CONSTRAINT df_activities_is_inactive DEFAULT (0),
    created_at DATETIME2(0) NOT NULL CONSTRAINT df_activities_created_at DEFAULT (SYSDATETIME()),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT df_activities_updated_at DEFAULT (SYSDATETIME()),
    CONSTRAINT fk_activities_customers FOREIGN KEY (customer_id) REFERENCES dbo.customers(id)
);

CREATE TABLE dbo.time_entries (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    customer_id INT NOT NULL,
    activity_id INT NOT NULL,
    entry_date DATE NOT NULL,
    hours DECIMAL(4,1) NOT NULL,
    note NVARCHAR(1000) NULL,
    is_deleted BIT NOT NULL CONSTRAINT df_time_entries_is_deleted DEFAULT (0),
    created_at DATETIME2(0) NOT NULL CONSTRAINT df_time_entries_created_at DEFAULT (SYSDATETIME()),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT df_time_entries_updated_at DEFAULT (SYSDATETIME()),
    CONSTRAINT fk_time_entries_customers FOREIGN KEY (customer_id) REFERENCES dbo.customers(id),
    CONSTRAINT fk_time_entries_activities FOREIGN KEY (activity_id) REFERENCES dbo.activities(id)
);

CREATE TABLE dbo.material_entries (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    customer_id INT NOT NULL,
    entry_date DATE NOT NULL,
    quantity DECIMAL(12,2) NOT NULL,
    unit NVARCHAR(50) NOT NULL,
    short_description NVARCHAR(500) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    is_deleted BIT NOT NULL CONSTRAINT df_material_entries_is_deleted DEFAULT (0),
    created_at DATETIME2(0) NOT NULL CONSTRAINT df_material_entries_created_at DEFAULT (SYSDATETIME()),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT df_material_entries_updated_at DEFAULT (SYSDATETIME()),
    CONSTRAINT fk_material_entries_customers FOREIGN KEY (customer_id) REFERENCES dbo.customers(id)
);

CREATE INDEX ix_activities_customer ON dbo.activities(customer_id, is_inactive);
CREATE INDEX ix_time_entries_customer_date ON dbo.time_entries(customer_id, entry_date, is_deleted);
CREATE INDEX ix_time_entries_activity ON dbo.time_entries(activity_id, is_deleted);
CREATE INDEX ix_material_entries_customer_date ON dbo.material_entries(customer_id, entry_date, is_deleted);

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'EISTECH')
BEGIN
    CREATE USER [EISTECH] FOR LOGIN [EISTECH];
END
GO

ALTER ROLE db_datareader ADD MEMBER [EISTECH];
ALTER ROLE db_datawriter ADD MEMBER [EISTECH];
GO
