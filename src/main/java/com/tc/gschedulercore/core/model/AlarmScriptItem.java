package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 告警脚本
 *
 * @author honggang.liu
 */
public class AlarmScriptItem implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 条目对应的规则ID
     */
    private Long alarmRuleId;

    /**
     * script id
     */
    private Long alarmScriptId;

    /**
     * 告警检查表达式
     */
    private String alarmCheckExp;

    /**
     * 告警消息表达式
     */
    private String alarmMsgExp;

    /**
     * cron表达式
     */
    private String cronExp;

    /**
     * 重试类型 @see com.xxl.job.core.enums.RetryType
     */
    private int retryType;

    /**
     * 重试配置
     */
    private String retryConf;

    /**
     * 脚本重试此处
     */
    private int scriptRetryCount;

    /**
     * 执行器主键ID
     */
    private int jobGroup;

    /**
     * jobId，jobGroup+jobId需要唯一
     */
    private int jobId;

    /**
     * 上次调度时间
     */
    private long triggerLastTime;
    /**
     * 下次调度时间
     */
    private long triggerNextTime;

    /**
     * 创建时间
     */
    private Date gmtCreate;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAlarmRuleId() {
        return alarmRuleId;
    }

    public void setAlarmRuleId(Long alarmRuleId) {
        this.alarmRuleId = alarmRuleId;
    }

    public String getAlarmCheckExp() {
        return alarmCheckExp;
    }

    public void setAlarmCheckExp(String alarmCheckExp) {
        this.alarmCheckExp = alarmCheckExp;
    }

    public String getAlarmMsgExp() {
        return alarmMsgExp;
    }

    public void setAlarmMsgExp(String alarmMsgExp) {
        this.alarmMsgExp = alarmMsgExp;
    }

    public String getCronExp() {
        return cronExp;
    }

    public void setCronExp(String cronExp) {
        this.cronExp = cronExp;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
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

    public Long getAlarmScriptId() {
        return alarmScriptId;
    }

    public void setAlarmScriptId(Long alarmScriptId) {
        this.alarmScriptId = alarmScriptId;
    }

    public String getRetryConf() {
        return retryConf;
    }

    public void setRetryConf(String retryConf) {
        this.retryConf = retryConf;
    }

    public int getRetryType() {
        return retryType;
    }

    public void setRetryType(int retryType) {
        this.retryType = retryType;
    }

    public int getScriptRetryCount() {
        return scriptRetryCount;
    }

    public void setScriptRetryCount(int scriptRetryCount) {
        this.scriptRetryCount = scriptRetryCount;
    }
}
