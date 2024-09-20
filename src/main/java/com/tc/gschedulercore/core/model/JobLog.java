package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * go-scheduler log, used to track trigger process
 *
 * @author honggang.liu  2015-12-19 23:19:09
 */
public class JobLog implements Serializable {

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

    /**
     * 不需要下发
     */
    public static final int DISPATCH_SUB_NO_NEED = 0;
    /**
     * 等待下发
     */
    public static final int DISPATCH_SUB_WAIT = 1;
    /**
     * 下发成功
     */
    public static final int DISPATCH_SUB_SUCCESS = 2;

    private Long id;

    /**
     * job info
     **/
    private int jobGroup;
    private int jobId;
    /**
     * 任务名称
     */
    private String jobName;

    /**
     * execute info
     **/
    private String executorAddress;
    private String executorHandler;
    private String executorParam;
    private String executorShardingParam;

    /**
     * 执行失败重试统计
     */
    private int executorFailRetryCount;

    public int getTaskTerminationFlag() {
        return taskTerminationFlag;
    }

    public void setTaskTerminationFlag(int taskTerminationFlag) {
        this.taskTerminationFlag = taskTerminationFlag;
    }

    private int taskTerminationFlag;

    /**
     * 重试触发时间
     */
    private long executorFailTriggerTime;
    /**
     * trigger info
     */
    private Date triggerTime;
    private int triggerCode;
    private String triggerMsg;

    /**
     * handle info
     */
    private Date handleTime;
    private int handleCode;
    private String handleMsg;

    /**
     * 处理消息
     */
    private String childrenExecutorParams;

    /**
     * alarm info
     */
    private int alarmStatus;

    /**
     * 执行阈值
     */
    private long executorThresholdTimeout;

    /**
     * 阈值告警状态
     */
    private int alarmThresholdStatus;

    /**
     * 日志类型：main_log,sub_log
     */
    private int logType;

    /**
     * 触发类型
     */
    private int triggerType;

    /**
     * 是否有子log
     */
    private boolean hasSub;

    /**
     * 父任务ID
     */
    private long parentLog;
    /**
     * 执行时长
     */
    private long executeTime;

    /**
     * 任务一次运行的实例ID
     */
    private String instanceId;

    /**
     * 标签名称，取自LogTag设置
     */
    private String tagName;

    /**
     * 下发子任务：0，无需下发；1：等待下发，2：下发完成
     */
    private int dispatchSub;

    /**
     * 开始执行时间，如果是延迟任务，这个时间是真正的开始执行时间；
     * 如果是立即执行任务，则此值为0
     */
    private Long startExecuteTimeInMs;

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

    public String getExecutorAddress() {
        return executorAddress;
    }

    public void setExecutorAddress(String executorAddress) {
        this.executorAddress = executorAddress;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParam() {
        return executorParam;
    }

    public void setExecutorParam(String executorParam) {
        this.executorParam = executorParam;
    }

    public String getExecutorShardingParam() {
        return executorShardingParam;
    }

    public void setExecutorShardingParam(String executorShardingParam) {
        this.executorShardingParam = executorShardingParam;
    }

    public int getExecutorFailRetryCount() {
        return executorFailRetryCount;
    }

    public void setExecutorFailRetryCount(int executorFailRetryCount) {
        this.executorFailRetryCount = executorFailRetryCount;
    }

    public Date getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(Date triggerTime) {
        this.triggerTime = triggerTime;
    }

    public int getTriggerCode() {
        return triggerCode;
    }

    public void setTriggerCode(int triggerCode) {
        this.triggerCode = triggerCode;
    }

    public String getTriggerMsg() {
        return triggerMsg;
    }

    public void setTriggerMsg(String triggerMsg) {
        this.triggerMsg = triggerMsg;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public int getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(int alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    public long getExecutorFailTriggerTime() {
        return executorFailTriggerTime;
    }

    public void setExecutorFailTriggerTime(long executorFailTriggerTime) {
        this.executorFailTriggerTime = executorFailTriggerTime;
    }

    public long getExecutorThresholdTimeout() {
        return executorThresholdTimeout;
    }

    public void setExecutorThresholdTimeout(long executorThresholdTimeout) {
        this.executorThresholdTimeout = executorThresholdTimeout;
    }

    public int getAlarmThresholdStatus() {
        return alarmThresholdStatus;
    }

    public void setAlarmThresholdStatus(int alarmThresholdStatus) {
        this.alarmThresholdStatus = alarmThresholdStatus;
    }

    public String getChildrenExecutorParams() {
        return childrenExecutorParams;
    }

    public void setChildrenExecutorParams(String childrenExecutorParams) {
        this.childrenExecutorParams = childrenExecutorParams;
    }

    public long getParentLog() {
        return parentLog;
    }

    public void setParentLog(long parentLog) {
        this.parentLog = parentLog;
    }

    public int getLogType() {
        return logType;
    }

    public void setLogType(int logType) {
        this.logType = logType;
    }

    public int getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(int triggerType) {
        this.triggerType = triggerType;
    }

    public boolean isHasSub() {
        return hasSub;
    }

    public void setHasSub(boolean hasSub) {
        this.hasSub = hasSub;
    }

    public long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public int getDispatchSub() {
        return dispatchSub;
    }

    public int getShardNum() {
        if (this.getExecutorShardingParam() == null) {
            return 1;
        }
        String[] arr = this.getExecutorShardingParam().split("/");
        if (arr.length == 1) {
            return 1;
        }
        return Integer.parseInt(arr[1]);
    }

    public void setDispatchSub(int dispatchSub) {
        this.dispatchSub = dispatchSub;
    }

    public Long getStartExecuteTimeInMs() {
        return startExecuteTimeInMs;
    }

    public void setStartExecuteTimeInMs(Long startExecuteTimeInMs) {
        this.startExecuteTimeInMs = startExecuteTimeInMs;
    }

    @Override
    public String toString() {
        return "JobLog{" +
                "id=" + id +
                ", jobGroup=" + jobGroup +
                ", jobId=" + jobId +
                ", executorAddress='" + executorAddress + '\'' +
                ", executorHandler='" + executorHandler + '\'' +
                ", executorParam='" + executorParam + '\'' +
                ", executorShardingParam='" + executorShardingParam + '\'' +
                ", executorFailRetryCount=" + executorFailRetryCount +
                ", executorFailTriggerTime=" + executorFailTriggerTime +
                ", triggerTime=" + triggerTime +
                ", triggerCode=" + triggerCode +
                ", triggerMsg='" + triggerMsg + '\'' +
                ", handleTime=" + handleTime +
                ", handleCode=" + handleCode +
                ", handleMsg='" + handleMsg + '\'' +
                ", childrenExecutorParams='" + childrenExecutorParams + '\'' +
                ", alarmStatus=" + alarmStatus +
                ", executorThresholdTimeout=" + executorThresholdTimeout +
                ", alarmThresholdStatus=" + alarmThresholdStatus +
                ", logType=" + logType +
                ", triggerType=" + triggerType +
                ", hasSub=" + hasSub +
                ", parentLog=" + parentLog +
                ", executeTime=" + executeTime +
                ", instanceId='" + instanceId + '\'' +
                '}';
    }
}
