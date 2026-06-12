package com.bepa.eis.server.api.DTO;

public class Department {

    private int departmentId;
    private int customerId;
    private String departmentName;
    private String departmentDescription;
    private Boolean active;

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentDescription(String departmentDescription) {
        this.departmentDescription = departmentDescription;
    }

    public String getDepartmentNameAndDescription() {
        return checkForNull(departmentName) + " - " + checkForNull(departmentDescription);
    }

    private String checkForNull(String value) {
        return value != null ? value : "";
    }

    public String getDepartmentDescription() {
        return departmentDescription;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean isActive() {
        return active;
    }
}
