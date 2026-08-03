IF COL_LENGTH(N'dbo.SUBSCRIPTION_PLAN', N'ValidFrom') IS NULL
BEGIN
    ALTER TABLE [dbo].[SUBSCRIPTION_PLAN]
    ADD [ValidFrom] DATE NOT NULL
        CONSTRAINT [DF_SUBSCRIPTION_PLAN_ValidFrom] DEFAULT (CONVERT(date, SYSUTCDATETIME()));
END;
GO

IF COL_LENGTH(N'dbo.SUBSCRIPTION_PLAN', N'ValidTo') IS NULL
BEGIN
    ALTER TABLE [dbo].[SUBSCRIPTION_PLAN]
    ADD [ValidTo] DATE NULL;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = N'UQ_SUBSCRIPTION_PLAN_ModuleCode_PlanName'
      AND parent_object_id = OBJECT_ID(N'[dbo].[SUBSCRIPTION_PLAN]')
)
BEGIN
    ALTER TABLE [dbo].[SUBSCRIPTION_PLAN]
    DROP CONSTRAINT [UQ_SUBSCRIPTION_PLAN_ModuleCode_PlanName];
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = N'UQ_SUBSCRIPTION_PLAN_ModuleCode_PlanName_ValidFrom'
      AND parent_object_id = OBJECT_ID(N'[dbo].[SUBSCRIPTION_PLAN]')
)
BEGIN
    ALTER TABLE [dbo].[SUBSCRIPTION_PLAN]
    ADD CONSTRAINT [UQ_SUBSCRIPTION_PLAN_ModuleCode_PlanName_ValidFrom]
    UNIQUE ([ModuleCode], [PlanName], [ValidFrom]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_SUBSCRIPTION_PLAN_ValidTo'
      AND parent_object_id = OBJECT_ID(N'[dbo].[SUBSCRIPTION_PLAN]')
)
BEGIN
    ALTER TABLE [dbo].[SUBSCRIPTION_PLAN]
    ADD CONSTRAINT [CK_SUBSCRIPTION_PLAN_ValidTo]
    CHECK ([ValidTo] IS NULL OR [ValidTo] >= [ValidFrom]);
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
            CONSTRAINT [DF_SUBSCRIPTION_PLAN_BILLING_PERIOD_Months] DEFAULT 1,
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
            FOREIGN KEY ([SubscriptionPlanId]) REFERENCES [dbo].[SUBSCRIPTION_PLAN] ([SubscriptionPlanId]),
        CONSTRAINT [UQ_SUBSCRIPTION_PLAN_BILLING_PERIOD_Plan_Code]
            UNIQUE ([SubscriptionPlanId], [BillingPeriodCode]),
        CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_PlanId]
            CHECK ([SubscriptionPlanId] > 0),
        CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_Code_NotEmpty]
            CHECK (LEN(LTRIM(RTRIM([BillingPeriodCode]))) > 0),
        CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_Name_NotEmpty]
            CHECK (LEN(LTRIM(RTRIM([BillingPeriodName]))) > 0),
        CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_Months]
            CHECK ([BillingPeriodMonths] > 0),
        CONSTRAINT [CK_SUBSCRIPTION_PLAN_BILLING_PERIOD_PriceAmount]
            CHECK ([PriceAmount] >= 0)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_SUBSCRIPTION_PLAN_BILLING_PERIOD_PlanId'
      AND object_id = OBJECT_ID(N'[dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD]')
)
BEGIN
    CREATE INDEX [IX_SUBSCRIPTION_PLAN_BILLING_PERIOD_PlanId]
        ON [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD] ([SubscriptionPlanId], [BillingPeriodMonths]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM [dbo].[SUBSCRIPTION_PLAN]
    WHERE [ModuleCode] = N'BASIS-MODULE'
      AND [PlanName] = N'Standard'
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
