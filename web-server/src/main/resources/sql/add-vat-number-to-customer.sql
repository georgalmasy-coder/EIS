/*
    Adds VatNumber to [dbo].[CUSTOMER].

    Existing rows keep NULL in VatNumber.
*/

IF COL_LENGTH('dbo.CUSTOMER', 'VatNumber') IS NULL
BEGIN
ALTER TABLE [dbo].[CUSTOMER]
    ADD [VatNumber] [nvarchar](20) NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_CUSTOMER_VatNumber_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_VatNumber_Latest]
    ON [dbo].[CUSTOMER] ([VatNumber], [Latest])
    WHERE [VatNumber] IS NOT NULL;
END
GO
