SET XACT_ABORT ON;
GO

IF OBJECT_ID(N'dbo.INTERFACES', N'U') IS NULL
    THROW 50000, 'Table dbo.INTERFACES does not exist.', 1;
GO

IF COLUMNPROPERTY(OBJECT_ID(N'dbo.INTERFACES'), N'InterfacePK', 'IsIdentity') = 0
BEGIN
    BEGIN TRANSACTION;

    DECLARE @PrimaryKeyConstraint sysname;
    SELECT @PrimaryKeyConstraint = kc.name
    FROM sys.key_constraints kc
    WHERE kc.parent_object_id = OBJECT_ID(N'dbo.INTERFACES')
      AND kc.[type] = 'PK';

    IF @PrimaryKeyConstraint IS NOT NULL
    BEGIN
        DECLARE @DropPrimaryKeySql nvarchar(max);
        SET @DropPrimaryKeySql =
            N'ALTER TABLE dbo.INTERFACES DROP CONSTRAINT ' + QUOTENAME(@PrimaryKeyConstraint) + N';';
        EXEC sys.sp_executesql @DropPrimaryKeySql;
    END;

    ALTER TABLE dbo.INTERFACES
        ADD InterfacePK_Identity int IDENTITY(1,1) NOT NULL;

    ALTER TABLE dbo.INTERFACES DROP COLUMN InterfacePK;
    EXEC sys.sp_rename
        @objname = N'dbo.INTERFACES.InterfacePK_Identity',
        @newname = N'InterfacePK',
        @objtype = N'COLUMN';

    ALTER TABLE dbo.INTERFACES
        ADD CONSTRAINT PK_INTERFACES PRIMARY KEY CLUSTERED (InterfacePK ASC);

    COMMIT TRANSACTION;
END;
GO
