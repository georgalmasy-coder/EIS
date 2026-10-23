# Systems Teamwork

This page shows system interfaces between a `From` system and a `To` system.

## Table

The table is split into three groups:

- **From**: SBS code, TRL, system name, system owner, and department.
- **Interface**: class and IRL information between the two systems.
- **To**: SBS code, TRL, system name, system owner, and department.

Click a column heading to sort ascending; click it again to sort descending. An arrow marks the active sort direction. From and To columns sort independently by displayed values. Class and IRL sort by the upper direction first, then the lower direction. Meeting dates sort chronologically. Empty values (shown as `--`) appear first when ascending and last when descending. Filters retain the sorting, and the PDF uses the same row order.

## Filters

Use the five drop panels at the top of the page to filter by:

- TRL
- System Owner
- Department
- Class
- IRL

Drop a value into a panel to filter rows where the value appears on either the `From` side or the `To` side.

Remove a selected filter value by using the `x` button inside the drop panel.

Drag individual classification codes (for example, `AA`) from either interface direction into the Class panel, one at a time. You can select several codes; rows matching any selected code are shown. Class filters combine with the other filter panels.

Drag IRL values from either interface direction into the IRL panel, one at a time. You can select several values; rows matching any selected IRL are shown. IRL filters combine with the other filter panels and apply to the PDF report.

## Editing

Double-click any cell in the `From` columns to open the edit page for the source system.

Double-click any cell in the `To` columns to open the edit page for the target system.

## PDF

Use the PDF button to download the current report as `systems-teamwork-<projectName>.pdf`.

The PDF title indicates whether the report is a complete document or a filtered document.
