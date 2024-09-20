package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import com.tc.gschedulercore.core.model.JobLogReport;
import com.tc.gschedulercore.core.util.DateUtils;
import com.tc.gschedulercore.core.util.I18nUtil;
import org.quartz.TriggerUtils;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * job log report helper
 *
 * @author honggang.liu
 */
public class JobLogReportHelper {
    private static Logger logger = LoggerFactory.getLogger(JobLogReportHelper.class.getSimpleName());
    private static final int PRE_READ_COUNT = 100;

    private volatile boolean toStop = false;
    public static final String JOB_GROUP_NAME_2 = "jobgroup_name_2";
    public static final String ENV_NAME = "env_name";
    public static final String JOB_ID = "job_id";
    public static final String JOB_DESC = "job_desc";
    public static final String JOB_SUCCESS_CNT = "job_success_cnt";
    public static final String JOB_RUNNING_CNT = "job_running_cnt";
    public static final String JOB_FAILED_CNT = "job_failed_cnt";

    public static final String JOB_EXPECTED_CNT = "job_expected_cnt";

    public static final String JOB_ACTUAL_CNT = "job_actual_cnt";
    /**
     * 生成随机数
     */
    private Random random = new Random();

    /**
     * 五分钟
     */
    public static final int FIVE_MIN = 60 * 5;

    private static final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new SynchronousQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    private static final JobLogReportHelper instance = new JobLogReportHelper();

    public static JobLogReportHelper getInstance() {
        return instance;
    }


    public void start() {
        poolExecutor.submit(() -> {
            while (!toStop) {

                // 1、log-report refresh: refresh log report in 3 days
                try {
                    List<JobGroup> jobGroupList = JobAdminConfig.getAdminConfig().getJobGroupDao().findAll();
                    if (!CollectionUtils.isEmpty(jobGroupList)) {
                        for (JobGroup jobGroup : jobGroupList) {
                            boolean sendEmail = !StringUtils.isEmpty(jobGroup.getScheduleConf())
                                    && null != jobGroup.isTriggerStatus() && jobGroup.isTriggerStatus()
                                    && System.currentTimeMillis() > jobGroup.getTriggerNextTime()
                                    && !StringUtils.isEmpty(jobGroup.getReportReceiver());
                            List<Map<String, Object>> reportList = new ArrayList<>();
                            List<JobInfo> jobInfoList = JobAdminConfig.getAdminConfig().getJobInfoDao().pageList(0, Integer.MAX_VALUE, Collections.singletonList(jobGroup.getId()), -1, null, null, null, null);
                            for (JobInfo jobInfo : jobInfoList) {
                                // today
                                Calendar itemDay = Calendar.getInstance();
                                itemDay.add(Calendar.DAY_OF_MONTH, 0);
                                itemDay.set(Calendar.HOUR_OF_DAY, 0);
                                itemDay.set(Calendar.MINUTE, 0);
                                itemDay.set(Calendar.SECOND, 0);
                                itemDay.set(Calendar.MILLISECOND, 0);

                                Date todayFrom = itemDay.getTime();
                                // 得到昨天
                                itemDay.add(Calendar.DATE, -1);
                                Date yesterdayFrom = itemDay.getTime();

                                itemDay.set(Calendar.HOUR_OF_DAY, 23);
                                itemDay.set(Calendar.MINUTE, 59);
                                itemDay.set(Calendar.SECOND, 59);
                                itemDay.set(Calendar.MILLISECOND, 999);

                                Date yesterdayTo = itemDay.getTime();

                                // refresh log-report every minute
                                JobLogReport jobLogReport = new JobLogReport();
                                jobLogReport.setTriggerDay(todayFrom);
                                jobLogReport.setRunningCount(0);
                                jobLogReport.setSucCount(0);
                                jobLogReport.setFailCount(0);
                                jobLogReport.setAppname(jobGroup.getAppname());
                                jobLogReport.setEnv(JobAdminConfig.getAdminConfig().getEnv());
                                jobLogReport.setJobId(jobInfo.getId());
                                jobLogReport.setJobDesc(jobInfo.getJobDesc());
                                jobLogReport.setJobGroup(jobGroup.getId());
                                Map<String, Object> triggerCountMap;
                                Cache logReportCache = JobAdminConfig.getAdminConfig().getCacheManager().getCache("logReportCache");
                                if (logReportCache != null) {
                                    triggerCountMap = logReportCache.get("log_report_" + jobGroup.getId() + jobInfo.getId() + yesterdayFrom + yesterdayTo, Map.class);
                                    if (triggerCountMap == null) {
                                        triggerCountMap = JobAdminConfig.getAdminConfig().getJobLogDao().findLogReport(jobGroup.getId(), jobInfo.getId(), yesterdayFrom, yesterdayTo);
                                        logReportCache.put("log_report_" + jobGroup.getId() + jobInfo.getId() + yesterdayFrom + yesterdayTo, triggerCountMap);
                                    }
                                } else {
                                    triggerCountMap = JobAdminConfig.getAdminConfig().getJobLogDao().findLogReport(jobGroup.getId(), jobInfo.getId(), yesterdayFrom, yesterdayTo);
                                }
                                if (triggerCountMap != null && !triggerCountMap.isEmpty()) {
                                    int triggerDayCount = triggerCountMap.containsKey("triggerDayCount") ? Integer.parseInt(String.valueOf(triggerCountMap.get("triggerDayCount"))) : 0;
                                    int triggerDayCountRunning = triggerCountMap.containsKey("triggerDayCountRunning") ? Integer.parseInt(String.valueOf(triggerCountMap.get("triggerDayCountRunning"))) : 0;
                                    int triggerDayCountSuc = triggerCountMap.containsKey("triggerDayCountSuc") ? Integer.parseInt(String.valueOf(triggerCountMap.get("triggerDayCountSuc"))) : 0;
                                    int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;
                                    int taskExpectedCnt = getTaskExpectedExecNumbersByScheduleExpression(jobInfo.getScheduleConf(), jobInfo.getScheduleType());  //根据corn 表达式计算定时任务一天内的理论执行次数

                                    jobLogReport.setRunningCount(triggerDayCountRunning);
                                    jobLogReport.setSucCount(triggerDayCountSuc);
                                    jobLogReport.setFailCount(triggerDayCountFail);
                                    jobLogReport.setExpectedCount(taskExpectedCnt);
                                }

                                // DB锁控制
                                Connection conn = null;
                                Boolean connAutoCommit = null;
                                PreparedStatement preparedStatement = null;
                                try {
                                    conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
                                    connAutoCommit = conn.getAutoCommit();
                                    conn.setAutoCommit(false);
                                    preparedStatement = conn.prepareStatement("select * from gs_job_lock where lock_name = 'report_lock' for update");
                                    preparedStatement.execute();
                                    // do refresh
                                    int ret = JobAdminConfig.getAdminConfig().getJobLogReportDao().update(jobLogReport);
                                    if (ret < 1) {
                                        JobAdminConfig.getAdminConfig().getJobLogReportDao().save(jobLogReport);
                                    }
                                } catch (Exception e) {
                                    if (!toStop) {
                                        logger.error(">>JobLogReportHelper error:", e);
                                    }
                                } finally {
                                    // commit
                                    if (conn != null) {
                                        try {
                                            conn.commit();
                                        } catch (SQLException e) {
                                            if (!toStop) {
                                                logger.error(e.getMessage(), e);
                                            }
                                        }
                                        try {
                                            conn.setAutoCommit(Boolean.TRUE.equals(connAutoCommit));
                                        } catch (SQLException e) {
                                            if (!toStop) {
                                                logger.error(e.getMessage(), e);
                                            }
                                        }
                                        try {
                                            conn.close();
                                        } catch (SQLException e) {
                                            if (!toStop) {
                                                logger.error(e.getMessage(), e);
                                            }
                                        }
                                    }

                                    // close PreparedStatement
                                    if (null != preparedStatement) {
                                        try {
                                            preparedStatement.close();
                                        } catch (SQLException e) {
                                            if (!toStop) {
                                                logger.error(e.getMessage(), e);
                                            }
                                        }
                                    }
                                }
                                // 如果当前时间>下一次要调度的时间，则需要发送报表统计
                                if (sendEmail) {
                                    int taskActualCnt = JobAdminConfig.getAdminConfig().getJobLogDao().getTaskActualRunNumbers(jobLogReport.getJobGroup(), jobLogReport.getJobId(), todayFrom, yesterdayFrom, PRE_READ_COUNT);
                                    Map<String, Object> report = new HashMap<>(9);
                                    report.put(JOB_GROUP_NAME_2, jobLogReport.getAppname());
                                    report.put(ENV_NAME, jobLogReport.getEnv());
                                    report.put(JOB_ID, jobLogReport.getJobId());
                                    report.put(JOB_DESC, jobLogReport.getJobDesc());
                                    report.put(JOB_SUCCESS_CNT, jobLogReport.getSucCount());
                                    report.put(JOB_RUNNING_CNT, jobLogReport.getRunningCount());
                                    report.put(JOB_FAILED_CNT, jobLogReport.getFailCount());
                                    report.put(JOB_EXPECTED_CNT, jobLogReport.getExpectedCount());
                                    report.put(JOB_ACTUAL_CNT, taskActualCnt);
                                    jobInfo.setResultCheck(true);
                                    //如果任务的期望执行次数> 实际执行次数，并且业务测配置了告警，则发送seatalk 告警
                                    if (jobInfo.getTriggerStatus() == 1 && jobLogReport.getExpectedCount() > 0 && jobLogReport.getExpectedCount() > taskActualCnt) {
                                        JobLog jobLog = new JobLog();
                                        jobLog.setJobGroup(jobInfo.getJobGroup());
                                        jobLog.setJobId(jobInfo.getId());
                                        jobLog.setTriggerTime(new Date());
                                        jobLog.setJobName(jobInfo.getJobName());
                                        logger.info("job execution times exception,JobId:{},expectedCount:{},actualCnt:{} :,todayFrom:{}, yesterdayFrom: {} ", jobLogReport.getJobId(), jobLogReport.getExpectedCount(), taskActualCnt, todayFrom, yesterdayFrom);
                                        JobAdminConfig.getAdminConfig().getJobAlarmer().jobExecTimesExceptionAlarm(jobInfo, jobGroup.getId(), jobLogReport.getExpectedCount(), taskActualCnt);
                                    }
                                    reportList.add(report);
                                }
                            }

                            // 发送报告邮件
                            if (sendEmail && !reportList.isEmpty()) {
                                logger.info("ready to sendEmail");

                                List<Object> arguments = new ArrayList<>();
                                for (Map<String, Object> report : reportList) {
                                    arguments.add(report.get(JOB_GROUP_NAME_2));
                                    arguments.add(report.get(ENV_NAME));
                                    arguments.add(report.get(JOB_ID));
                                    arguments.add(report.get(JOB_DESC));
                                    arguments.add(report.get(JOB_SUCCESS_CNT));
                                    arguments.add(report.get(JOB_RUNNING_CNT));
                                    arguments.add(report.get(JOB_FAILED_CNT));
                                    arguments.add(report.get(JOB_EXPECTED_CNT));
                                    arguments.add(report.get(JOB_ACTUAL_CNT));
                                }
                                String content = MessageFormat.format(loadEmailReportTemplate(reportList.size()), arguments.toArray());
                                String personal = I18nUtil.getString("admin_name_full");
                                String title = I18nUtil.getString("jobconf_monitor_report");
                                Set<String> emailSet = new HashSet<>(Arrays.asList(jobGroup.getReportReceiver().split(",")));
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
                                        logger.info("send email success,email:{}", email);
                                    } catch (Exception e) {
                                        logger.error(">>send report error, JobGroupId:{}", jobGroup.getId(), e);
                                    }
                                }
                                logger.info("send email success,update TriggerNextTime:{}", jobGroup.getTriggerNextTime());
                                // 更新jobGroup的调度时间
                                jobGroup.setTriggerLastTime(jobGroup.getTriggerNextTime());
                                try {
                                    Date fromTime = jobGroup.getTriggerNextTime() == 0 ? new Date() : new Date(jobGroup.getTriggerNextTime());
                                    jobGroup.setTriggerNextTime(JobScheduleHelper.generateNextValidTime(jobGroup, fromTime).getTime());
                                    // 如果计算的下一次执行时间<当前时间，这说明数据已经错过了，需要把fromTime修改为当前时间，重新计算一次
                                    if (System.currentTimeMillis() > jobGroup.getTriggerNextTime()) {
                                        fromTime = new Date();
                                        jobGroup.setTriggerNextTime(JobScheduleHelper.generateNextValidTime(jobGroup, fromTime).getTime());
                                    }
                                    logger.info("ready to sendEmail,fromTime:{},next Send Time:{},jobId:{}", fromTime, jobGroup.getTriggerNextTime(), jobGroup.getId());
                                } catch (Exception e) {
                                    jobGroup.setTriggerNextTime(0);
                                    jobGroup.setTriggerLastTime(0);
                                    jobGroup.setTriggerStatus(false);
                                    logger.error("获取下一次trigger时间失败, JobGroupId:{}", jobGroup.getId(), e);
                                }
                                JobAdminConfig.getAdminConfig().getJobGroupDao().scheduleUpdate(jobGroup);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error("job log report thread error:", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(random.nextInt(60) + FIVE_MIN);
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void toStop() {
        toStop = true;
        poolExecutor.shutdown();
    }

    /**
     * 加载发送业务报表的模板
     *
     * @return 模板
     */
    private static String loadEmailReportTemplate(int trLen) {
        String head = "<h5>" + I18nUtil.getString("jobconf_report_monitor_detail") + "-" + JobAdminConfig.getAdminConfig().getEnv() + "-" + DateUtils.getToday() +
                "：</span>" +
                "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
                "   <thead style=\"font-weight: bold;color: #ffffff;background-color:#008B00;\" >" +
                "      <tr>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("jobgroup_name_2") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("env_name") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("job_id") + "</td>\n" +
                "         <td width=\"20%\" >" + I18nUtil.getString("job_desc") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("job_success_cnt") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("job_running_cnt") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("job_failed_cnt") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("job_expected_cnt") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("job_actual_cnt") + "</td>\n" +
                "      </tr>\n" +
                "   </thead>\n";
        String tail = "</table>";
        String body = "";
        if (trLen > 0) {
            body += " <tbody>\n";
            for (int i = 0; i < trLen; i++) {
                body += "      <tr>\n" +
                        "         <td>{" + (i * 9) + "}</td>\n" +
                        "         <td>{" + (i * 9 + 1) + "}</td>\n" +
                        "         <td>{" + (i * 9 + 2) + "}</td>\n" +
                        "         <td>{" + (i * 9 + 3) + "}</td>\n" +
                        "         <td>{" + (i * 9 + 4) + "}</td>\n" +
                        "         <td>{" + (i * 9 + 5) + "}</td>\n" +
                        "         <td>{" + (i * 9 + 6) + "}</td>\n" +
                        "         <td>{" + (i * 9 + 7) + "}</td>\n" +
                        "         <td>{" + (i * 9 + 8) + "}</td>\n" +
                        "      </tr>\n";
            }
            body += "   </tbody>\n";
        }
        return head + body + tail;
    }

    public static int getTaskExpectedExecNumbersByScheduleExpression(String scheduleExpression, String scheduleType) {
        int number = 0;
        try {
            //固定频率
            if (scheduleType.equals("FIX_RATE")) {
                int frequency = Integer.parseInt(scheduleExpression);
                if (frequency == 0) {
                    return number;
                }
                number = 24 * 60 * 60 / frequency;
                return number;
            } else if (scheduleType.equals("CRON")) {
                //cron 表达式
                if (scheduleExpression == null) {
                    return number;
                }
                CronTriggerImpl cronTriggerImpl = new CronTriggerImpl();

                cronTriggerImpl.setCronExpression(scheduleExpression);

                Calendar itemDay = Calendar.getInstance();
                itemDay.add(Calendar.DAY_OF_MONTH, -1); // Get yesterday
                itemDay.set(Calendar.HOUR_OF_DAY, 0);
                itemDay.set(Calendar.MINUTE, 0);
                itemDay.set(Calendar.SECOND, 0);
                itemDay.set(Calendar.MILLISECOND, 0);

                Date yesterdayFrom = itemDay.getTime();

                itemDay.set(Calendar.HOUR_OF_DAY, 23);
                itemDay.set(Calendar.MINUTE, 59);
                itemDay.set(Calendar.SECOND, 59);
                Date yesterdayTo = itemDay.getTime();

//                Calendar calendar = Calendar.getInstance();
//                Date now = calendar.getTime();
                // 把统计的区间段设置为从现在到1天后的今天
//                calendar.add(Calendar.DAY_OF_YEAR, -1);
//                Date yesterday = calendar.getTime();

                // 这里的时间是根据corn表达式算出来的值
                List<Date> dates = TriggerUtils.computeFireTimesBetween(
                        cronTriggerImpl, null, yesterdayFrom,
                        yesterdayTo);
                number = dates.size();
            } else {
                return number;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return number;
    }

}
