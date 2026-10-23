# Hierarkidiagram for systemkrav V2

Siden viser systemkravene for det valgte projekt som et hierarkisk diagram. Krav vises som bokse, og linjerne viser forholdet mellem overordnede krav og underkrav.

## Hvad viser siden?

- Valgt kunde og projekt.
- Aktuel bruger.
- Status for indlæsning af data.
- Antal systemkrav.
- Et hierarkisk diagram over kravene.
- Farvekodning af kravstatus.

Kunde, projekt og bruger vises øverst. Under sidetitlen vises antallet af indlæste krav.

## Diagrammet

Hver boks repræsenterer et systemkrav og viser normalt krav-ID, navn og status. Linjerne angiver hierarkiet.

Et krav med ID `1` kan eksempelvis have underkravene `1.1`, `1.2` og `1.3`. Kravet `1.1` kan have underkravene `1.1.1` og `1.1.2`. Diagrammet viser hierarkiet til det niveau, siden understøtter.

## Projektboksen

Den første boks repræsenterer projektet. Systemkravene på øverste niveau vises under den.

## Farver og status

Farven nederst i hver kravboks viser kravets status. Farveforklaringen findes nær toppen af siden.

Mulige statusværdier er blandt andet:

- **New**: Nyt.
- **Changed**: Ændret.
- **Validated**: Valideret.
- **Approved**: Godkendt.
- **Deprecated**: Udfaset.
- **Potential Duplicate**: Mulig dublet.
- **Incomplete**: Ufuldstændigt.
- **Sample**: Eksempel.
- **Out of Scope**: Uden for projektets omfang.

Farverne gør det lettere hurtigt at finde krav med en bestemt status.

## Søgning og filtrering

Søgefeltet søger blandt andet i ID, navn, beskrivelse, verifikationsstatus, forretningsprioritet og kravstatus. Diagrammet opdateres automatisk, mens du skriver.

Når et krav matcher, vises relevante overordnede krav og underkrav også, så hierarkiet stadig er forståeligt.

## Ryd filter

**Clear filter** rydder søgningen og viser alle krav. Du kan også trykke på `Escape`, mens markøren er i søgefeltet.

## Kravdetaljer

Klik på en kravboks for at åbne detaljerne. Dialogen viser blandt andet ID, navn, beskrivelse, verifikationsstatus, forretningsprioritet og kravstatus. Du kan rulle i dialogen ved lange beskrivelser.

## Hent PDF

Downloadknappen eksporterer diagrammet for det aktuelle projekt som PDF. Store diagrammer kan blive fordelt over flere sider.

## Hvis diagrammet ikke vises

Det kan skyldes, at data stadig indlæses, at projektet ikke har systemkrav, at filteret ikke matcher nogen krav, eller at indlæsningen fejlede. Kontrollér feltet **Data** øverst for status og eventuelle fejl.

## Gode arbejdsvaner

- Brug søgningen til hurtigt at finde et bestemt krav.
- Brug farverne til at forstå kravstatus.
- Klik på et krav for at se detaljer.
- Eksporter til PDF, når diagrammet skal deles eller dokumenteres.
