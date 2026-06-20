package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserDetailCache extends GenericLookup {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserDetailCache.class);

    private ConcurrentMap<Integer, User> mapOfUsers = new ConcurrentHashMap<>();

    private static final String LOOKUP_SQL =

            "SELECT U.* " +
            "FROM USERS U, USER_CUSTOMER UC " +
            "WHERE U.UserId = UC.UserId " +
            "AND   UC.CustomerId=? " +
            "ORDER BY U.Name";

    public UserDetailCache(Integer customerId, Integer projectId) {
        setLookupSql(LOOKUP_SQL, customerId, projectId);
        reloadCache();
    }

    public void reloadCache() {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(getLookupSql());
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("UserId"));
                user.setName(rs.getString("Name"));
                user.setEmail(rs.getString("Email"));
                user.setPhone(rs.getString("Phone"));
                user.setInitials(rs.getString("Initials"));

                user.setDepartmentId(rs.getInt("departmentId"));
                user.setActive(rs.getBoolean("Active"));

                mapOfUsers.put(user.getUserId(), user);
            }

            log.debug("Bulk-loaded {} status rows into Ehcache", mapOfUsers.size());
        } catch (SQLException e) {
            log.error("Error bulk-loading status lookup data: {}", e.getMessage());
        }
    }

    public User getUser(Integer userId) {
        return userId != null ? mapOfUsers.get(userId) : null;
    }

}
