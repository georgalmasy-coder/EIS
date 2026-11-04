/*
One-off SQL Server interface fixture for CustomerId=1 / ProjectId=6, per entity type.
Run in the EIS test database while the test project is idle.

1. Choose @EntityType, @InterfacePercent, @ChangedByUserId and @IrlId below.
2. Run with @ApplyChanges=0 to preview counts without inserting data.
3. Set @ApplyChanges=1 and run the entire script to insert.

Latest entities are included regardless of Active. Pairs are directed (A->B and
B->A are separate candidates); self-interfaces are excluded. NEWID() randomly
selects distinct pairs without a regular pattern. No reverse pair is added implicitly.

ANY existing INTERFACES row for this customer/project/entity type skips the run,
including historical rows. Other entity types do not block this type. No completion
marker is written. Deleting all interfaces of this type permits another run.

Inserts match InterfaceMatrixProvider: Version=1, Latest=1, selected IRL,
NextIrlMeeting=NULL, ClassificationIds=NULL. IRL 1 normally means Not Checked.
The transaction holds locks and selection sorts N*(N-1) candidates in tempdb.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

-- Configuration
DECLARE @ApplyChanges bit = 0;
DECLARE @EntityType int = 2; -- 2=Physical architecture, 5=Stakeholder requirement,
                           -- 6=System requirement, 8=Logical, 9=Functional structure.
DECLARE @InterfacePercent decimal(5, 2) = 17.00; -- e.g. 3.00 or 17.00 percent.
DECLARE @ChangedByUserId int = 1; -- Existing active audit user.
DECLARE @IrlId int = 1; -- Existing IRL in this customer/project, used for all inserts.

DECLARE @CustomerId int = 1;
DECLARE @ProjectId int = 6;
DECLARE @LockResult int;
DECLARE @LockResource nvarchar(255) = CONCAT(N'EIS.InterfaceSeed.Customer1.Project6.Type', @EntityType);
DECLARE @ChangedDateTime datetime2 = SYSDATETIME();

IF @CustomerId <> 1 OR @ProjectId <> 6
    THROW 50101, 'This script is restricted to CustomerId=1 and ProjectId=6.', 1;
IF @EntityType IS NULL OR @EntityType NOT IN (2, 5, 6, 8, 9)
    THROW 50102, 'Supported EntityType values are 2, 5, 6, 8 and 9.', 1;
IF @InterfacePercent IS NULL OR @InterfacePercent <= 0 OR @InterfacePercent > 100
    THROW 50103, 'InterfacePercent must be greater than 0 and at most 100.', 1;
IF @@TRANCOUNT <> 0
    THROW 50104, 'Run this script outside an existing transaction.', 1;

-- Allow preview followed by application in the same console/session.
DROP TABLE IF EXISTS #InterfaceSeedEntities;
DROP TABLE IF EXISTS #InterfaceSeedSelected;
DROP TABLE IF EXISTS #InterfaceSeedInserted;

BEGIN TRY
    BEGIN TRANSACTION;

    EXEC @LockResult = sys.sp_getapplock
        @Resource = @LockResource,
        @LockMode = N'Exclusive',
        @LockOwner = N'Transaction',
        @LockTimeout = 10000;

    IF @LockResult < 0
        THROW 50105, 'Could not obtain the interface seed lock. No changes were made.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.INTERFACES WITH (UPDLOCK, HOLDLOCK)
        WHERE CustomerId = @CustomerId AND ProjectId = @ProjectId AND EntityType = @EntityType
    )
    BEGIN
        SELECT N'Skipped: interface records already exist for this customer/project/entity type.' AS Result,
               DB_NAME() AS DatabaseName, @CustomerId AS CustomerId,
               @ProjectId AS ProjectId, @EntityType AS EntityType;
        COMMIT TRANSACTION;
        RETURN;
    END;

    CREATE TABLE #InterfaceSeedEntities (EntityId int NOT NULL PRIMARY KEY);
    INSERT #InterfaceSeedEntities (EntityId)
    SELECT DISTINCT EntityId
    FROM dbo.ENTITY WITH (HOLDLOCK)
    WHERE CustomerId = @CustomerId AND ProjectId = @ProjectId
      AND EntityType = @EntityType AND Latest = 1;

    DECLARE @Entities bigint = (SELECT COUNT_BIG(*) FROM #InterfaceSeedEntities);
    IF @Entities < 2
    BEGIN
        SELECT DB_NAME() AS DatabaseName, @CustomerId AS CustomerId, @ProjectId AS ProjectId,
               @EntityType AS EntityType, @Entities AS LatestEntities;
        THROW 50106, 'At least two latest entities of the selected type are required. No interfaces inserted.', 1;
    END;

    DECLARE @Combinations bigint = @Entities * (@Entities - 1);
    DECLARE @ToInsert bigint = CONVERT(bigint,
        ROUND(CONVERT(decimal(28, 0), @Combinations) * @InterfacePercent / 100.0, 0));

    SELECT DB_NAME() AS DatabaseName, @CustomerId AS CustomerId, @ProjectId AS ProjectId,
           @EntityType AS EntityType, @ApplyChanges AS ApplyChanges,
           @ChangedByUserId AS ChangedByUserId, @IrlId AS IrlId,
           @Entities AS LatestEntities, @Combinations AS DirectedCombinations,
           @InterfacePercent AS TargetPercent, @ToInsert AS NewInterfaces;

    IF @ApplyChanges = 0
    BEGIN
        ROLLBACK TRANSACTION;
        SELECT N'Preview only. Set @ApplyChanges=1 to insert interfaces.' AS Result;
        RETURN;
    END;

    IF NOT EXISTS (SELECT 1 FROM dbo.USERS WHERE UserId = @ChangedByUserId AND Active = 1)
        THROW 50107, 'ChangedByUserId must identify an existing active user.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM dbo.IRL
        WHERE CustomerId = @CustomerId AND ProjectId = @ProjectId AND IrlId = @IrlId
    )
        THROW 50108, 'IrlId must exist for this customer/project.', 1;

    SELECT TOP (@ToInsert) A.EntityId AS FromEntityId, B.EntityId AS ToEntityId
    INTO #InterfaceSeedSelected
    FROM #InterfaceSeedEntities A
    CROSS JOIN #InterfaceSeedEntities B
    WHERE A.EntityId <> B.EntityId
    ORDER BY NEWID();

    IF (SELECT COUNT_BIG(*) FROM #InterfaceSeedSelected) <> @ToInsert
        THROW 50109, 'Random selection did not produce the expected number of pairs.', 1;

    CREATE TABLE #InterfaceSeedInserted (
        InterfacePK bigint NOT NULL,
        FromEntityId int NOT NULL,
        ToEntityId int NOT NULL
    );

    INSERT dbo.INTERFACES
        (CustomerId, ProjectId, EntityType, Version, Latest, ChangedByUserId,
         ChangedDateTime, FromEntityId, ToEntityId, IrlId, NextIrlMeeting, ClassificationIds)
    OUTPUT inserted.InterfacePK, inserted.FromEntityId, inserted.ToEntityId
        INTO #InterfaceSeedInserted (InterfacePK, FromEntityId, ToEntityId)
    SELECT @CustomerId, @ProjectId, @EntityType, 1, 1, @ChangedByUserId,
           @ChangedDateTime, FromEntityId, ToEntityId, @IrlId, NULL, NULL
    FROM #InterfaceSeedSelected;

    DECLARE @Inserted bigint = (SELECT COUNT_BIG(*) FROM #InterfaceSeedInserted);
    IF @Inserted <> @ToInsert
        THROW 50110, 'Inserted row count differs from the requested count.', 1;

    COMMIT TRANSACTION;

    SELECT N'Applied successfully; later runs skip while interface records of this type exist.' AS Result,
           @CustomerId AS CustomerId, @ProjectId AS ProjectId, @EntityType AS EntityType,
           @Inserted AS InsertedInterfaces, @Combinations AS DirectedCombinations,
           CONVERT(decimal(9, 4), 100.0 * @Inserted / @Combinations) AS ActualPercent,
           @ChangedDateTime AS ChangedDateTime;

    SELECT TOP (20) InterfacePK, FromEntityId, ToEntityId
    FROM #InterfaceSeedInserted ORDER BY InterfacePK;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
