package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.*;

/**
 * 系统用户
 *
 * @author honggang.liu
 */
public class JobUser implements Serializable {
    /**
     * 主键ID
     */
    private int id;
    /**
     * 账号
     */
    private String username;
    /**
     * 密码
     */
    private String pwd;
    /**
     * 邮箱
     */
    private String email;

    /**
     * 角色：0-普通用户、1-管理员
     */
    private String roleName;
    /**
     * 组权限：访问组名称，多个逗号分割
     */
    private String permissionGroups;
    /**
     * 平台权限：平台名称列表，多个逗号分割
     */
    private String permissionPlatforms;
    /**
     * 增加时间
     */
    private Date addTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 认证token
     */
    private String authToken;

    /**
     * 组列表
     *
     * @return 组列表
     */
    public List<String> getPermissionGroupList() {
        if (permissionGroups != null && permissionGroups.trim().length() > 0) {
            return new ArrayList<>(Arrays.asList(permissionGroups.split(",")));
        }
        return new ArrayList<>(0);
    }

    /**
     * 访问平台
     *
     * @return 访问平台
     */
    public List<String> getPermissionPlatformList() {
        if (permissionPlatforms != null && permissionPlatforms.trim().length() > 0) {
            return new ArrayList<>(Arrays.asList(permissionPlatforms.split(",")));
        }
        return new ArrayList<>(0);
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

//    public String getPermissionUrls() {
//        return permissionUrls;
//    }
//
//    public void setPermissionUrls(String permissionUrls) {
//        this.permissionUrls = permissionUrls;
//    }

    public String getPermissionGroups() {
        return permissionGroups;
    }

    public void setPermissionGroups(String permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    public String getPermissionPlatforms() {
        return permissionPlatforms;
    }

    public void setPermissionPlatforms(String permissionPlatforms) {
        this.permissionPlatforms = permissionPlatforms;
    }

//    public String getPermissionJobGroups() {
//        return permissionJobGroups;
//    }
//
//    public void setPermissionJobGroups(String permissionJobGroups) {
//        this.permissionJobGroups = permissionJobGroups;
//    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    //    /**
//     * 权限娇艳
//     * @param jobGroup
//     * @return
//     */
//    public boolean validPermission(int jobGroup) {
//        if (this.roleName == "SUPER_ADMIN") {
//            return true;
//        } else {
//            if (StringUtils.hasText(this.permission)) {
//                for (String permissionItem : this.permission.split(",")) {
//                    if (String.valueOf(jobGroup).equals(permissionItem)) {
//                        return true;
//                    }
//                }
//            }
//            return false;
//        }
//
//    }

}
