# Lookup Maintenance

Use this page to maintain the shared lookup values used throughout the application.

## Tabs

- **TRL**: Technology readiness levels.
- **IRL**: Integration readiness levels.
- **SRL**: System readiness levels.
- **Classification**: Classification values and examples.

## Working with rows

- Click a row to open the edit dialog.
- Use the **Active** flag to enable or disable a row.
- Some lookup types allow a color value. That color is shown in the list and can be changed in the dialog.

## Edit dialog

- Update the fields shown for the selected lookup type.
- Save to persist changes.
- Cancel to close the dialog without saving.

## Notes

- The list is grouped by lookup type.
- Disabled rows remain visible, but are marked as inactive.
- Activating a level from 1 to 9 activates the same level in TRL, IRL and SRL for the current customer and project.
- Deactivating a level from 1 to 9 deactivates that level and all higher levels in TRL, IRL and SRL together.
- Deactivation is rejected if an affected TRL is used by a latest system version, or an affected IRL is used by a latest interface version on any entity type. Inactive entities are included; older versions and other customers/projects are excluded.
- SRL is calculated and is not checked for usage.
