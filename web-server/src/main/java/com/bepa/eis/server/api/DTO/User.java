package com.bepa.eis.server.api.DTO;

import com.bepa.eis.common.enums.user.UserRoles;

public class User {

    private Integer userId;
    private Integer userRoleId;
    private String initials;
    private String name;
    private String email;
    private String phone;
    private Integer departmentId;
    private Boolean active;

    public User() {
        userId = -1;
        initials = "??";
        name = "<Unknown User>";
        email = "";
        active = false;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
    }

    public UserRoles getUserRole() {
        return UserRoles.fromId(userRoleId);
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public String getInitials() {
        return initials;
    }

    public String getInitialsAndName() {
        return getInitials() + " (" + getName() + ")";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getActive() {
        return active;
    }

    public boolean isActive() {
        return active != null ? active : false;
    }

    public String toString() {
        return "User [userId=" + userId + ", userRoleId=" + userRoleId + ", initials=" + initials + ", name=" + name + ", email=" + email + ", phone=" + phone + ", departmentId=" + departmentId + ", active=" + active + "]";
    }
}
