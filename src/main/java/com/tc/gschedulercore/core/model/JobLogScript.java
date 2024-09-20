package com.tc.gschedulercore.core.model;

import java.io.Serializable;

/**
 * 脚本告警检查日志
 *
 * @author honggang.liu
 */
public class JobLogScript implements Serializable {

    private Long id;

    /**
     * job info
     **/
    private int jobGroup;
    /**
     * 任务ID
     */
    private int jobId;
    /**
     * 任务日志ID
     */
    private Long jobLogId;
    /**
     * 告警规则ID
     */
    private Long alarmRuleId;
    /**
     * 告警脚本ID
     */
    private Long alarmScriptId;
    /**
     * 重试次数
     */
    private int scriptRetryCount;
    /**
     * 告警检查时间
     */
    private long scriptCheckTriggerTime;

    /**
     * 重试类型 @see com.xxl.job.core.enums.RetryType
     */
    private int retryType;

    /**
     * 脚本重试配置，此配置值作用于 "告警管理"-》"脚本告警"，
     * 重试类型只支持 用户自定义重试
     */
    private String scriptRetryConf;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Long getJobLogId() {
        return jobLogId;
    }

    public void setJobLogId(Long jobLogId) {
        this.jobLogId = jobLogId;
    }

    public Long getAlarmRuleId() {
        return alarmRuleId;
    }

    public void setAlarmRuleId(Long alarmRuleId) {
        this.alarmRuleId = alarmRuleId;
    }

    public Long getAlarmScriptId() {
        return alarmScriptId;
    }

    public void setAlarmScriptId(Long alarmScriptId) {
        this.alarmScriptId = alarmScriptId;
    }

    public int getScriptRetryCount() {
        return scriptRetryCount;
    }

    public void setScriptRetryCount(int scriptRetryCount) {
        this.scriptRetryCount = scriptRetryCount;
    }

    public long getScriptCheckTriggerTime() {
        return scriptCheckTriggerTime;
    }

    public void setScriptCheckTriggerTime(long scriptCheckTriggerTime) {
        this.scriptCheckTriggerTime = scriptCheckTriggerTime;
    }

    public String getScriptRetryConf() {
        return scriptRetryConf;
    }

    public void setScriptRetryConf(String scriptRetryConf) {
        this.scriptRetryConf = scriptRetryConf;
    }

    public int getRetryType() {
        return retryType;
    }

    public void setRetryType(int retryType) {
        this.retryType = retryType;
    }

    @Override
    public String toString() {
        return "JobLogScript{" +
                "id=" + id +
                ", jobGroup=" + jobGroup +
                ", jobId=" + jobId +
                ", jobLogId=" + jobLogId +
                ", alarmRuleId=" + alarmRuleId +
                ", alarmScriptId=" + alarmScriptId +
                ", scriptRetryCount=" + scriptRetryCount +
                ", scriptCheckTriggerTime=" + scriptCheckTriggerTime +
                ", retryType=" + retryType +
                ", scriptRetryConf='" + scriptRetryConf + '\'' +
                '}';
    }
}
