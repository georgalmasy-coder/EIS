# Hjælp til sporbarhedsmatricen

Sporbarhedsmatricen viser relationerne mellem **interessentkrav (Stakeholder Requirements)** og **systemkrav (Systems Requirements)**.

Rækkerne repræsenterer interessentkrav, og kolonnerne repræsenterer systemkrav. Hver celle viser, om der er en relation mellem kravene i den pågældende række og kolonne.

## Matricens farver

- **Hvid**: Ingen regel matcher, og der er intet forslag til sporbarhed.
- **Gul**: Der er fundet en mulig relation.
- **Grøn**: Der findes en bekræftet relation.
- **Grå med kursiv**: Relationen er markeret som ikke relevant.
- **Rød**: Manglende sporbarhed.

## Celleværdier

- En tom celle har ingen bekræftet relation.
- `X` betyder bekræftet relation.
- `NR` betyder, at relationen er markeret som ikke relevant.

## Arbejd med relationer

Højreklik på en celle for at åbne genvejsmenuen. Handlingerne afhænger af cellens aktuelle status.

## Gule celler – mulig relation

En gul celle betyder, at systemet har fundet en mulig relation mellem interessentkravet og systemkravet.

Forslag bygger på sammenligning af ord i kravenes navne og beskrivelser. Kravkoder medtages ikke. Som standard kræves to forskellige matchende ord; gentagelser tæller kun én gang. Sammenligningen ser bort fra store og små bogstaver og tegnsætning og kan tillade mindre staveforskelle i længere ord. Administratoren kan ændre disse standarder.

Almindelige danske og engelske ord som "og", "at", "and" og "the" er som standard udeladt. Administratoren kan tilpasse listen over stopord.

I dialogen **Traceability Details** vises matchende ord med **fed** i både navne og beskrivelser. Dialogen viser også antallet af forskellige matchende ordpar. Gentagelser fremhæves, men tæller kun én gang.

### Confirm relation

Opretter en bekræftet relation. Cellen får grøn baggrund og værdien `X`.

### Relation Not Relevant

Markerer relationen som ikke relevant. Brug handlingen, når forslaget ikke er gyldigt eller ikke skal regnes som en sporbarhedsrelation. Cellen bliver grå med kursiv og får værdien `NR`.

## Grønne celler – bekræftet relation

En grøn celle betyder, at der allerede findes en bekræftet relation.

### Remove Confirmed Relation

Fjerner den bekræftede relation. Hvis en mulig relation stadig kan beregnes, bliver cellen normalt gul med tom værdi.

### Relation Not Relevant

Ændrer relationen fra bekræftet til ikke relevant. Brug handlingen, hvis relationen ikke længere er gyldig og skal markeres udtrykkeligt som ikke relevant frem for blot at blive fjernet. Cellen bliver grå med kursiv og får værdien `NR`.

## Grå celler med kursiv – ikke relevant relation

Disse celler har en relation, som udtrykkeligt er markeret som ikke relevant.

### Confirm relation

Ændrer relationen til bekræftet. Cellen får grøn baggrund og værdien `X`.

### Remove Not Relevant Relation

Fjerner markeringen som ikke relevant. Hvis en mulig relation stadig kan beregnes, bliver cellen normalt gul med tom værdi.

## Åbn kravdetaljer

Dobbeltklik på en celle for at åbne **Traceability Details**. Dialogen viser interessentkravet og systemkravet, så du kan sammenligne teksterne, før du bekræfter relationen eller markerer den som ikke relevant.

## Åbn redigeringssider

- Dobbeltklik på en kolonneoverskrift for at redigere systemkravet.
- Dobbeltklik på en rækkeoverskrift for at redigere interessentkravet.

## Oplysninger ved markøren

Når du holder markøren over en celle, vises dens række- og kolonnekontekst. Tekniske værdier som intern celleværdi og stil vises ikke.

## Anbefalet arbejdsgang

1. Gennemgå først de gule celler.
2. Dobbeltklik på en celle for at åbne detaljerne.
3. Sammenlign interessentkravet og systemkravet.
4. Vælg **Confirm relation** eller **Relation Not Relevant** i genvejsmenuen.
5. Gennemgå de grønne celler jævnligt.
6. Fjern eller ændr relationer, som ikke længere er gyldige.

## Bemærkninger

- Bekræftede relationer vises som grønne celler med `X`.
- Ikke relevante relationer vises som grå celler med kursiv og `NR`.
- Handlinger i genvejsmenuen opdaterer den valgte celle direkte.
- Hvis en handling mislykkes, vises en fejlmeddelelse, og cellen opdateres ikke.
