package com.bepa.eis.server.api.DTO;

import java.sql.Timestamp;
import java.time.LocalDate;

public class TrlRecord {
    private Integer customerId;
    private Integer projectId;
    private Integer EntityId;
    private Integer trlId;
    private Timestamp nextTrlDeadline;

    public TrlRecord(Integer customerId, Integer projectId, Integer entityId, Integer trlId, Timestamp nextTrlDeadline) {
        this.customerId = customerId;
        this.projectId = projectId;
        this.EntityId = entityId;
        this.trlId = trlId;
        this.nextTrlDeadline = nextTrlDeadline;
    }

    public Integer getEntityId() {
        return EntityId;
    }

   public Integer getTrlId() {
        return trlId;
   }

   public Timestamp getNextTrlDeadline() {
        return nextTrlDeadline;
    }

}
