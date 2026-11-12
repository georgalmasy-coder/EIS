package com.bepa.eis.server.api.web.application.views.pro.dashboard.teamwork;

import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;

public class TeamworkRecord {

    private EntityId fromEntityId;
    private String fromEntityCode;
    private String fromEntityName;
    private String fromEntityTrl;
    private String fromEntityOwner;
    private String fromEntityDepartment;
    private String fromIrlId;
    private String fromClassificationIds;

    private EntityId toEntityId;
    private String toEntityCode;
    private String toEntityName;
    private String toEntityTrl;
    private String toEntityOwner;
    private String toEntityDepartment;

    private String toIrlId;
    private String toClassificationIds;
    private String nextIrlMeeting;

    public TeamworkRecord() {
    }

    public String getFromEntityId() {
        return fromEntityId != null && fromEntityId.getValue() != null ? fromEntityId.getValue().toString() : "";
    }

    public void setFromEntityId(EntityId fromEntityId) {
        this.fromEntityId = fromEntityId;
    }

    public String getFromEntityCode() {
        return fromEntityCode != null ? fromEntityCode : "";
    }

    public void setFromEntityCode(String fromSbsCode) {
        this.fromEntityCode = fromSbsCode;
    }

    public String getFromEntityName() {
        return fromEntityName != null ? fromEntityName : "";
    }

    public void setFromEntityName(String fromSystemName) {
        this.fromEntityName = fromSystemName;
    }

    public String getFromTrlId() {
        return fromEntityTrl != null ? fromEntityTrl : "";
    }

    public void setFromEntityTrl(String fromEntityTrl) {
        this.fromEntityTrl = fromEntityTrl;
    }

    public String getFromOwnerId() {
        return fromEntityOwner != null ? fromEntityOwner : "";
    }

    public void setFromEntityOwner(String fromSystemOwner) {
        this.fromEntityOwner = fromSystemOwner;
    }

    public String getFromDepartmentId() {
        return fromEntityDepartment != null ? fromEntityDepartment : "";
    }

    public void setFromDepartment(String fromDepartment) {
        this.fromEntityDepartment = fromDepartment;
    }

    public String getFromIrlId() {
        return fromIrlId != null ? fromIrlId.toString() : "";
    }

    public void setFromIrlId(Integer fromIrlId) {
        this.fromIrlId = fromIrlId != null ? fromIrlId.toString() : "";
    }

    public String getFromClassificationIds() {
        return fromClassificationIds;
    }

    public void setFromClassificationIds(String fromClassificationIds) {
        this.fromClassificationIds = fromClassificationIds;
    }

    public String getToEntityId() {
        return toEntityId != null && toEntityId.getValue() != null ? toEntityId.getValue().toString() : "";
    }

    public void setToEntityId(EntityId toEntityId) {
        this.toEntityId = toEntityId;
    }

    public String getToEntityCode() {
        return toEntityCode != null ? toEntityCode : "";
    }

    public void setToCode(String toSbsCode) {
        this.toEntityCode = toSbsCode;
    }

    public String getToName() {
        return toEntityName != null ? toEntityName : "";
    }

    public void setToName(String toSystemName) {
        this.toEntityName = toSystemName;
    }

    public String getToTrlId() {
        return toEntityTrl != null ? toEntityTrl : "";
    }

    public void setToTrlId(String toTrl) {
        this.toEntityTrl = toTrl;
    }

    public String getToOwnerId() {
        return toEntityOwner != null ? toEntityOwner : "";
    }

    public void setToOwner(String toSystemOwner) {
        this.toEntityOwner = toSystemOwner;
    }

    public String getToDepartmentId() {
        return toEntityDepartment != null ? toEntityDepartment : "";
    }

    public void setToDepartment(String toDepartment) {
        this.toEntityDepartment = toDepartment;
    }

    public String getToIrlId() {
        return toIrlId != null ? toIrlId.toString() : "";
    }

    public void setToIrlId(String toIrlId) {
        this.toIrlId = toIrlId != null ? toIrlId : "";
    }

    public void setToIrlId(Integer toIrlId) {
        this.toIrlId = toIrlId != null ? toIrlId.toString() : "";
    }

    public String getToClassificationIds() {
        return toClassificationIds;
    }

    public void setToClassificationIds(String toClassificationIds) {
        this.toClassificationIds = toClassificationIds;
    }

    public String getNextIrlMeeting() {
        return nextIrlMeeting;
    }

    public void setNextIrlMeeting(String nextIrlMeeting) {
        this.nextIrlMeeting = nextIrlMeeting;
    }

}
