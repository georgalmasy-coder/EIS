# Database mail template catalogue

Open `/web/view?page=mail-templates` as a BEPA system administrator. Add this URL to the administration menu with the existing Menu Editor if desired.

Templates are stored in the existing dbo.MAIL_TEMPLATE database table. Neither server installs templates or converts layouts at startup. Legacy .mail files, seed SQL and their generator have been removed. A new environment must receive its database schema through database provisioning and its templates through the catalogue JSON import.

The editor stores the subject and full HTML document, preserves tables and inline styling, and displays mail content inside script-disabled sandboxed frames. It has basic formatting, lists, alignment, links, undo/redo and a field-insertion menu. Pasted content is plain text. Preview uses example values and disables workflow links. Original placeholders must remain intact. Saves require administrator access, a session CSRF token and the current revision.

Preview and **Send test email** share server-side sample values. Test delivery queues an explicitly requested unsaved draft to a single manually entered address, using the existing mail queue and integration-server SMTP worker. The subject starts with `[TEST]`. Preview and test actions do not save the draft or change normal mail delivery. Browser tests verify rendered frame content for all populated templates; endpoint tests mock queue writes and do not send actual emails.

The PNG is stored as a data URI for the editor and preview. The SMTP sender replaces that exact data URI with a CID reference and embeds the same PNG as an inline MIME part. Both web-server and integration-server packages must be updated. Run `node docs/convert-mail-logo.cjs` to regenerate the transparent high-resolution PNG from the sidebar SVG when updating the logo asset.

Normal delivery now reads the current database template whenever a new mail is created. Saved edits and imported templates apply immediately to newly queued messages. Already queued messages retain their recorded subject and body. Missing database templates or database errors fail explicitly; files are not used as a fallback.

**Export** downloads all templates in one versioned JSON file, including subjects, HTML, embedded PNG logos and placeholders. **Import** validates the entire file and displays a confirmation before writing. Matching keys are replaced, new keys are inserted, and templates absent from the file are kept. The write is transactional: any failure rolls back the entire import. Revisions and audit information belong to the destination environment and are not copied from the source. Required existing placeholders cannot be removed.

Arbitrary image attachments and version history are not implemented. Revision numbers prevent conflicting updates; they are not historical snapshots.
