package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CustomerWorkflowMaintenanceProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerWorkflowMaintenanceProvider.class);

    private static final int DEFAULT_STUCK_LOCK_TIMEOUT_MINUTES = 30;

    private static final String RELEASE_STUCK_WORKFLOW_LOCKS_SQL =
            "UPDATE [dbo].[CUSTOMER_WORKFLOW] " +
                    "SET " +
                    "    LockedAt = NULL, " +
                    "    LockedBy = NULL, " +
                    "    LastError = CASE " +
                    "        WHEN LastError IS NULL OR LTRIM(RTRIM(LastError)) = '' THEN 'Workflow lock timed out and was released automatically' " +
                    "        ELSE LastError " +
                    "    END, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE LockedAt IS NOT NULL " +
                    "  AND LockedAt < DATEADD(MINUTE, -?, SYSUTCDATETIME()) " +
                    "  AND WorkflowStatus = 'ACTIVE' ";

    private static final String RESET_STUCK_SUSPENDED_LOCKS_SQL =
            "UPDATE [dbo].[CUSTOMER_WORKFLOW] " +
                    "SET " +
                    "    LockedAt = NULL, " +
                    "    LockedBy = NULL, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE LockedAt IS NOT NULL " +
                    "  AND LockedAt < DATEADD(MINUTE, -?, SYSUTCDATETIME()) " +
                    "  AND WorkflowStatus IN ('SUSPENDED', 'WAITING_FOR_MANUAL_ATTENTION', 'CANCELLED', 'COMPLETED') ";

    private static final String CANCEL_EXPIRED_PENDING_EMAIL_CONFIRMATION_SQL =
            "UPDATE [dbo].[CUSTOMER_WORKFLOW] " +
                    "SET " +
                    "    WorkflowStatus = 'CANCELLED', " +
                    "    CurrentState = 'CANCELLED', " +
                    "    LastEventType = 'CUSTOMER_EMAIL_CONFIRMATION_EXPIRED', " +
                    "    LastEventAt = SYSUTCDATETIME(), " +
                    "    LastError = 'Customer email confirmation expired', " +
                    "    LockedAt = NULL, " +
                    "    LockedBy = NULL, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE WorkflowStatus = 'ACTIVE' " +
                    "  AND CurrentState = 'PENDING_EMAIL_CONFIRMATION' " +
                    "  AND NextActionAt IS NOT NULL " +
                    "  AND NextActionAt <= SYSUTCDATETIME() " +
                    "  AND LockedAt IS NULL ";

    public CustomerWorkflowMaintenanceProvider(WebSession webSession) {
        super(webSession);
    }

    public int releaseStuckWorkflowLocks() {
        return releaseStuckWorkflowLocks(DEFAULT_STUCK_LOCK_TIMEOUT_MINUTES);
    }

    public int releaseStuckWorkflowLocks(int timeoutMinutes) {
        int safeTimeoutMinutes = normalizeTimeoutMinutes(timeoutMinutes);

        int activeReleased = executeUpdate(
                RELEASE_STUCK_WORKFLOW_LOCKS_SQL,
                safeTimeoutMinutes,
                "release active stuck customer workflow locks"
        );

        int terminalReleased = executeUpdate(
                RESET_STUCK_SUSPENDED_LOCKS_SQL,
                safeTimeoutMinutes,
                "release terminal stuck customer workflow locks"
        );

        int releasedTotal = activeReleased + terminalReleased;

        if (releasedTotal > 0) {
            log.warn(
                    "Released {} stuck customer workflow lock(s). activeReleased={}, terminalReleased={}, timeoutMinutes={}",
                    releasedTotal,
                    activeReleased,
                    terminalReleased,
                    safeTimeoutMinutes
            );
        }

        return releasedTotal;
    }

    public int cancelExpiredPendingEmailConfirmations() {
        int cancelled = executeUpdateWithoutParameter(
                CANCEL_EXPIRED_PENDING_EMAIL_CONFIRMATION_SQL,
                "cancel expired pending email confirmation workflows"
        );

        if (cancelled > 0) {
            log.info("Cancelled {} expired pending customer email confirmation workflow(s).", cancelled);
        }

        return cancelled;
    }

    public CustomerWorkflowMaintenanceResult runMaintenance() {
        return runMaintenance(DEFAULT_STUCK_LOCK_TIMEOUT_MINUTES);
    }

    public CustomerWorkflowMaintenanceResult runMaintenance(int stuckLockTimeoutMinutes) {
        int releasedLocks = releaseStuckWorkflowLocks(stuckLockTimeoutMinutes);
        int cancelledEmailConfirmations = cancelExpiredPendingEmailConfirmations();

        return new CustomerWorkflowMaintenanceResult(
                releasedLocks,
                cancelledEmailConfirmations
        );
    }

    private int executeUpdate(
            String sql,
            int parameter,
            String operationDescription
    ) {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, parameter);

            return statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Could not {}.", operationDescription, e);
            return 0;
        }
    }

    private int executeUpdateWithoutParameter(
            String sql,
            String operationDescription
    ) {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            return statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Could not {}.", operationDescription, e);
            return 0;
        }
    }

    private int normalizeTimeoutMinutes(int timeoutMinutes) {
        return Math.max(1, Math.min(timeoutMinutes, 1440));
    }

    public static class CustomerWorkflowMaintenanceResult {

        private final int releasedLocks;
        private final int cancelledExpiredEmailConfirmations;

        public CustomerWorkflowMaintenanceResult(
                int releasedLocks,
                int cancelledExpiredEmailConfirmations
        ) {
            this.releasedLocks = Math.max(0, releasedLocks);
            this.cancelledExpiredEmailConfirmations = Math.max(0, cancelledExpiredEmailConfirmations);
        }

        public int getReleasedLocks() {
            return releasedLocks;
        }

        public int getCancelledExpiredEmailConfirmations() {
            return cancelledExpiredEmailConfirmations;
        }

        public int getTotalAffectedRows() {
            return releasedLocks + cancelledExpiredEmailConfirmations;
        }

        public boolean hasChanges() {
            return getTotalAffectedRows() > 0;
        }

        @Override
        public String toString() {
            return "CustomerWorkflowMaintenanceResult [releasedLocks=" + releasedLocks
                    + ", cancelledExpiredEmailConfirmations=" + cancelledExpiredEmailConfirmations
                    + ", totalAffectedRows=" + getTotalAffectedRows()
                    + "]";
        }
    }
}