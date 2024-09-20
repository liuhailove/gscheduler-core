package com.tc.gschedulercore.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;

/**
 * 平台model
 *
 * @author honggang.liu
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class JobPlatform implements Serializable {
    /**
     * 主键ID
     */
    private int id;
    /**
     * 平台名称，只允许英文
     */
    private String platformName;

    /**
     * 平台描述
     */
    private String platformDesc;

    /**
     * 平台地址
     */
    private String platformAddress;
    /**
     * 环境
     */
    private String env;
    /**
     * 区域
     */
    private String region;

    /**
     * 状态
     */
    private int platStatus;

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformAddress() {
        return platformAddress;
    }

    public void setPlatformAddress(String platformAddress) {
        this.platformAddress = platformAddress;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getPlatStatus() {
        return platStatus;
    }

    public void setPlatStatus(int platStatus) {
        this.platStatus = platStatus;
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

    public String getPlatformDesc() {
        return platformDesc;
    }

    public void setPlatformDesc(String platformDesc) {
        this.platformDesc = platformDesc;
    }

    @Override
    public String toString() {
        return "XxlJobPlatform{" +
                "id=" + id +
                ", platformName='" + platformName + '\'' +
                ", platformDesc='" + platformDesc + '\'' +
                ", platformAddress='" + platformAddress + '\'' +
                ", env='" + env + '\'' +
                ", region='" + region + '\'' +
                ", platStatus=" + platStatus +
                ", author='" + author + '\'' +
                ", addTime=" + addTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
