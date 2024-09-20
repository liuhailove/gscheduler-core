package com.tc.gschedulercore.core.dto;

import java.io.Serializable;

/**
 * 触发参数
 *
 * @author honggang.liu
 */
public class TriggerParam implements Serializable {
    private static final long serialVersionUID = 42L;

    private int jobId;
    /**
     * 执行器名称
     */
    private String appname;
    private String executorHandler;
    private String executorParams;
    private String executorBlockStrategy;
    private int executorTimeout;

    private long logId;
    /**
     * 父log
     */
    private long parentLog;

    /**
     * 任务一次运行的实例ID
     */
    private String instanceId;

    private long logDateTime;

    private String glueType;
    private String glueSource;

    public String getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(String additionalParams) {
        this.additionalParams = additionalParams;
    }

    private String additionalParams;
    private long glueUpdatetime;

    private int broadcastIndex;
    private int broadcastTotal;

    /**
     * 业务开始执行时间容忍阈值
     */
    private int businessStartExecutorToleranceThresholdInMin;

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParams() {
        return executorParams;
    }

    public void setExecutorParams(String executorParams) {
        this.executorParams = executorParams;
    }

    public String getExecutorBlockStrategy() {
        return executorBlockStrategy;
    }

    public void setExecutorBlockStrategy(String executorBlockStrategy) {
        this.executorBlockStrategy = executorBlockStrategy;
    }

    public int getExecutorTimeout() {
        return executorTimeout;
    }

    public void setExecutorTimeout(int executorTimeout) {
        this.executorTimeout = executorTimeout;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public long getLogDateTime() {
        return logDateTime;
    }

    public void setLogDateTime(long logDateTime) {
        this.logDateTime = logDateTime;
    }

    public String getGlueType() {
        return glueType;
    }

    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }

    public String getGlueSource() {
        return glueSource;
    }

    public void setGlueSource(String glueSource) {
        this.glueSource = glueSource;
    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    public void setGlueUpdatetime(long glueUpdatetime) {
        this.glueUpdatetime = glueUpdatetime;
    }

    public int getBroadcastIndex() {
        return broadcastIndex;
    }

    public void setBroadcastIndex(int broadcastIndex) {
        this.broadcastIndex = broadcastIndex;
    }

    public int getBroadcastTotal() {
        return broadcastTotal;
    }

    public void setBroadcastTotal(int broadcastTotal) {
        this.broadcastTotal = broadcastTotal;
    }

    public long getParentLog() {
        return parentLog;
    }

    public void setParentLog(long parentLog) {
        this.parentLog = parentLog;
    }

    public int getBusinessStartExecutorToleranceThresholdInMin() {
        return businessStartExecutorToleranceThresholdInMin;
    }

    public void setBusinessStartExecutorToleranceThresholdInMin(int businessStartExecutorToleranceThresholdInMin) {
        this.businessStartExecutorToleranceThresholdInMin = businessStartExecutorToleranceThresholdInMin;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String toString() {
        return "TriggerParam{" +
                "jobId=" + jobId +
                ", appname='" + appname + '\'' +
                ", executorHandler='" + executorHandler + '\'' +
                ", executorParams='" + executorParams + '\'' +
                ", executorBlockStrategy='" + executorBlockStrategy + '\'' +
                ", executorTimeout=" + executorTimeout +
                ", logId=" + logId +
                ", parentLog=" + parentLog +
                ", logDateTime=" + logDateTime +
                ", glueType='" + glueType + '\'' +
                ", glueSource='" + glueSource + '\'' +
                ", glueUpdatetime=" + glueUpdatetime +
                ", broadcastIndex=" + broadcastIndex +
                ", broadcastTotal=" + broadcastTotal +
                ", additionalParams=" + additionalParams +
                ", businessStartExecutorToleranceThresholdInMin=" + businessStartExecutorToleranceThresholdInMin +
                '}';
    }
}
