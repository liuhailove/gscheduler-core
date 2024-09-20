package com.tc.gschedulercore.core.model;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * go-scheduler info
 *
 * @author honggang.liu  2016-1-12 18:25:49
 */
public class JobInfo implements Serializable {

    /**
     * 运行态
     */
    public static final int TRIGGER_STATUS_RUNNING = 1;

    /**
     * 停止态
     */
    public static final int TRIGGER_STATUS_STOPPED = 0;

    /**
     * 主键ID
     */
    private int id;

    /**
     * 执行器主键ID
     */
    private int jobGroup;

    public String getVoiceAlarmTels() {
        return voiceAlarmTels;
    }

    public void setVoiceAlarmTels(String voiceAlarmTels) {
        this.voiceAlarmTels = voiceAlarmTels;
    }

    private String voiceAlarmTels;

    public String getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(String additionalParams) {
        this.additionalParams = additionalParams;
    }

    private String additionalParams;
    /**
     * job名称
     */
    private String jobName;
    /**
     * 执行器名称
     */
    private String appName;
    private String jobDesc;

    private Date addTime;
    private Date updateTime;

    /**
     * 负责人
     */
    private String author;
    /**
     * 报警邮件
     */
    private String alarmEmail;

    /**
     * 调度类型
     */
    private String scheduleType;
    /**
     * 调度配置，值含义取决于调度类型
     */
    private String scheduleConf;

    /**
     * 调度过期策略
     */
    private String misfireStrategy;

    /**
     * 执行器路由策略
     */
    private String executorRouteStrategy;
    /**
     * 执行器，任务Handler名称
     */
    private String executorHandler;
    /**
     * 执行器，任务参数
     */
    private String executorParam;
    /**
     * 阻塞处理策略 @see ExecutorBlockStrategyEnum
     */
    private String executorBlockStrategy;
    /**
     * 任务执行超时时间，单位秒
     */
    private int executorTimeout;
    /**
     * 失败重试次数
     */
    private int executorFailRetryCount;
    /**
     * 执行阈值
     */
    private int executorThreshold;

    /**
     * 分片类型：0.按照执行器节点数分区 2.自定义分区数 @see com.xxl.job.core.enums.ShardingType
     */
    private int shardingType;

    /**
     * 自定义分区数量
     */
    private int shardingNum;

    /**
     * GLUE类型	#com.xxl.job.core.glue.GlueTypeEnum
     */
    private String glueType;
    /**
     * GLUE源代码
     */
    private String glueSource;
    /**
     * GLUE备注
     */
    private String glueRemark;
    /**
     * GLUE更新时间
     */
    private Date glueUpdatetime;

    /**
     * 子任务ID，多个逗号分隔
     */
    private String childJobId;

    /**
     * 调度状态：0-停止，1-运行
     */
    private int triggerStatus;
    /**
     * 上次调度时间
     */
    private long triggerLastTime;
    /**
     * 下次调度时间
     */
    private long triggerNextTime;

    /**
     * 下次调度时间
     */
    private Date triggerNextDateTime;

    /**
     * alarm告警通知
     */
    private String alarmSeatalk;

    /**
     * 重试类型 @see com.xxl.job.core.enums.RetryType
     */
    private int retryType;

    /**
     * 重试配置
     */
    private String retryConf;

    /**
     * 从父任务继承参数
     */
    private boolean paramFromParent;

    /**
     * 是否需要结果回差
     */
    private boolean resultCheck;

    /**
     * 仅最终失败发送告警
     */
    private boolean finalFailedSendAlarm;

    /**
     * 变更人
     */
    private String updateBy;

    /**
     * 是否在父任务完成后开始
     */
    private boolean beginAfterParent;

    /**
     * 父任务ID，多个逗号分隔
     */
    private String parentJobId;

    /**
     * 下发限流，仅在有子任务时生效
     */
    private double dispatchThreshold;

    /**
     * 日志保留时间，如果为0，则和系统时间保持一致，否则按照配置时间清理，设置为1-90的整数
     */
    private int logRetentionDays;

    /**
     * 业务执行容忍阈值
     */
    private int startExecutorToleranceThresholdInMin;

    /**
     * 告警静默期，单位分钟
     */
    private int alarmSilence;

    /**
     * 告警静默期到，在此之前告警会被忽略，初始值为0，时间戳
     */
    private long alarmSilenceTo;

    /**
     * 路由标签，标签可以控制数据路由的IP地址，如果任何IP对应的标签都和设置的标签不匹配，则退化为无标签版本
     * routerFlag不可以包含逗号，api上报中的逗号(,)也会被自动替换为下划线(_)
     */
    private String routerFlag;

    /**
     * 任务运行中告警开关
     */
    private Boolean taskRunningAlarm;

    /**
     * 是否延迟
     */
    private boolean isDelay;

    /**
     * 延迟时间
     */
    private long delayInMs;

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

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
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

    public Date getGlueUpdatetime() {
        return glueUpdatetime;
    }

    public void setGlueUpdatetime(Date glueUpdatetime) {
        this.glueUpdatetime = glueUpdatetime;
    }

    public String getChildJobId() {
        return childJobId;
    }

    public void setChildJobId(String childJobId) {
        this.childJobId = childJobId;
    }

    public int getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(int triggerStatus) {
        this.triggerStatus = triggerStatus;
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

    public String getAlarmSeatalk() {
        return alarmSeatalk;
    }

    public void setAlarmSeatalk(String alarmSeatalk) {
        this.alarmSeatalk = alarmSeatalk;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
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

    public boolean getParamFromParent() {
        return paramFromParent;
    }

    public void setParamFromParent(boolean paramFromParent) {
        this.paramFromParent = paramFromParent;
    }

    public boolean isParamFromParent() {
        return paramFromParent;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
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

    public List<Integer> getParents() {
        if (StringUtils.isEmpty(this.parentJobId)) {
            return new ArrayList<>(0);
        }
        return Arrays.stream(StringUtils.split(this.parentJobId, ",")).map(Integer::parseInt).collect(Collectors.toList());
    }

    public List<Integer> getChildren() {
        if (StringUtils.isEmpty(this.childJobId)) {
            return new ArrayList<>(0);
        }
        return Arrays.stream(StringUtils.split(this.childJobId, ",")).map(Integer::parseInt).collect(Collectors.toList());
    }

    public double getDispatchThreshold() {
        return dispatchThreshold;
    }

    public void setDispatchThreshold(double dispatchThreshold) {
        this.dispatchThreshold = dispatchThreshold;
    }

    public int getLogRetentionDays() {
        return logRetentionDays;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    public int getStartExecutorToleranceThresholdInMin() {
        return startExecutorToleranceThresholdInMin;
    }

    public void setStartExecutorToleranceThresholdInMin(int startExecutorToleranceThresholdInMin) {
        this.startExecutorToleranceThresholdInMin = startExecutorToleranceThresholdInMin;
    }

    public int getAlarmSilence() {
        return alarmSilence;
    }

    public void setAlarmSilence(int alarmSilence) {
        this.alarmSilence = alarmSilence;
    }

    public long getAlarmSilenceTo() {
        return alarmSilenceTo;
    }

    public void setAlarmSilenceTo(long alarmSilenceTo) {
        this.alarmSilenceTo = alarmSilenceTo;
    }

    public Date getTriggerNextDateTime() {
        return triggerNextDateTime;
    }

    public void setTriggerNextDateTime(Date triggerNextDateTime) {
        this.triggerNextDateTime = triggerNextDateTime;
    }

    public String getRouterFlag() {
        return routerFlag;
    }

    public void setRouterFlag(String routerFlag) {
        this.routerFlag = routerFlag;
    }

    public Boolean getTaskRunningAlarm() {
        return taskRunningAlarm;
    }

    public void setTaskRunningAlarm(Boolean taskRunningAlarm) {
        this.taskRunningAlarm = taskRunningAlarm;
    }

    public boolean isDelay() {
        return isDelay;
    }

    public void setDelay(boolean delay) {
        isDelay = delay;
    }

    public long getDelayInMs() {
        return delayInMs;
    }

    public void setDelayInMs(long delayInMs) {
        this.delayInMs = delayInMs;
    }

    @Override
    public String toString() {
        return "JobInfo{" +
                "id=" + id +
                ", jobGroup=" + jobGroup +
                ", jobName='" + jobName + '\'' +
                ", appName='" + appName + '\'' +
                ", jobDesc='" + jobDesc + '\'' +
                ", addTime=" + addTime +
                ", updateTime=" + updateTime +
                ", author='" + author + '\'' +
                ", alarmEmail='" + alarmEmail + '\'' +
                ", scheduleType='" + scheduleType + '\'' +
                ", scheduleConf='" + scheduleConf + '\'' +
                ", misfireStrategy='" + misfireStrategy + '\'' +
                ", executorRouteStrategy='" + executorRouteStrategy + '\'' +
                ", executorHandler='" + executorHandler + '\'' +
                ", executorParam='" + executorParam + '\'' +
                ", executorBlockStrategy='" + executorBlockStrategy + '\'' +
                ", executorTimeout=" + executorTimeout +
                ", executorFailRetryCount=" + executorFailRetryCount +
                ", executorThreshold=" + executorThreshold +
                ", shardingType=" + shardingType +
                ", shardingNum=" + shardingNum +
                ", glueType='" + glueType + '\'' +
                ", glueSource='" + glueSource + '\'' +
                ", glueRemark='" + glueRemark + '\'' +
                ", glueUpdatetime=" + glueUpdatetime +
                ", childJobId='" + childJobId + '\'' +
                ", triggerStatus=" + triggerStatus +
                ", triggerLastTime=" + triggerLastTime +
                ", triggerNextTime=" + triggerNextTime +
                ", triggerNextDateTime=" + triggerNextDateTime +
                ", alarmSeatalk='" + alarmSeatalk + '\'' +
                ", retryType=" + retryType +
                ", retryConf='" + retryConf + '\'' +
                ", paramFromParent=" + paramFromParent +
                ", resultCheck=" + resultCheck +
                ", finalFailedSendAlarm=" + finalFailedSendAlarm +
                ", updateBy='" + updateBy + '\'' +
                ", beginAfterParent=" + beginAfterParent +
                ", parentJobId='" + parentJobId + '\'' +
                ", dispatchThreshold=" + dispatchThreshold +
                ", logRetentionDays=" + logRetentionDays +
                ", startExecutorToleranceThresholdInMin=" + startExecutorToleranceThresholdInMin +
                ", alarmSilence=" + alarmSilence +
                ", alarmSilenceTo=" + alarmSilenceTo +
                ", routerFlag='" + routerFlag + '\'' +
                '}';
    }
}
