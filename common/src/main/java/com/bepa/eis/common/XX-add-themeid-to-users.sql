/*
    Adds [ThemeId] to [dbo].[USERS].

    The column is nullable so existing users keep working without a forced default.
*/

IF COL_LENGTH('dbo.USERS', 'ThemeId') IS NULL
BEGIN
ALTER TABLE [dbo].[USERS]
    ADD [ThemeId] [int] NULL;
END
GO
