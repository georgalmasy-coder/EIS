IF DB_ID('EIS-TECH') IS NULL
BEGIN
    CREATE DATABASE [EIS-TECH];
END
GO

USE [EIS-TECH];
GO

IF OBJECT_ID('dbo.invoice_approvals', 'U') IS NOT NULL DROP TABLE dbo.invoice_approvals;
IF OBJECT_ID('dbo.accounting_lines', 'U') IS NOT NULL DROP TABLE dbo.accounting_lines;
IF OBJECT_ID('dbo.accounting_entries', 'U') IS NOT NULL DROP TABLE dbo.accounting_entries;
IF OBJECT_ID('dbo.accounts', 'U') IS NOT NULL DROP TABLE dbo.accounts;
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

CREATE TABLE dbo.accounts (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    account_number NVARCHAR(20) NOT NULL,
    account_name NVARCHAR(200) NOT NULL,
    account_type NVARCHAR(40) NOT NULL,
    system_key NVARCHAR(80) NULL,
    is_active BIT NOT NULL CONSTRAINT df_accounts_is_active DEFAULT (1),
    created_at DATETIME2(0) NOT NULL CONSTRAINT df_accounts_created_at DEFAULT (SYSDATETIME()),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT df_accounts_updated_at DEFAULT (SYSDATETIME()),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number)
);

CREATE TABLE dbo.accounting_entries (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    entry_date DATE NOT NULL,
    accounting_year INT NOT NULL,
    accounting_month INT NOT NULL,
    entry_type NVARCHAR(80) NOT NULL,
    description NVARCHAR(500) NOT NULL,
    reference_type NVARCHAR(80) NULL,
    reference_id NVARCHAR(120) NULL,
    correction_of_entry_id BIGINT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT df_accounting_entries_created_at DEFAULT (SYSDATETIME()),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT df_accounting_entries_updated_at DEFAULT (SYSDATETIME()),
    CONSTRAINT fk_accounting_entries_correction FOREIGN KEY (correction_of_entry_id) REFERENCES dbo.accounting_entries(id)
);

CREATE TABLE dbo.accounting_lines (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    account_id INT NOT NULL,
    line_text NVARCHAR(500) NOT NULL,
    debit_amount DECIMAL(12,2) NOT NULL CONSTRAINT df_accounting_lines_debit DEFAULT (0),
    credit_amount DECIMAL(12,2) NOT NULL CONSTRAINT df_accounting_lines_credit DEFAULT (0),
    vat_code NVARCHAR(40) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT df_accounting_lines_created_at DEFAULT (SYSDATETIME()),
    CONSTRAINT fk_accounting_lines_entries FOREIGN KEY (entry_id) REFERENCES dbo.accounting_entries(id),
    CONSTRAINT fk_accounting_lines_accounts FOREIGN KEY (account_id) REFERENCES dbo.accounts(id),
    CONSTRAINT ck_accounting_lines_amount CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR (credit_amount > 0 AND debit_amount = 0)
    )
);

CREATE TABLE dbo.invoice_approvals (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    customer_id INT NOT NULL,
    invoice_year INT NOT NULL,
    invoice_month INT NOT NULL,
    invoice_number NVARCHAR(80) NOT NULL,
    invoice_date DATE NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    vat_amount DECIMAL(12,2) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    accounting_entry_id BIGINT NOT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT df_invoice_approvals_created_at DEFAULT (SYSDATETIME()),
    CONSTRAINT fk_invoice_approvals_customers FOREIGN KEY (customer_id) REFERENCES dbo.customers(id),
    CONSTRAINT fk_invoice_approvals_entries FOREIGN KEY (accounting_entry_id) REFERENCES dbo.accounting_entries(id),
    CONSTRAINT uq_invoice_approvals_period UNIQUE (customer_id, invoice_year, invoice_month)
);

CREATE INDEX ix_activities_customer ON dbo.activities(customer_id, is_inactive);
CREATE INDEX ix_time_entries_customer_date ON dbo.time_entries(customer_id, entry_date, is_deleted);
CREATE INDEX ix_time_entries_activity ON dbo.time_entries(activity_id, is_deleted);
CREATE INDEX ix_material_entries_customer_date ON dbo.material_entries(customer_id, entry_date, is_deleted);
CREATE INDEX ix_accounting_entries_period ON dbo.accounting_entries(accounting_year, accounting_month, entry_date);
CREATE INDEX ix_accounting_lines_entry ON dbo.accounting_lines(entry_id);
CREATE UNIQUE INDEX ux_accounts_system_key ON dbo.accounts(system_key) WHERE system_key IS NOT NULL;

INSERT INTO dbo.accounts (account_number, account_name, account_type, system_key) VALUES
    (N'1010', N'Bank', N'ASSET', N'BANK'),
    (N'1200', N'Accounts receivable', N'ASSET', N'ACCOUNTS_RECEIVABLE'),
    (N'3000', N'Consulting revenue', N'REVENUE', N'REVENUE'),
    (N'3100', N'Material revenue', N'REVENUE', N'MATERIAL_REVENUE'),
    (N'5600', N'Computer equipment', N'EXPENSE', N'COMPUTER_EQUIPMENT'),
    (N'5610', N'Development licenses', N'EXPENSE', N'DEVELOPMENT_LICENSES'),
    (N'5800', N'Bank fees', N'EXPENSE', N'BANK_FEES'),
    (N'5900', N'Interest expenses', N'EXPENSE', N'INTEREST_EXPENSE'),
    (N'6300', N'Salary expense', N'EXPENSE', N'SALARY_EXPENSE'),
    (N'6310', N'Expense reimbursement', N'EXPENSE', N'EXPENSE_REIMBURSEMENT'),
    (N'6610', N'Input VAT', N'ASSET', N'INPUT_VAT'),
    (N'6620', N'Output VAT', N'LIABILITY', N'OUTPUT_VAT'),
    (N'6690', N'VAT settlement', N'LIABILITY', N'VAT_SETTLEMENT'),
    (N'8100', N'Interest income', N'REVENUE', N'INTEREST_INCOME');

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'EISTECH')
BEGIN
    CREATE USER [EISTECH] FOR LOGIN [EISTECH];
END
GO

ALTER ROLE db_datareader ADD MEMBER [EISTECH];
ALTER ROLE db_datawriter ADD MEMBER [EISTECH];
GO
