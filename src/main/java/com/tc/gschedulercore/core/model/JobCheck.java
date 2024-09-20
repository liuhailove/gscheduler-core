package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * go-scheduler info
 *
 * @author honggang.liu
 */
public class JobCheck implements Serializable {

    /**
     * 主键ID
     */
    private int id;

    /**
     * 告警规则ID
     */
    private Long alarmRuleId;

    /**
     * 执行器主键ID
     */
    private int jobGroup;

    /**
     * jobId，jobGroup+jobId需要唯一
     */
    private int jobId;

    /**
     * 触发频率
     */
    private int triggerFixedRateInMin;

    /**
     * 上次调度时间
     */
    private long triggerLastTime;

    /**
     * 下次调度时间
     */
    private long triggerNextTime;

    /**
     * 添加时间
     */
    private Date gmtCreate;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getTriggerFixedRateInMin() {
        return triggerFixedRateInMin;
    }

    public void setTriggerFixedRateInMin(int triggerFixedRateInMin) {
        this.triggerFixedRateInMin = triggerFixedRateInMin;
    }

    public long getTriggerLastTime() {
        return triggerLastTime;
    }

    public void setTriggerLastTime(long triggerLastTime) {
        this.triggerLastTime = triggerLastTime;
    }

    public long getTriggerNextTime() {
        return triggerNextTime;
    }

    public void setTriggerNextTime(long triggerNextTime) {
        this.triggerNextTime = triggerNextTime;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Long getAlarmRuleId() {
        return alarmRuleId;
    }

    public void setAlarmRuleId(Long alarmRuleId) {
        this.alarmRuleId = alarmRuleId;
    }
}
