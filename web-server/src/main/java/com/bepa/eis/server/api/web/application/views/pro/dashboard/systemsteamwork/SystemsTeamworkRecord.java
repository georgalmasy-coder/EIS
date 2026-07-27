package com.bepa.eis.server.api.web.application.views.pro.dashboard.systemsteamwork;

import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.system.TRL;
import com.bepa.eis.server.dataprovider.fields.strings.SBSCode;
import com.bepa.eis.server.dataprovider.fields.strings.SystemName;

public class SystemsTeamworkRecord {

    private EntityId fromEntityId;
    private SBSCode fromSbsCode;
    private SystemName fromSystemName;
    private TRL fromTrl;
    private SystemOwner fromSystemOwner;
    private SystemDepartment fromSystemDepartment;
    private String fromIrlId;
    private String fromClassificationIds;

    private EntityId toEntityId;
    private SBSCode toSbsCode;
    private SystemName toSystemName;
    private TRL toTrl;
    private SystemOwner toSystemOwner;
    private SystemDepartment toSystemDepartment;

    private String toIrlId;
    private String toClassificationIds;

    public SystemsTeamworkRecord() {
    }

    public String getFromEntityId() {
        return fromEntityId != null && fromEntityId.getValue() != null ? fromEntityId.getValue().toString() : "";
    }

    public void setFromEntityId(EntityId fromEntityId) {
        this.fromEntityId = fromEntityId;
    }

    public String getFromSbsCode() {
        return fromSbsCode != null && fromSbsCode.getValue() != null ? fromSbsCode.getValue() : "";
    }

    public void setFromSbsCode(SBSCode fromSbsCode) {
        this.fromSbsCode = fromSbsCode;
    }

    public String getFromSystemName() {
        return fromSystemName != null && fromSystemName.getValue() != null ? fromSystemName.getValue() : "";
    }

    public void setFromSystemName(SystemName fromSystemName) {
        this.fromSystemName = fromSystemName;
    }

    public String getFromTrlId() {
        return fromTrl != null && fromTrl.getValue() != null ? fromTrl.getValue() .toString(): "";
    }

    public void setFromTrl(TRL fromTrl) {
        this.fromTrl = fromTrl;
    }

    public String getFromSystemOwnerId() {
        return fromSystemOwner != null && fromSystemOwner.getValue() != null ? fromSystemOwner.getValue().toString(): "";
    }

    public void setFromSystemOwner(SystemOwner fromSystemOwner) {
        this.fromSystemOwner = fromSystemOwner;
    }

    public String getFromSystemDepartmentId() {
        return fromSystemDepartment != null && fromSystemDepartment.getValue() != null ? fromSystemDepartment.getValue().toString(): "";
    }

    public void setFromSystemDepartment(SystemDepartment fromDepartment) {
        this.fromSystemDepartment = fromDepartment;
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

    public String getToSbsCode() {
        return toSbsCode != null && toSbsCode.getValue() != null ? toSbsCode.getValue() : "";
    }

    public void setToSbsCode(SBSCode toSbsCode) {
        this.toSbsCode = toSbsCode;
    }

    public String getToSystemName() {
        return toSystemName != null && toSystemName.getValue() != null ? toSystemName.getValue() : "";
    }

    public void setToSystemName(SystemName toSystemName) {
        this.toSystemName = toSystemName;
    }

    public String getToTrlId() {
        return toTrl != null && toTrl.getValue() != null ? toTrl.getValue() .toString(): "";
    }

    public void setToTrlId(TRL toTrl) {
        this.toTrl = toTrl;
    }

    public String getToSystemOwnerId() {
        return toSystemOwner != null && toSystemOwner.getValue() != null ? toSystemOwner.getValue().toString(): "";
    }

    public void setToSystemOwner(SystemOwner toSystemOwner) {
        this.toSystemOwner = toSystemOwner;
    }

    public String getToSystemDepartmentId() {
        return toSystemDepartment != null && toSystemDepartment.getValue() != null ? toSystemDepartment.getValue().toString(): "";
    }

    public void setToSystemDepartment(SystemDepartment toDepartment) {
        this.toSystemDepartment = toDepartment;
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
}
