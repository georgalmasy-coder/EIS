# Mail templates

The list shows template names, subjects, last updated times and revisions. Double-click a row or choose **Edit**.

Edit the subject and email content directly in the dialog. The green banner text is also editable. Select text to change its formatting or link. **Insert field** inserts a placeholder used by the template. Keep placeholders with double braces intact, including in link addresses.

**Preview** shows your current changes using sample data without saving. Preview links do not perform any actions. **Save** stores the template in the database. **Cancel** closes the dialog without saving. You will be notified if someone else has changed the template in the meantime.

**Send test email** opens a dialog where you enter one recipient email address manually. The test uses the current draft and the same sample values as Preview; it does not save the template. The subject starts with **[TEST]**. A successful response means the message has been queued; the integration server handles delivery. Sample links are inactive demonstration addresses.

**Export** downloads all templates as a JSON file. Use **Import** in another environment to select that file and confirm the import. Matching templates are replaced; templates absent from the file are kept. The entire import is validated and saved together, so an error leaves all templates unchanged.

New emails use the templates stored in the database. Saved changes and imports affect newly created emails immediately. Messages already queued retain their original content.
