# Forgot password

`forgot-password.html` modtager emailadressen og opretter en mail i den eksisterende
MAIL_QUEUE via `/api/security/password-reset-request`. Integration-serverens normale
mailjob sender mailen. Der oprettes kun mails for aktive brugere, og den offentlige
besked er den samme for kendte og ukendte adresser.

Knappen **Send reset link** under User Administration → Password bruger samme
mailkø, reset-side og automatisk bestemte base-URL som Forgot password. Den valgte
bruger modtager linket, og den administrator, der sender det, registreres som afsender
af anmodningen.

Ved administratorens oprettelse af en ny bruger sendes automatisk en velkomstmail
med skabelonen `user-created.mail` og samme engangslink til password-siden.
Mailen forklarer, at administratoren har oprettet kontoen, og beder brugeren vælge
et password for at få adgang til EIS. Kontoen oprettes uden password; login er først
muligt, når brugeren har gemt et password. En redigering af en eksisterende bruger
sender ikke velkomstmail igen. Dette gælder både brugeroprettelsesdialogen og
brugeradministrationens API.

Mailen oprettes efter, at brugeren er gemt. Hvis mailkøen fejler, beholdes brugeren,
og fejlen logges. Administratoren kan sende et nyt link via **Send reset link**.

Email-linket peger på `enter-new-password.html`. Tokenen er tilfældig, gemmes som
SHA-256-hash i USER_PASSWORD_RESET_TOKEN og er gyldig i 24 timer. En ny anmodning
invaliderer ældre links. Gamle mails med `forgot-password.html?token=...` viderestilles
til den nye side.

Det nye password skal være 8–128 tegn, ikke kun mellemrum, og de to indtastninger
skal matche. Passwordet PBKDF2-hashes før opdatering af USERS. Tokenen forbruges
atomisk sammen med password-opdateringen. Brugte og udløbne tokens afvises.
Efter ændringen vises et link til forsiden med login-dialogen åben; der logges ikke
automatisk ind.

## Drift

- Reset-links bruger den aktuelle requests protokol, værtsnavn, port og context path.
  Standardportene 80 og 443 udelades; andre porte medtages. Eksempelvis bliver et
  HTTPS-request til `https://localhost` til et link uden `:443`, mens en testserver
  på `https://test.example.dk:8443/eis` beholder port og applikationssti.
- Ved direkte adgang til Tomcat kræves ingen base-URL i konfigurationen til reset.
  Bag en reverse proxy skal Tomcats Connector eller RemoteIpValve være sat op til
  den offentlige protokol, vært og port. Forwarded headers skal kun accepteres fra
  betroede proxies, og ukendte Host-navne skal afvises i proxyens/Tomcats routing.
  Koden læser ikke forwarded headers direkte. Adressen følger den browseradresse,
  Tomcat ser; `localhost` er derfor kun egnet til lokale modtagere.
- `customer.workflow.portal.base.url` bruges fortsat af customer-workflowets emails,
  men læses ikke længere af Forgot password eller Send reset link i brugeradministrationen.
- Den eksisterende mailkø, SMTP-konfiguration og integration-serverens mailjob skal
  være aktive, ligesom ved New Customer.
- `password-reset.mail` følger med common-JAR'en som standard. En eksisterende fil
  i `mail.template.folder` har fortsat forrang; den skal indeholde `{{resetLink}}`.
- Det samme gælder velkomstskabelonen `user-created.mail`, som også følger med
  common-JAR'en. Eksisterende eksterne skabeloner skal indeholde `{{resetLink}}`.
- Hvis `dbo.USER_PASSWORD_RESET_TOKEN` endnu ikke findes, oprettes den med
  `create-user-password-reset-token.sql`. Scriptet ændrer ikke en eksisterende tabel.

## Kontrol efter deployment

1. Åbn Forgot password fra login-dialogen, og indtast en eksisterende aktiv brugers email.
2. Kontroller MAIL_QUEUE og den modtagne mail.
3. Åbn linket og prøv et kort password og en forkert bekræftelse.
4. Gem et gyldigt password og log ind med det nye password; det gamle skal afvises.
5. Åbn samme link igen; det skal være ugyldigt.

Frontend-regressionstesten kan køres med:

```powershell
node web-server/src/test/js/password-reset-pages.test.mjs
```

`PasswordResetFlowTest` er en standalone Java-regressionstest med falske providers:
den tester endpoint-validering, respons for ukendte brugere, bevarelse af passwords
og standard-/ekstern mailskabelon uden at sende rigtige emails.
