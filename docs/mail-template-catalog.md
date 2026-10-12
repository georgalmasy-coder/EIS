# Database mail template catalogue

Open `/web/view?page=mail-templates` as a BEPA system administrator. Add this URL to the administration menu with the existing Menu Editor if desired.

The web-server installs `dbo.MAIL_TEMPLATE` and imports the 13 existing `.mail` files when the catalogue servlet starts. The original subjects and message content are preserved; the legacy blue heading row is removed and a rounded green banner with a PNG converted from the existing light EIS SVG logo is inserted immediately after the opening body tag. The banner heading stays on one line. The original system-notification file is empty; its row has an empty subject and only the new banner, ready to edit. Enum values without source files are not fabricated.

Installation SQL is also available at `common/src/main/resources/db/mail-templates.sql` for running manually before deployment if the application database account has no CREATE TABLE permission. Initial installation is transactional, serialised with a SQL Server application lock, and only inserts missing keys. A repeatable Java layout migration also removes the old blue heading and adjusts the green banner in existing database rows, preserving edited subjects and message content. Changed layouts increment the revision; later restarts leave them untouched. The original source files are retained as seed references.

The editor stores the subject and full HTML document, preserves tables and inline styling, and displays mail content inside script-disabled sandboxed frames. It has basic formatting, lists, alignment, links, undo/redo and a field-insertion menu. Pasted content is plain text. Preview uses example values and disables workflow links. Original placeholders must remain intact. Saves require administrator access, a session CSRF token and the current revision.

Preview and **Send test email** share server-side sample values. Test delivery queues an explicitly requested unsaved draft to a single manually entered address, using the existing mail queue and integration-server SMTP worker. The subject starts with `[TEST]`. Preview and test actions do not save the draft or change normal mail delivery. Browser tests verify rendered frame content for all populated templates; endpoint tests mock queue writes and do not send actual emails.

The PNG is stored as a data URI for the editor and preview. The SMTP sender replaces that exact data URI with a CID reference and embeds the same PNG as an inline MIME part. Both web-server and integration-server packages must be updated. Existing SVG banner logos are migrated to PNG without changing edited subjects, headings or message content. Run `node docs/convert-mail-logo.cjs` to regenerate the transparent high-resolution PNG from the sidebar SVG, then regenerate the seed SQL.

Normal delivery now reads the current database template whenever a new mail is created. Saved edits and imported templates apply immediately to newly queued messages. Already queued messages retain their recorded subject and body. Missing database templates or database errors fail explicitly; files are not used as a fallback. Both servers install missing catalogue entries at startup from the bundled seed.

**Export** downloads all templates in one versioned JSON file, including subjects, HTML, embedded PNG logos and placeholders. **Import** validates the entire file and displays a confirmation before writing. Matching keys are replaced, new keys are inserted, and templates absent from the file are kept. The write is transactional: any failure rolls back the entire import. Revisions and audit information belong to the destination environment and are not copied from the source. Required existing placeholders cannot be removed.

Arbitrary image attachments and version history are not implemented. Revision numbers prevent conflicting updates; they are not historical snapshots.

To regenerate the initial SQL from the unchanged source templates and current logo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File docs/generate-mail-template-seed.ps1
```
