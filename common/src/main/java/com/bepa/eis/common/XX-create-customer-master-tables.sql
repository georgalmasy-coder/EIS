IF OBJECT_ID(N'[dbo].[CUSTOMER]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[CUSTOMER] (
    [CustomerPK] INT IDENTITY(1,1) NOT NULL,

    [CustomerId] INT NOT NULL,
    [Version] INT NOT NULL,

    [CustomerName] NVARCHAR(255) NOT NULL,
    [CvrNumber] NVARCHAR(20) NULL,
    [VatNumber] NVARCHAR(20) NULL,
    [Phone] NVARCHAR(50) NULL,

    [Address] NVARCHAR(255) NULL,
    [ZipCode] NVARCHAR(50) NULL,
    [City] NVARCHAR(100) NULL,
    [Country] NVARCHAR(100) NULL,

    [ContactName] NVARCHAR(255) NULL,
    [ContactEmail] NVARCHAR(320) NULL,

    [CustomerStatus] INT NOT NULL,

    [ChangedByUserId] INT NULL,
    [ChangedDateTime] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_ChangedDateTime] DEFAULT SYSUTCDATETIME(),

    [Latest] BIT NOT NULL
    CONSTRAINT [DF_CUSTOMER_Latest] DEFAULT 1,

    CONSTRAINT [PK_CUSTOMER]
    PRIMARY KEY CLUSTERED ([CustomerPK] ASC),

    CONSTRAINT [UQ_CUSTOMER_CustomerId_Version]
    UNIQUE ([CustomerId], [Version]),

    CONSTRAINT [CK_CUSTOMER_CustomerId]
    CHECK ([CustomerId] > 0),

    CONSTRAINT [CK_CUSTOMER_Version]
    CHECK ([Version] > 0),

    CONSTRAINT [CK_CUSTOMER_CustomerName_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([CustomerName]))) > 0),

    CONSTRAINT [CK_CUSTOMER_CustomerStatus]
    CHECK ([CustomerStatus] IN (
            1,  -- CREATED
            2,  -- PENDING_CONFIRMATION
            3,  -- TRIAL_ACTIVE
            4,  -- PENDING_SUBSCRIPTION_CONFIRMATION
            5,  -- PAYMENT_PENDING
            6,  -- SUBSCRIPTION_ACTIVE
            7,  -- SUBSCRIPTION_EXPIRING
            8,  -- PAYMENT_OVERDUE
            9,  -- SUSPENDED
            10  -- CANCELLED
                               ))
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_CUSTOMER_CustomerId_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER]')
)
BEGIN
CREATE UNIQUE INDEX [UX_CUSTOMER_CustomerId_Latest]
    ON [dbo].[CUSTOMER] ([CustomerId])
    WHERE [Latest] = 1;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_CustomerId'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_CustomerId]
    ON [dbo].[CUSTOMER] ([CustomerId]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_CustomerStatus_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_CustomerStatus_Latest]
    ON [dbo].[CUSTOMER] ([CustomerStatus], [Latest])
    INCLUDE ([CustomerId], [CustomerName], [ContactEmail]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_CvrNumber_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_CvrNumber_Latest]
    ON [dbo].[CUSTOMER] ([CvrNumber], [Latest])
    WHERE [CvrNumber] IS NOT NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_VatNumber_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_VatNumber_Latest]
    ON [dbo].[CUSTOMER] ([VatNumber], [Latest])
    WHERE [VatNumber] IS NOT NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_ContactEmail_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_ContactEmail_Latest]
    ON [dbo].[CUSTOMER] ([ContactEmail], [Latest])
    WHERE [ContactEmail] IS NOT NULL;
END;
GO


IF OBJECT_ID(N'[dbo].[CUSTOMER_PAYMENT_METHOD]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[CUSTOMER_PAYMENT_METHOD] (
    [CustomerPaymentMethodId] INT IDENTITY(1,1) NOT NULL,

    [CustomerId] INT NOT NULL,

    [PaymentProvider] NVARCHAR(100) NOT NULL,
    [ProviderPaymentMethodReference] NVARCHAR(255) NULL,

    [CardholderName] NVARCHAR(255) NULL,
    [CardBrand] NVARCHAR(50) NULL,
    [MaskedCardNumber] NVARCHAR(50) NULL,
    [ExpiryMonth] INT NULL,
    [ExpiryYear] INT NULL,
    [BillingZipCode] NVARCHAR(50) NULL,

    [PaymentMethodStatus] INT NOT NULL
    CONSTRAINT [DF_CUSTOMER_PAYMENT_METHOD_Status] DEFAULT 1,

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_PAYMENT_METHOD_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [UpdatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_PAYMENT_METHOD_UpdatedAt] DEFAULT SYSUTCDATETIME(),

    [Latest] BIT NOT NULL
    CONSTRAINT [DF_CUSTOMER_PAYMENT_METHOD_Latest] DEFAULT 1,

    CONSTRAINT [PK_CUSTOMER_PAYMENT_METHOD]
    PRIMARY KEY CLUSTERED ([CustomerPaymentMethodId] ASC),

    CONSTRAINT [CK_CUSTOMER_PAYMENT_METHOD_CustomerId]
    CHECK ([CustomerId] > 0),

    CONSTRAINT [CK_CUSTOMER_PAYMENT_METHOD_Provider_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([PaymentProvider]))) > 0),

    CONSTRAINT [CK_CUSTOMER_PAYMENT_METHOD_ExpiryMonth]
    CHECK ([ExpiryMonth] IS NULL OR ([ExpiryMonth] >= 1 AND [ExpiryMonth] <= 12)),

    CONSTRAINT [CK_CUSTOMER_PAYMENT_METHOD_ExpiryYear]
    CHECK ([ExpiryYear] IS NULL OR [ExpiryYear] >= 2000),

    CONSTRAINT [CK_CUSTOMER_PAYMENT_METHOD_Status]
    CHECK ([PaymentMethodStatus] IN (
            1,  -- ACTIVE
            2,  -- EXPIRED
            3,  -- DISABLED
            4   -- DELETED
                                    ))
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_PAYMENT_METHOD_CustomerId_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_PAYMENT_METHOD]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_PAYMENT_METHOD_CustomerId_Latest]
    ON [dbo].[CUSTOMER_PAYMENT_METHOD] ([CustomerId], [Latest])
    INCLUDE ([PaymentProvider], [ProviderPaymentMethodReference], [PaymentMethodStatus]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_PAYMENT_METHOD_ProviderReference'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_PAYMENT_METHOD]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_PAYMENT_METHOD_ProviderReference]
    ON [dbo].[CUSTOMER_PAYMENT_METHOD] ([PaymentProvider], [ProviderPaymentMethodReference])
    WHERE [ProviderPaymentMethodReference] IS NOT NULL;
END;
GO


IF OBJECT_ID(N'[dbo].[SUBSCRIPTION_PLAN]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[SUBSCRIPTION_PLAN] (
    [SubscriptionPlanId] INT IDENTITY(1,1) NOT NULL,

    [ModuleCode] NVARCHAR(100) NOT NULL,
    [ModuleName] NVARCHAR(255) NOT NULL,
    [PlanName] NVARCHAR(255) NOT NULL,

    [Description] NVARCHAR(1000) NULL,

    [ValidFrom] DATE NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_ValidFrom] DEFAULT (CONVERT(date, SYSUTCDATETIME())),

    [ValidTo] DATE NULL,

    [PriceAmount] DECIMAL(18,2) NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_PriceAmount] DEFAULT 0,

    [Currency] NVARCHAR(10) NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_Currency] DEFAULT N'EUR',

    [BillingPeriodMonths] INT NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_BillingPeriodMonths] DEFAULT 1,

    [TrialDays] INT NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_TrialDays] DEFAULT 14,

    [Active] BIT NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_Active] DEFAULT 1,

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [UpdatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_UpdatedAt] DEFAULT SYSUTCDATETIME(),

    CONSTRAINT [PK_SUBSCRIPTION_PLAN]
    PRIMARY KEY CLUSTERED ([SubscriptionPlanId] ASC),

    CONSTRAINT [UQ_SUBSCRIPTION_PLAN_ModuleCode_PlanName]
    UNIQUE ([ModuleCode], [PlanName], [ValidFrom]),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_ModuleCode_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([ModuleCode]))) > 0),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_ModuleName_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([ModuleName]))) > 0),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_PlanName_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([PlanName]))) > 0),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_ValidTo]
    CHECK ([ValidTo] IS NULL OR [ValidTo] >= [ValidFrom]),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_PriceAmount]
    CHECK ([PriceAmount] >= 0),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_BillingPeriodMonths]
    CHECK ([BillingPeriodMonths] > 0),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_TrialDays]
    CHECK ([TrialDays] >= 0)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_SUBSCRIPTION_PLAN_ModuleCode_Active'
      AND object_id = OBJECT_ID(N'[dbo].[SUBSCRIPTION_PLAN]')
)
BEGIN
CREATE INDEX [IX_SUBSCRIPTION_PLAN_ModuleCode_Active]
    ON [dbo].[SUBSCRIPTION_PLAN] ([ModuleCode], [Active]);
END;
GO

IF OBJECT_ID(N'[dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD] (
    [SubscriptionPlanBillingPeriodId] INT IDENTITY(1,1) NOT NULL,

    [SubscriptionPlanId] INT NOT NULL,
    [BillingPeriodCode] NVARCHAR(100) NOT NULL,
    [BillingPeriodName] NVARCHAR(255) NOT NULL,
    [Description] NVARCHAR(1000) NULL,

    [BillingPeriodMonths] INT NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_BILLING_PERIOD_BillingPeriodMonths] DEFAULT 1,

    [PriceAmount] DECIMAL(18,2) NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_BILLING_PERIOD_PriceAmount] DEFAULT 0,

    [Currency] NVARCHAR(10) NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_BILLING_PERIOD_Currency] DEFAULT N'EUR',

    [Active] BIT NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_BILLING_PERIOD_Active] DEFAULT 1,

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_BILLING_PERIOD_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [UpdatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_SUBSCRIPTION_PLAN_BILLING_PERIOD_UpdatedAt] DEFAULT SYSUTCDATETIME(),

    CONSTRAINT [PK_SUBSCRIPTION_PLAN_BILLING_PERIOD]
    PRIMARY KEY CLUSTERED ([SubscriptionPlanBillingPeriodId] ASC),

    CONSTRAINT [FK_SUBSCRIPTION_PLAN_BILLING_PERIOD_PLAN]
    FOREIGN KEY ([SubscriptionPlanId]) REFERENCES [dbo].[SUBSCRIPTION_PLAN]([SubscriptionPlanId]),

    CONSTRAINT [UQ_SUBSCRIPTION_PLAN_BILLING_PERIOD_Plan_Code]
    UNIQUE ([SubscriptionPlanId], [BillingPeriodCode]),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_PlanId]
    CHECK ([SubscriptionPlanId] > 0),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_Code_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([BillingPeriodCode]))) > 0),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_Name_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([BillingPeriodName]))) > 0),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_BillingPeriodMonths]
    CHECK ([BillingPeriodMonths] > 0),

    CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_PriceAmount]
    CHECK ([PriceAmount] >= 0)
    );
END;
GO


IF OBJECT_ID(N'[dbo].[CUSTOMER_MODULE]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[CUSTOMER_MODULE] (
    [CustomerModuleId] INT IDENTITY(1,1) NOT NULL,

    [CustomerId] INT NOT NULL,
    [SubscriptionPlanId] INT NOT NULL,
    [SubscriptionPlanBillingPeriodId] INT NULL,

    [ModuleCode] NVARCHAR(100) NOT NULL,
    [ModuleName] NVARCHAR(255) NULL,

    [CustomerModuleStatus] INT NOT NULL
    CONSTRAINT [DF_CUSTOMER_MODULE_Status] DEFAULT 1,

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_MODULE_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [UpdatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_MODULE_UpdatedAt] DEFAULT SYSUTCDATETIME(),

    [Latest] BIT NOT NULL
    CONSTRAINT [DF_CUSTOMER_MODULE_Latest] DEFAULT 1,

    CONSTRAINT [PK_CUSTOMER_MODULE]
    PRIMARY KEY CLUSTERED ([CustomerModuleId] ASC),

    CONSTRAINT [CK_CUSTOMER_MODULE_CustomerId]
    CHECK ([CustomerId] > 0),

    CONSTRAINT [CK_CUSTOMER_MODULE_SubscriptionPlanId]
    CHECK ([SubscriptionPlanId] > 0),

    CONSTRAINT [FK_CUSTOMER_MODULE_SubscriptionPlanBillingPeriod]
    FOREIGN KEY ([SubscriptionPlanBillingPeriodId]) REFERENCES [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD]([SubscriptionPlanBillingPeriodId]),

    CONSTRAINT [CK_CUSTOMER_MODULE_ModuleCode_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([ModuleCode]))) > 0),

    CONSTRAINT [CK_CUSTOMER_MODULE_Status]
    CHECK ([CustomerModuleStatus] IN (
            1,  -- ACTIVE
            2,  -- TRIAL
            3,  -- SUSPENDED
            4,  -- CANCELLED
            5   -- EXPIRED
                                     ))
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_MODULE_CustomerId_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_MODULE]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_MODULE_CustomerId_Latest]
    ON [dbo].[CUSTOMER_MODULE] ([CustomerId], [Latest])
    INCLUDE ([SubscriptionPlanId], [ModuleCode], [CustomerModuleStatus]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_MODULE_BillingPeriod_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_MODULE]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_MODULE_BillingPeriod_Latest]
    ON [dbo].[CUSTOMER_MODULE] ([SubscriptionPlanBillingPeriodId], [Latest])
    INCLUDE ([CustomerId], [SubscriptionPlanId], [ModuleCode], [CustomerModuleStatus]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_MODULE_ModuleCode_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_MODULE]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_MODULE_ModuleCode_Latest]
    ON [dbo].[CUSTOMER_MODULE] ([ModuleCode], [Latest])
    INCLUDE ([CustomerId], [SubscriptionPlanId], [CustomerModuleStatus]);
END;
GO


IF NOT EXISTS (
    SELECT 1
    FROM [dbo].[SUBSCRIPTION_PLAN]
    WHERE [ModuleCode] = N'BASIS-MODULE'
      AND [PlanName] = N'Standard'
      AND [ValidFrom] = CONVERT(date, SYSUTCDATETIME())
)
BEGIN
INSERT INTO [dbo].[SUBSCRIPTION_PLAN] (
    [ModuleCode],
    [ModuleName],
    [PlanName],
    [Description],
    [ValidFrom],
    [ValidTo],
    [PriceAmount],
    [Currency],
    [BillingPeriodMonths],
    [TrialDays],
    [Active]
)
VALUES (
    N'BASIS-MODULE',
    N'Basis',
    N'Standard',
    N'Basis Module is to support the complete lifecycle of requirements: from capturing stakeholder needs, deriving system-level specifications, establishing traceability, to verifying compliance.',
    CONVERT(date, SYSUTCDATETIME()),
    NULL,
    1000,
    N'EUR',
    1,
    14,
    1
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM [dbo].[SUBSCRIPTION_PLAN]
    WHERE [ModuleCode] = N'PRO-MODULE'
      AND [PlanName] = N'Standard'
      AND [ValidFrom] = CONVERT(date, SYSUTCDATETIME())
)
BEGIN
INSERT INTO [dbo].[SUBSCRIPTION_PLAN] (
    [ModuleCode],
    [ModuleName],
    [PlanName],
    [Description],
    [ValidFrom],
    [ValidTo],
    [PriceAmount],
    [Currency],
    [BillingPeriodMonths],
    [TrialDays],
    [Active]
)
VALUES (
    N'PRO-MODULE',
    N'Pro',
    N'Standard',
    N'Pro Module is to support and integrate the complete lifecycle of full RFLP methodologies: (R) from capturing stakeholder and system requirements, establishing traceability between stakeholder and system requirement, including all functions in Basis Module.',
    CONVERT(date, SYSUTCDATETIME()),
    NULL,
    0,
    N'EUR',
    1,
    14,
    0
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM [dbo].[SUBSCRIPTION_PLAN]
    WHERE [ModuleCode] = N'MASTER-MODULE'
      AND [PlanName] = N'Standard'
      AND [ValidFrom] = CONVERT(date, SYSUTCDATETIME())
)
BEGIN
INSERT INTO [dbo].[SUBSCRIPTION_PLAN] (
    [ModuleCode],
    [ModuleName],
    [PlanName],
    [Description],
    [ValidFrom],
    [ValidTo],
    [PriceAmount],
    [Currency],
    [BillingPeriodMonths],
    [TrialDays],
    [Active]
)
VALUES (
    N'MASTER-MODULE',
    N'Master',
    N'Standard',
    N'Master Module includes all functionality from Pro Module plus TRL, IRL, DRL and Configuration / Modularisation architectures.',
    CONVERT(date, SYSUTCDATETIME()),
    NULL,
    0,
    N'EUR',
    1,
    14,
    0
    );
END;
GO

INSERT INTO [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD] (
    [SubscriptionPlanId],
    [BillingPeriodCode],
    [BillingPeriodName],
    [Description],
    [BillingPeriodMonths],
    [PriceAmount],
    [Currency],
    [Active]
)
SELECT
    SP.[SubscriptionPlanId],
    BP.[BillingPeriodCode],
    BP.[BillingPeriodName],
    BP.[Description],
    BP.[BillingPeriodMonths],
    CASE
        WHEN SP.[ModuleCode] = N'BASIS-MODULE' AND BP.[BillingPeriodCode] = N'MONTHLY' THEN 1000
        WHEN SP.[ModuleCode] = N'BASIS-MODULE' AND BP.[BillingPeriodCode] = N'QUARTERLY' THEN 3000
        WHEN SP.[ModuleCode] = N'BASIS-MODULE' AND BP.[BillingPeriodCode] = N'SEMI_ANNUAL' THEN 6000
        WHEN SP.[ModuleCode] = N'BASIS-MODULE' AND BP.[BillingPeriodCode] = N'ANNUAL' THEN 12000
        ELSE 0
    END,
    N'EUR',
    1
FROM [dbo].[SUBSCRIPTION_PLAN] SP
CROSS JOIN (
    VALUES
        (N'MONTHLY', N'Monthly', N'Monthly billing period', 1),
        (N'QUARTERLY', N'Quarterly', N'Quarterly billing period', 3),
        (N'SEMI_ANNUAL', N'Semi-annual', N'Semi-annual billing period', 6),
        (N'ANNUAL', N'Annually', N'Annual billing period', 12)
) BP ([BillingPeriodCode], [BillingPeriodName], [Description], [BillingPeriodMonths])
WHERE NOT EXISTS (
    SELECT 1
    FROM [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD] Existing
    WHERE Existing.[SubscriptionPlanId] = SP.[SubscriptionPlanId]
      AND Existing.[BillingPeriodCode] = BP.[BillingPeriodCode]
);
GO
