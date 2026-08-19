-- SQL Script til genoprettelse af EISDB bruger og login

-- 1) Dropper EISDB user i EISDB databasen
USE [EISDB];
GO
IF EXISTS (SELECT * FROM sys.database_principals WHERE name = 'EISDB')
BEGIN
    DROP USER [EISDB];
END
GO

-- 2) Dropper EISDB login i master
USE [master];
GO
IF EXISTS (SELECT * FROM sys.server_principals WHERE name = 'EISDB')
BEGIN
    DROP LOGIN [EISDB];
END
GO

-- 3) Opretter EISDB login i master med default database EISDB og password s3cret
-- Bemærk: CHECK_POLICY er sat til OFF for at tillade det simple password 's3cret'
CREATE LOGIN [EISDB] 
    WITH PASSWORD = 's3cret', 
    DEFAULT_DATABASE = [EISDB],
    CHECK_EXPIRATION = OFF,
    CHECK_POLICY = OFF;
GO

-- 4) Opretter EISDB user i EISDB databasen med default schema dbo
USE [EISDB];
GO
CREATE USER [EISDB] FOR LOGIN [EISDB] WITH DEFAULT_SCHEMA = [dbo];
-- Tildeler de nødvendige rettigheder (typisk db_owner for EISDB brugeren)
ALTER ROLE [db_owner] ADD MEMBER [EISDB];
GO
