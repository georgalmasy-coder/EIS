package com.bepa.eis.server.dataprovider.misc;

import com.bepa.eis.server.api.DTO.Menu;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MenuProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(MenuProvider.class);

    private static final String MAINMENU_SQL =
            "SELECT MenuId, MenuItemText, MenuItemUrl, DisplayOrder FROM MENU WHERE ParentMenuId IS NULL ORDER BY DisplayOrder";

    private static final String SUBMENU_SQL =
            "SELECT ParentMenuId, MenuId, MenuItemText, MenuItemUrl, DisplayOrder FROM MENU WHERE ParentMenuId = ? ORDER BY DisplayOrder";

    public MenuProvider(WebSession webSession) {
        super(webSession);
    }

    public Menu getMenuItems(String sessionId) {
        return getMainMenuItems(sessionId);
    }

    private Menu getMainMenuItems(String sessionId) {

        Menu menu = new Menu();

        try {
            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(MAINMENU_SQL)) {

                //setString(ps, sessionId, 1);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        int menuItemId = rs.getInt("MenuId");
                        ;
                        String menuItemText = rs.getString("MenuItemText");
                        ;
                        String menuItemUrl = rs.getString("MenuItemUrl");
                        int displayOrder = rs.getInt("DisplayOrder");
                        Menu.MainMenuItem mainMenuItem = menu.addMainMenuItem(menuItemId, menuItemText, menuItemUrl, displayOrder);

                        getSubMenuItems(sessionId, mainMenuItem);

                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting menu items: {}", e.getMessage(), e);
        }
        // Add exit menu item
        menu.addMainMenuItem(999, "Exit EIS", "/api/logout", 999);
        return menu;
    }

    private void getSubMenuItems(String sessionId, Menu.MainMenuItem mainMenuItem) throws SQLException {

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SUBMENU_SQL)) {

            setInt(ps, mainMenuItem.getMenuItemId(), 1);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int parentMenuItemId = rs.getInt("ParentMenuId");
                    int menuItemId = rs.getInt("MenuId");;
                    String menuItemText= rs.getString("MenuItemText");;
                    String menuItemUrl = rs.getString("MenuItemUrl");
                    int displayOrder = rs.getInt("DisplayOrder");
                    mainMenuItem.addSubMenuItem(menuItemId, menuItemText, menuItemUrl, parentMenuItemId, displayOrder);
                }
            }
        }

    }
}
