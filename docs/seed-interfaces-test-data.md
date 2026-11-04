# Tilfældige interfaces til performancetest

Åbn [SQL-scriptet](sql/seed-interfaces-customer-1-project-6.sql) i IntelliJ Database Console eller SQL Server Management Studio på EIS-testdatabasen. Scriptet er begrænset til **customer 1 / project 6** og er ikke kørt mod en database som del af oprettelsen.

Indstillingerne ligger øverst:

| Parameter | Standard | Betydning |
|---|---:|---|
| `@EntityType` | 2 | 2=Physical architecture, 5=Stakeholder requirement, 6=System requirement, 8=Logical structure, 9=Functional structure |
| `@InterfacePercent` | 17.00 | Procent af mulige rettede forbindelser, fx 3.00 eller 17.00 |
| `@ChangedByUserId` | 1 | Eksisterende aktiv audit-bruger |
| `@IrlId` | 1 | IRL fra projektets opslagstabel; 1 er normalt Not Checked |
| `@ApplyChanges` | 0 | 0=forhåndsvisning, 1=indsættelse |

1. Vælg entitetstype, procent, audit-bruger og IRL.
2. Kør hele scriptet med `@ApplyChanges=0`, og kontrollér database, projekt, entitetsantal og forventet antal interfaces.
3. Sæt `@ApplyChanges=1`, og kør hele scriptet. Gem resultatet med `InsertedInterfaces` og `ActualPercent`.
4. Gentag for en anden `@EntityType`, hvis nødvendigt.

Kun `ENTITY.Latest=1` kræves. Både aktive og inaktive entiteter indgår. Med N entiteter er der **N × (N−1)** mulige forbindelser: A→B og B→A er forskellige interfaces, og forbindelser til sig selv udelades. Antallet afrundes til nærmeste heltal. Ved 1.900 entiteter og 17 % oprettes 613.377 interfaces. Tilfældig udvælgelse med `NEWID()` vælger par uden gentagelser; en modsat rettet forbindelse kan blive valgt uafhængigt.

Hvis **mindst én record** allerede findes i `INTERFACES` for customer 1 / project 6 / den valgte entityType, springes hele kørslen over. Historiske records tæller også. Interfaces for andre typer blokerer ikke. Der gemmes ingen engangsmarkering; sletter man alle interfaces for typen, kan scriptet køres igen.

Nye interfaces får `Version=1`, `Latest=1`, den valgte IRL, audit-bruger og tidspunkt. `NextIrlMeeting` og `ClassificationIds` er NULL. Eksisterende data ændres ikke. Scriptet bruger samme indsættelseskolonner som applikationens `InterfaceMatrixProvider`.

Kør mens testprojektet er i ro. Optælling og indsættelse sker i én transaktion med låse og rollback ved fejl. Den tilfældige udvælgelse sorterer kombinationerne i `tempdb`, som skal have plads til datasættet. Scriptet er statisk gennemgået, men ikke databaseafprøvet.
