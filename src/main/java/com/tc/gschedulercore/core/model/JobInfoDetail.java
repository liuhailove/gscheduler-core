package com.tc.gschedulercore.core.model;

import cn.afterturn.easypoi.excel.annotation.Excel;

import java.io.Serializable;

public class JobInfoDetail implements Serializable {

    /**
     * 应用名称
     */
    @Excel(name = "应用名称", width = 15)
    private String appname;

    /**
     * title
     */
    @Excel(name = "名称", width = 15)
    private String title;

    /**
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    @Excel(name = "执行器地址类型(0=自动注册、1=手动录入)", width = 20)
    private int addressType;

    /**
     * alarm告警通知
     */
    @Excel(name = "告警Seatalk", width = 20)
    private String alarmSeatalk;

    /**
     * job id
     */
    @Excel(name = "jobId", width = 20)
    private int jobId;

    /**
     * job name
     */
    @Excel(name = "jobName", width = 20)
    private String jobName;

    /**
     * job描述
     */
    @Excel(name = "job描述", width = 20)
    private String jobDesc;

    /**
     * 负责人
     */
    @Excel(name = "负责人", width = 15)
    private String author;

    /**
     * 报警邮件
     */
    @Excel(name = "报警邮件", width = 15)
    private String alarmEmail;

    /**
     * 调度类型
     */
    @Excel(name = "调度类型", width = 15)
    private String scheduleType;

    /**
     * 调度配置，值含义取决于调度类型
     */
    @Excel(name = "调度配置", width = 15)
    private String scheduleConf;

    /**
     * 调度过期策略
     */
    @Excel(name = "调度过期策略", width = 15)
    private String misfireStrategy;

    /**
     * 执行器路由策略
     */
    @Excel(name = "执行器路由策略", width = 15)
    private String executorRouteStrategy;

    /**
     * 执行器，任务Handler名称
     */
    @Excel(name = "任务Handler名称", width = 15)
    private String executorHandler;

    /**
     * 执行器，任务参数
     */
    @Excel(name = "任务参数", width = 15)
    private String executorParam;
    /**
     * 阻塞处理策略
     */
    @Excel(name = "阻塞处理策略", width = 15)
    private String executorBlockStrategy;

    /**
     * 任务执行超时时间，单位秒
     */
    @Excel(name = "任务执行超时时间", width = 15)
    private int executorTimeout;

    /**
     * 失败重试次数
     */
    @Excel(name = "失败重试次数", width = 15)
    private int executorFailRetryCount;

    /**
     * 执行阈值
     */
    @Excel(name = "执行阈值", width = 15)
    private int executorThreshold;

    /**
     * 分片类型：0.按照执行器节点数分区 2.自定义分区数 @see com.xxl.job.core.enums.ShardingType
     */
    @Excel(name = "分片类型", width = 15)
    private int shardingType;

    /**
     * 自定义分区数量
     */
    @Excel(name = "自定义分区数量", width = 15)
    private int shardingNum;

    /**
     * GLUE类型	#com.xxl.job.core.glue.GlueTypeEnum
     */
    @Excel(name = "GLUE类型", width = 15)
    private String glueType;

    /**
     * GLUE源代码
     */
    @Excel(name = "GLUE源代码", width = 15)
    private String glueSource;
    /**
     * GLUE备注
     */
    @Excel(name = "GLUE备注", width = 15)
    private String glueRemark;

    /**
     * 子任务ID，多个逗号分隔
     */
    @Excel(name = "子任务ID", width = 15)
    private String childJobId;

    /**
     * 重试类型 @see com.xxl.job.core.enums.RetryType
     */
    @Excel(name = "重试类型", width = 15)
    private int retryType;

    /**
     * 重试配置
     */
    @Excel(name = "重试配置", width = 15)
    private String retryConf;

    /**
     * 从父任务继承参数
     */
    @Excel(name = "从父任务继承参数", width = 15)
    private boolean paramFromParent;

    /**
     * 是否需要结果回查
     */
    @Excel(name = "需要结果回查", width = 15)
    private boolean resultCheck;

    /**
     * 仅最终失败发送告警
     */
    @Excel(name = "仅最终失败发送告警", width = 15)
    private boolean finalFailedSendAlarm;

    /**
     * 是否在父任务完成后开始
     */
    @Excel(name = "父任务完成后开始", width = 15)
    private boolean beginAfterParent;

    /**
     * 父任务ID，多个逗号分隔
     */
    @Excel(name = "父任务ID", width = 15)
    private String parentJobId;

    @Excel(name = "启动状态", width = 15)
    private int triggerStatus;

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getAddressType() {
        return addressType;
    }

    public void setAddressType(int addressType) {
        this.addressType = addressType;
    }

    public String getAlarmSeatalk() {
        return alarmSeatalk;
    }

    public void setAlarmSeatalk(String alarmSeatalk) {
        this.alarmSeatalk = alarmSeatalk;
    }


    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAlarmEmail() {
        return alarmEmail;
    }

    public void setAlarmEmail(String alarmEmail) {
        this.alarmEmail = alarmEmail;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getScheduleConf() {
        return scheduleConf;
    }

    public void setScheduleConf(String scheduleConf) {
        this.scheduleConf = scheduleConf;
    }

    public String getMisfireStrategy() {
        return misfireStrategy;
    }

    public void setMisfireStrategy(String misfireStrategy) {
        this.misfireStrategy = misfireStrategy;
    }

    public String getExecutorRouteStrategy() {
        return executorRouteStrategy;
    }

    public void setExecutorRouteStrategy(String executorRouteStrategy) {
        this.executorRouteStrategy = executorRouteStrategy;
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

    public int getExecutorFailRetryCount() {
        return executorFailRetryCount;
    }

    public void setExecutorFailRetryCount(int executorFailRetryCount) {
        this.executorFailRetryCount = executorFailRetryCount;
    }

    public String getGlueType() {
        return glueType;
    }

    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }

    public String getChildJobId() {
        return childJobId;
    }

    public void setChildJobId(String childJobId) {
        this.childJobId = childJobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public int getExecutorThreshold() {
        return executorThreshold;
    }

    public void setExecutorThreshold(int executorThreshold) {
        this.executorThreshold = executorThreshold;
    }

    public int getShardingType() {
        return shardingType;
    }

    public void setShardingType(int shardingType) {
        this.shardingType = shardingType;
    }

    public int getShardingNum() {
        return shardingNum;
    }

    public void setShardingNum(int shardingNum) {
        this.shardingNum = shardingNum;
    }

    public String getGlueSource() {
        return glueSource;
    }

    public void setGlueSource(String glueSource) {
        this.glueSource = glueSource;
    }

    public String getGlueRemark() {
        return glueRemark;
    }

    public void setGlueRemark(String glueRemark) {
        this.glueRemark = glueRemark;
    }

    public int getRetryType() {
        return retryType;
    }

    public void setRetryType(int retryType) {
        this.retryType = retryType;
    }

    public String getRetryConf() {
        return retryConf;
    }

    public void setRetryConf(String retryConf) {
        this.retryConf = retryConf;
    }

    public boolean isParamFromParent() {
        return paramFromParent;
    }

    public void setParamFromParent(boolean paramFromParent) {
        this.paramFromParent = paramFromParent;
    }

    public boolean isResultCheck() {
        return resultCheck;
    }

    public void setResultCheck(boolean resultCheck) {
        this.resultCheck = resultCheck;
    }

    public boolean isFinalFailedSendAlarm() {
        return finalFailedSendAlarm;
    }

    public void setFinalFailedSendAlarm(boolean finalFailedSendAlarm) {
        this.finalFailedSendAlarm = finalFailedSendAlarm;
    }

    public boolean isBeginAfterParent() {
        return beginAfterParent;
    }

    public void setBeginAfterParent(boolean beginAfterParent) {
        this.beginAfterParent = beginAfterParent;
    }

    public String getParentJobId() {
        return parentJobId;
    }

    public void setParentJobId(String parentJobId) {
        this.parentJobId = parentJobId;
    }

    public int getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(int triggerStatus) {
        this.triggerStatus = triggerStatus;
    }
}
