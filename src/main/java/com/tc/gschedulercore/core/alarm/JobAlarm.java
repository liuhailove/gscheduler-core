package com.tc.gschedulercore.core.alarm;


import com.tc.gschedulercore.core.model.*;

import java.util.List;
import java.util.Map;

/**
 * 告警接口
 *
 * @author honggang.liu
 */
public interface JobAlarm {

    /**
     * job alarm
     *
     * @param info
     * @param jobLog
     * @return
     */
    boolean doAlarm(JobInfo info, JobLog jobLog, String env);


    /**
     * job threshold alarm
     *
     * @param info   task信息
     * @param jobLog 执行日志
     * @return 成功返回true, 否则失败
     */
    boolean doThresholdAlarm(JobInfo info, JobLog jobLog, String env);

    boolean doJobExecTimesExceptionAlarm(JobInfo info, int jobGroupId, int jobExpectedTimes, int jobActualTimes, String env);


    /**
     * 执行器下线告警
     *
     * @param groupList 执行器列表
     * @return 执行器下线告警
     */
    boolean doGroupOfflineAlarm(List<JobGroup> groupList, String env);

    /**
     * 执行失败统计告警
     *
     * @param alarmList 告警数据
     * @return 执行失败统计告警
     */
    boolean doLogFailCountAlarmAlarm(List<Map<String, Object>> alarmList, String env);

    /**
     * 日志插入失败告警
     *
     * @param info   task信息
     * @param jobLog 执行日志
     * @return 成功返回true, 否则失败
     */
    boolean doSaveLogFailAlarm(JobInfo info, JobLog jobLog, String msg, String env);

    /**
     * 任务延迟运行告警
     *
     * @param info     job任务
     * @param delayLog 延迟消息
     * @param env      环境
     * @return 成功返回true, 否则失败
     */
    boolean doRunTaskDelayAlarm(JobInfo info, DelayLog delayLog, String env);

    /**
     * 告警规则触发告警
     *
     * @param notifyInfo 告警消息
     * @param env        环境
     * @return 成功返回真，否则返回假
     */
    boolean doNotifyAlarm(NotifyInfo notifyInfo, String env);

    /**
     * 补偿任务触发告警
     *
     * @param info   task信息
     * @param jobLog 执行日志
     * @return 成功返回true, 否则失败
     */
    boolean doCompensateAlarm(JobInfo info, JobLog jobLog, String msg, String env);
}
