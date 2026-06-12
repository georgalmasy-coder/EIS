IF OBJECT_ID(N'[dbo].[MAIL_QUEUE]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[MAIL_QUEUE] (
    [MailId] INT IDENTITY(1,1) NOT NULL,

    [TemplateType] NVARCHAR(100) NOT NULL,

    [FromName] NVARCHAR(255) NULL,
    [FromEmail] NVARCHAR(320) NOT NULL,

    [ToName] NVARCHAR(255) NULL,
    [ToEmail] NVARCHAR(320) NOT NULL,

    [CcEmails] NVARCHAR(MAX) NULL,
    [BccEmails] NVARCHAR(MAX) NULL,

    [Subject] NVARCHAR(500) NOT NULL,
    [BodyText] NVARCHAR(MAX) NULL,
    [BodyHtml] NVARCHAR(MAX) NULL,

    [ParametersJson] NVARCHAR(MAX) NULL,

    [Status] NVARCHAR(30) NOT NULL
    CONSTRAINT [DF_MAIL_QUEUE_Status] DEFAULT N'QUEUED',

    [AttemptCount] INT NOT NULL
    CONSTRAINT [DF_MAIL_QUEUE_AttemptCount] DEFAULT 0,

    [MaxAttempts] INT NOT NULL
    CONSTRAINT [DF_MAIL_QUEUE_MaxAttempts] DEFAULT 5,

    [NextAttemptAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_MAIL_QUEUE_NextAttemptAt] DEFAULT SYSUTCDATETIME(),

    [CreatedAt] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_MAIL_QUEUE_CreatedAt] DEFAULT SYSUTCDATETIME(),

    [CreatedByUserId] INT NULL,

    [LastAttemptAt] DATETIME2(0) NULL,
    [SentAt] DATETIME2(0) NULL,

    [LastError] NVARCHAR(MAX) NULL,
    [SmtpMessageId] NVARCHAR(255) NULL,

    [LockedAt] DATETIME2(0) NULL,
    [LockedBy] NVARCHAR(255) NULL,

    CONSTRAINT [PK_MAIL_QUEUE]
    PRIMARY KEY CLUSTERED ([MailId] ASC),

    CONSTRAINT [CK_MAIL_QUEUE_Status]
    CHECK ([Status] IN (
            N'QUEUED',
            N'SENDING',
            N'SENT',
            N'FAILED',
            N'UNDELIVERED',
            N'CANCELLED'
                       )),

    CONSTRAINT [CK_MAIL_QUEUE_AttemptCount]
    CHECK ([AttemptCount] >= 0),

    CONSTRAINT [CK_MAIL_QUEUE_MaxAttempts]
    CHECK ([MaxAttempts] >= 1),

    CONSTRAINT [CK_MAIL_QUEUE_Email_ToEmail_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([ToEmail]))) > 0),

    CONSTRAINT [CK_MAIL_QUEUE_Email_FromEmail_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([FromEmail]))) > 0),

    CONSTRAINT [CK_MAIL_QUEUE_Subject_NotEmpty]
    CHECK (LEN(LTRIM(RTRIM([Subject]))) > 0)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_MAIL_QUEUE_Status_NextAttemptAt'
      AND object_id = OBJECT_ID(N'[dbo].[MAIL_QUEUE]')
)
BEGIN
CREATE INDEX [IX_MAIL_QUEUE_Status_NextAttemptAt]
    ON [dbo].[MAIL_QUEUE] ([Status], [NextAttemptAt])
    INCLUDE ([AttemptCount], [MaxAttempts], [LockedAt], [LockedBy]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_MAIL_QUEUE_CreatedAt'
      AND object_id = OBJECT_ID(N'[dbo].[MAIL_QUEUE]')
)
BEGIN
CREATE INDEX [IX_MAIL_QUEUE_CreatedAt]
    ON [dbo].[MAIL_QUEUE] ([CreatedAt]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_MAIL_QUEUE_ToEmail'
      AND object_id = OBJECT_ID(N'[dbo].[MAIL_QUEUE]')
)
BEGIN
CREATE INDEX [IX_MAIL_QUEUE_ToEmail]
    ON [dbo].[MAIL_QUEUE] ([ToEmail]);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_MAIL_QUEUE_SentAt'
      AND object_id = OBJECT_ID(N'[dbo].[MAIL_QUEUE]')
)
BEGIN
CREATE INDEX [IX_MAIL_QUEUE_SentAt]
    ON [dbo].[MAIL_QUEUE] ([SentAt])
    WHERE [SentAt] IS NOT NULL;
END;
GO