/*
    Resets the SQL Server login and database user for EISDB.

    Use this after restoring/moving the database to a new SQL Server instance
    when the database user is orphaned or mapped to the wrong login.

    Run as a sysadmin or with equivalent permissions.
    Replace the password before executing.
*/

DECLARE @DatabaseName sysname = N'EISDB';
DECLARE @LoginName sysname = N'EISDB';
DECLARE @LoginPassword nvarchar(128) = N'REPLACE_WITH_STRONG_PASSWORD';

IF DB_ID(@DatabaseName) IS NULL
BEGIN
    THROW 50000, 'Database EISDB was not found.', 1;
END

USE [EISDB];
GO

/*
    If the database user owns any schemas, move those schema ownership to dbo
    before dropping the user.
*/
DECLARE @DropUserSql nvarchar(max) = N'';

SELECT @DropUserSql += N'ALTER AUTHORIZATION ON SCHEMA::' + QUOTENAME(s.name) + N' TO dbo;'
FROM sys.schemas AS s
WHERE s.principal_id = DATABASE_PRINCIPAL_ID(N'EISDB');

IF (@DropUserSql <> N'')
BEGIN
    EXEC sp_executesql @DropUserSql;
END

IF EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'EISDB')
BEGIN
    DROP USER [EISDB];
END
GO

USE [master];
GO

IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'EISDB')
BEGIN
    DROP LOGIN [EISDB];
END
GO

CREATE LOGIN [EISDB]
WITH PASSWORD = N'REPLACE_WITH_STRONG_PASSWORD',
     DEFAULT_DATABASE = [EISDB],
     CHECK_POLICY = ON,
     CHECK_EXPIRATION = ON;
GO

USE [EISDB];
GO

CREATE USER [EISDB]
FOR LOGIN [EISDB]
WITH DEFAULT_SCHEMA = [dbo];
GO

ALTER ROLE [db_owner] ADD MEMBER [EISDB];
GO
