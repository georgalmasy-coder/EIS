package com.bepa.eis.server.dataprovider.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.api.DTO.Menu;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MenuProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(MenuProvider.class);

    private static final String SELECT_ALL_MENU_ROWS_SQL =
            "SELECT MenuId, MenuItemText, MenuItemUrl, ParentMenuId, DisplayOrder, CustomerIdRequired, ProjectIdRequired, UserRoles, Active " +
            "FROM MENU ";

    private static final String SELECT_MENU_ROW_SQL =
            SELECT_ALL_MENU_ROWS_SQL + " WHERE MenuId = ?";

    private static final String ORDER_BY_SQL = " ORDER BY CASE WHEN ParentMenuId IS NULL THEN 0 ELSE 1 END, ParentMenuId, CASE WHEN DisplayOrder IS NULL THEN 1 ELSE 0 END, DisplayOrder, MenuId";

    private static final String SELECT_SIBLING_ROWS_SQL =
            SELECT_ALL_MENU_ROWS_SQL + " WHERE ((ParentMenuId IS NULL AND ? IS NULL) OR ParentMenuId = ?) ";

    private static final String INSERT_MENU_ROW_SQL =
            "INSERT INTO dbo.MENU (MenuItemText, MenuItemUrl, ParentMenuId, DisplayOrder, CustomerIdRequired, ProjectIdRequired, UserRoles, Active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_MENU_ROW_SQL =
            "UPDATE dbo.MENU " +
            "SET MenuItemText = ?, MenuItemUrl = ?, CustomerIdRequired = ?, ProjectIdRequired = ?, UserRoles = ?, Active = ? " +
            "WHERE MenuId = ?";

    private static final String UPDATE_MENU_ORDER_SQL =
            "UPDATE dbo.MENU SET DisplayOrder = ? WHERE MenuId = ?";

    public MenuProvider(WebSession webSession) {
        super(webSession);
    }

    public Menu getMenuItems(String sessionId) {
        return getNavigationMenu();
    }

    public List<MenuRow> getAllNavigationMenuRows() {
        String customerFilter = getWebSession().getCustomerId() != null ? "" : "AND CustomerIdRequired = 1 ";
        String projectFilter = getWebSession().getProjectId() != null ? "" : "AND ProjectIdRequired = 1 ";

        String sql = SELECT_ALL_MENU_ROWS_SQL +
                     "WHERE 1 = 1 " +
                     "AND Active = 1 " +
//                     customerFilter +
//                     projectFilter +
                     ORDER_BY_SQL;
        return  getMenuRows(sql);
    }

    public List<MenuRow> getAllMenuRows() {
        String sql = SELECT_ALL_MENU_ROWS_SQL +
                     ORDER_BY_SQL;

        return  getMenuRows(sql);
    }

    private List<MenuRow> getMenuRows(String sql) {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<MenuRow> rows = new ArrayList<>();

            while (resultSet.next()) {
                rows.add(mapMenuRow(resultSet));
            }

            return rows;
        } catch (SQLException e) {
            log.error("Error loading menu rows", e);
            throw new RuntimeException(e);
        }
    }

    public MenuRow getMenuRow(Integer menuId) {
        if (menuId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_MENU_ROW_SQL)) {

            setInt(statement, menuId, 1);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapMenuRow(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading menu row {}", menuId, e);
            throw new RuntimeException(e);
        }

        return null;
    }

    public MenuRow saveMenuRow(MenuRow menuRow) {
        if (menuRow == null) {
            throw new IllegalArgumentException("Menu row is required.");
        }

        String menuItemText = safeText(menuRow.menuItemText());
        String menuItemUrl = normalizeUrl(menuRow.menuItemUrl());
        String userRoles = normalizeRoles(menuRow.userRoles());
        boolean customerIdRequired = toBoolean(menuRow.customerIdRequired());
        boolean projectIdRequired = toBoolean(menuRow.projectIdRequired());
        boolean active = menuRow.active() == null || menuRow.active();
        Integer menuId = menuRow.menuId();
        Integer parentMenuId = menuRow.parentMenuId();

        validateMenuRow(menuItemText, menuItemUrl, userRoles);

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                Integer savedMenuId;

                if (menuId == null || menuId <= 0) {
                    Integer nextDisplayOrder = getNextDisplayOrder(connection, parentMenuId);
                    savedMenuId = insertMenuRow(
                            connection,
                            menuItemText,
                            menuItemUrl,
                            parentMenuId,
                            nextDisplayOrder,
                            customerIdRequired,
                            projectIdRequired,
                            userRoles,
                            active
                    );
                } else {
                    MenuRow existingMenuRow = getMenuRow(connection, menuId);

                    if (existingMenuRow == null) {
                        throw new IllegalArgumentException("Menu item was not found.");
                    }

                    parentMenuId = existingMenuRow.parentMenuId();
                    savedMenuId = updateMenuRow(
                            connection,
                            menuId,
                            menuItemText,
                            menuItemUrl,
                            customerIdRequired,
                            projectIdRequired,
                            userRoles,
                            active
                    );
                }

                connection.commit();
                return getMenuRow(savedMenuId);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error saving menu row {}", menuRow, e);
            throw new RuntimeException(e);
        }
    }

    public boolean moveMenuRow(
            Integer menuId,
            boolean moveUp
    ) {
        if (menuId == null || menuId <= 0) {
            throw new IllegalArgumentException("MenuId is required.");
        }

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                MenuRow targetRow = getMenuRow(connection, menuId);

                if (targetRow == null) {
                    throw new IllegalArgumentException("Menu item was not found.");
                }

                List<MenuRow> siblings = getSiblingRows(connection, targetRow.parentMenuId());
                int targetIndex = indexOfMenuRow(siblings, menuId);

                if (targetIndex < 0) {
                    throw new IllegalStateException("Menu item is missing from its sibling list.");
                }

                int nextIndex = moveUp ? targetIndex - 1 : targetIndex + 1;

                if (nextIndex < 0 || nextIndex >= siblings.size()) {
                    connection.rollback();
                    return false;
                }

                MenuRow movedRow = siblings.remove(targetIndex);
                siblings.add(nextIndex, movedRow);

                renameDisplayOrders(connection, siblings);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error moving menu row {}", menuId, e);
            throw new RuntimeException(e);
        }
    }

    private Menu getNavigationMenu() {
        Menu menu = new Menu();
        List<MenuRow> rows = getAllNavigationMenuRows();

        Map<Integer, List<MenuRow>> childRowsByParent = new LinkedHashMap<>();

        for (MenuRow row : rows) {
            if (row == null) {
                continue;
            }

            if (row.parentMenuId() == null) {
                if (isVisible(row)) {
                    childRowsByParent.putIfAbsent(row.menuId(), new ArrayList<>());
                }
            } else if (isVisible(row)) {
                childRowsByParent.computeIfAbsent(row.parentMenuId(), ignored -> new ArrayList<>()).add(row);
            }
        }

        List<MenuRow> parentRows = rows.stream()
                .filter(row -> row != null && row.parentMenuId() == null && isVisible(row))
                .sorted(menuRowComparator())
                .collect(Collectors.toList());

        for (MenuRow parentRow : parentRows) {
            int displayOrder = parentRow.displayOrder() == null ? parentRow.menuId() : parentRow.displayOrder();
            Menu.MainMenuItem mainMenuItem = menu.addMainMenuItem(
                    parentRow.menuId(),
                    parentRow.menuItemText(),
                    parentRow.menuItemUrl(),
                    displayOrder
            );

            List<MenuRow> childRows = childRowsByParent.getOrDefault(parentRow.menuId(), List.of());
            childRows.stream()
                    .sorted(menuRowComparator())
                    .forEach(childRow -> mainMenuItem.addSubMenuItem(
                            childRow.menuId(),
                            childRow.menuItemText(),
                            childRow.menuItemUrl(),
                            childRow.parentMenuId(),
                            childRow.displayOrder() == null ? childRow.menuId() : childRow.displayOrder()
                    ));
        }

        // Add exit menu item
        menu.addMainMenuItem(999, "Exit EIS", "/api/logout", 999);

        return menu;
    }

    private Integer getNextDisplayOrder(
            Connection connection,
            Integer parentMenuId
    ) throws SQLException {
        List<MenuRow> siblings = getSiblingRows(connection, parentMenuId);

        return siblings.stream()
                .map(MenuRow::displayOrder)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private Integer insertMenuRow(
            Connection connection,
            String menuItemText,
            String menuItemUrl,
            Integer parentMenuId,
            Integer displayOrder,
            boolean customerIdRequired,
            boolean projectIdRequired,
            String userRoles,
            boolean active
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_MENU_ROW_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setString(statement, menuItemText, 1);
            setString(statement, menuItemUrl, 2);
            setInt(statement, parentMenuId, 3);
            setInt(statement, displayOrder, 4);
            setBoolean(statement, customerIdRequired, 5);
            setBoolean(statement, projectIdRequired, 6);
            setString(statement, userRoles, 7);
            setBoolean(statement, active, 8);

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insert menu item failed.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not read generated menu id.");
    }

    private Integer updateMenuRow(
            Connection connection,
            Integer menuId,
            String menuItemText,
            String menuItemUrl,
            boolean customerIdRequired,
            boolean projectIdRequired,
            String userRoles,
            boolean active
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_MENU_ROW_SQL)) {
            setString(statement, menuItemText, 1);
            setString(statement, menuItemUrl, 2);
            setBoolean(statement, customerIdRequired, 3);
            setBoolean(statement, projectIdRequired, 4);
            setString(statement, userRoles, 5);
            setBoolean(statement, active, 6);
            setInt(statement, menuId, 7);

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new IllegalArgumentException("Menu item not found.");
            }
        }

        return menuId;
    }

    private List<MenuRow> getSiblingRows(
            Connection connection,
            Integer parentMenuId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT_SIBLING_ROWS_SQL + " ORDER BY CASE WHEN DisplayOrder IS NULL THEN 1 ELSE 0 END, DisplayOrder, MenuId"
        )) {
            setInt(statement, parentMenuId, 1);
            setInt(statement, parentMenuId, 2);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<MenuRow> siblings = new ArrayList<>();

                while (resultSet.next()) {
                    siblings.add(mapMenuRow(resultSet));
                }

                return siblings;
            }
        }
    }

    private MenuRow getMenuRow(
            Connection connection,
            Integer menuId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_MENU_ROW_SQL)) {
            setInt(statement, menuId, 1);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapMenuRow(resultSet);
                }
            }
        }

        return null;
    }

    private void renameDisplayOrders(
            Connection connection,
            List<MenuRow> siblings
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_MENU_ORDER_SQL)) {
            for (int index = 0; index < siblings.size(); index++) {
                MenuRow sibling = siblings.get(index);
                int nextDisplayOrder = index + 1;

                setInt(statement, nextDisplayOrder, 1);
                setInt(statement, sibling.menuId(), 2);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private MenuRow mapMenuRow(ResultSet resultSet) throws SQLException {
        return new MenuRow(
                getNullableInt(resultSet, "MenuId"),
                resultSet.getString("MenuItemText"),
                resultSet.getString("MenuItemUrl"),
                getNullableInt(resultSet, "ParentMenuId"),
                getNullableInt(resultSet, "DisplayOrder"),
                getNullableBoolean(resultSet, "CustomerIdRequired"),
                getNullableBoolean(resultSet, "ProjectIdRequired"),
                safeText(resultSet.getString("UserRoles")),
                getNullableBoolean(resultSet, "Active")
        );
    }

    private boolean isVisible(MenuRow row) {

        if (row != null && (row.active() == null || row.active())) {

            if (row.customerIdRequired() == true && getWebSession().getCustomerId() == null) {
                return false;
            }

            if (row.projectIdRequired() == true && getWebSession().getProjectId() == null) {
                return false;
            }

            return hasUserRoleAccess(row);
        }

        return false;
    }

    boolean hasUserRoleAccess(MenuRow row) {

        UserRoles userRole = CustomerLookupCache.getUserRole(getWebSession());

        if (userRole != UserRoles.INVASIVE_USER_ROLE && row != null) {

            if (userRole == UserRoles.BEPA_SYSTEM_ADMINISTRATOR) {
                return true;
            }

            if (row.userRoles() != null) {

                String userRoleId = String.valueOf(userRole.getId());

                String[] roles = row.userRoles().split(",");
                for (String role : roles) {
                    if (role.trim().equals(userRoleId)) {
                        return true;
                    }
                }
            }
       }

        return false;
    }


    private Comparator<MenuRow> menuRowComparator() {
        return Comparator
                .comparing((MenuRow row) -> row.displayOrder() == null ? Integer.MAX_VALUE : row.displayOrder())
                .thenComparing(row -> row.menuId() == null ? Integer.MAX_VALUE : row.menuId());
    }

    private int indexOfMenuRow(
            List<MenuRow> rows,
            Integer menuId
    ) {
        if (rows == null || menuId == null) {
            return -1;
        }

        for (int index = 0; index < rows.size(); index++) {
            MenuRow row = rows.get(index);
            if (row != null && menuId.equals(row.menuId())) {
                return index;
            }
        }

        return -1;
    }

    private void validateMenuRow(
            String menuItemText,
            String menuItemUrl,
            String userRoles
    ) {
        if (menuItemText.isBlank()) {
            throw new IllegalArgumentException("MenuItemText is required.");
        }

        if (menuItemText.length() > 50) {
            throw new IllegalArgumentException("MenuItemText must be maximum 50 characters.");
        }

        if (menuItemUrl != null && menuItemUrl.length() > 50) {
            throw new IllegalArgumentException("MenuItemUrl must be maximum 50 characters.");
        }

        if (userRoles != null && userRoles.length() > 20) {
            throw new IllegalArgumentException("UserRoles must be maximum 20 characters.");
        }
    }

    private String normalizeUrl(String value) {
        String normalized = safeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeRoles(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        List<String> roleIds = new ArrayList<>();

        for (String roleToken : value.split(",")) {
            String normalizedToken = roleToken == null ? "" : roleToken.trim();

            if (normalizedToken.isBlank()) {
                continue;
            }

            try {
                int roleId = Integer.parseInt(normalizedToken);
                UserRoles role = UserRoles.fromId(roleId);

                if (role == null || role == UserRoles.INVASIVE_USER_ROLE) {
                    continue;
                }

                String roleIdText = String.valueOf(role.getId());

                if (!roleIds.contains(roleIdText)) {
                    roleIds.add(roleIdText);
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed role identifiers.
            }
        }

        return String.join(",", roleIds);
    }

    private boolean toBoolean(Boolean value) {
        return value != null && value;
    }

    private Integer getNullableInt(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private Boolean getNullableBoolean(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        boolean value = resultSet.getBoolean(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    public record MenuRow(
            Integer menuId,
            String menuItemText,
            String menuItemUrl,
            Integer parentMenuId,
            Integer displayOrder,
            Boolean customerIdRequired,
            Boolean projectIdRequired,
            String userRoles,
            Boolean active
    ) {
    }
}
