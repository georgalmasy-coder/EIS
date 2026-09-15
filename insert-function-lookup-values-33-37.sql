SET QUOTED_IDENTIFIER ON
GO

SET XACT_ABORT ON
GO

BEGIN TRANSACTION;

MERGE dbo.LOOKUP_TABLE AS target
USING (VALUES
    (33, 'System', NULL, 1, NULL, 1),
    (33, 'Subsystem', NULL, 1, NULL, 2),
    (33, 'Assembly', NULL, 1, NULL, 3),
    (33, 'Component', NULL, 1, NULL, 4),
    (33, 'External', NULL, 1, NULL, 5),

    (34, 'Off', NULL, 1, NULL, 1),
    (34, 'Standby', NULL, 1, NULL, 2),
    (34, 'Start-up', NULL, 1, NULL, 3),
    (34, 'Normal Operation', NULL, 1, NULL, 4),
    (34, 'Degraded Operation', NULL, 1, NULL, 5),
    (34, 'Emergency', NULL, 1, NULL, 6),
    (34, 'Maintenance', NULL, 1, NULL, 7),
    (34, 'Shutdown', NULL, 1, NULL, 8),

    (35, 'Systems', NULL, 1, NULL, 1),
    (35, 'Software', NULL, 1, NULL, 2),
    (35, 'Electrical', NULL, 1, NULL, 3),
    (35, 'Electronics', NULL, 1, NULL, 4),
    (35, 'Mechanical', NULL, 1, NULL, 5),
    (35, 'Control', NULL, 1, NULL, 6),
    (35, 'Safety', NULL, 1, NULL, 7),
    (35, 'Security', NULL, 1, NULL, 8),
    (35, 'Human Factors', NULL, 1, NULL, 9),
    (35, 'Operations', NULL, 1, NULL, 10),
    (35, 'External', NULL, 1, NULL, 11),

    (36, 'Common/Core', NULL, 1, NULL, 1),
    (36, 'Base Variant', NULL, 1, NULL, 2),
    (36, 'Optional Variant', NULL, 1, NULL, 3),
    (36, 'Variant-specific', NULL, 1, NULL, 4),
    (36, 'Customer-specific', NULL, 1, NULL, 5),
    (36, 'Not Applicable', NULL, 1, NULL, 6),

    (37, 'Mandatory', NULL, 1, NULL, 1),
    (37, 'Optional', NULL, 1, NULL, 2),
    (37, 'Conditional', NULL, 1, NULL, 3),
    (37, 'Not Applicable', NULL, 1, NULL, 4)
) AS source (LookupType, LookupCode, LookupDescription, Active, Color, DisplayOrder)
ON target.LookupType = source.LookupType
   AND target.LookupCode = source.LookupCode
WHEN MATCHED THEN
    UPDATE SET
        LookupDescription = source.LookupDescription,
        Active = source.Active,
        Color = source.Color,
        DisplayOrder = source.DisplayOrder
WHEN NOT MATCHED BY TARGET THEN
    INSERT (LookupType, LookupCode, LookupDescription, Active, Color, DisplayOrder)
    VALUES (source.LookupType, source.LookupCode, source.LookupDescription, source.Active, source.Color, source.DisplayOrder);

COMMIT TRANSACTION;
GO
