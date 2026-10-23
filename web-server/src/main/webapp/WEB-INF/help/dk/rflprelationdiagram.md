# Hjælp til RFLP-relationsdiagram

Diagrammet viser relationer mellem elementer i fem visninger:

1. Interessentkrav -> Systemkrav.
2. Systemkrav -> Funktionelle strukturer.
3. Funktionelle strukturer -> Logiske strukturer.
4. Logiske strukturer -> Fysisk arkitektur.
5. Interessentkrav -> Systemkrav -> Funktionelle strukturer -> Logiske strukturer -> Fysisk arkitektur.

Skift visning med knapperne øverst til højre.

## Sådan læses diagrammet

- Hver boks er et element fra XML-dataene.
- Koden er elementets interne kode, og titlen er dets navn.
- Linjer forbinder relaterede elementer.
- Hold markøren over en boks for at fremhæve hele den forbundne kæde.
- Klik for at åbne detaljer, eller dobbeltklik for at åbne redigering.

## Filtre

- **Show only entities without relations** skjuler elementer med forbindelser i det aktuelle datasæt.
- **Show only entities with relations** skjuler isolerede elementer.
- Søgefeltet søger i ID, navn og beskrivelse.
- Træk et element ind i fokusområdet for kun at vise det og hele dets forbundne kæde.
- Når fokus er aktivt, vises det valgte element og et ryd-ikon i området.
- Brug ryd-ikonet til at fjerne fokus, før du vælger et andet element.

## Bemærkninger

- Standardvisningen viser hele kæden fra interessentkrav til fysisk arkitektur.
- I visninger med én relationstype skjules de ubrugte kolonner og deres linjer.
