package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 调度server自身的注册信息
 * serverAddress每30s更新一次，若地址90s没有被更新，则移除
 */
public class ServerRegistry implements Serializable {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 服务地址
     */
    private String serverAddress;
    /**
     * 更新时间
     */
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
