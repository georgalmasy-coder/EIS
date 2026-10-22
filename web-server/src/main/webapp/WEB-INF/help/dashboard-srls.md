# Dashboard SRL

Review the readiness of your project's physical systems and compare each system with its level 1 group.

## Project readiness (PRL)

The PRL line at the top provides three reference points:

- **Low:** the lowest calculated SRL in the project.
- **Current Level:** the average of the calculated SRL levels, rounded to the nearest whole number.
- **High:** the highest calculated SRL in the project.

The green marker represents Current Level. The dark markers represent Low and High. Markers align with the SRL headings in the table.

### When PRL markers overlap

If two or three reference points have the same rounded SRL, they share one marker:

- The left half is green and the right half is black.
- Labels are joined with **/**, for example **Current Level / High** or **Low / Current Level / High**.
- Hover over the marker to see the corresponding SRL lookup description.

## Systems and groups

Systems are sorted by SBS code and grouped under their level 1 system. All groups start collapsed.

- **Expand or collapse:** select a level 1 section bar.
- **Section bar:** shows the level 1 code, system name and number of underlying systems.
- **Hierarchy:** indented SBS codes indicate deeper levels; system names share the same column.

### Red and green system markers

- **Red:** the system's own calculated SRL.
- **Green:** the average SRL of the calculated systems beneath its level 1 system.
- **Half green / half red:** the system's own SRL and the group average are equal. One marker represents both values.

The green average is calculated once for each level 1 group and shown at the same position for every system in that group, including the lowest levels. The line ends at whichever marker is furthest to the right.

### How the green average is calculated

1. Include all calculated systems beneath level 1, at every depth.
2. Count each system once; exclude level 1 itself and systems without a calculated SRL.
3. Calculate the arithmetic mean and round to the nearest whole SRL. For example, **4.4 becomes 4** and **4.5 becomes 5**.
4. Show that same whole SRL on level 1 and every underlying system.

The green colour matches Current Level on the PRL line. Hover over a marker to see its meaning; the green tooltip also identifies its level 1 group.

### Missing calculations

- Systems without their own calculated SRL have no red marker and are excluded from averages.
- They can still show their group's green marker when that group has a calculated average.
- Groups without calculated descendants, or systems without a level 1 parent, have no green marker.
- If neither value is available, the row displays **Not calculated**.

## Working with the dashboard

### Edit a system

Double-click a system row or its group header to open **Edit Physical Architecture**. Saving reloads the dashboard and recalculates its values.

### Adjust columns

- Drag the right edge of **SBS Code**, **System Name** or **# Subsystems** to resize the column.
- Alternatively, focus a column divider and use the arrow keys.
- Column widths are saved in your user preferences.
- System names stay on one line. Hover over a shortened name to read it in full.

### Open interface management

Select the **N2** button beside PDF to open **Interface Management - Physical Architecture**. The button is available when that page is included in your menu.

### Download a PDF

Select **PDF** at the top right to export the complete dashboard, including systems in collapsed groups.

The PDF includes Project PRL, subsystem counts, system markers and page numbers. It uses the same shared green averages, split markers and combined PRL labels as the dashboard. Level 1 codes and names are bold.

## Temporary calculation details for testing

For systems at every level, including level 1 section bars, hover over the row, system name or SRL line to inspect the calculation details:

- TRL and IRL normalisation factors.
- Each outgoing interface contribution, including the system's diagonal contribution.
- The weighted sum, interface count and division.
- SRL transition references, bounds and the selected result.

These details explain the system's own calculated SRL. Final SRL levels are whole numbers. Intermediate calculation values can contain decimals; displayed decimals are rounded, while the calculation and level selection use unrounded values.

## Tips

- Expand a group to compare its systems with the shared green reference.
- Hover over a marker to distinguish the system's own SRL from its group average.
- Reload the page to load the latest calculations for the current project.
