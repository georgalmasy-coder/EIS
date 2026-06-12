IF OBJECT_ID(N'[dbo].[CUSTOMER_WORKFLOW]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[CUSTOMER_WORKFLOW] (
    [WorkflowId] INT IDENTITY(1,1) NOT NULL,

    [CustomerId] INT NOT NULL,

    [WorkflowType] NVARCHAR(100) NOT NULL
    CONSTRAINT [DF_CUSTOMER_WORKFLOW_WorkflowType] DEFAULT N'CUSTOMER_ONBOARDING',

    [WorkflowStatus] NVARCHAR(30) NOT NULL
    CONSTRAINT [DF_CUSTOMER_WORKFLOW_WorkflowStatus] DEFAULT N'ACTIVE',

    [CurrentState] NVARCHAR(100) NOT NULL,

    [SubscriptionId] INT NULL,
    [PaymentId] INT NULL,

    [NextActionAt] DATETIME2(0) NULL,

    [RetryCount] INT NOT NULL
    CONSTRAINT [DF_CUSTOMER_WORKFLOW_RetryCount] DEFAULT 0,

    [LastEventType] NVARCHAR(100) NULL,
    [LastEventAt] DATETIME2(0) NULL,

    [LastError] NVARCHAR(MAX) NULL,

    [LockedAt] DATETIME2(0) NULL,
    [LockedBy] NVARCHAR(255) NULL,

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_WORKFLOW_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [UpdatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_WORKFLOW_UpdatedAt] DEFAULT SYSUTCDATETIME(),

    CONSTRAINT [PK_CUSTOMER_WORKFLOW]
    PRIMARY KEY CLUSTERED ([WorkflowId] ASC),

    CONSTRAINT [CK_CUSTOMER_WORKFLOW_WorkflowStatus]
    CHECK ([WorkflowStatus] IN (
            N'ACTIVE',
            N'COMPLETED',
            N'CANCELLED',
            N'SUSPENDED',
            N'WAITING_FOR_MANUAL_ATTENTION'
                               )),

    CONSTRAINT [CK_CUSTOMER_WORKFLOW_RetryCount]
    CHECK ([RetryCount] >= 0)
    );
END;
GO

IF OBJECT_ID(N'[dbo].[CUSTOMER_WORKFLOW_EVENT]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[CUSTOMER_WORKFLOW_EVENT] (
    [WorkflowEventId] INT IDENTITY(1,1) NOT NULL,

    [WorkflowId] INT NOT NULL,
    [CustomerId] INT NOT NULL,

    [EventType] NVARCHAR(100) NOT NULL,
    [EventCategory] NVARCHAR(50) NULL,

    [FromState] NVARCHAR(100) NULL,
    [ToState] NVARCHAR(100) NULL,

    [Description] NVARCHAR(1000) NULL,
    [PayloadJson] NVARCHAR(MAX) NULL,

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_WORKFLOW_EVENT_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [CreatedByUserId] INT NULL,

    CONSTRAINT [PK_CUSTOMER_WORKFLOW_EVENT]
    PRIMARY KEY CLUSTERED ([WorkflowEventId] ASC)
    );
END;
GO

IF OBJECT_ID(N'[dbo].[CUSTOMER_SUBSCRIPTION]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[CUSTOMER_SUBSCRIPTION] (
    [SubscriptionId] INT IDENTITY(1,1) NOT NULL,

    [CustomerId] INT NOT NULL,

    [SubscriptionStatus] NVARCHAR(100) NOT NULL,

    [SubscriptionPlanId] INT NULL,
    [SubscriptionPlanName] NVARCHAR(255) NULL,

    [TrialStartAt] DATETIME2(0) NULL,
    [TrialEndAt] DATETIME2(0) NULL,
    [TrialReminderSentAt] DATETIME2(0) NULL,

    [PeriodStartAt] DATETIME2(0) NULL,
    [PeriodEndAt] DATETIME2(0) NULL,
    [RenewalReminderSentAt] DATETIME2(0) NULL,

    [ContinuationConfirmedAt] DATETIME2(0) NULL,
    [RenewalConfirmedAt] DATETIME2(0) NULL,

    [GracePeriodEndsAt] DATETIME2(0) NULL,

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_SUBSCRIPTION_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [UpdatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_SUBSCRIPTION_UpdatedAt] DEFAULT SYSUTCDATETIME(),

    CONSTRAINT [PK_CUSTOMER_SUBSCRIPTION]
    PRIMARY KEY CLUSTERED ([SubscriptionId] ASC)
    );
END;
GO

IF OBJECT_ID(N'[dbo].[CUSTOMER_PAYMENT]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[CUSTOMER_PAYMENT] (
    [PaymentId] INT IDENTITY(1,1) NOT NULL,

    [CustomerId] INT NOT NULL,
    [SubscriptionId] INT NULL,

    [PaymentStatus] NVARCHAR(100) NOT NULL,

    [PaymentProvider] NVARCHAR(100) NULL,
    [PaymentProviderReference] NVARCHAR(255) NULL,

    [Amount] DECIMAL(18,2) NOT NULL
    CONSTRAINT [DF_CUSTOMER_PAYMENT_Amount] DEFAULT 0,

    [Currency] NVARCHAR(10) NOT NULL
    CONSTRAINT [DF_CUSTOMER_PAYMENT_Currency] DEFAULT N'EUR',

    [PaymentDueAt] DATETIME2(0) NULL,
    [GracePeriodEndsAt] DATETIME2(0) NULL,

    [RequestedAt] DATETIME2(0) NULL,
    [AuthorizedAt] DATETIME2(0) NULL,
    [CapturedAt] DATETIME2(0) NULL,
    [SucceededAt] DATETIME2(0) NULL,
    [FailedAt] DATETIME2(0) NULL,
    [CancelledAt] DATETIME2(0) NULL,

    [FailureReason] NVARCHAR(1000) NULL,

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_PAYMENT_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [UpdatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_PAYMENT_UpdatedAt] DEFAULT SYSUTCDATETIME(),

    CONSTRAINT [PK_CUSTOMER_PAYMENT]
    PRIMARY KEY CLUSTERED ([PaymentId] ASC),

    CONSTRAINT [CK_CUSTOMER_PAYMENT_Amount]
    CHECK ([Amount] >= 0)
    );
END;
GO

IF OBJECT_ID(N'[dbo].[CUSTOMER_TOKEN]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[CUSTOMER_TOKEN] (
    [TokenId] INT IDENTITY(1,1) NOT NULL,

    [CustomerId] INT NOT NULL,
    [WorkflowId] INT NULL,
    [SubscriptionId] INT NULL,
    [PaymentId] INT NULL,

    [TokenType] NVARCHAR(100) NOT NULL,
    [TokenHash] NVARCHAR(255) NOT NULL,

    [ExpiresAt] DATETIME2(0) NOT NULL,
    [UsedAt] DATETIME2(0) NULL,

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_CUSTOMER_TOKEN_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [CreatedByUserId] INT NULL,

    CONSTRAINT [PK_CUSTOMER_TOKEN]
    PRIMARY KEY CLUSTERED ([TokenId] ASC),

    CONSTRAINT [UQ_CUSTOMER_TOKEN_TokenHash]
    UNIQUE ([TokenHash])
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_WORKFLOW_CustomerId'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_WORKFLOW]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_WORKFLOW_CustomerId]
    ON [dbo].[CUSTOMER_WORKFLOW] ([CustomerId]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_WORKFLOW_Status_NextActionAt'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_WORKFLOW]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_WORKFLOW_Status_NextActionAt]
    ON [dbo].[CUSTOMER_WORKFLOW] ([WorkflowStatus], [NextActionAt])
    INCLUDE ([CurrentState], [LockedAt], [LockedBy]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_WORKFLOW_EVENT_WorkflowId'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_WORKFLOW_EVENT]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_WORKFLOW_EVENT_WorkflowId]
    ON [dbo].[CUSTOMER_WORKFLOW_EVENT] ([WorkflowId], [CreatedAt]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_WORKFLOW_EVENT_CustomerId'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_WORKFLOW_EVENT]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_WORKFLOW_EVENT_CustomerId]
    ON [dbo].[CUSTOMER_WORKFLOW_EVENT] ([CustomerId], [CreatedAt]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_SUBSCRIPTION_CustomerId'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_SUBSCRIPTION]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_SUBSCRIPTION_CustomerId]
    ON [dbo].[CUSTOMER_SUBSCRIPTION] ([CustomerId]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_SUBSCRIPTION_Status'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_SUBSCRIPTION]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_SUBSCRIPTION_Status]
    ON [dbo].[CUSTOMER_SUBSCRIPTION] ([SubscriptionStatus]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_PAYMENT_CustomerId'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_PAYMENT]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_PAYMENT_CustomerId]
    ON [dbo].[CUSTOMER_PAYMENT] ([CustomerId]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_PAYMENT_SubscriptionId'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_PAYMENT]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_PAYMENT_SubscriptionId]
    ON [dbo].[CUSTOMER_PAYMENT] ([SubscriptionId]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_PAYMENT_Status'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_PAYMENT]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_PAYMENT_Status]
    ON [dbo].[CUSTOMER_PAYMENT] ([PaymentStatus]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_TOKEN_CustomerId_TokenType'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_TOKEN]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_TOKEN_CustomerId_TokenType]
    ON [dbo].[CUSTOMER_TOKEN] ([CustomerId], [TokenType]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_TOKEN_ExpiresAt'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_TOKEN]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_TOKEN_ExpiresAt]
    ON [dbo].[CUSTOMER_TOKEN] ([ExpiresAt])
    INCLUDE ([UsedAt], [TokenType]);
END;
GO