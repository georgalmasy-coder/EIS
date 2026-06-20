package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.NotificationId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.NotificationType;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.common.CreatedBy;
import com.bepa.eis.server.dataprovider.fields.strings.NotificationText;
import com.bepa.eis.server.dataprovider.fields.timestamp.AcknowledgeDateTime;
import com.bepa.eis.server.dataprovider.fields.timestamp.CreatedDateTime;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class NotificationProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(NotificationProvider.class);

    private static final String GET_NOTIFICATIONS_BY_USER_ID_SQL =
            "SELECT * FROM NOTIFICATION " +
                    "WHERE CustomerId = ? " +
                    "AND  ProjectId = ? " +
                    "AND  RecipientId = ? " +
                    "ORDER BY CreatedTime DESC";

    private static final String GET_MY_NOTIFICATION_COUNT_SQL =
            "SELECT COUNT(1) AS MY_NOTIFICATION_COUNT FROM NOTIFICATION " +
                    "WHERE CustomerId = ? " +
                    "AND  ProjectId = ? " +
                    "AND  RecipientId = ? ";

    public NotificationProvider(WebSession webSession) {
        super(webSession);
    }

    public Notifications getNotificationsByUserAndProjectId() throws SQLException {

        Notifications notifications = new Notifications(getWebSession());

        if (getWebSession() != null && getWebSession().getProjectId() != null) {

            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(GET_NOTIFICATIONS_BY_USER_ID_SQL)) {

                setInt(ps, getWebSession().getCustomerId(), 1);
                setInt(ps, getWebSession().getProjectId(), 2);
                setInt(ps, getWebSession().getUserId(), 3);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {

                        Notification notification = notifications.getNewNotification();

                        notification.addElement(new NotificationId(rs.getInt(NotificationId.FIELD_NAME)));
                        notification.addElement(new CustomerId(rs.getInt(CustomerId.FIELD_NAME)));
                        notification.addElement(new ProjectId(rs.getInt(ProjectId.FIELD_NAME)));
                        notification.addElement(new NotificationType(rs.getInt(NotificationType.FIELD_NAME)));
                        notification.addElement(new NotificationText(rs.getString(NotificationText.FIELD_NAME)));

                        CreatedBy createdBy = new CreatedBy(getWebSession(), rs.getInt(CreatedBy.FIELD_NAME));
                        notification.addElement(createdBy);

                        notification.addElement(new CreatedDateTime(rs.getTimestamp(CreatedDateTime.FIELD_NAME)));
                        notification.addElement(new AcknowledgeDateTime(rs.getTimestamp(AcknowledgeDateTime.FIELD_NAME)));

                        notifications.addNotification(notification);
                    }

                }
            }
        }
        return notifications;
    }

    public int getMyNotificationCount(Integer customerId, Integer projectId, Integer userId)  {

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_MY_NOTIFICATION_COUNT_SQL)) {

            setInt(ps, customerId, 1);
            setInt(ps, projectId, 2);
            setInt(ps, userId, 3);

            try (ResultSet rs = ps.executeQuery()) {

                return rs.next() ? rs.getInt("MY_NOTIFICATION_COUNT") : 0;
            }

        } catch (SQLException e) {
            log.error("Could not load notification count from database.", e);
        }
        return 0;
    }
}
