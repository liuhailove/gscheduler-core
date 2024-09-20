package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务延迟执行上报日志
 *
 * @author honggang.liu
 */
public class DelayLog implements Serializable {

    /**
     * 默认状态
     */
    public static final int ALARM_STATUS_DEFAULT = 0;
    /**
     * 上锁
     */
    public static final int ALARM_STATUS_LOCK = -1;
    /**
     * 不需要告警
     */
    public static final int ALARM_STATUS_NO_NEED = 1;
    /**
     * 告警成功
     */
    public static final int ALARM_STATUS_SUCCESS = 2;
    /**
     * 告警失败
     */
    public static final int ALARM_STATUS_FAILED = 3;

    private Long id;

    /**
     * 执行日志ID
     */
    private Long jobLogId;
    /**
     * job info
     **/
    private int jobGroup;
    /**
     * job info id
     */
    private int jobId;
    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 日志生成
     */
    private Date logDate;

    /**
     * 任务开始执行时间
     */
    private Date startExecutorDate;


    /**
     * 业务执行容忍阈值
     */
    private int startExecutorToleranceThresholdInMin;

    /**
     * 开始执行耗时（ms）
     */
    private int timeElapseInMs;

    /**
     * 地址
     */
    private String address;

    /**
     * alarm info
     */
    private int alarmStatus;

    /**
     * 创建时间
     */
    private Date addTime;

    /**
     * 创建时间
     */
    private Date updateTime;

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

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Date getLogDate() {
        return logDate;
    }

    public void setLogDate(Date logDate) {
        this.logDate = logDate;
    }

    public Date getStartExecutorDate() {
        return startExecutorDate;
    }

    public void setStartExecutorDate(Date startExecutorDate) {
        this.startExecutorDate = startExecutorDate;
    }

    public int getStartExecutorToleranceThresholdInMin() {
        return startExecutorToleranceThresholdInMin;
    }

    public void setStartExecutorToleranceThresholdInMin(int startExecutorToleranceThresholdInMin) {
        this.startExecutorToleranceThresholdInMin = startExecutorToleranceThresholdInMin;
    }

    public int getTimeElapseInMs() {
        return timeElapseInMs;
    }

    public void setTimeElapseInMs(int timeElapseInMs) {
        this.timeElapseInMs = timeElapseInMs;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public int getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(int alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    @Override
    public String toString() {
        return "XxlDelayLog{" +
                "id=" + id +
                ", jobLogId=" + jobLogId +
                ", jobGroup=" + jobGroup +
                ", jobId=" + jobId +
                ", jobName='" + jobName + '\'' +
                ", alarmStatus='" + alarmStatus + '\'' +
                ", logDate=" + logDate +
                ", startExecutorDate=" + startExecutorDate +
                ", businessStartExecutorToleranceThresholdInMin=" + startExecutorToleranceThresholdInMin +
                ", timeElapseInMs=" + timeElapseInMs +
                ", address='" + address + '\'' +
                ", createTime=" + addTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
