package com.bepa.eis.server.api.web.application.views.admin.department;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.api.DTO.Department;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DepartmentMaintenanceProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(DepartmentMaintenanceProvider.class);

    private static final String SELECT_DEPARTMENTS_SQL =
            "SELECT DepartmentId, CustomerId, DepartmentName, DepartmentDescription, Active " +
            "FROM dbo.DEPARTMENT " +
            "WHERE CustomerId = ? " +
            "ORDER BY DepartmentName, DepartmentId";

    private static final String SELECT_DEPARTMENT_BY_ID_SQL =
            "SELECT DepartmentId, CustomerId, DepartmentName, DepartmentDescription, Active " +
            "FROM dbo.DEPARTMENT " +
            "WHERE DepartmentId = ? AND CustomerId = ?";

    private static final String INSERT_DEPARTMENT_SQL =
            "INSERT INTO dbo.DEPARTMENT (CustomerId, DepartmentName, DepartmentDescription, Active) " +
            "VALUES (?, ?, ?, ?)";

    private static final String UPDATE_DEPARTMENT_SQL =
            "UPDATE dbo.DEPARTMENT " +
            "SET DepartmentName = ?, DepartmentDescription = ?, Active = ? " +
            "WHERE DepartmentId = ? AND CustomerId = ?";

    public DepartmentMaintenanceProvider(WebSession webSession) {
        super(webSession);
    }

    public List<Department> getDepartments() {
        List<Department> rows = new ArrayList<>();
        Integer customerId = getCustomerId();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_DEPARTMENTS_SQL)) {

            setInt(statement, customerId, 1);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapDepartment(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error getting departments for customer {}", customerId, e);
            throw new RuntimeException(e);
        }

        return rows;
    }

    public Department getDepartmentById(Integer departmentId) {
        if (departmentId == null) {
            return null;
        }

        Integer customerId = getCustomerId();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_DEPARTMENT_BY_ID_SQL)) {

            setInt(statement, departmentId, 1);
            setInt(statement, customerId, 2);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapDepartment(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error getting department {} for customer {}", departmentId, customerId, e);
            throw new RuntimeException(e);
        }

        return null;
    }

    public Department saveDepartment(Department department) {
        if (department == null) {
            throw new IllegalArgumentException("Department is required.");
        }

        Integer customerId = getCustomerId();
        Integer departmentId = department.getDepartmentId();
        String departmentName = safeText(department.getDepartmentName());
        String departmentDescription = safeText(department.getDepartmentDescription());
        boolean active = department.isActive() != null && department.isActive();

        validateDepartmentInput(departmentName, departmentDescription);

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            Integer savedDepartmentId;

            try {
                if (departmentId == null) {
                    savedDepartmentId = insertDepartment(
                            connection,
                            customerId,
                            departmentName,
                            departmentDescription,
                            active
                    );
                } else {
                    savedDepartmentId = updateDepartment(
                            connection,
                            departmentId,
                            customerId,
                            departmentName,
                            departmentDescription,
                            active
                    );
                }

                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

            EhcacheProvider.clearCacheEntry(customerId);

            Department savedDepartment = getDepartmentById(savedDepartmentId);
            if (savedDepartment == null) {
                throw new IllegalStateException("Department was saved but could not be reloaded.");
            }

            return savedDepartment;
        } catch (SQLException e) {
            log.error("Error saving department {}", department, e);
            throw new RuntimeException(e);
        }
    }

    private Integer insertDepartment(
            Connection connection,
            Integer customerId,
            String departmentName,
            String departmentDescription,
            boolean active
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_DEPARTMENT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setInt(statement, customerId, 1);
            setString(statement, departmentName, 2);
            setString(statement, departmentDescription, 3);
            setBoolean(statement, active, 4);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert department failed.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not read generated department id.");
    }

    private Integer updateDepartment(
            Connection connection,
            Integer departmentId,
            Integer customerId,
            String departmentName,
            String departmentDescription,
            boolean active
    ) throws SQLException {
        if (departmentId == null) {
            throw new IllegalArgumentException("DepartmentId is required for update.");
        }

        try (PreparedStatement statement = connection.prepareStatement(UPDATE_DEPARTMENT_SQL)) {
            setString(statement, departmentName, 1);
            setString(statement, departmentDescription, 2);
            setBoolean(statement, active, 3);
            setInt(statement, departmentId, 4);
            setInt(statement, customerId, 5);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Department not found for current customer.");
            }
        }

        return departmentId;
    }

    private Department mapDepartment(ResultSet resultSet) throws SQLException {
        Department department = new Department();
        department.setDepartmentId(resultSet.getInt("DepartmentId"));
        department.setCustomerId(resultSet.getInt("CustomerId"));
        department.setDepartmentName(resultSet.getString("DepartmentName"));
        department.setDepartmentDescription(resultSet.getString("DepartmentDescription"));
        department.setActive(resultSet.getBoolean("Active"));
        return department;
    }

    private void validateDepartmentInput(String departmentName, String departmentDescription) {
        if (departmentName.isBlank()) {
            throw new IllegalArgumentException("DepartmentName is required.");
        }

        if (departmentName.length() > 10) {
            throw new IllegalArgumentException("DepartmentName must be maximum 10 characters.");
        }

        if (departmentDescription.length() > 255) {
            throw new IllegalArgumentException("DepartmentDescription must be maximum 255 characters.");
        }
    }

    private Integer getCustomerId() {
        if (getWebSession() == null || getWebSession().getCustomerId() == null) {
            throw new IllegalStateException("CustomerId is missing from the current session.");
        }

        return getWebSession().getCustomerId();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
