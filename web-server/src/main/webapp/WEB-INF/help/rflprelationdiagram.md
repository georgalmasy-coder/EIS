# RFLP Relation Diagram Help

The relation diagram shows item relationships across five views:

1. Stakeholder Requirements -> Systems Requirements
2. Systems Requirements -> Functional Structures
3. Functional Structures -> Logical Structures
4. Logical Structures -> Physical Structures
5. Stakeholder Requirements -> Systems Requirements -> Functional Structures -> Logical Structures -> Physical Structures

Use the view buttons in the top-right corner to switch between these layouts.

## How to read the diagram

- Each box represents an item from the XML payload.
- The code shown in each box is the internal item code.
- The title text in each box is the item name.
- Lines connect related items.
- Hover a box to highlight the full connected chain.
- Click a box to open the details dialog.
- Double-click a box to open the edit dialog.

## Filters

- "Show only entities without relations" hides items that are connected to anything in the current data set.
- "Show only entities with relations" hides isolated items.
- The search field matches ID, name, and description.
- You can drag an entity into the focus zone to show only that entity and its full connected chain.
- While focus is active, the zone shows the selected entity and a clear icon.
- Use the clear icon to remove the focus before selecting another entity.

## Notes

- The default view is Stakeholder Requirements -> Systems Requirements -> Functional Structures -> Logical Structures -> Physical Structures.
- In the single-relation views, the unused columns are hidden together with their lines.
