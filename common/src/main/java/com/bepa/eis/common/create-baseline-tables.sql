IF OBJECT_ID(N'[dbo].[BASELINE]', N'U') IS NULL
BEGIN
CREATE TABLE [dbo].[BASELINE] (
    [BaselinePK] INT IDENTITY(1,1) NOT NULL,

    [CustomerId] INT NOT NULL,
    [ProjectId] INT NOT NULL,

    [TagName] NVARCHAR(150) NOT NULL,
    [Description] NVARCHAR(MAX) NOT NULL,

    [ChangedByUserId] INT NOT NULL,

    [ChangedDateTime] DATETIME2(0) NOT NULL
    CONSTRAINT [DF_BASELINE_ChangedDateTime] DEFAULT SYSDATETIME(),

    CONSTRAINT [PK_BASELINE]
    PRIMARY KEY CLUSTERED ([BaselinePK] ASC),

    CONSTRAINT [CK_BASELINE_TagName_NotBlank]
    CHECK (LEN(LTRIM(RTRIM([TagName]))) > 0),

    CONSTRAINT [CK_BASELINE_Description_NotBlank]
    CHECK (LEN(LTRIM(RTRIM([Description]))) > 0)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UQ_BASELINE_CustomerId_ProjectId_TagName'
      AND object_id = OBJECT_ID(N'[dbo].[BASELINE]')
)
BEGIN
CREATE UNIQUE INDEX [UQ_BASELINE_CustomerId_ProjectId_TagName]
    ON [dbo].[BASELINE] (
    [CustomerId],
    [ProjectId],
    [TagName]
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_BASELINE_CustomerId_ProjectId_ChangedDateTime'
      AND object_id = OBJECT_ID(N'[dbo].[BASELINE]')
)
BEGIN
CREATE INDEX [IX_BASELINE_CustomerId_ProjectId_ChangedDateTime]
    ON [dbo].[BASELINE] (
    [CustomerId],
    [ProjectId],
    [ChangedDateTime] DESC
    )
    INCLUDE (
    [TagName],
    [ChangedByUserId]
    );
END;
GO
