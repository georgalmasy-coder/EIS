# Hjælp til relationsdiagram

Relationsdiagrammet viser sammenhænge mellem krav i tre mulige visninger:

1. Interessentkrav -> Systemkrav.
2. Interessentkrav -> Systemkrav -> Fysisk arkitektur.
3. Systemkrav -> Fysisk arkitektur.

Skift mellem visningerne med knapperne øverst til højre.

## Sådan læses diagrammet

- Hver boks repræsenterer et krav eller et element i den fysiske arkitektur.
- Betegnelsen længst til venstre er den interne kode fra XML-dataene.
- Boksens titel er elementets navn.
- Linjer forbinder relaterede elementer.
- Hold markøren over en boks for at fremhæve dens relationer.
- Klik på en boks for at åbne detaljedialogen.

## Filtre

- **Show only requirements without relations** skjuler elementer med forbindelser i det aktuelle datasæt.
- **Show only requirements with relations** skjuler elementer uden forbindelser.
- Søgefeltet søger i ID, navn og beskrivelse.
- Træk et element ind i fokusområdet for kun at vise det og de elementer, det har direkte relationer til.
- Når fokus er aktivt, vises det valgte element og et ryd-ikon i området.
- Ryd-ikonet vises kun, når fokus er aktivt.
- Brug ryd-ikonet til at fjerne fokus, før du vælger et andet element.

## Bemærkninger

- Standardvisningen er Interessentkrav -> Systemkrav -> Fysisk arkitektur.
- I visninger med én relationstype skjules den ubrugte kolonne og dens linjer.
