IF COL_LENGTH('dbo.LOOKUP_TABLE', 'DisplayOrder') IS NULL
BEGIN
    ALTER TABLE dbo.LOOKUP_TABLE
        ADD DisplayOrder int NULL;
END;
GO

UPDATE lookupTable
SET DisplayOrder = ordered.RowNumber
FROM dbo.LOOKUP_TABLE lookupTable
INNER JOIN (
    SELECT
        LookupId,
        ROW_NUMBER() OVER (
            PARTITION BY LookupType
            ORDER BY LookupCode, LookupId
        ) AS RowNumber
    FROM dbo.LOOKUP_TABLE
    WHERE DisplayOrder IS NULL
) ordered ON ordered.LookupId = lookupTable.LookupId
WHERE lookupTable.DisplayOrder IS NULL;
GO
