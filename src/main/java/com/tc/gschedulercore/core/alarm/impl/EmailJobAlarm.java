package com.tc.gschedulercore.core.alarm.impl;

import com.tc.gschedulercore.core.alarm.JobAlarm;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.core.util.I18nUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.*;

/**
 * job alarm by email
 *
 * @author honggang.liu
 */
@Component
public class EmailJobAlarm implements JobAlarm {
    private static Logger logger = LoggerFactory.getLogger(EmailJobAlarm.class.getSimpleName());

    /**
     * fail alarm
     *
     * @param jobLog
     */
    @Override
    public boolean doAlarm(JobInfo info, JobLog jobLog, String env) {
        boolean alarmResult = true;
        // send monitor email
        if (info != null && info.getAlarmEmail() != null && info.getAlarmEmail().trim().length() > 0) {

            // alarmContent
            String alarmContent = "Alarm Job LogId=" + jobLog.getId();
            if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "<br>TriggerMsg=<br>" + jobLog.getTriggerMsg();
            }
            if (jobLog.getHandleCode() > 0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "<br>HandleCode=" + jobLog.getHandleMsg();
            }

            // email info
            JobGroup group = JobAdminConfig.getAdminConfig().getJobGroupDao().load(Integer.valueOf(info.getJobGroup()));
            String personal = I18nUtil.getString("admin_name_full");
            String title = I18nUtil.getString("jobconf_monitor");
            String content = MessageFormat.format(loadEmailJobAlarmTemplate(env),
                    group != null ? group.getTitle() : "null",
                    info.getId(),
                    info.getJobDesc(),
                    alarmContent);

            Set<String> emailSet = new HashSet<>(Arrays.asList(info.getAlarmEmail().split(",")));
            for (String email : emailSet) {
                // make mail
                try {
                    MimeMessage mimeMessage = JobAdminConfig.getAdminConfig().getMailSender().createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                    helper.setFrom(JobAdminConfig.getAdminConfig().getEmailFrom(), personal);
                    helper.setTo(email);
                    helper.setSubject(title);
                    helper.setText(content, true);

                    JobAdminConfig.getAdminConfig().getMailSender().send(mimeMessage);
                } catch (Exception e) {
                    logger.error(">>job fail alarm email send error, JobLogId:{}", jobLog.getId(), e);

                    alarmResult = false;
                }
            }
        }
        return alarmResult;
    }

    /**
     * load email job alarm template
     *
     * @return
     */
    private static final String loadEmailJobAlarmTemplate(String env) {
        String mailBodyTemplate = "<h5>" + I18nUtil.getString("jobconf_monitor_detail") + "-" + env + "：</span>" +
                "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
                "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
                "      <tr>\n" +
                "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobgroup") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("jobinfo_field_id") + "</td>\n" +
                "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobdesc") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("jobconf_monitor_alarm_title") + "</td>\n" +
                "         <td width=\"40%\" >" + I18nUtil.getString("jobconf_monitor_alarm_content") + "</td>\n" +
                "      </tr>\n" +
                "   </thead>\n" +
                "   <tbody>\n" +
                "      <tr>\n" +
                "         <td>{0}</td>\n" +
                "         <td>{1}</td>\n" +
                "         <td>{2}</td>\n" +
                "         <td>" + I18nUtil.getString("jobconf_monitor_alarm_type") + "</td>\n" +
                "         <td>{3}</td>\n" +
                "      </tr>\n" +
                "   </tbody>\n" +
                "</table>";

        return mailBodyTemplate;
    }

    @Override
    public boolean doThresholdAlarm(JobInfo info, JobLog jobLog, String env) {
        return true;
    }

    @Override
    public boolean doJobExecTimesExceptionAlarm(JobInfo info, int jobGroupId, int jobExpectedTimes, int jobActualTimes, String env) {
        return true;
    }

    @Override
    public boolean doGroupOfflineAlarm(List<JobGroup> groupList, String env) {
        return true;
    }

    @Override
    public boolean doLogFailCountAlarmAlarm(List<Map<String, Object>> alarmList, String env) {
        return true;
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
        return true;
    }

    @Override
    public boolean doCompensateAlarm(JobInfo info, JobLog jobLog, String msg, String env) {
        return true;
    }
}
