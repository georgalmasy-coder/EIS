# Prøve på et sprogkatalog

Dette er kun en prøve til vurdering af indhold og størrelse. Ingen kode eller konfiguration bruger filerne, og de ligger uden for applikationens ressourcer.

## Filer og størrelse

| Fil | Indhold | Bytes | kB (1.000 bytes) |
|---|---|---:|---:|
| [source.example.json](source.example.json) | Læsbart JSON-katalog med indryk | 134.034 | 134,0 |
| [source.example.min.json](source.example.min.json) | Samme katalog uden indryk | 127.132 | 127,1 |
| [source.example.min.json.gz](source.example.min.json.gz) | Samme kompakte katalog, gzip-komprimeret | 34.715 | 34,7 |

Kataloget har **1.725 tekstnøgler**. Alle tre filer indeholder de samme tekster. Gzip-størrelsen illustrerer overførsel med HTTP-komprimering; den faktiske server skal konfigureres til komprimering og caching, når løsningen senere tages i brug.

## Format

Filen er et almindeligt UTF-8 JSON-objekt med tekstnøgler og tekstværdier. Det kan læses i Java med eksempelvis Jackson og i browseren med `response.json()`. Hver platform skal have sin egen lille funktion til opslag. Der er endnu ikke implementeret en sådan forbindelse til kataloget.

Nøglerne er genereret automatisk for denne prøve. I den endelige løsning bør de vælges efter betydning, eksempelvis `user.saved`, frem for at afspejle ordlyden. Nogle automatisk genererede nøgler har en hash som suffiks for at skelne lignende tekster.

## Grundlag og begrænsninger

Prøven bygger på de 2.157 forekomster i [Java-tekstlisten](../java-tekster/tekstliste.csv). Dubletter er samlet, Java-escapes er afkodet, tomme tekstfragmenter er udeladt, og fejltekster i SQL samt faste tekster i et HTML-dokument er trukket ud af den tekniske indpakning.

Teksterne er bevaret på deres oprindelige sprog, overvejende engelsk. Dette er et kildekatalog, ikke en færdig engelsk oversættelse. Kandidaterne indeholder også historik, eksempeldata, tekniske fejl og beskedfragmenter. En endelig gennemgang skal afgøre, hvilke der skal oversættes, og samle fragmenter til hele beskeder med navngivne parametre.

Størrelsen gælder dette Java-udtræk. Følgende er ikke med:

- Tekster fra `getFieldLabelName()`, `getFieldHeaderName()` og `PageType`, som var udeladt fra den oprindelige opgørelse.
- Øvrige tekster fra JavaScript, HTML, database og eksterne tjenester.
- Markdown-hjælpen, der allerede har separate dokumenter i `en` og `dk`.

Et fuldt katalog for hele applikationen vil derfor have et andet omfang. Normalt oprettes én katalogfil pr. sprog, og klienten henter kun det valgte sprog.
