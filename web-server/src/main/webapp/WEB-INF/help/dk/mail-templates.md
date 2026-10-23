# Mailskabeloner

Listen viser skabelonnavne, emner, seneste opdateringer og revisioner. Dobbeltklik på en række, eller vælg **Edit**.

Rediger emne og mailindhold direkte i dialogen. Teksten i det grønne banner kan også redigeres. Markér tekst for at ændre formatering eller link. **Insert field** indsætter et flettefelt. Bevar felterne med dobbelte krølleparenteser, også i linkadresser.

**Preview** viser de aktuelle ændringer med eksempeldata uden at gemme. Links i forhåndsvisningen udfører ingen handlinger. **Save** gemmer skabelonen i databasen. **Cancel** lukker uden at gemme. Du får besked, hvis en anden har ændret skabelonen i mellemtiden.

**Send test email** åbner en dialog, hvor du indtaster én modtageradresse. Testen bruger det aktuelle udkast og de samme eksempelværdier som Preview; skabelonen gemmes ikke. Emnet begynder med **[TEST]**. Et vellykket svar betyder, at mailen er lagt i kø. Integrationsserveren håndterer leveringen. Eksempellinks er inaktive demonstrationsadresser.

**Export** henter alle skabeloner som en JSON-fil. Brug **Import** i et andet miljø, vælg filen, og bekræft importen. Tilsvarende skabeloner erstattes. Skabeloner, som ikke findes i filen, bevares. Hele importen valideres og gemmes samlet; ved en fejl ændres ingen skabeloner.

Nye mails bruger skabelonerne fra databasen. Gemte ændringer og importer gælder straks for nyoprettede mails. Mails, som allerede er i kø, beholder deres oprindelige indhold.
