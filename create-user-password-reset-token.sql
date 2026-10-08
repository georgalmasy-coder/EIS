-- SQL Server: create the table used by Forgot password.
-- Select the EIS database used by Tomcat before executing this script.
-- Can be run in both installations and rerun without deleting existing tokens.
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.USERS', N'U') IS NULL
    THROW 50000, 'dbo.USERS was not found. Select the correct EIS database.', 1;

IF COL_LENGTH(N'dbo.USERS', N'UserId') IS NULL
    THROW 50001, 'dbo.USERS.UserId was not found.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.USER_PASSWORD_RESET_TOKEN', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.USER_PASSWORD_RESET_TOKEN (
            TokenId int IDENTITY(1,1) NOT NULL,
            UserId int NOT NULL,
            -- Only the hash of the random email token is stored here.
            -- Base64 tokens are case-sensitive, so use a binary collation.
            TokenHash varchar(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
            ExpiresAt datetime2(3) NOT NULL,
            UsedAt datetime2(3) NULL,
            CreatedAt datetime2(3) NOT NULL
                CONSTRAINT DF_USER_PASSWORD_RESET_TOKEN_CreatedAt DEFAULT SYSUTCDATETIME(),
            CreatedByUserId int NULL,

            CONSTRAINT PK_USER_PASSWORD_RESET_TOKEN PRIMARY KEY (TokenId),
            CONSTRAINT UQ_USER_PASSWORD_RESET_TOKEN_Hash UNIQUE (TokenHash),
            CONSTRAINT FK_USER_PASSWORD_RESET_TOKEN_User
                FOREIGN KEY (UserId) REFERENCES dbo.USERS(UserId),
            CONSTRAINT FK_USER_PASSWORD_RESET_TOKEN_CreatedByUser
                FOREIGN KEY (CreatedByUserId) REFERENCES dbo.USERS(UserId)
        );

        CREATE INDEX IX_USER_PASSWORD_RESET_TOKEN_User
            ON dbo.USER_PASSWORD_RESET_TOKEN(UserId, UsedAt);

        PRINT 'dbo.USER_PASSWORD_RESET_TOKEN created successfully.';
    END
    ELSE
        PRINT 'dbo.USER_PASSWORD_RESET_TOKEN already exists; no changes made.';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

-- Confirm database and column definitions without displaying tokens or passwords.
SELECT DB_NAME() AS DatabaseName, N'dbo.USER_PASSWORD_RESET_TOKEN' AS TableName;

SELECT
    c.name AS ColumnName,
    TYPE_NAME(c.user_type_id) AS DataType,
    c.max_length AS MaxLengthBytes,
    c.is_nullable AS IsNullable,
    c.is_identity AS IsIdentity
FROM sys.columns c
WHERE c.object_id = OBJECT_ID(N'dbo.USER_PASSWORD_RESET_TOKEN')
ORDER BY c.column_id;
