package com.tc.gschedulercore.core.alarm.impl;

import com.google.gson.annotations.SerializedName;
import com.tc.gschedulercore.core.alarm.JobAlarm;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.DoDParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.SeatalkParam;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.util.JobRemotingUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.*;

/**
 * seatalk报警
 *
 * @author honggang.liu
 */
@Component
public class SeatalkJobAlarm implements JobAlarm {
    private static final Logger logger = LoggerFactory.getLogger(SeatalkJobAlarm.class.getSimpleName());

    private static final String dodSpexSdu = "fastescrow.xxljob.br.live.master.default";
    private static final String dodSpexKey = "dc72f4feecac77999a394377a77c58d9";
    private static final String dodSpexUrl = "https://http-gateway.spex.shopee.sg/sprpc/platform.dod.api.get_dod";
    private static final int alertNotificationWayNo = 0;
    private static final int alertNotificationWayMain = 1;
    private static final int alertNotificationWayBackUp = 2;

    /**
     * 加载发送业务报表的模板
     *
     * @return 模板
     */
    private static String loadSeatalkGroupOfflineTemplate(int trLen, String env) {
        String head = I18nUtil.getString("job_group_offline_alarm") + "-"
                + env + "\n"
                + I18nUtil.getString("jobinfo_field_jobgroup") + "\n";
        StringBuilder body = new StringBuilder();
        if (trLen > 0) {
            for (int i = 0; i < trLen; i++) {
                body.append("{").append(i).append("}\n");
            }
        }
        return head + body;
    }

    /**
     * 加载执行日志错误报表
     *
     * @return 模板
     */
    private static final String loadSeatalkLogFailTemplate(int trLen, String env) {
        String head = I18nUtil.getString("job_log_fail_count_alarm") + "-" + env + "\n"
                + I18nUtil.getString("jobgroup_name_2") + "\t"
                + I18nUtil.getString("jobinfo_field_jobname") + "\t"
                + I18nUtil.getString("job_id") + "\t"
                + I18nUtil.getString("job_dashboard_trigger_fail_num") + "\t\n";
        StringBuilder body = new StringBuilder();
        if (trLen > 0) {
            for (int i = 0; i < trLen; i++) {
                body.append("{").append(i).append("}\t");
                body.append("{").append(i + 1).append("}\t");
                body.append("{").append(i + 2).append("}\t");
                body.append("{").append(i + 3).append("}\t\n");
            }
        }
        return head + body;
    }

    @Override
    public boolean doAlarm(JobInfo info, JobLog jobLog, String env) {
        boolean alarmResult = true;
        // send monitor seatlk
        if (info != null && info.getAlarmSeatalk() != null && !info.getAlarmSeatalk().isEmpty()) {
            // alarmContent
            String alarmContent = "Alarm Job LogId=" + jobLog.getId();
            if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "\nTriggerMsg=\n" + jobLog.getTriggerMsg();
            }
            if (jobLog.getHandleCode() > 0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "\nHandleCode=" + jobLog.getHandleMsg();
            }

            // email info
            JobGroup group = JobAdminConfig.getAdminConfig().getJobGroupDao().load(info.getJobGroup());
            String content = MessageFormat.format(loadSeatalkJobAlarmTemplate(env),
                    group != null ? group.getTitle() : "null",
                    info.getId(),
                    info.getJobDesc(),
                    I18nUtil.getString("jobconf_monitor_alarm_title"),
                    alarmContent,
                    I18nUtil.getString("jobconf_monitor_alarm_type"));
            content = content.replaceAll("<br>", "\n");
            content = content.replaceAll("<span style=\"color:#00c0ef;\" >", "");
            content = content.replaceAll("</span>", "");
            SeatalkParam seatalkParam = new SeatalkParam();
            SeatalkParam.Text text = new SeatalkParam.Text();
            text.setContent(content.length() <= 1200 ? content : content.substring(0, 1200));
            text.setAtAll(false);
            seatalkParam.setText(text);
            // send seatalk
            try {
                DoD dod = GetDoDInfo(group.getAlertNotificationWay(), group.getDodTeamId());
                //如果用户传的teamId有误，或者时间不对，dod 就是null
                if (dod != null && dod.getEmail() != null
                        && group.getAlertNotificationWay() != alertNotificationWayNo) {
                    text.setMentionedEmailList(Collections.singletonList(dod.getEmail()));
                    seatalkParam.setText(text);
                }

                JobRemotingUtil.postBody(info.getAlarmSeatalk(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
            } catch (Exception e) {
                logger.error(">>job fail alarm email send error, JobLogId:{}", jobLog.getId(), e);
                alarmResult = false;
            }

        }
        return alarmResult;
    }

    public DoD GetDoDInfo(int DodType, int teamId) {
        long unixTimestamp = System.currentTimeMillis() / 1000;
        Map<String, String> additionalProperties = new HashMap<>(2);
        additionalProperties.put("x-sp-sdu", dodSpexSdu);
        additionalProperties.put("x-sp-servicekey", dodSpexKey);
        DoDParam dodParam = DoDParam.buildDoDParam(DodType, teamId, unixTimestamp);
        ReturnT<DoD> result = JobRemotingUtil.postBody(dodSpexUrl, null, JobAdminConfig.getAdminConfig().getProxyAddr(), 30, dodParam, DoD.class, additionalProperties);
        //am 要求
        // 1、如果没有配置备用dod，但还是选择了备用dod策略，就通知主dod。
        // 2、如果配置了备用dod，备用dod 邮箱不可能为空，如果为空就无法保存，所以不需要判断邮箱
        if ((result.getDod() == null && DodType == alertNotificationWayBackUp)) {
            DoDParam dodParamMain = DoDParam.buildDoDParam(alertNotificationWayMain, teamId, unixTimestamp);
            ReturnT<DoD> resultMain = JobRemotingUtil.postBody(dodSpexUrl, null, JobAdminConfig.getAdminConfig().getProxyAddr(), 30, dodParamMain, DoD.class, additionalProperties);
            return resultMain.getDod();
        }
        return result.getDod();
    }

    /**
     * load email job alarm template
     *
     * @return
     */
    private String loadSeatalkJobAlarmTemplate(String env) {
        return I18nUtil.getString("jobconf_monitor_detail") + "-" + env + "\n" +
                I18nUtil.getString("jobinfo_field_jobgroup") + "：" + "{0}" + "\t" +
                I18nUtil.getString("jobinfo_field_id") + "：" + "{1}" + "\t" +
                I18nUtil.getString("jobinfo_field_jobdesc") + "：" + "{2}" + "\t" +
                I18nUtil.getString("jobconf_monitor_alarm_title") + "：" + "{3}" + "\t" +
                I18nUtil.getString("jobconf_monitor_alarm_content") + "：" + "{4}" + "\t\n" +
                I18nUtil.getString("jobconf_monitor_alarm_type") + "{5}" + "\n";
    }

    @Override
    public boolean doThresholdAlarm(JobInfo info, JobLog jobLog, String env) {
        boolean alarmResult = true;
        // send monitor seatlk
        if (info != null && !StringUtils.isEmpty(info.getAlarmSeatalk())) {
            JobGroup group = JobAdminConfig.getAdminConfig().getJobGroupDao().load(info.getJobGroup());
            String content = MessageFormat.format(loadSeatalkJobThresholdAlarmTemplate(env),
                    group != null ? group.getTitle() : "null",
                    info.getId(),
                    info.getJobName(),
                    jobLog.getId(),
                    jobLog.getTriggerTime(),
                    (System.currentTimeMillis() - jobLog.getTriggerTime().getTime()) / 1000);
            SeatalkParam seatalkParam = new SeatalkParam();
            SeatalkParam.Text text = new SeatalkParam.Text();
            text.setContent(content.length() <= 1200 ? content : content.substring(0, 1200));
            text.setAtAll(false);
            seatalkParam.setText(text);
            // send seatalk
            try {
                JobRemotingUtil.postBody(info.getAlarmSeatalk(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
            } catch (Exception e) {
                logger.error(">>job fail alarm email send error, JobLogId:{}", jobLog.getId(), e);

                alarmResult = false;
            }
        }
        return alarmResult;
    }

    @Override
    public boolean doJobExecTimesExceptionAlarm(JobInfo info, int jobGroupId, int jobExpectedTimes, int jobActualTimes, String env) {
        boolean alarmResult = true;
        // send monitor seatlk
        if (info != null && !StringUtils.isEmpty(info.getAlarmSeatalk())) {
            JobGroup group = JobAdminConfig.getAdminConfig().getJobGroupDao().load(info.getJobGroup());
            String content = MessageFormat.format(loadSeatalkJobExecTimesAlarmTemplate(env),
                    group != null ? group.getAppname() : "null",
                    info.getId(),
                    info.getJobName(),
                    I18nUtil.getString("jobconf_monitor_job_exec_times_alarm_type"),
                    jobExpectedTimes,
                    jobActualTimes);
            SeatalkParam seatalkParam = new SeatalkParam();
            SeatalkParam.Text text = new SeatalkParam.Text();
            text.setContent(content.length() <= 1200 ? content : content.substring(0, 1200));
            text.setAtAll(false);
            seatalkParam.setText(text);
            // send seatalk
            try {
                JobRemotingUtil.postBody(info.getAlarmSeatalk(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
            } catch (Exception e) {
                logger.error(">>job fail alarm email send error, jobGroupId:{}", jobGroupId, e);

                alarmResult = false;
            }
        }
        return alarmResult;
    }

    /**
     * load seatalk job alarm template
     *
     * @return 模板字符串
     */
    private String loadSeatalkJobThresholdAlarmTemplate(String env) {
        return I18nUtil.getString("jobconf_monitor_detail") + "-" + env + "\n" +
                I18nUtil.getString("jobinfo_field_jobgroup") + "\t" +
                I18nUtil.getString("jobinfo_field_id") + "\t" +
                I18nUtil.getString("jobinfo_field_jobname") + "\t" +
                I18nUtil.getString("joblog_field_id") + "\t" +
                I18nUtil.getString("jobconf_monitor_alarm_title") + "\t" +
                I18nUtil.getString("joblog_trigger_time") + "\t" +
                I18nUtil.getString("joblog_elased_time") + "\t\n" +
                "{0}\t{1}\t{2}\t" +
                "{3}\t" +
                I18nUtil.getString("jobconf_monitor_threshold_alarm_type") + "\t{4}\t{5}\t\n";
    }

    /**
     * load seatalk job alarm template
     *
     * @return 模板字符串
     */
    private String loadSeatalkJobExecTimesAlarmTemplate(String env) {
        return I18nUtil.getString("jobconf_monitor_detail") + "-" + env + "\n" +
                I18nUtil.getString("jobinfo_field_jobgroup") + "：" + "{0}" + "\n" +
                I18nUtil.getString("jobinfo_field_id") + "：" + "{1}" + "\n" +
                I18nUtil.getString("jobinfo_field_jobname") + "：" + "{2}" + "\n" +
                I18nUtil.getString("jobconf_monitor_alarm_title") + "：" + "{3}" + "\n" +
                I18nUtil.getString("job_expected_cnt") + "：" + "{4}" + "\n" +
                I18nUtil.getString("job_actual_cnt") + "：" + "{5}" + "\n";
    }

    /**
     * 执行器下线告警
     *
     * @param groupList 执行器列表
     * @return 执行器下线告警
     */
    @Override
    public boolean doGroupOfflineAlarm(List<JobGroup> groupList, String env) {
        boolean alarmResult = true;
        if (!CollectionUtils.isEmpty(groupList) && !StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getSystemAlarm())) {
            List<String> params = new ArrayList<>(groupList.size());
            for (JobGroup group : groupList) {
                params.add(group.getAppname());
            }
            String content = MessageFormat.format(loadSeatalkGroupOfflineTemplate(groupList.size(), env), params.toArray());
            SeatalkParam seatalkParam = new SeatalkParam();
            SeatalkParam.Text text = new SeatalkParam.Text();
            text.setContent(content.length() <= 1200 ? content : content.substring(0, 1200));
            text.setAtAll(false);
            seatalkParam.setText(text);
            // send seatalk
            try {
                JobRemotingUtil.postBody(JobAdminConfig.getAdminConfig().getSystemAlarm(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
            } catch (Exception e) {
                logger.error(">>group offline alarm  send error,", e);
                alarmResult = false;
            }
        }
        return alarmResult;
    }

    /**
     * 执行失败统计告警
     *
     * @param alarmList 告警数据
     * @return 执行失败统计告警
     */
    @Override
    public boolean doLogFailCountAlarmAlarm(List<Map<String, Object>> alarmList, String env) {
        boolean alarmResult = true;
        if (!CollectionUtils.isEmpty(alarmList) && !StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getSystemAlarm())) {
            List<Object> params = new ArrayList<>();
            for (Map<String, Object> data : alarmList) {
                params.add(data.get("appName"));
                params.add(data.get("jobName"));
                params.add(data.get("jobId"));
                params.add(data.get("triggerCountFail"));
            }
            String content = MessageFormat.format(loadSeatalkLogFailTemplate(alarmList.size(), env), params.toArray());
            SeatalkParam seatalkParam = new SeatalkParam();
            SeatalkParam.Text text = new SeatalkParam.Text();
            text.setContent(content.length() <= 1200 ? content : content.substring(0, 1200));
            text.setAtAll(false);
            seatalkParam.setText(text);
            // send seatalk
            try {
                JobRemotingUtil.postBody(JobAdminConfig.getAdminConfig().getSystemAlarm(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
            } catch (Exception e) {
                logger.error(">>log fail count alarm  send error,", e);
                alarmResult = false;
            }
        }
        return alarmResult;
    }

    /**
     * 日志插入失败告警
     *
     * @param info   task信息
     * @param jobLog 执行日志
     * @param env
     * @return 成功返回true, 否则失败
     */
    @Override
    public boolean doSaveLogFailAlarm(JobInfo info, JobLog jobLog, String msg, String env) {
        if (msg != null && msg.length() > 200) {
            msg = msg.substring(0, 200);
        }
        boolean alarmResult = true;
        if (info != null && (!StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getSystemAlarm()) || !StringUtils.isEmpty(info.getAlarmSeatalk()))) {
            JobGroup group = JobAdminConfig.getAdminConfig().getJobGroupDao().load(info.getJobGroup());
            String content = MessageFormat.format(loadSeatalkSaveLogFailedAlarmTemplate(env),
                    group != null ? group.getTitle() : "null",
                    info.getId(),
                    info.getJobName(),
                    jobLog.getId(),
                    jobLog.getTriggerTime(),
                    msg);
            SeatalkParam seatalkParam = new SeatalkParam();
            SeatalkParam.Text text = new SeatalkParam.Text();
            text.setContent(content.length() <= 1200 ? content : content.substring(0, 1200));
            text.setAtAll(false);
            seatalkParam.setText(text);
            // send seatalk
            try {
                if (!StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getSystemAlarm())) {
                    JobRemotingUtil.postBody(JobAdminConfig.getAdminConfig().getSystemAlarm(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
                }
                if (!StringUtils.isEmpty(info.getAlarmSeatalk())) {
                    JobRemotingUtil.postBody(info.getAlarmSeatalk(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
                }
            } catch (Exception e) {
                logger.error("job fail alarm email send error, JobId={}", info.getId(), e);

                alarmResult = false;
            }
        }
        return alarmResult;
    }

    /**
     * load seatalk job alarm template
     *
     * @return 模板字符串
     */
    private String loadSeatalkSaveLogFailedAlarmTemplate(String env) {
        return I18nUtil.getString("jobconf_monitor_detail") + "-" + env + "\n" +
                I18nUtil.getString("jobinfo_field_jobgroup") + "\t" +
                I18nUtil.getString("jobinfo_field_id") + "\t" +
                I18nUtil.getString("jobinfo_field_jobname") + "\t" +
                I18nUtil.getString("joblog_field_id") + "\t" +
                I18nUtil.getString("jobconf_monitor_alarm_title") + "\t" +
                I18nUtil.getString("joblog_trigger_time") + "\t" +
                I18nUtil.getString("jobconf_monitor_db_error") + "\t\n" +
                "{0}\t{1}\t{2}\t" +
                "{3}\t" +
                I18nUtil.getString("jobconf_monitor_db_alarm_type") + "\t{4}\t{5}\t\n";
    }

    /**
     * load seatalk job alarm template
     *
     * @return 模板字符串
     */
    private String loadSeatalkCompensateAlarmTemplate(String env) {
        return I18nUtil.getString("jobconf_monitor_detail") + "-" + env + "\n" +
                I18nUtil.getString("jobinfo_field_jobgroup") + "\t" +
                I18nUtil.getString("jobinfo_field_id") + "\t" +
                I18nUtil.getString("jobinfo_field_jobname") + "\t" +
                I18nUtil.getString("joblog_field_id") + "\t" +
                I18nUtil.getString("jobconf_monitor_alarm_title") + "\t" +
                I18nUtil.getString("joblog_trigger_time") + "\t" +
                I18nUtil.getString("jobconf_monitor_compensate") + "\t\n" +
                "{0}\t{1}\t{2}\t" +
                "{3}\t" +
                I18nUtil.getString("jobconf_monitor_compensate_type") + "\t{4}\t{5}\t\n";
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
        boolean alarmResult = true;
        if (info != null && (!StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getSystemAlarm()) || !StringUtils.isEmpty(info.getAlarmSeatalk()))) {
            JobGroup group = JobAdminConfig.getAdminConfig().getJobGroupDao().load(info.getJobGroup());
            String content = MessageFormat.format(loadSeatalkRunTaskDelayAlarmTemplate(env),
                    group != null ? group.getTitle() : "null",
                    info.getId(),
                    info.getJobName(),
                    delayLog.getJobLogId(),
                    delayLog.getLogDate(),
                    delayLog.getStartExecutorToleranceThresholdInMin(),
                    delayLog.getTimeElapseInMs());
            SeatalkParam seatalkParam = new SeatalkParam();
            SeatalkParam.Text text = new SeatalkParam.Text();
            text.setContent(content.length() <= 1200 ? content : content.substring(0, 1200));
            text.setAtAll(false);
            seatalkParam.setText(text);
            // send seatalk
            try {
                if (!StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getSystemAlarm())) {
                    JobRemotingUtil.postBody(JobAdminConfig.getAdminConfig().getSystemAlarm(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
                }
                if (!StringUtils.isEmpty(info.getAlarmSeatalk())) {
                    JobRemotingUtil.postBody(info.getAlarmSeatalk(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
                }
            } catch (Exception e) {
                logger.error(">>job fail alarm email send error, JobId={}", info.getId(), e);

                alarmResult = false;
            }
        }
        return alarmResult;
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
        boolean alarmResult = true;
        if (notifyInfo != null && !StringUtils.isEmpty(notifyInfo.getNotifyUrl())) {
            String content = MessageFormat.format(loadNotifyAlarmTemplate(env),
                    notifyInfo.getApp(),
                    AlarmRule.queryAlarmLevelDesc(notifyInfo.getAlarmLevel()),
                    AlarmItem.queryAlarmTypeDesc(notifyInfo.getAlarmType()),
                    notifyInfo.getAlarmName(),
                    notifyInfo.getId(),
                    notifyInfo.getNotifyContent());
            SeatalkParam seatalkParam = new SeatalkParam();
            SeatalkParam.Text text = new SeatalkParam.Text();
            text.setContent(content.length() <= 1200 ? content : content.substring(0, 1200));
            text.setAtAll(false);
            if (notifyInfo.getNotifyUsers() != null) {
                text.setMentionedEmailList(Arrays.asList(notifyInfo.getNotifyUsers().split(",")));
            }
            seatalkParam.setText(text);
            // send seatalk
            try {
                JobRemotingUtil.postBody(notifyInfo.getNotifyUrl(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
            } catch (Exception e) {
                logger.error("notify send error, event_id={}", notifyInfo.getId(), e);

                alarmResult = false;
            }
        }
        return alarmResult;
    }

    @Override
    public boolean doCompensateAlarm(JobInfo info, JobLog jobLog, String msg, String env) {
        if (msg != null && msg.length() > 200) {
            msg = msg.substring(0, 200);
        }
        boolean alarmResult = true;
        if (info != null && (!StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getSystemAlarm()) || !StringUtils.isEmpty(info.getAlarmSeatalk()))) {
            JobGroup group = JobAdminConfig.getAdminConfig().getJobGroupDao().load(info.getJobGroup());
            String content = MessageFormat.format(loadSeatalkCompensateAlarmTemplate(env),
                    group != null ? group.getTitle() : "null",
                    info.getId(),
                    info.getJobName(),
                    jobLog.getId(),
                    jobLog.getTriggerTime(),
                    msg);
            SeatalkParam seatalkParam = new SeatalkParam();
            SeatalkParam.Text text = new SeatalkParam.Text();
            text.setContent(content.length() <= 1200 ? content : content.substring(0, 1200));
            text.setAtAll(false);
            seatalkParam.setText(text);
            // send seatalk
            try {
                if (!StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getSystemAlarm())) {
                    JobRemotingUtil.postBody(JobAdminConfig.getAdminConfig().getSystemAlarm(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
                }
                if (!StringUtils.isEmpty(info.getAlarmSeatalk())) {
                    JobRemotingUtil.postBody(info.getAlarmSeatalk(), "", JobAdminConfig.getAdminConfig().getProxyAddr(), 3, seatalkParam, String.class);
                }
            } catch (Exception e) {
                logger.error("job fail alarm email send error, JobId={}", info.getId(), e);
                alarmResult = false;
            }
        }
        return alarmResult;
    }

    /**
     * load seatalk job alarm template
     *
     * @return 模板字符串
     */
    private String loadNotifyAlarmTemplate(String env) {
        return I18nUtil.getString("jobconf_monitor_detail") + "-" + env + "\n" +
                I18nUtil.getString("jobinfo_field_jobgroup") + ":{0}\n" +
                I18nUtil.getString("alarm_level") + ":{1}\n" +
                I18nUtil.getString("alarm_type") + ":{2}\n" +
                I18nUtil.getString("alarm_name") + ":{3}\n" +
                I18nUtil.getString("alarm_event_id") + ":{4}\n" +
                I18nUtil.getString("notify_content") + ":{5}\n";
    }

    /**
     * load seatalk job alarm template
     *
     * @return 模板字符串
     */
    private String loadSeatalkRunTaskDelayAlarmTemplate(String env) {
        return I18nUtil.getString("jobconf_monitor_detail") + "-" + env + "\n" +
                I18nUtil.getString("jobinfo_field_jobgroup") + "\t" +
                I18nUtil.getString("jobinfo_field_id") + "\t" +
                I18nUtil.getString("jobinfo_field_jobname") + "\t" +
                I18nUtil.getString("joblog_field_id") + "\t" +
                I18nUtil.getString("jobconf_monitor_alarm_title") + "\t" +
                I18nUtil.getString("joblog_trigger_time") + "\t" +
                I18nUtil.getString("jobinfo_start_executor_tolerance_threshold") + "\t" +
                I18nUtil.getString("jobconf_start_executor_elpase") + "\t\n" +
                "{0}\t{1}\t{2}\t" +
                "{3}\t" +
                I18nUtil.getString("jobconf_monitor_delay_executor_alarm_type") + "\t{4}\t{5}\t{6}\t\n";
    }

    static class DoD {
        @SerializedName("dev_id")
        public Integer devId;

        @SerializedName("username")
        public String username;

        public String getxSpError() {
            return xSpError;
        }

        public void setxSpError(String xSpError) {
            this.xSpError = xSpError;
        }

        @SerializedName("X-Sp-Error")
        public String xSpError;
        @SerializedName("team_id")
        public Integer teamId;
        @SerializedName("start_time")
        public Integer startTime;
        @SerializedName("endTime")
        public Integer endTime;
        @SerializedName("mm_handle")
        public String mmHandle;

        public String getContact() {
            return contact;
        }

        public void setContact(String contact) {
            this.contact = contact;
        }

        public String getDialingCode() {
            return dialingCode;
        }

        public void setDialingCode(String dialingCode) {
            this.dialingCode = dialingCode;
        }

        @SerializedName("contact")
        public String contact;
        @SerializedName("dialing_code")
        public String dialingCode;
        @SerializedName("email")
        public String email;
        @SerializedName("dod_type")
        public Integer dodType;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Integer getTeamId() {
            return teamId;
        }

        public void setTeamId(Integer teamId) {
            this.teamId = teamId;
        }

        public Integer getStartTime() {
            return startTime;
        }

        public void setStartTime(Integer startTime) {
            this.startTime = startTime;
        }

        public Integer getEndTime() {
            return endTime;
        }

        public void setEndTime(Integer endTime) {
            this.endTime = endTime;
        }

        public String getMmHandle() {
            return mmHandle;
        }

        public void setMmHandle(String mmHandle) {
            this.mmHandle = mmHandle;
        }


        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Integer getDodType() {
            return dodType;
        }

        public void setDodType(Integer dodType) {
            this.dodType = dodType;
        }

        public Integer getDevId() {
            return devId;
        }

        public void setDevId(Integer devId) {
            this.devId = devId;
        }
    }
}
