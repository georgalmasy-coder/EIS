# Administrationsdashboard

Denne side giver et overblik over administrativ status og driftsoplysninger for BEPA EIS.

Brug dashboardets sektioner til at følge kunder, brugere, integrationer, abonnementer, systemstatus, revisions- og sikkerhedsoplysninger samt andre administrationsområder.

De tilgængelige oplysninger afhænger af dine rettigheder og de aktiverede administrationsmoduler.

# Systemets tilstand

System Health opsummerer de senest registrerede kørsler af mailkøjobbet, kundeworkflowjobbet, SMTP-levering, CVR-opslag, VIES-momsvalidering og Geo-IP-opslag. Database- og API-forbindelser indgår ikke i scoren.

Hver målt komponent vægter lige meget: OK = 100 point, Warning = 50 og Critical = 0. Procenten er det afrundede gennemsnit. Komponenter med status Unknown indgår ikke i scoren. Dashboardet viser målingernes dækning og hver komponents seneste observation. Uden observationer er scoren ukendt.

Aktiverede job får status Critical fem minutter efter et overskredet cron-tidspunkt. En mislykket kørsel er Critical; enkelte fejl i en gennemført kundeworkflowbatch giver Warning. Deaktiverede job og deaktiveret VIES-validering er udeladt. Integrationsstatus bygger på faktiske kald og bliver Unknown efter 24 timer. Et virksomheds- eller momsopslag uden resultat regnes som et vellykket servicesvar, hvis selve tjenesten svarede korrekt.

Observationerne gemmes som COMPONENT_HEALTH-poster i AUDIT_EVENT, så begge servere deler de samme data. Webserveren og integrationsserveren skal udrulles sammen for at aktivere jobobservationer. Tidligere kørsler efterregistreres ikke. Nye job og integrationer tilføjes i ComponentHealthProvider, hvor deres kørselsresultater skal registreres.

## Fælles cron-planlægning

Alle job på integrationsserveren bruger den samme cron-planlægger med fem felter og samme genindlæsning af konfigurationen. Mailkøens standard er `mail.queue.job.cron=* * * * *` (hvert minut), og kundeworkflowets standard er `customer.workflow.job.cron=*/10 * * * *` (hvert tiende minut).

Begge understøtter `job.enabled` og `job.timezone` under deres respektive præfiks. Standardtidszonen er Europe/Copenhagen. De tidligere nøgler `mail.queue.job.interval.seconds` og `customer.workflow.job.interval.seconds` bruges ikke længere. Erstat egne intervaller med det ønskede cron-udtryk. Manglende cron-nøgler tilføjes automatisk.

Job venter på næste cron-tidspunkt efter opstart eller konfigurationsændringer. De kører ikke straks ved opstart. Lange eller mislykkede kørsler medfører hverken indhentning af oversprungne tidspunkter eller øjeblikkelige genforsøg.

## Oprydning i data

Integrationsserveren kører tre uafhængige oprydningsjob, som normalt kører dagligt kl. 02.00 i Europe/Copenhagen:

- SESSION-poster ældre end 1 dag, målt fra Created.
- INCIDENTS-poster ældre end 120 dage, målt fra LogCreated.
- PERFORMANCE-poster ældre end 120 dage, målt fra Created.

Alder måles som forløbne dage med en skarp grænse. Sletningen sker i portioner på 1.000 rækker. Opstart af serveren udløser ikke i sig selv oprydning.

Konfigurer jobbene i den fælles `eis-global.properties` med `cleanup.session.job.*`, `cleanup.incidents.job.*` og `cleanup.performance.job.*`. Hvert præfiks understøtter `enabled`, `cron`, `timezone` og `retention.days`.

Cron har fem felter: minut, time, dag i måneden, måned og ugedag. Der er understøttelse for `*`, tal-lister, intervaller og trin. Eksempelvis kører `cron=0 2 * * *` dagligt kl. 02.00, mens `cron=30 3 * * 0` kører søndage kl. 03.30. Opbevaringstiden skal være 1–36500 dage. Ugyldige indstillinger stopper det pågældende oprydningsjob og giver Critical. Indstillinger genindlæses gennem den eksisterende lytter til global konfiguration.

Cron-tidspunkter, der overskrides, mens serveren er stoppet, indhentes ikke. Den gentagne time ved skift til vintertid køres kun én gang; et planlagt tidspunkt i den manglende time ved skift til sommertid springes over. Oprydningsjob bliver Critical fem minutter efter et overskredet cron-tidspunkt. De er Unknown indtil første registrerede kørsel. Service Status viser hvert jobs senest afsluttede kørsel, også mislykkede kørsler.

## Oprydning i logarkiver

Integrationsserveren sletter også `.gz`-logarkiver, der er ældre end 30 dage målt fra seneste ændring, dagligt kl. 02.00 i Europe/Copenhagen. Kun filer direkte i de to konfigurerede mapper gennemgås. Aktive `.log`-filer, undermapper og symbolske links er udeladt. Manglende mapper springes over.

Indstillingerne er `cleanup.logs.job.enabled`, `cron`, `timezone` og `retention.days` samt `cleanup.logs.job.web-server.directory` og `cleanup.logs.job.integration-server.directory`. Tomme mappeindstillinger bruger som standard `web-server` og `integration-server` under `eis.logs.dir`.

Mapperne skal svare til de faktiske placeringer af Log4j-arkiverne og være tilgængelige for integrationsserverens konto. Manglende nøgler tilføjes automatisk til den fælles `eis-global.properties` ved opstart eller genindlæsning. Jobbets status og seneste kørsel indgår i System Health og Service Status.
