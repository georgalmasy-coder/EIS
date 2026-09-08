# Functional Architecture

This page is used to view, create, edit, import and export functional structures for the selected project.

Functional structures describe needs, expectations, constraints or requests from stakeholders. These functional structures can later be linked to system requirements and other engineering information.

## What does this page show?

The page shows a table of functional architectures.

At the top of the page, you can see:

- customer name
- project name
- user name
- data loading status

The main table contains the functional architectures available for the selected project.

![System Connections](/images/help/SystemConnections.jpg)

## Main actions

The page contains the following main actions:

- **Import**
- **Export**
- **Help**
- **Add Functional architecture**

## Import

Use **Import** to import functional architectures from a file.

The import file may be one of the supported formats shown in the import dialog.

Typical formats may include:

- XML
- XLSX
- CSV

After selecting a file, the import dialog can be used to start the import.

## Export

Use **Export** to export functional architectures from the current project.

The export dialog allows you to choose the export format.

Available export formats may include:

- XLSX
- CSV
- PDF
- XML

The export dialog may also include options such as whether inactive functional architectures should be included.

## Add Functional architecture

Use **Add Functional architecture** to create a new functional architecture.

When adding or editing a functional architecture, a dialog is opened with several tabs.

## Edit dialog

The edit dialog contains the following tabs:

- **Basis Info**
- **History**
- **Attachments**
- **Notes**
- **Links**
- **Relations**

## Basis Info

The **Basis Info** tab contains the main information about the functional architecture.

Depending on the project configuration, the fields may include information such as:

- logical code
- logical name
- logical description
- status
- priority
- owner
- dates
- other project-specific fields

Some fields may be read-only, while others can be edited.

Required fields may be marked with an asterisk.

## History

The **History** tab shows previous changes to the functional architecture.

The history table may include:

- changed date and time
- changed by
- version

This can be used to understand when the functional architecture was updated and by whom.

## Attachments

The **Attachments** tab shows files attached to the functional architecture.

Use **Add Attachment** to add a new file.

When adding an attachment, select a file and enter a description.

Attachments can be useful for supporting documentation such as:

- drawings
- specifications
- meeting notes
- external documents
- screenshots

## Notes

The **Notes** tab shows notes related to the functional architecture.

Use **Add Note** to create a new note.

Notes can be used for comments, clarification, review input or internal observations.

## Links

The **Links** tab is reserved for links related to the functional architecture.

If the tab shows **Under Construction**, the functionality is not yet available.

## Relations

The **Relations** tab shows relations from the functional architecture to other entities.

This can help show how a functional architecture is connected to other parts of the project, such as system requirements.

## Saving changes

Use **Save** in the edit dialog to save changes.

Use **Cancel** to close the dialog without saving.

## If data is not shown

If no functional architectures are shown, it may be because:

- data is still loading
- no functional architectures exist for the selected project
- there was an error while loading data
- the current user does not have access to the expected data

Check the **Data** field at the top of the page to see whether data has been loaded or whether an error occurred.

## Good practices

- Use clear and consistent logical names.
- Write descriptions so they can be understood by both technical and non-technical stakeholders.
- Add notes when clarification is needed.
- Use attachments for supporting documentation.
- Use relations to connect functional architectures to relevant system requirements.
- Export the list when functional architectures need to be reviewed outside the system.


