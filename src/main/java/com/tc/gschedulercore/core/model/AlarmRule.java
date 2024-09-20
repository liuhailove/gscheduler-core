package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 告警规则
 *
 * @author honggang.liu
 */
public class AlarmRule implements Serializable {

    /**
     * 任意
     */
    public static final Integer TRIGGER_CONDITION_ANY = 0;

    /**
     * 所有
     */
    public static final Integer TRIGGER_CONDITION_ALL = 1;

    /**
     * 执行日志
     */
    public static final Integer EXECUTOR_LOG = 1;

    /**
     * 过程日志
     */
    public static final Integer PROCESS_LOG = 2;

    public static final Integer PROCESS_MONITOR = 3;

    /**
     * 通知级别
     */
    public static final Integer NOTIFY_LEVEL = 1;

    /**
     * 告警级别
     */
    public static final Integer ALARM_LEVEL = 2;

    /**
     * 严重级别
     */
    public static final Integer SEVERITY_LEVEL = 3;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 执行器ID
     */
    private Integer jobGroupId;

    /**
     * 执行器名称
     */
    private String jobGroupName;

    /**
     * 资源类别(1：执行日志，2：过程日志)
     */
    private Integer resourceType;

    /**
     * 告警名称
     */
    private String alarmName;

    /**
     * 告警级别（1：通知，2：告警，3：严重）
     */
    private Integer alarmLevel;

    /**
     * 告警条件
     * <p>
     * 分为所有和任意两类：
     * 所有：当设置的触发条件全部满足时，系统会发送告警。
     * 任意：当设置的触发条件满足一条或以上时，系统会发送告警
     */
    private Integer triggerCondition;

    /**
     * 是否开启
     * <p>
     * 打开是否开启开关：在告警规则创建完成时，规则立即生效。
     * 关闭是否开启开关：在告警规则创建完成时，规则不生效。您可以后续编辑，手动打开开关，使规则生效。
     */
    private Boolean open;

    /**
     * 通知链接，此处主要指seatalk群链接
     */
    private String notifyUrl;

    /**
     * 通知具体人，如果不配置，则仅通知
     */
    private String notifyUsers;

    /**
     * 值班电话，英文,分割，告警级别为严重时启用
     */
    private String voiceAlarmTels;

    /**
     * 告警规则关联的任务ID
     */
    private String jobIdes;
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


    public Integer getResourceType() {
        return resourceType;
    }

    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public Integer getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(Integer alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public Integer getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(Integer triggerCondition) {
        this.triggerCondition = triggerCondition;
    }


    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getNotifyUsers() {
        return notifyUsers;
    }

    public void setNotifyUsers(String notifyUsers) {
        this.notifyUsers = notifyUsers;
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

    public String getJobIdes() {
        return jobIdes;
    }

    public void setJobIdes(String jobIdes) {
        this.jobIdes = jobIdes;
    }

    public Integer getJobGroupId() {
        return jobGroupId;
    }

    public void setJobGroupId(Integer jobGroupId) {
        this.jobGroupId = jobGroupId;
    }

    public String getJobGroupName() {
        return jobGroupName;
    }

    public void setJobGroupName(String jobGroupName) {
        this.jobGroupName = jobGroupName;
    }

    public String getVoiceAlarmTels() {
        return voiceAlarmTels;
    }

    public void setVoiceAlarmTels(String voiceAlarmTels) {
        this.voiceAlarmTels = voiceAlarmTels;
    }


    public static String queryAlarmLevelDesc(Integer alarmLevel) {
        if (alarmLevel == null) {
            return "UNKNOWN";
        }
        if (alarmLevel == 1) {
            return "通知";
        }
        if (alarmLevel == 2) {
            return "告警";
        }
        if (alarmLevel == 3) {
            return "严重";
        }
        return "UNKNOWN";
    }
}
