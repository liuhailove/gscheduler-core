package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * JobInfo变更历史
 *
 * @author honggang.liu
 */
public class JobInfoHistory implements Serializable {
    /**
     * 主键ID
     */
    private int id;
    /**
     * 任务主键ID
     */
    private int jobId;
    /**
     *  job json串
     */
    private String jobSource;
    /**
     * 备注
     */
    private String jobRemark;
    /**
     * 添加时间
     */
    private Date addTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getJobSource() {
        return jobSource;
    }

    public void setJobSource(String jobSource) {
        this.jobSource = jobSource;
    }

    public String getJobRemark() {
        return jobRemark;
    }

    public void setJobRemark(String jobRemark) {
        this.jobRemark = jobRemark;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }
}
