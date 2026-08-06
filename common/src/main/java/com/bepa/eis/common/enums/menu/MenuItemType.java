package com.bepa.eis.common.enums.menu;

public enum MenuItemType {

    HEADER(
            1,
            "Header",
            true
    ),

    MENU_ITEM(
            2,
            "Menu item",
            false
    );

    private final int id;
    private final String label;
    private final boolean header;

    MenuItemType(
            int id,
            String label,
            boolean header
    ) {
        this.id = id;
        this.label = label;
        this.header = header;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public boolean isHeader() {
        return header;
    }

    public boolean isMenuItem() {
        return !header;
    }

    public static MenuItemType fromId(Integer id) {
        if (id == null) {
            return null;
        }

        for (MenuItemType type : values()) {
            if (type.id == id) {
                return type;
            }
        }

        return null;
    }
}
