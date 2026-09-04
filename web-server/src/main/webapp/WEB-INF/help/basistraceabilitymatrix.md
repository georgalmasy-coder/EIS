# Traceability Matrix Help

The Traceability Matrix shows the relationship between **Stakeholder Requirements** and **Systems Requirements**.

Rows represent Stakeholder Requirements.  
Columns represent Systems Requirements.

Each cell indicates whether there is a relation between the requirement in the row and the requirement in the column.

---

## Matrix Colors

The matrix uses colors to show the current traceability status.

| Color | Meaning |
|---|---|
| White | No rule matched / no traceability suggestion |
| Yellow | A possible relation has been identified |
| Green | A confirmed relation exists |
| Grey italic | The relation has been marked as not relevant |
| Red | Missing traceability |

---

## Cell Values

| Value | Meaning |
|---|---|
| Empty | No confirmed relation |
| `X` | Confirmed relation |
| `NR` | Relation marked as not relevant |

---

## Working with Relations

You can right-click a matrix cell to open the context menu.

The available actions depend on the current status of the cell.

---

## Yellow Cells - Possible Relation

A yellow cell means that the system has identified a possible relation between the Stakeholder Requirement and the Systems Requirement.

Available actions:

### Confirm relation

Creates a confirmed relation between the Stakeholder Requirement and the Systems Requirement.

After confirmation, the cell changes to:

- Green background
- Value `X`

### Relation Not Relevant

Marks the relation as not relevant.

Use this when the suggested relation is not valid or should not be treated as a traceability relation.

After marking as not relevant, the cell changes to:

- Grey italic background
- Value `NR`

---

## Green Cells - Confirmed Relation

A green cell means that a confirmed relation already exists.

Available actions:

### Remove Confirmed Relation

Removes the confirmed relation.

After removal, the cell changes back to a possible relation state when applicable.

Typically, this means:

- Yellow background
- Empty value

### Relation Not Relevant

Changes the relation from confirmed to not relevant.

Use this when an existing confirmed relation should no longer be considered valid, but should be explicitly marked as not relevant instead of simply being removed.

After marking as not relevant, the cell changes to:

- Grey italic background
- Value `NR`

---

## Grey Italic Cells - Not Relevant Relation

A grey italic cell means that the relation has been explicitly marked as not relevant.

Available actions:

### Confirm relation

Changes the relation from not relevant to confirmed.

After confirmation, the cell changes to:

- Green background
- Value `X`

### Remove Not Relevant Relation

Removes the not relevant relation.

After removal, the cell changes back to a possible relation state when applicable.

Typically, this means:

- Yellow background
- Empty value

---

## Opening Requirement Details

Double-click a matrix cell to open the Traceability Details dialog.

The dialog shows information about:

- The Stakeholder Requirement
- The Systems Requirement

This is useful when you need to compare the requirement texts before deciding whether a relation should be confirmed or marked as not relevant.

---

## Opening Requirement Edit Pages

You can double-click headers to open the corresponding requirement edit page.

### Systems Requirement

Double-click a column header to open the Systems Requirement edit page.

### Stakeholder Requirement

Double-click a row header to open the Stakeholder Requirement edit page.

---

## Tooltip Information

Hover text on matrix cells shows the row and column context.

Technical values such as internal cell value and style are not shown in the hover text.

---

## Recommended Workflow

1. Review yellow cells first.
2. Open the details dialog by double-clicking a cell.
3. Compare the Stakeholder Requirement and Systems Requirement.
4. Choose one of the available context menu actions:
    - Confirm relation
    - Relation Not Relevant
5. Review existing green cells regularly.
6. Remove or change relations if they are no longer valid.

---

## Notes

- Confirmed relations are shown as green cells with `X`.
- Not relevant relations are shown as grey italic cells with `NR`.
- Context menu actions update the selected cell directly.
- If an action fails, an error message is shown and the matrix cell is not updated.