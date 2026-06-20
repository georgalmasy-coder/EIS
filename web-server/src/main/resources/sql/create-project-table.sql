USE [EISDB]
GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

IF OBJECT_ID(N'[dbo].[PROJECT]', N'U') IS NOT NULL
BEGIN
DROP TABLE [dbo].[PROJECT]
END
GO

CREATE TABLE [dbo].[PROJECT](
    [ProjectPK] [int] IDENTITY(1,1) NOT NULL,
    [CustomerId] [int] NOT NULL,
    [ProjectId] [int] NOT NULL,
    [Version] [int] NOT NULL,
    [Latest] [bit] NOT NULL,

    [ProjectName] [nvarchar](100) NOT NULL,
    [OwnerId] [int] NULL,
    [CategoryId] [int] NULL,
    [PriorityId] [int] NULL,
    [ProjectStatus] [int] NOT NULL,
    [StartDate] [datetime] NULL,
    [EndDate] [datetime] NULL,
    [BudgetInDays] [int] NULL,
    [BudgetInValue] [decimal](19, 2) NULL,
    [DepartmentId] [int] NULL,

    [ChangedByUserId] [int] NOT NULL,
    [ChangedDateTime] [datetime] NOT NULL,

    CONSTRAINT [PK_PROJECT] PRIMARY KEY CLUSTERED
(
[ProjectPK] ASC
) WITH (
      PAD_INDEX = OFF,
      STATISTICS_NORECOMPUTE = OFF,
      IGNORE_DUP_KEY = OFF,
      ALLOW_ROW_LOCKS = ON,
      ALLOW_PAGE_LOCKS = ON,
      OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF
      ) ON [PRIMARY]
    ) ON [PRIMARY]
    GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_PROJECT_ProjectId_Version]
ON [dbo].[PROJECT] (
    [ProjectId] ASC,
    [Version] ASC
)
GO

CREATE NONCLUSTERED INDEX [IX_PROJECT_ProjectId_Latest]
ON [dbo].[PROJECT] (
    [ProjectId] ASC,
    [Latest] ASC
)
GO

CREATE NONCLUSTERED INDEX [IX_PROJECT_CustomerId_Latest]
ON [dbo].[PROJECT] (
    [CustomerId] ASC,
    [Latest] ASC
)
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_PROJECT_ProjectId_Latest]
ON [dbo].[PROJECT] (
    [ProjectId] ASC
)
WHERE [Latest] = 1
GO

ALTER TABLE [dbo].[PROJECT]
    ADD CONSTRAINT [DF_PROJECT_Version]
    DEFAULT ((1)) FOR [Version]
    GO

ALTER TABLE [dbo].[PROJECT]
    ADD CONSTRAINT [DF_PROJECT_Latest]
    DEFAULT ((1)) FOR [Latest]
    GO

ALTER TABLE [dbo].[PROJECT]
    ADD CONSTRAINT [DF_PROJECT_ChangedDateTime]
    DEFAULT (SYSUTCDATETIME()) FOR [ChangedDateTime]
    GO