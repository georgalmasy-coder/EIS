# Tilfældige confirmed relationer til performancetest

Brug [SQL-scriptet](sql/seed-traceability-customer-1-project-7.sql) i IntelliJ Database Console eller SQL Server Management Studio. SQL er enklest her: indsættelsen sker samlet på serveren, og Java, applikationslogin og enkeltvise databasekald er ikke nødvendige.

1. Vælg den EIS-testdatabase, der indeholder customer **1**, project **6**.
2. Kør hele scriptet med `@ApplyChanges = 0`. Hvis der findes relationsrecords for customer 1 / project 6, springes kørslen helt over. Ellers vises antal aktive krav, antal kombinationer og antal relationer, der vil blive oprettet. Forhåndsvisningen indsætter ingen data.
3. Kontrollér databasen og optællingerne. `@CreatedById = 1` angiver audit-brugeren; ret den til en anden eksisterende aktiv bruger, hvis det er nødvendigt.
4. Sæt `@ApplyChanges = 1`, og kør hele scriptet én gang. Gem resultatet fra konsollen. Scriptet viser antal indsatte relationer, den resulterende procent og et udsnit af de nye relationers id'er.
5. Genindlæs traceability-matricen med customer **1** / project **6**, og mål igen, når indsættelsen er færdig.

Scriptet sigter efter **3 % af alle kombinationer af aktive, seneste stakeholder/system requirements**. For eksempel giver 1.885 × 1.900 krav 3.581.500 kombinationer og et mål på 107.445 confirmed kravpar. Optællingen foretages på projekt 6; eksemplet er ikke en antagelse om dets faktiske størrelse.

Hvert ledigt kravpar får en tilfældig rang med SQL Servers `NEWID()`, og de nødvendige par vælges uden gentagelse. Der vælges ikke hver 33. entitet, og alle krav får ikke nødvendigvis lige mange forbindelser.

Før optælling og udvælgelse tjekker scriptet, om der findes mindst én record i `dbo.ENTITY_RELATIONS` med `CustomerId=1` og `ProjectId=6`. Hvis ja, springes hele kørslen over. Tjekket gælder alle entitetstyper, relationstyper og historiske records, uanset `Latest`. Nye relationer får `RelationType=1`, `Version=1`, `Latest=1`, audit-bruger og oprettelsestidspunkt som i applikationens relationsmodel.

Der gemmes ingen engangsmarkering eller extended property. Eksisterende relationsrecords er den eneste beskyttelse mod genkørsel. Hvis alle projektets relationsrecords senere slettes, kan scriptet køres igen. Indsættelsen sker i én transaktion med rollback ved fejl. Ingen permanente tabeller oprettes, og der kræves ikke rettighed til extended properties.

Kør mens testprojektet er i ro: transaktionen holder låse under optælling og indsættelse. Den tilfældige udvælgelse sorterer de ledige kombinationer i `tempdb`; den skal derfor have plads til matricens størrelse. Begrænsningen til customer 1 / project 6 er indbygget i scriptet.

Scriptet er skrevet efter projektets SQL Server-provider og enum-værdier. Det er ikke automatisk kørt mod en database.
