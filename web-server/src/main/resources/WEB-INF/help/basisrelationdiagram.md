# Relation Diagram Help

The relation diagram shows requirement relationships across three possible views:

1. Stakeholder Requirements -> System Requirements
2. Stakeholder Requirements -> System Requirements -> Physical Structure
3. System Requirements -> Physical Structure

Use the view buttons in the top-right corner to switch between these layouts.

## How to read the diagram

- Each box represents a requirement or physical structure item.
- The leftmost label is the internal code shown in the XML data.
- The title text in each box is the name of the item.
- Lines connect related items.
- Hover a box to highlight its connected relations.
- Click a box to open the details dialog.

## Filters

- "Show only requirements without relations" hides items that are connected to anything in the current data set.
- "Show only requirements with relations" hides isolated items.
- The search field matches ID, name, and description.
- You can drag an entity into the focus zone to show only that entity and its directly related entities.
- While focus is active, the zone shows the selected entity and a clear icon.
- The clear icon only appears while a focus is active.
- Use the clear icon to remove the focus before selecting another entity.

## Notes

- The default view is Stakeholder Requirements -> System Requirements -> Physical Structure.
- In the single-relation views, the unused column is hidden together with its lines.
