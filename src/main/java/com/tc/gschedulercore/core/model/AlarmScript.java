package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 告警脚本
 *
 * @author honggang.liu
 */
public class AlarmScript implements Serializable {

    /**
     * 主键ID
     */
    private Long id;
    private long triggerLastTime;

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

    /**
     * 下次调度时间
     */
    private long triggerNextTime;

    /**
     * 条目对应的规则ID
     */
    private Long alarmRuleId;

    /**
     * 执行器主键ID
     */
    private int jobGroup;

    /**
     * 脚本名称
     */
    private String scriptName;

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
     * 脚本重试配置
     */
    private String scriptRetryConf;

    /**
     * 脚本重试此处
     */
    private int scriptRetryCount;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 更新时间
     */
    private Date gmtModified;

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

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
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

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public int getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(int jobGroup) {
        this.jobGroup = jobGroup;
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

    public int getScriptRetryCount() {
        return scriptRetryCount;
    }

    public void setScriptRetryCount(int scriptRetryCount) {
        this.scriptRetryCount = scriptRetryCount;
    }

    @Override
    public String toString() {
        return "AlarmScript{" +
                "id=" + id +
                ", alarmRuleId=" + alarmRuleId +
                ", jobGroup=" + jobGroup +
                ", scriptName='" + scriptName + '\'' +
                ", alarmCheckExp='" + alarmCheckExp + '\'' +
                ", alarmMsgExp='" + alarmMsgExp + '\'' +
                ", cronExp='" + cronExp + '\'' +
                ", scriptRetryConf='" + scriptRetryConf + '\'' +
                ", gmtCreate=" + gmtCreate +
                ", gmtModified=" + gmtModified +
                '}';
    }
}
