package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 日志补偿表
 *
 * @author honggang.liu
 */
public class JobCompensate implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 日志ID
     */
    private Long jobLogId;

    /**
     * job info
     **/
    private int jobGroup;

    /**
     * 任务ID
     */
    private int jobId;

    /**
     * 过期时间，过期后会自动清理，默认6h过期
     */
    private Date expireTime;

    /**
     * 增加时间
     */
    private Date addTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobLogId() {
        return jobLogId;
    }

    public void setJobLogId(Long jobLogId) {
        this.jobLogId = jobLogId;
    }

    public int getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(int jobGroup) {
        this.jobGroup = jobGroup;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

}
