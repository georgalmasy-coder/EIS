IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[MENU]') AND name = N'Description')
BEGIN
    ALTER TABLE [dbo].[MENU] ADD [Description] [nvarchar](max) NULL;
END
GO
