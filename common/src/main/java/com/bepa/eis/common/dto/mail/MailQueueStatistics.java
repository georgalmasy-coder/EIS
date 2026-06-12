package com.bepa.eis.common.dto.mail;

import java.sql.Timestamp;

public class MailQueueStatistics {

    private Integer queuedCount;
    private Integer sendingCount;
    private Integer failedCount;
    private Integer undeliveredCount;
    private Integer cancelledCount;
    private Integer sentLast24HoursCount;
    private Integer totalOpenCount;
    private Timestamp oldestQueuedAt;

    public MailQueueStatistics() {
        queuedCount = 0;
        sendingCount = 0;
        failedCount = 0;
        undeliveredCount = 0;
        cancelledCount = 0;
        sentLast24HoursCount = 0;
        totalOpenCount = 0;
        oldestQueuedAt = null;
    }

    public Integer getQueuedCount() {
        return queuedCount;
    }

    public void setQueuedCount(Integer queuedCount) {
        this.queuedCount = safeInteger(queuedCount);
        recalculateTotalOpenCount();
    }

    public Integer getSendingCount() {
        return sendingCount;
    }

    public void setSendingCount(Integer sendingCount) {
        this.sendingCount = safeInteger(sendingCount);
        recalculateTotalOpenCount();
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = safeInteger(failedCount);
        recalculateTotalOpenCount();
    }

    public Integer getUndeliveredCount() {
        return undeliveredCount;
    }

    public void setUndeliveredCount(Integer undeliveredCount) {
        this.undeliveredCount = safeInteger(undeliveredCount);
    }

    public Integer getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(Integer cancelledCount) {
        this.cancelledCount = safeInteger(cancelledCount);
    }

    public Integer getSentLast24HoursCount() {
        return sentLast24HoursCount;
    }

    public void setSentLast24HoursCount(Integer sentLast24HoursCount) {
        this.sentLast24HoursCount = safeInteger(sentLast24HoursCount);
    }

    public Integer getTotalOpenCount() {
        return totalOpenCount;
    }

    public void setTotalOpenCount(Integer totalOpenCount) {
        this.totalOpenCount = safeInteger(totalOpenCount);
    }

    public Timestamp getOldestQueuedAt() {
        return oldestQueuedAt;
    }

    public void setOldestQueuedAt(Timestamp oldestQueuedAt) {
        this.oldestQueuedAt = oldestQueuedAt;
    }

    public boolean hasQueuedMails() {
        return queuedCount != null && queuedCount > 0;
    }

    public boolean hasSendingMails() {
        return sendingCount != null && sendingCount > 0;
    }

    public boolean hasFailedMails() {
        return failedCount != null && failedCount > 0;
    }

    public boolean hasUndeliveredMails() {
        return undeliveredCount != null && undeliveredCount > 0;
    }

    public boolean hasOpenMails() {
        return totalOpenCount != null && totalOpenCount > 0;
    }

    private void recalculateTotalOpenCount() {
        totalOpenCount = safeInteger(queuedCount)
                + safeInteger(sendingCount)
                + safeInteger(failedCount);
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    @Override
    public String toString() {
        return "MailQueueStatistics [queuedCount=" + queuedCount
                + ", sendingCount=" + sendingCount
                + ", failedCount=" + failedCount
                + ", undeliveredCount=" + undeliveredCount
                + ", cancelledCount=" + cancelledCount
                + ", sentLast24HoursCount=" + sentLast24HoursCount
                + ", totalOpenCount=" + totalOpenCount
                + ", oldestQueuedAt=" + oldestQueuedAt
                + "]";
    }
}