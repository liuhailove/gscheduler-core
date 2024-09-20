package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知信息
 *
 * @author honggang.liu
 */
public class NotifyInfo implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 应用名称
     */
    private String app;

    /**
     * 对应的规则ID
     */
    private Long alarmRuleId;

    /**
     * 防护名称
     */
    private String alarmName;

    /**
     * 指标对应的具体值
     * <p>
     * 执行日志：
     * log.execute.elapse 日志执行耗时
     * log.expected.not.match.actual 预期与实际执行不匹配
     * <p>
     * 过程日志：
     * log.process.msg	   过程消息
     * log.process.key1	   处理消息key1
     * log.process.value1  处理消息val1
     * log.process.key2	   处理消息key2
     * log.process.value2  处理消息value2
     * log.process.key3	   处理消息key3
     * log.process.value3  处理消息value3
     * <p>
     */
    private Integer alarmType;

    /**
     * 告警级别（1：通知，2：告警，3：严重）
     */
    private Integer alarmLevel;

    /**
     * 告警内容
     * <p>
     * val：触发告警时的数值。
     * relation：告警规则中设置的生效关系。
     * threshold：告警规则中设置的告警阈值。
     */
    private String notifyContent;

    /**
     * 任务ID
     */
    private Integer jobId;
    /**
     * 关联的日志ID
     */
    private Long logId;

    /**
     * 通知链接，此处主要指seatalk群链接
     */
    private String notifyUrl;

    /**
     * 通知具体人，如果不配置，则仅通知
     */
    private String notifyUsers;

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

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
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

    public Long getAlarmRuleId() {
        return alarmRuleId;
    }

    public void setAlarmRuleId(Long alarmRuleId) {
        this.alarmRuleId = alarmRuleId;
    }

    public String getNotifyContent() {
        return notifyContent;
    }

    public void setNotifyContent(String notifyContent) {
        this.notifyContent = notifyContent;
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


    public Integer getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(Integer alarmType) {
        this.alarmType = alarmType;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }
}
