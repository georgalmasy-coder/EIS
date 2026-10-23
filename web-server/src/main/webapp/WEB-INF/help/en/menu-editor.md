# Menu Editor

This page is used to manage the application menu tree.

## What you can do

- reload the menu tree
- create a new parent menu item
- select a row to focus a menu item
- double-click a row to edit it
- use the context menu for move and create actions
- edit menu item properties in the dialog

## Menu tree

The table shows parent items and any expanded child items.

Use the arrow button on a parent row to expand or collapse its children.

The footer shows the number of rendered rows and the total number of menu items.

## Editing

Use the dialog to edit menu item text, URL, icon, roles, required customer or project flags, active state, and display order.

## Help button

The help button opens this page.

## Transfer menus between databases

Use Export to download all menu items as XML. On the target database, use Import, select the XML file, review the preview and confirm. Matching MenuIds are updated and missing IDs are created; other menu items are kept. The hierarchy, display order, icons, roles and other settings are preserved. Only system administrators can export or import menus. Invalid imports are rejected, and database errors roll back the entire import.
