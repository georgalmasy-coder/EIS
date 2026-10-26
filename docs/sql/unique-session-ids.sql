-- Apply to EISDB before deploying the new session implementation.
-- Preserves all session rows, login history and user preferences.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.SESSION') AND name = N'SessionId'
      AND collation_name <> N'Latin1_General_100_BIN2'
)
    ALTER TABLE dbo.[SESSION]
    ALTER COLUMN SessionId varchar(100) COLLATE Latin1_General_100_BIN2 NOT NULL;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.SESSION') AND name = N'UX_SESSION_SessionId'
)
    CREATE UNIQUE INDEX UX_SESSION_SessionId ON dbo.[SESSION] (SessionId);

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.SESSION') AND name = N'IX_SESSION_LastAccessed'
)
    CREATE INDEX IX_SESSION_LastAccessed ON dbo.[SESSION] (LastAccessed) INCLUDE (UserId);

COMMIT TRANSACTION;
