package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.*;

/**
 * 系统角色model
 *
 * @author honggang.liu
 */
public class JobRole implements Serializable {
    /**
     * 主键ID
     */
    private int id;
    /**
     * 角色名称，只允许英文
     */
    private String roleName;
    /**
     * 操作人
     */
    private String author;
    /**
     * 菜单名称
     */
    private String permissionMenus;
    /**
     * 允许访问的URLs
     */
    private String permissionUrls;
    /**
     * 角色描述
     */
    private String roleDesc;

    /**
     * 增加时间
     */
    private Date addTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 菜单列表
     *
     * @return 菜单列表
     */
    public List<String> getPermissionMenuList() {
        if (permissionMenus != null && permissionMenus.trim().length() > 0) {
            return new ArrayList<>(Arrays.asList(permissionMenus.split(",")));
        }
        return new ArrayList<>(0);
    }

    /**
     * 访问Url
     *
     * @return 访问Url
     */
    public List<String> getPermissionUrlList() {
        if (permissionUrls != null && permissionUrls.trim().length() > 0) {
            return new ArrayList<>(Arrays.asList(permissionUrls.split(",")));
        }
        return new ArrayList<>(0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPermissionUrls() {
        return permissionUrls;
    }

    public void setPermissionUrls(String permissionUrls) {
        this.permissionUrls = permissionUrls;
    }

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

    public String getRoleDesc() {
        return roleDesc;
    }

    public void setRoleDesc(String roleDesc) {
        this.roleDesc = roleDesc;
    }

    public String getPermissionMenus() {
        return permissionMenus;
    }

    public void setPermissionMenus(String permissionMenus) {
        this.permissionMenus = permissionMenus;
    }
}
