# Systems Requirement Hierarchy Diagram V2

This page shows system requirements as a hierarchical diagram.

The diagram is used to provide a visual overview of the system requirements in the selected project. Requirements are shown as boxes, and the connections between the boxes show the hierarchy between parent and child requirements.

## What does this page show?

The page shows:

- the selected customer name
- the selected project name
- the current user
- the data loading status
- the number of system requirements
- a hierarchical diagram of system requirements
- color coding for requirement status

At the top of the page, you can see information about the customer, project and user.

Below the page title, you can see the number of system requirements loaded into the diagram.

## The diagram

Each box in the diagram represents a system requirement.

A requirement box normally shows:

- requirement ID
- requirement name
- requirement status

The connections between the boxes show the relationship between the requirements.

For example, a requirement with ID `1` may have child requirements such as:

- `1.1`
- `1.2`
- `1.3`

A requirement with ID `1.1` may have further child requirements such as:

- `1.1.1`
- `1.1.2`

The diagram shows the requirement hierarchy down to the level supported by this page.

## The project box

The first box in the diagram represents the project.

The top-level system requirements are shown under the project box.

## Colors and status

The color at the bottom of each requirement box shows the requirement status.

The status legend is shown near the top of the page.

Possible statuses may include:

- New
- Changed
- Validated
- Approved
- Deprecated
- Potential Duplicate
- Incomplete
- Sample
- Out of Scope

The colors make it easier to quickly identify which requirements are new, changed, approved, incomplete or in another status.

## Search and filtering

You can use the search field to find specific requirements in the diagram.

The search can match, among other things:

- ID
- name
- description
- verification status
- business priority
- requirement status

When you type in the search field, the diagram is updated automatically.

If a requirement matches the search, relevant parent and child requirements are also shown, so the hierarchy remains understandable.

## Clear filter

The **Clear filter** button clears the current search and shows all requirements again.

You can also press `Escape` while the cursor is in the search field to clear the search.

## Requirement details

Click a requirement box to open the requirement details.

The details dialog shows, among other things:

- ID
- Name
- Description
- Verification Status
- Business Priority
- Requirement Status

If the description is long, the dialog can be scrolled.

## Download as PDF

The download button can be used to export the diagram as a PDF file.

The PDF contains the diagram for the current project.

If the diagram is large, the PDF may be split across several pages.

## If the diagram is not shown

If the diagram is not shown, it may be because:

- data is still loading
- no system requirements exist for the selected project
- the current filter does not match any requirements
- an error occurred while loading data

Check the **Data** field at the top of the page to see whether data has been loaded or whether an error occurred.

## Good practices

- Use the search field to quickly find a specific requirement.
- Use the colors to understand requirement status.
- Click a requirement to see more details.
- Export the diagram to PDF if it needs to be shared or documented.