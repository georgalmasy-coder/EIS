# SRL-dashboard

Gennemgå projektets fysiske systemers modenhed, og sammenlign hvert system med dets niveau 1-gruppe.

## Projektets modenhed – PRL

PRL-linjen øverst har tre referencepunkter:

- **Low**: Den laveste beregnede SRL i projektet.
- **Current Level**: Gennemsnittet af de beregnede SRL-niveauer, afrundet til nærmeste hele tal.
- **High**: Den højeste beregnede SRL i projektet.

Den grønne markør viser Current Level. De mørke markører viser Low og High. Markørerne flugter med tabellens SRL-overskrifter.

### Når PRL-markører overlapper

Hvis to eller tre referencepunkter har samme afrundede SRL, deler de én markør:

- Venstre halvdel er grøn, og højre halvdel er sort.
- Betegnelserne samles med **/**, eksempelvis **Current Level / High** eller **Low / Current Level / High**.
- Hold markøren over punktet for at se den tilhørende SRL-beskrivelse fra opslagsdataene.

## Systemer og grupper

Systemerne er sorteret efter SBS-kode og grupperet under deres niveau 1-system. Alle grupper starter sammenfoldet.

- **Fold ud eller sammen**: Klik på en niveau 1-sektionslinje.
- **Sektionslinje**: Viser niveau 1-kode, systemnavn og antal underliggende systemer.
- **Hierarki**: Indrykkede SBS-koder angiver dybere niveauer. Systemnavnene deler samme kolonne.

### Røde og grønne systemmarkører

- **Rød**: Systemets egen beregnede SRL.
- **Grøn**: Gennemsnitlig SRL for de beregnede systemer under dets niveau 1-system.
- **Halvt grøn / halvt rød**: Systemets egen SRL er lig med gruppegennemsnittet. Én markør viser begge værdier.

Det grønne gennemsnit beregnes én gang pr. niveau 1-gruppe og vises samme sted for alle systemer i gruppen, også på de dybeste niveauer. Linjen slutter ved den markør, der ligger længst mod højre.

### Beregning af det grønne gennemsnit

1. Medtag alle beregnede systemer under niveau 1, uanset dybde.
2. Tæl hvert system én gang. Udelad selve niveau 1-systemet og systemer uden beregnet SRL.
3. Beregn det aritmetiske gennemsnit, og afrund til nærmeste hele SRL. Eksempelvis bliver **4,4 til 4**, og **4,5 til 5**.
4. Vis det samme hele SRL-niveau på niveau 1 og alle underliggende systemer.

Den grønne farve svarer til Current Level på PRL-linjen. Hold markøren over et punkt for at se betydningen. Den grønne hjælpetekst angiver også niveau 1-gruppen.

### Manglende beregninger

- Systemer uden egen beregnet SRL har ingen rød markør og medtages ikke i gennemsnit.
- De kan stadig vise gruppens grønne markør, hvis gruppen har et beregnet gennemsnit.
- Grupper uden beregnede underliggende systemer og systemer uden et niveau 1-overelement har ingen grøn markør.
- Hvis ingen af værdierne findes, viser rækken **Not calculated**.

## Arbejd med dashboardet

### Rediger et system

Dobbeltklik på en systemrække eller dens gruppeoverskrift for at åbne **Edit Physical Architecture**. Når du gemmer, genindlæses dashboardet, og værdierne beregnes igen.

### Tilpas kolonner

- Træk højre kant af **SBS Code**, **System Name** eller **# Subsystems** for at ændre kolonnebredden.
- Du kan også sætte fokus på en kolonneadskillelse og bruge piletasterne.
- Bredderne gemmes i dine brugerindstillinger.
- Systemnavne vises på én linje. Hold markøren over et afkortet navn for at læse hele navnet.

### Åbn grænsefladestyring

Vælg **N2** ved siden af PDF for at åbne **Interface Management - Physical Architecture**. Knappen er tilgængelig, når siden er med i din menu.

### Hent PDF

Vælg **PDF** øverst til højre for at eksportere hele dashboardet, inklusive systemer i sammenfoldede grupper.

PDF'en indeholder projektets PRL, antal undersystemer, systemmarkører og sidetal. Den bruger de samme fælles grønne gennemsnit, delte markører og kombinerede PRL-betegnelser som dashboardet. Niveau 1-koder og -navne vises med fed.

## Midlertidige beregningsdetaljer til test

Hold markøren over en række, et systemnavn eller en SRL-linje for at se beregningsdetaljer på alle niveauer, også niveau 1:

- Normaliseringsfaktorer for TRL og IRL.
- Bidraget fra hver udgående grænseflade, inklusive systemets diagonale bidrag.
- Vægtet sum, antal grænseflader og division.
- SRL-overgangsreferencer, grænser og det valgte resultat.

Detaljerne forklarer systemets egen beregnede SRL. De endelige niveauer er hele tal. Mellemregninger kan have decimaler. De viste decimaler afrundes, mens beregning og niveauvalg bruger uafrundede værdier.

## Tips

- Fold en gruppe ud for at sammenligne dens systemer med den fælles grønne reference.
- Hold markøren over et punkt for at skelne mellem egen SRL og gruppegennemsnittet.
- Genindlæs siden for at hente de nyeste beregninger for det aktuelle projekt.
