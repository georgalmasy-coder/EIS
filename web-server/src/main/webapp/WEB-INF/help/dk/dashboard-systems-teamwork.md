# Samarbejde om systemer

Siden viser systemgrænseflader mellem et afsendersystem (**From**) og et modtagersystem (**To**).

## Tabel

Tabellen er opdelt i tre grupper:

- **From**: SBS-kode, TRL, systemnavn, systemejer og afdeling.
- **Interface**: Klassifikations- og IRL-oplysninger mellem systemerne.
- **To**: SBS-kode, TRL, systemnavn, systemejer og afdeling.

Klik på en kolonneoverskrift for at sortere stigende, og klik igen for at sortere faldende. En pil viser sorteringsretningen. From- og To-kolonner sorterer uafhængigt efter de viste værdier. Class og IRL sorterer først efter øverste retning og dernæst efter nederste retning. Mødedatoer sorteres kronologisk. Tomme værdier, vist som `--`, står først ved stigende og sidst ved faldende sortering. Filtrering bevarer sorteringen, og PDF'en bruger samme rækkefølge.

## Filtre

Brug de fem områder øverst til at filtrere efter **TRL**, **System Owner**, **Department**, **Class** og **IRL**.

Træk en værdi ind i et filterområde for at vise rækker, hvor værdien findes på enten From- eller To-siden. Fjern en valgt værdi med områdets `x`-knap.

Træk individuelle klassifikationskoder, eksempelvis `AA`, fra en af grænsefladens retninger til Class-området, én ad gangen. Du kan vælge flere koder. Rækker, der matcher mindst én valgt kode, vises. Class kombineres med de øvrige filtre.

Træk IRL-værdier fra en af retningerne til IRL-området, én ad gangen. Du kan vælge flere værdier. Rækker, der matcher mindst én valgt IRL, vises. IRL kombineres med de øvrige filtre og gælder også for PDF-rapporten.

## Redigering

Dobbeltklik i en From-kolonne for at redigere afsendersystemet. Dobbeltklik i en To-kolonne for at redigere modtagersystemet.

## PDF

Brug PDF-knappen til at hente rapporten som `systems-teamwork-<projectName>.pdf`. Titlen angiver, om rapporten er komplet eller filtreret.
