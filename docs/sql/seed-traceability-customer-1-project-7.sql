/*
One-off SQL Server performance fixture for CustomerId=1 / ProjectId=6 only.

1. Select the EIS test database in your SQL console.
2. Run unchanged (@ApplyChanges=0) to preview counts. No application data is changed.
3. Review the counts and @CreatedById, then set @ApplyChanges=1 and run once.

Confirmed pairs are created as ROUND(active stakeholder count * active system
count * 0.03). NEWID() randomly ranks eligible pairs; selection is not based on every nth entity.

A transaction-scoped application lock serializes copies of this script. Held locks
also keep requirements and the relation snapshot stable during insertion. Run while
this test project is idle. Selection sorts the Cartesian product in tempdb.

Any existing ENTITY_RELATIONS record for this customer/project skips the entire
run, regardless of entity type, relation type or Latest. No completion marker or
extended property is written. If all project relations are deleted, rerunning is possible.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @ApplyChanges bit = 0;
DECLARE @CreatedById int = 1; -- Existing active USERS.UserId used for audit attribution.

DECLARE @CustomerId int = 1;
DECLARE @ProjectId int = 6;
DECLARE @StakeholderType int = 5; -- EntityType.STAKEHOLDER_REQUIREMENT
DECLARE @SystemType int = 6;      -- EntityType.SYSTEM_REQUIREMENT
DECLARE @ConfirmedType int = 1;   -- RelationType.CONFIRMED
DECLARE @LockResult int;
DECLARE @CreatedTime datetime2 = SYSDATETIME();

IF @CustomerId <> 1 OR @ProjectId <> 6
    THROW 50001, 'This script is restricted to CustomerId=1 and ProjectId=6.', 1;

IF @@TRANCOUNT <> 0
    THROW 50002, 'Run this script outside an existing transaction.', 1;

-- Allow preview followed by application in the same console/session.
DROP TABLE IF EXISTS #TraceSeedStakeholders;
DROP TABLE IF EXISTS #TraceSeedSystems;
DROP TABLE IF EXISTS #TraceSeedSelected;
DROP TABLE IF EXISTS #TraceSeedInserted;

BEGIN TRY
    BEGIN TRANSACTION;

    EXEC @LockResult = sys.sp_getapplock
        @Resource = N'EIS.TraceabilitySeed.Customer1.Project6',
        @LockMode = N'Exclusive',
        @LockOwner = N'Transaction',
        @LockTimeout = 10000;

    IF @LockResult < 0
        THROW 50003, 'Could not obtain the project seed lock. No changes were made.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.ENTITY_RELATIONS WITH (UPDLOCK, HOLDLOCK)
        WHERE CustomerId = @CustomerId AND ProjectId = @ProjectId
    )
    BEGIN
        SELECT N'Skipped: relation records already exist for CustomerId=1 / ProjectId=6.' AS Result;
        COMMIT TRANSACTION;
        RETURN;
    END;

    CREATE TABLE #TraceSeedStakeholders (EntityId int NOT NULL PRIMARY KEY);
    CREATE TABLE #TraceSeedSystems (EntityId int NOT NULL PRIMARY KEY);

    INSERT #TraceSeedStakeholders (EntityId)
    SELECT DISTINCT EntityId
    FROM dbo.ENTITY WITH (HOLDLOCK)
    WHERE CustomerId = @CustomerId AND ProjectId = @ProjectId
      AND EntityType = @StakeholderType AND Latest = 1 AND Active = 1;

    INSERT #TraceSeedSystems (EntityId)
    SELECT DISTINCT EntityId
    FROM dbo.ENTITY WITH (HOLDLOCK)
    WHERE CustomerId = @CustomerId AND ProjectId = @ProjectId
      AND EntityType = @SystemType AND Latest = 1 AND Active = 1;

    DECLARE @Stakeholders bigint = (SELECT COUNT_BIG(*) FROM #TraceSeedStakeholders);
    DECLARE @Systems bigint = (SELECT COUNT_BIG(*) FROM #TraceSeedSystems);
    IF @Stakeholders = 0 OR @Systems = 0
    BEGIN
        SELECT DB_NAME() AS DatabaseName, @CustomerId AS CustomerId, @ProjectId AS ProjectId,
               @Stakeholders AS ActiveStakeholderRequirements,
               @Systems AS ActiveSystemRequirements;

        -- Diagnose project placement and flags without changing the requested scope.
        SELECT ProjectId, EntityType, Latest, Active, COUNT_BIG(*) AS RecordCount
        FROM dbo.ENTITY
        WHERE CustomerId = @CustomerId
          AND EntityType IN (@StakeholderType, @SystemType)
        GROUP BY ProjectId, EntityType, Latest, Active
        ORDER BY ProjectId, EntityType, Latest DESC, Active DESC;

        DECLARE @MissingRequirementsMessage nvarchar(2048) = CONCAT(
            N'Database ', DB_NAME(), N': CustomerId=1 / ProjectId=6 has ',
            @Stakeholders, N' active latest stakeholder requirements (type 5) and ',
            @Systems, N' active latest system requirements (type 6). No relations inserted. See diagnostic results.');
        THROW 50004, @MissingRequirementsMessage, 1;
    END;

    DECLARE @Combinations bigint = @Stakeholders * @Systems;
    DECLARE @Target bigint = CONVERT(bigint, ROUND(CONVERT(decimal(28, 0), @Combinations) * 0.17, 0));
    DECLARE @ToInsert bigint = @Target;

    SELECT DB_NAME() AS DatabaseName, @CustomerId AS CustomerId, @ProjectId AS ProjectId,
           @ApplyChanges AS ApplyChanges, @CreatedById AS CreatedById,
           @Stakeholders AS ActiveStakeholderRequirements, @Systems AS ActiveSystemRequirements,
           @Combinations AS Combinations, @Target AS TargetConfirmedPairs,
           @ToInsert AS NewConfirmedPairs;

    IF @ApplyChanges = 0
    BEGIN
        ROLLBACK TRANSACTION;
        SELECT N'Preview only. Set @ApplyChanges=1 to insert relations.' AS Result;
        RETURN;
    END;

    IF @ToInsert > 0 AND NOT EXISTS (
        SELECT 1 FROM dbo.USERS WHERE UserId = @CreatedById AND Active = 1
    )
        THROW 50006, 'CreatedById must identify an existing active user.', 1;

    SELECT TOP (@ToInsert) S.EntityId AS StakeholderId, Y.EntityId AS SystemId
    INTO #TraceSeedSelected
    FROM #TraceSeedStakeholders S
    CROSS JOIN #TraceSeedSystems Y
    ORDER BY NEWID();

    IF (SELECT COUNT_BIG(*) FROM #TraceSeedSelected) <> @ToInsert
        THROW 50007, 'Random selection did not produce the expected number of pairs.', 1;

    CREATE TABLE #TraceSeedInserted (
        EntityRelationPK bigint NOT NULL,
        StakeholderId int NOT NULL,
        SystemId int NOT NULL
    );

    -- No earlier record exists for these pairs, so Version=1 and Latest=1.
    INSERT dbo.ENTITY_RELATIONS
        (CustomerId, ProjectId, EntityType, EntityId, RelatedEntityType, RelatedEntityId,
         RelationType, Version, CreatedById, CreatedTime, Latest)
    OUTPUT inserted.EntityRelationPK, inserted.EntityId, inserted.RelatedEntityId
        INTO #TraceSeedInserted (EntityRelationPK, StakeholderId, SystemId)
    SELECT @CustomerId, @ProjectId, @StakeholderType, StakeholderId, @SystemType, SystemId,
           @ConfirmedType, 1, @CreatedById, @CreatedTime, 1
    FROM #TraceSeedSelected;

    DECLARE @Inserted bigint = (SELECT COUNT_BIG(*) FROM #TraceSeedInserted);
    IF @Inserted <> @ToInsert
        THROW 50008, 'Inserted row count differs from the requested count.', 1;

    COMMIT TRANSACTION;

    SELECT N'Applied successfully; later runs skip while project relation records exist.' AS Result,
           @CustomerId AS CustomerId, @ProjectId AS ProjectId, @Inserted AS InsertedRelations,
           @Inserted AS ConfirmedPairs, @Combinations AS Combinations,
           CONVERT(decimal(9, 4), 100.0 * (@Inserted) / @Combinations) AS ConfirmedPercent,
           @CreatedTime AS CreatedTime;

    -- Small sample for inspection; these are actual inserted ids, not another draw.
    SELECT TOP (20) EntityRelationPK, StakeholderId, SystemId
    FROM #TraceSeedInserted ORDER BY EntityRelationPK;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
