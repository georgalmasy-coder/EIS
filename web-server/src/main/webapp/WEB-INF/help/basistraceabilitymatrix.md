# Traceability Matrix

The Traceability Matrix shows the relationship between Stakeholder Requirements and System Requirements.

Rows represent Stakeholder Requirements.  
Columns represent System Requirements.  
Each cell shows the current traceability status between one Stakeholder Requirement and one System Requirement.

## Purpose

Use this page to:

- Review traceability between Stakeholder Requirements and System Requirements.
- Identify missing or unconfirmed relations.
- Confirm suggested relations.
- Remove existing relations.
- Open related requirements for editing.

## Matrix Layout

The matrix is organized as follows:

- **Rows**: Stakeholder Requirements.
- **Columns**: System Requirements.
- **Cells**: Relationship status between the row requirement and the column requirement.
- **Legend**: Explains the meaning of the cell colors.

The information bar above the matrix shows:

- Number of rows.
- Number of columns.
- The column group label.
- The color legend.
- The Help button.

## Color Legend

The colors indicate the traceability status of each relation.

Typical meanings are:

- **Green with X**: A confirmed relation exists.
- **Yellow**: A suggested or unconfirmed relation exists.
- **White / Normal**: No relation is currently registered.
- **Other colors**: May indicate additional status values depending on the matrix configuration.

## Open System Requirement

To open a System Requirement:

1. Double-click a column header.
2. The System Requirement edit page opens.
3. Use **Cancel** to return to the Traceability Matrix.

## Open Stakeholder Requirement

To open a Stakeholder Requirement:

1. Double-click a row header.
2. The Stakeholder Requirement edit page opens.
3. Use **Cancel** to return to the Traceability Matrix.

## Cell Context Menu

Right-click a matrix cell to open the context menu.

The available action depends on the current cell status.

### Remove relation

This action is available when the cell is green and contains `X`.

Use **Remove relation** to remove the existing relation between the Stakeholder Requirement and the System Requirement.

When the action succeeds, the cell is updated immediately without reloading the full matrix.

### Confirm relation

This action is available when the cell is yellow.

Use **Confirm relation** to confirm the suggested relation between the Stakeholder Requirement and the System Requirement.

When the action succeeds, the cell is updated immediately without reloading the full matrix.

## Updating Cells

When a relation action is completed successfully:

- The server returns the new cell status.
- The cell style is updated.
- The cell value is updated.
- The full matrix is not reloaded.

If the update fails, an error message is shown and the cell remains unchanged.

## Navigation

Use the standard application menu to navigate to other pages.

When you open a requirement from the matrix, the edit page receives a return URL.  
This ensures that **Cancel** returns you to the Traceability Matrix.

## Tips

- Use the color legend to quickly understand the matrix status.
- Double-click column headers to inspect System Requirements.
- Double-click row headers to inspect Stakeholder Requirements.
- Right-click cells to confirm or remove relations.
- If a cell does not show a context menu, no action is available for its current status.