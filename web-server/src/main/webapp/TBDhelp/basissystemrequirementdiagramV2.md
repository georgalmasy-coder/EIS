# System Requirement Hierarchy Diagram V2

Denne side viser systemkrav som et hierarkisk diagram.

Diagrammet bruges til at få et visuelt overblik over systemkravene i det valgte projekt. Kravene vises som bokse, og forbindelserne mellem boksene viser hierarkiet mellem overordnede og underordnede krav.

## Hvad viser siden?

Siden viser:

- det valgte kundenavn
- det valgte projektnavn
- den aktuelle bruger
- status for indlæsning af data
- antal systemkrav
- et hierarkisk diagram over systemkrav
- farvekoder for requirement status

Øverst på siden vises information om kunde, projekt og bruger.

Under overskriften vises antallet af systemkrav, som er indlæst i diagrammet.

## Diagrammet

Hver boks i diagrammet repræsenterer et systemkrav.

Boksen viser typisk:

- kravets ID
- kravets navn
- kravets status

Forbindelserne mellem boksene viser relationen mellem kravene.

Et krav med ID `1` kan for eksempel have underkrav som:

- `1.1`
- `1.2`
- `1.3`

Et krav med ID `1.1` kan have yderligere underkrav som:

- `1.1.1`
- `1.1.2`

Diagrammet viser kravhierarkiet ned til det niveau, som siden understøtter.

## Projektboksen

Den første boks i diagrammet repræsenterer projektet.

Under projektboksen vises de øverste systemkrav i hierarkiet.

## Farver og status

Farven nederst i hver kravboks viser kravets status.

Statusforklaringen vises i legend-linjen øverst på siden.

Mulige statusser kan være:

- New
- Changed
- Validated
- Approved
- Deprecated
- Potential Duplicate
- Incomplete
- Sample
- Out of Scope

Farverne gør det nemmere hurtigt at se, hvilke krav der for eksempel er nye, ændrede, godkendte eller ufuldstændige.

## Søgning og filtrering

Du kan bruge søgefeltet til at finde bestemte krav i diagrammet.

Søgningen kan bruges på blandt andet:

- ID
- navn
- beskrivelse
- verification status
- business priority
- requirement status

Når du skriver i søgefeltet, opdateres diagrammet automatisk.

Hvis et krav matcher søgningen, vises også relevante overordnede og underordnede krav, så sammenhængen i hierarkiet bevares.

## Ryd filter

Knappen **Clear filter** rydder søgningen og viser alle krav igen.

Du kan også trykke `Escape`, mens markøren står i søgefeltet, for at rydde søgningen.

## Detaljer om et krav

Klik på en kravboks for at åbne detaljer om kravet.

Dialogen viser blandt andet:

- ID
- Name
- Description
- Verification Status
- Business Priority
- Requirement Status

Hvis beskrivelsen er lang, kan der scrolles i dialogen.

## Download som PDF

Download-knappen kan bruges til at eksportere diagrammet som PDF.

PDF-filen indeholder diagrammet for det aktuelle projekt.

Hvis diagrammet er højt, kan PDF'en blive opdelt på flere sider.

## Hvis diagrammet ikke vises

Hvis diagrammet ikke vises, kan det skyldes:

- at data stadig indlæses
- at der ikke findes systemkrav for projektet
- at filteret ikke matcher nogen krav
- at der er opstået en fejl ved hentning af data

Se feltet **Data** øverst på siden for at se, om data er indlæst, eller om der er opstået en fejl.

## Gode råd

- Brug søgefeltet til hurtigt at finde et bestemt krav.
- Brug farverne til at vurdere kravstatus.
- Klik på et krav for at se flere detaljer.
- Eksportér diagrammet til PDF, hvis det skal deles eller dokumenteres.