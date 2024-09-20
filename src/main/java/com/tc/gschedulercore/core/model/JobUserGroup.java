package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.*;

/**
 * 组model
 *
 * @author honggang.liu
 */
public class JobUserGroup implements Serializable {
    /**
     * 主键ID
     */
    private int id;
    /**
     * 组名，只允许英文
     */
    private String groupName;
    /**
     * 执行器权限：执行器appname列表，多个逗号分割
     */
    private String permissionJobGroups;
    /**
     * 平台权限：平台名称，多个逗号分割
     */
    private String permissionPlatforms;
    /**
     * 组描述
     */
    private String groupDesc;
    /**
     * 操作人
     */
    private String author;
    /**
     * 增加时间
     */
    private Date addTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    public List<String> getPermissionPlatformList() {
        if (permissionPlatforms != null && permissionPlatforms.trim().length() > 0) {
            return new ArrayList<>(Arrays.asList(permissionPlatforms.split(",")));
        }
        return new ArrayList<>(0);
    }

    public List<String> getPermissionJobGroupList() {
        if (permissionJobGroups != null && permissionJobGroups.trim().length() > 0) {
            return new ArrayList<>(Arrays.asList(permissionJobGroups.split(",")));
        }
        return new ArrayList<>(0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getPermissionJobGroups() {
        return permissionJobGroups;
    }

    public void setPermissionJobGroups(String permissionJobGroups) {
        this.permissionJobGroups = permissionJobGroups;
    }

    public String getPermissionPlatforms() {
        return permissionPlatforms;
    }

    public void setPermissionPlatforms(String permissionPlatforms) {
        this.permissionPlatforms = permissionPlatforms;
    }

    public String getGroupDesc() {
        return groupDesc;
    }

    public void setGroupDesc(String groupDesc) {
        this.groupDesc = groupDesc;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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
}
