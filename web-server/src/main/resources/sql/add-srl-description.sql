ALTER TABLE [dbo].[SRL]
ADD [SRLDescription] NVARCHAR(255) NOT NULL
    CONSTRAINT [DF_SRL_SRLDescription] DEFAULT ('');
GO

UPDATE [dbo].[SRL]
SET [SRLDescription] = [SRLName]
WHERE [SRLDescription] = '' OR [SRLDescription] IS NULL;
GO
