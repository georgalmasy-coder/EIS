-- Legacy script. Prefer create-user-password-reset-token.sql for new installations.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.USER_PASSWORD_RESET_TOKEN', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.USER_PASSWORD_RESET_TOKEN (
            TokenId int IDENTITY(1,1) NOT NULL PRIMARY KEY,
            UserId int NOT NULL,
            TokenHash varchar(64) NOT NULL,
            ExpiresAt datetime2(3) NOT NULL,
            UsedAt datetime2(3) NULL,
            CreatedAt datetime2(3) NOT NULL CONSTRAINT DF_USER_PASSWORD_RESET_TOKEN_CreatedAt DEFAULT SYSUTCDATETIME(),
            CreatedByUserId int NULL,
            CONSTRAINT FK_USER_PASSWORD_RESET_TOKEN_User FOREIGN KEY (UserId) REFERENCES dbo.USERS(UserId),
            CONSTRAINT UQ_USER_PASSWORD_RESET_TOKEN_Hash UNIQUE (TokenHash)
        );
        CREATE INDEX IX_USER_PASSWORD_RESET_TOKEN_User ON dbo.USER_PASSWORD_RESET_TOKEN(UserId, UsedAt);
    END;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
