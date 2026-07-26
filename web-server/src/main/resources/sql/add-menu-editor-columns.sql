IF COL_LENGTH('dbo.MENU', 'CustomerIdRequired') IS NULL
BEGIN
    ALTER TABLE dbo.MENU
        ADD CustomerIdRequired bit NULL;
END;
GO

IF COL_LENGTH('dbo.MENU', 'ProjectIdRequired') IS NULL
BEGIN
    ALTER TABLE dbo.MENU
        ADD ProjectIdRequired bit NULL;
END;
GO

IF COL_LENGTH('dbo.MENU', 'UserRoles') IS NULL
BEGIN
    ALTER TABLE dbo.MENU
        ADD UserRoles nvarchar(20) NULL;
END;
GO

IF COL_LENGTH('dbo.MENU', 'Active') IS NULL
BEGIN
    ALTER TABLE dbo.MENU
        ADD Active bit NULL;
END;
GO

UPDATE dbo.MENU
SET CustomerIdRequired = COALESCE(CustomerIdRequired, 0),
    ProjectIdRequired = COALESCE(ProjectIdRequired, 0),
    UserRoles = COALESCE(UserRoles, ''),
    Active = COALESCE(Active, 1);
GO
