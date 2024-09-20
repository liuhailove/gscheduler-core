package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 开放API model
 *
 * @author honggang.liu
 */
public class OpenApi implements Serializable {
    /**
     * 主键ID
     */
    private int id;
    /**
     * api名称，只允许英文
     */
    private String apiName;
    /**
     * 访问的URL列表
     */
    private String urls;
    /**
     * 描述
     */
    private String apiDesc;
    /**
     * 执行器Id
     */
    private Integer jobGroup;

    /**
     * 执行器名称
     */
    private String jobGroupName;

    /**
     * 访问token
     */
    private String accessToken;

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

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public String getApiDesc() {
        return apiDesc;
    }

    public void setApiDesc(String apiDesc) {
        this.apiDesc = apiDesc;
    }

    public Integer getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(Integer jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
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

    public String getJobGroupName() {
        return jobGroupName;
    }

    public void setJobGroupName(String jobGroupName) {
        this.jobGroupName = jobGroupName;
    }
}
