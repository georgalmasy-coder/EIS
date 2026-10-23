# User Administration

This page lists the users for the current customer and opens the user edit dialog on double click.

## Edit dialog

The dialog is split into tabs:

- **Basis** contains the main user fields.
- **Projects** lists the active projects for the current customer and lets the administrator choose access.
- **Security** contains MFA-related fields and actions.
- **Password** shows whether a password is already set and provides password reset actions.

## Security tab

- **Reset MFA**: clears the current MFA setup and requires the user to set it up again.
- **Disable MFA**: disables MFA for the user.
- **Mark reset required**: marks MFA as requiring a reset on next login.
- **Clear reset required**: removes the MFA reset requirement.

## Password tab

- **Send reset link**: sends a password reset link to the user by email.
- **Password set**: read-only indicator showing whether a password currently exists.

## Common actions

- **Save**: stores the current changes.
- **Cancel**: closes the dialog without saving.

## Table features

- Search filters the list.
- Group by lets you group rows by dragging a column into the drop zone.
- Column widths can be resized and are stored in browser local storage.
