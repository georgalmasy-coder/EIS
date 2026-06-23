CREATE TABLE [dbo].[ENTITY_RELATIONS](
    [EntityRelationPK] [int] IDENTITY(1,1) NOT NULL,
    [CustomerId] [int] NOT NULL,
    [ProjectId] [int] NOT NULL,
    [EntityType] [int] NOT NULL,
    [EntityId] [int] NOT NULL,
    [RelatedEntityType] [int] NOT NULL,
    [RelatedEntityId] [int] NOT NULL,
    [RelationType] [int] NOT NULL,
    [Version] [int] NOT NULL,
    [Latest] [bit] NOT NULL,
    [CreatedById] [int] NOT NULL,
    [CreatedTime] [datetime2](0) NOT NULL,
    CONSTRAINT [PK_ENTITY_RELATION] PRIMARY KEY CLUSTERED
(
[EntityRelationPK] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO


/****** Object:  Index [ENTITY_RELATION_KEY]    Script Date: 23/06/2026 13.10.09 ******/
CREATE UNIQUE NONCLUSTERED INDEX [ENTITY_RELATION_KEY] ON [dbo].[ENTITY_RELATIONS]
(
	[CustomerId] ASC,
	[ProjectId] ASC,
	[EntityType] ASC,
	[EntityId] ASC,
	[RelatedEntityType] ASC,
	[RelatedEntityId] ASC,
	[Version] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO

INSERT INTO ENTITY_RELATIONS (CustomerId, ProjectId, EntityType, EntityId, RelatedEntityType, RelatedEntityId, CreatedById, CreatedTime, RelationType, Version, Latest)
SELECT [CustomerId]
        ,[ProjectId]
        ,[EntityType]
        ,[EntityId]
        ,[RelatedEntityType]
        ,[RelatedEntityId]
        ,[CreatedById]
        ,[CreatedTime]
        , 1 AS RelationType
        , 1 AS Version
        , 1 AS Latest
FROM ENTITY_RELATIONS_OLD
