package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.model.JobInfo;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 系统告警
 *
 * @author honggang.liu
 */
public class JobSystemReportHelper {
    private static Logger logger = LoggerFactory.getLogger(JobSystemReportHelper.class.getSimpleName());
    /**
     * 单线程的调度任务，用户系统告警统计(执行器)
     */
    private ScheduledExecutorService executorScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    /**
     * 单线程的调度任务，用户系统告警统计(log)
     */
    private ScheduledExecutorService logScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private static JobSystemReportHelper instance = new JobSystemReportHelper();

    public static JobSystemReportHelper getInstance() {
        return instance;
    }


    public void start() {
        executorScheduledExecutorService.scheduleAtFixedRate(() -> {
            Date now = new Date();
            Date fiveMinBeforeDate = DateUtils.addMinutes(now, -5);
            List<JobGroup> jobGroups = JobAdminConfig.getAdminConfig().getJobGroupDao().findOffline(fiveMinBeforeDate, now);
            if (!CollectionUtils.isEmpty(jobGroups)) {
                JobAdminConfig.getAdminConfig().getJobAlarmer().groupOfflineAlarm(jobGroups);
            }
        }, 60, 300, TimeUnit.SECONDS);

        logScheduledExecutorService.scheduleAtFixedRate(() -> {
            // DB锁控制
            Connection conn = null;
            Boolean connAutoCommit = null;
            PreparedStatement preparedStatement = null;
            try {
                conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
                connAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                preparedStatement = conn.prepareStatement("select * from gs_job_lock where lock_name = 'system_report_lock' for update");
                preparedStatement.execute();
                Date to = new Date();
                Date from = DateUtils.addMinutes(to, -5);
                List<JobGroup> jobGroupList = JobAdminConfig.getAdminConfig().getJobGroupDao().findAll();
                if (!CollectionUtils.isEmpty(jobGroupList)) {
                    List<Map<String, Object>> alarmList = new ArrayList<>();
                    for (JobGroup jobGroup : jobGroupList) {
                        List<JobInfo> jobInfoList = JobAdminConfig.getAdminConfig().getJobInfoDao().pageList(0, Integer.MAX_VALUE, Collections.singletonList(jobGroup.getId()), -1, null, null, null, null);
                        for (JobInfo jobInfo : jobInfoList) {
                            Map<String, Object> triggerCountMap = JobAdminConfig.getAdminConfig().getJobLogDao().findLogReport(jobGroup.getId(), jobInfo.getId(), from, to);
                            if (triggerCountMap != null && !triggerCountMap.isEmpty()) {
                                int triggerDayCount = triggerCountMap.containsKey("triggerDayCount") ? Integer.parseInt(String.valueOf(triggerCountMap.get("triggerDayCount"))) : 0;
                                int triggerDayCountRunning = triggerCountMap.containsKey("triggerDayCountRunning") ? Integer.parseInt(String.valueOf(triggerCountMap.get("triggerDayCountRunning"))) : 0;
                                int triggerDayCountSuc = triggerCountMap.containsKey("triggerDayCountSuc") ? Integer.parseInt(String.valueOf(triggerCountMap.get("triggerDayCountSuc"))) : 0;
                                int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;
                                if (triggerDayCountFail > 0) {
                                    Map<String, Object> alarmData = new HashMap<>(4);
                                    alarmData.put("appName", jobGroup.getAppname());
                                    alarmData.put("jobName", jobInfo.getJobName());
                                    alarmData.put("jobId", jobInfo.getId());
                                    alarmData.put("triggerCountFail", triggerDayCountFail);
                                    alarmList.add(alarmData);
                                }
                            }
                        }
                    }
                    if (!CollectionUtils.isEmpty(alarmList)) {
                        JobAdminConfig.getAdminConfig().getJobAlarmer().logFailCountAlarm(alarmList);
                    }
                }
            } catch (Exception e) {
                logger.error(">>JobSystemReportHelper error:", e);
            } finally {
                // commit
                if (conn != null) {
                    try {
                        conn.commit();
                    } catch (SQLException e) {
                        logger.error(e.getMessage(), e);
                    }
                    try {
                        conn.setAutoCommit(connAutoCommit);
                    } catch (SQLException e) {
                        logger.error(e.getMessage(), e);
                    }
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                // close PreparedStatement
                if (null != preparedStatement) {
                    try {
                        preparedStatement.close();
                    } catch (SQLException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }, 30, 300, TimeUnit.SECONDS);

    }

    public void toStop() {
        logger.info("JobSystemReportHelper destroy");
        logScheduledExecutorService.shutdown();
        executorScheduledExecutorService.shutdown();
    }

}
