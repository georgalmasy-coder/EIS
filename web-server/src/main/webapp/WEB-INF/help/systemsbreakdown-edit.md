# Physical Structure Edit

This page is used to create, edit, and review a Physical Structure item.

## Modes

The page can be opened in different modes:

- **Edit System** updates an existing system.
- **Create Root System** creates a new top-level system.
- **Create Sub System** creates a new system below an existing parent system.
- **Historical version** opens a previous version in read-only mode.

The current mode is shown below the page title.

## Basis Info

The **Basis Info** tab contains the editable fields for the system.

Fields are rendered from the XML returned by the server.

A field may be:

- editable
- read-only
- required
- hidden
- shown as text input, textarea, date, datetime, checkbox, or select

Required fields are marked with an asterisk.

## Validation

When saving, required fields are validated before the data is sent to the server.

If validation fails:

- the save is stopped
- a validation message is shown
- focus is moved to the first invalid field when possible

## History

The **History** tab shows previous versions of the system.

Opening a historical version makes the page read-only. In read-only mode:

- Save is disabled
- fields cannot be changed
- notes and attachments cannot be changed

## Attachments

The **Attachments** tab shows files attached to the system.

Use **Add Attachment** to add a new attachment when the page is not read-only.

## Notes

The **Notes** tab shows notes related to the system.

Use **Add Note** to add a new note when the page is not read-only.

## Additional tabs

The following tabs are currently placeholders or reserved for later functionality:

- Links
- Subsystem Owners
- Supplier
- Contractor
- Linked To ...

## Save and cancel

Use **Save** to persist changes.

Use **Cancel** to return to the previous page without saving.

After a successful save, the page returns to the configured return URL.