# Traceability: blokvis celleberegning

Klienten bruger POST til `TraceabilityMatrixServlet.doPost`:

- `?cmd=snapshot`: Indlæser krav, matchindstillinger og relationsindeks. Returnerer eksisterende XML-headerformat med `snapshotId` og `blockSize`, men ingen beregnede celler.
- `?cmd=block`: Formparametre `snapshotId`, `rowStart`, `columnStart`. Startindeks skal være multipla af 32. Svaret er JSON med snapshot-id, start/slutindeks (eksklusiv slut), sparse `cells` og `cacheHit`.
- `?cmd=export`: Eksisterende XLSX-kald, efter at alle nødvendige blokke er hentet.

Blokke bruger samme `applyRule` som den eksisterende fulde matrix. Det gamle overview-endpoint er bevaret. Normal/tom udelades fortsat i sparse celledata; klienten registrerer hele den hentede blok som indlæst, også hvis `cells` er tom.

Klienten prioriterer synlige blokke og henter en buffer på én blok omkring området, med højst to samtidige requests. Ventende forespørgsler til områder, som brugeren er scrollet væk fra, fjernes. DOM-opdateringer samles med `requestAnimationFrame`. Ikke-indlæste celler vises med et timeglas som SVG-ikon på hvid baggrund i både farve- og ikonvisning, og deres dobbeltklik og relationsmenu er inaktive. En blokfejl giver fejlmeddelelse og genprøves ikke i en automatisk løkke; genindlæs siden for at prøve igen.

Et snapshot er bundet til login-session, customer, project og bruger. Det repræsenterer de indlæste krav, relationer og matchindstillinger på oprettelsestidspunktet. Et nyt snapshot oprettes ved genindlæsning og efter sidens redigeringer. Relationshandlinger returnerer den ændrede celle og begge headerstatusser direkte. Det aktuelle snapshots relationsindeks opdateres, kun den berørte serverblok fjernes fra cachen, og snapshot-id roteres. Klienten beholder sine øvrige celledata og scrollposition. Andre snapshots for det pågældende customer/project ugyldiggøres. Blokindlæsning pauses under skrivningen, og gamle svar ignoreres. Ved udløb genindlæses data; selve skrivekaldet gentages aldrig automatisk. Ændringer fra andre redigeringssider ses ved næste genindlæsning; der implementeres ikke global push-invalidering.

Servercachen er proceslokal og begrænset til fire snapshots og 256 LRU-blokke pr. snapshot. Snapshots udløber efter 15 minutter fra oprettelsen. Udløb, manglende snapshot eller forkert ejer giver HTTP 409; klienten opretter automatisk et nyt snapshot. Gentages fejlen inden for 30 sekunder, stoppes automatisk genindlæsning med en fejlmeddelelse. Ved servergenstart bortfalder cachen. Et miljø med flere serverinstanser kræver, at blokkald går til samme instans som snapshot-kaldet.

Eksportknappen er tilgængelig for ikke-tomme matricer. Over 22.500 dataceller kræves brugerens bekræftelse, før manglende blokke beregnes. For store matricer afvises før eksportarbejde. Eksportørens eksisterende maksimum på 1.000.000 regnearksceller **inklusive headers** samt Excel-dimensionsgrænserne bevares. Under hentning af eksportblokke vises procent i den eksisterende statusindikator. En eksport afbrydes med fejl, hvis dens snapshot udløber eller erstattes.

`doPost` måler snapshot, blok, eksport og relationshandlinger i `finally`. Delegation til den generiske servlet måles fortsat dér. Blokkald logger desuden snapshot-id, område, antal beregnede celler og cache-hit. Regelberegninger og XML-parsning er ikke flyttet til klienten.

Verifikation: Maven compile, `TraceabilitySnapshotStoreTest`, `traceability-block-loader.test.cjs`, `traceability-matrix.test.mjs` og `traceability-matrix-browser-test.cjs`. Lokale tests kræver ikke en database; realistiske svartider skal måles på den kørende server med projektets data.
