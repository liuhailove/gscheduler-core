package com.tc.gschedulercore.core.alarm.impl;

import com.tc.gschedulercore.core.alarm.JobAlarm;
import com.tc.gschedulercore.core.alarm.JobAlarmer;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.TelParam;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.core.util.TelUtils;
import com.tc.gschedulercore.util.JobRemotingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 电话告警
 *
 * @author honggang.liu
 */
@Component
public class TelJobAlarm implements JobAlarm {

    /**
     * LOGGER
     */
    private static Logger LOGGER = LoggerFactory.getLogger(JobAlarmer.class.getSimpleName());

    /**
     * 应用context
     */
    private static final String PRE_TEL = "86";

    @Value("${spring.profiles.spx.taskid}")
    private Integer taskid;

    @Value("${spring.profiles.spx.url}")
    private String spxUrl;

    @Value("${spring.profiles.spx.sdu}")
    private String sdu;

    @Value("${spring.profiles.spx.servicekey}")
    private String servicekey;

    /**
     * job alarm
     *
     * @param info   job信息
     * @param jobLog job日志
     * @param env    环境信息
     * @return 告警结果
     */
    @Override
    public boolean doAlarm(JobInfo info, JobLog jobLog, String env) {
        String tels = info.getVoiceAlarmTels();
        if (!StringUtils.hasLength(tels)) {
            LOGGER.info("Voice Alarm tel is null, jobName={},jobLogId={},tel={}", info.getJobName(), jobLog.getId(), tels);
            return true;
        }
        // 触发电话告警，如果任务信息正确，并且任务配置了电话告警，每一个电话都合法
        String[] phoneNumbers = tels.split(",");
        for (String phoneNumber : phoneNumbers) {
            if (!TelUtils.isValidPhoneNumber(phoneNumber)) {
                LOGGER.error("Voice Alarm not invalid phoneNumber: {}", phoneNumber);
                continue;
            }
            LOGGER.info("Voice Alarm begin: jobName:{},tel:{}", info.getJobName(), tels);
            phoneNumber = PRE_TEL + phoneNumber;
            //拼接请求体
            TelParam telParam = TelParam.buildTelParam(taskid, phoneNumber, env, info.getJobName());
            Map<String, String> additionalProperties = new HashMap<>(2);
            additionalProperties.put("x-sp-sdu", sdu);
            additionalProperties.put("x-sp-servicekey", servicekey);
            ReturnT<String> result = JobRemotingUtil.postBody(spxUrl, null, JobAdminConfig.getAdminConfig().getProxyAddr(), 30, telParam, String.class, additionalProperties);
            //打印响应
            LOGGER.debug("Voice Alarm Response={}, jobName={},logId={},tels={}", result, info.getJobName(), jobLog.getId(), tels);
        }
        return true;
    }

    /**
     * job threshold alarm
     *
     * @param info   task信息
     * @param jobLog 执行日志
     * @param env
     * @return 成功返回true, 否则失败
     */
    @Override
    public boolean doThresholdAlarm(JobInfo info, JobLog jobLog, String env) {
        return true;
    }

    @Override
    public boolean doJobExecTimesExceptionAlarm(JobInfo info, int jobGroupId, int jobExpectedTimes, int jobActualTimes, String env) {
        return true;
    }

    /**
     * 执行器下线告警
     *
     * @param groupList 执行器列表
     * @param env
     * @return 执行器下线告警
     */
    @Override
    public boolean doGroupOfflineAlarm(List<JobGroup> groupList, String env) {
        return true;
    }

    /**
     * 执行失败统计告警
     *
     * @param alarmList 告警数据
     * @param env
     * @return 执行失败统计告警
     */
    @Override
    public boolean doLogFailCountAlarmAlarm(List<Map<String, Object>> alarmList, String env) {
        return true;
    }

    /**
     * 日志插入失败告警
     *
     * @param info   task信息
     * @param jobLog 执行日志
     * @param msg
     * @param env
     * @return 成功返回true, 否则失败
     */
    @Override
    public boolean doSaveLogFailAlarm(JobInfo info, JobLog jobLog, String msg, String env) {
        return true;
    }

    /**
     * 任务延迟运行告警
     *
     * @param info     job任务
     * @param delayLog 延迟消息
     * @param env      环境
     * @return 成功返回true, 否则失败
     */
    @Override
    public boolean doRunTaskDelayAlarm(JobInfo info, DelayLog delayLog, String env) {
        return true;
    }

    /**
     * 告警规则触发告警
     *
     * @param notifyInfo 告警消息
     * @param env        环境
     * @return 成功返回真，否则返回假
     */
    @Override
    public boolean doNotifyAlarm(NotifyInfo notifyInfo, String env) {
        if (!AlarmRule.SEVERITY_LEVEL.equals(notifyInfo.getAlarmLevel())) {
            return true;
        }
        AlarmRule alarmRule = JobAdminConfig.getAdminConfig().getAlarmRuleDao().load(notifyInfo.getAlarmRuleId());
        if (alarmRule == null) {
            return true;
        }
        String tels = alarmRule.getVoiceAlarmTels();
        if (!StringUtils.hasLength(tels)) {
            LOGGER.info("[doNotifyAlarm] Voice Alarm tel is null,alarmName={},notifyId={},tels={}", notifyInfo.getAlarmName(), notifyInfo.getId(), tels);
            return true;
        }
        // 触发电话告警，如果任务信息正确，并且任务配置了电话告警，每一个电话都合法
        String[] phoneNumbers = tels.split(",");
        for (String phoneNumber : phoneNumbers) {
            if (!TelUtils.isValidPhoneNumber(phoneNumber)) {
                LOGGER.error("[doNotifyAlarm] Voice Alarm not invalid phoneNumber: {}", phoneNumber);
                continue;
            }
            phoneNumber = PRE_TEL + phoneNumber;
            //拼接请求体
            TelParam telParam = TelParam.buildTelParam(taskid, phoneNumber, env, notifyInfo.getAlarmName());
            Map<String, String> additionalProperties = new HashMap<>(2);
            additionalProperties.put("x-sp-sdu", sdu);
            additionalProperties.put("x-sp-servicekey", servicekey);
            ReturnT result = JobRemotingUtil.postBody(spxUrl, null, JobAdminConfig.getAdminConfig().getProxyAddr(), 30, telParam, String.class, additionalProperties);
            //打印响应
            LOGGER.debug("Voice Alarm Response={}, alarmName={},notifyId={},tels={}", result, notifyInfo.getAlarmName(), notifyInfo.getId(), tels);
        }
        return true;
    }

    /**
     * 补偿任务触发告警
     *
     * @param info   task信息
     * @param jobLog 执行日志
     * @param msg
     * @param env
     * @return 成功返回true, 否则失败
     */
    @Override
    public boolean doCompensateAlarm(JobInfo info, JobLog jobLog, String msg, String env) {
        return true;
    }


}
