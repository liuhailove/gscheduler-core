package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * job 执行阈值 monitor instance
 *
 * @author honggang.liu
 */
public class JobThresholdMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobThresholdMonitorHelper.class.getSimpleName());

    /**
     * 预读数量
     */
    private static final int PRE_READ_COUNT = 100;


    private static JobThresholdMonitorHelper instance = new JobThresholdMonitorHelper();

    public static JobThresholdMonitorHelper getInstance() {
        return instance;
    }

    // ---------------------- monitor ----------------------

    /**
     * 单线程的调度任务，用于health检查
     */
    private ScheduledExecutorService monitorExecutorService = Executors.newSingleThreadScheduledExecutor();


    public void start() {
        monitorExecutorService.scheduleAtFixedRate(() -> {
            try {
                List<Integer> jobGroupIdes = JobAdminConfig.getAdminConfig().getJobService().findAllGroupIdCached();
                for (int jobGroup : jobGroupIdes) {
                    long nowTime = System.currentTimeMillis();
                    List<Long> thresholdTimeoutLogIds = JobAdminConfig.getAdminConfig().getJobLogDao().findThresholdTimeoutJobLogIds(jobGroup, nowTime, PRE_READ_COUNT);
                    if (thresholdTimeoutLogIds != null && !thresholdTimeoutLogIds.isEmpty()) {
                        for (long thresholdTimeoutLogId : thresholdTimeoutLogIds) {
                            JobLog log;
                            try (HintManager manager = HintManager.getInstance()) {
                                manager.setWriteRouteOnly();
                                log = JobAdminConfig.getAdminConfig().getJobLogDao().load(jobGroup, thresholdTimeoutLogId);
                            }
                            fireAlarm(log);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(">>job threshold monitor thread error:", e);
            }
        }, 60, 30, TimeUnit.SECONDS);

    }

    /**
     * 触发告警
     *
     * @param log log
     * @return
     */
    private void fireAlarm(JobLog log) {
        // lock log
        int lockRet = JobAdminConfig.getAdminConfig().getJobLogDao().updateThresholdAlarmStatus(log.getId(), JobLog.ALARM_STATUS_DEFAULT, JobLog.ALARM_STATUS_LOCK);
        if (lockRet < 1) {
            return;
        }
        JobInfo info = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(log.getJobId());
        // 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
        int newAlarmStatus;
        if (info != null && !StringUtils.isEmpty(info.getAlarmSeatalk())) {
            boolean alarmResult = JobAdminConfig.getAdminConfig().getJobAlarmer().thresholdAlarm(info, log);
            newAlarmStatus = alarmResult ? JobLog.ALARM_STATUS_SUCCESS : JobLog.ALARM_STATUS_FAILED;
        } else {
            newAlarmStatus = JobLog.ALARM_STATUS_NO_NEED;
        }
        JobAdminConfig.getAdminConfig().getJobLogDao().updateThresholdAlarmStatus(log.getId(), JobLog.ALARM_STATUS_LOCK, newAlarmStatus);
    }

    /**
     * 停止调度
     */
    public void toStop() {
        monitorExecutorService.shutdown();
    }


}
