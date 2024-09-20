package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobInfo;
import net.sf.ehcache.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 日志清理
 *
 * @author honggang.liu
 */
public class JobLogCleanHelper {

    /**
     * logger
     */
    private static Logger logger = LoggerFactory.getLogger(JobLogCleanHelper.class.getSimpleName());

    /**
     * last clean log time
     */
    private long lastCleanLogTime = 0;

    /**
     * last clean log time
     */
    private long lastJobCleanLogTime = 0;

    /**
     * 一天
     */
    private static final long FOUR_HOUR = 4 * 60 * 60 * 1000L;

    /**
     * 15分钟
     */
    private static final long FIFTEEN_MIN = 15 * 60 * 1000L;


    @SuppressWarnings("PMD.JobWaitParentHelper")
    private ScheduledExecutorService jobLogCleanPoolExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("job-log-clean-helper", true));

    private static JobLogCleanHelper instance = new JobLogCleanHelper();

    public static JobLogCleanHelper getInstance() {
        return instance;
    }

    public void start() {
        jobLogCleanPoolExecutor.scheduleWithFixedDelay(this::logClean, 0, 1, TimeUnit.MINUTES);
    }

    /**
     * 日志清理
     */
    private void logClean() {
        // 如果两次清理时间大于1天，则进行清理 24 * 60 * 60 * 1000
        try {
            if (System.currentTimeMillis() - lastJobCleanLogTime > FIFTEEN_MIN) {
                logger.info("log clean 1 begin,nowTime:{},lastTime:{}", System.currentTimeMillis(), lastJobCleanLogTime);
                List<JobInfo> jobInfoList = JobAdminConfig.getAdminConfig().getJobInfoDao().findAllRetentionGreatThanZero();
                if (!CollectionUtils.isEmpty(jobInfoList)) {
                    Collections.shuffle(jobInfoList);
                    for (JobInfo jobInfo : jobInfoList) {
                        logger.info("log clean 1 print jobId:{},nowTime:{},lastTime:{}", jobInfo.getId(), System.currentTimeMillis(), lastJobCleanLogTime);
                        // expire-time
                        Calendar expiredDay = Calendar.getInstance();
                        expiredDay.add(Calendar.DAY_OF_MONTH, -1 * jobInfo.getLogRetentionDays());
                        expiredDay.set(Calendar.MINUTE, 0);
                        expiredDay.set(Calendar.SECOND, 0);
                        expiredDay.set(Calendar.MILLISECOND, 0);
                        Date clearBeforeTime = expiredDay.getTime();
                        // clean expired log
                        int count = 0;
                        List<Long> logIds;
                        List<Long> logMetricsIds;
                        do {
                            // 清理log
                            logIds = JobAdminConfig.getAdminConfig().getJobLogDao().findClearLogIds(jobInfo.getJobGroup(), jobInfo.getId(), clearBeforeTime, 0, 500);
                            if (logIds != null && !logIds.isEmpty()) {
                                JobAdminConfig.getAdminConfig().getJobLogDao().clearLog(jobInfo.getJobGroup(), logIds);
                            }
                            // 清理log metrics
                            logMetricsIds = JobAdminConfig.getAdminConfig().getLogMetricDao().findClearLogMetricsIds(jobInfo.getId(), clearBeforeTime, 500);
                            if (logMetricsIds != null && !logMetricsIds.isEmpty()) {
                                JobAdminConfig.getAdminConfig().getLogMetricDao().clearLog(jobInfo.getId(), logMetricsIds);
                            }
                            // 循环次数大于1W次，则break，让其他任务执行，没有清理的数据等待下次调度
                            if (count++ > 10000) {
                                logger.info("log clean 1 sleep, err:{}", jobInfo.getJobName());
                                break;
                            }
                            try {
                                // 随机sleep，一方面降低循环速度，进而降低DB压力，另外一方面避免多个实例在同一刻执行，减少等待锁
                                Thread.sleep((long) (2 + Math.random() * 10));
                            } catch (Exception e) {
                                logger.error("log clean 1 sleep,err:{}", e.getMessage());
                            }
                        } while (logIds != null && !logIds.isEmpty());
                        // 每个job完成要休眠
                        try {
                            Thread.sleep((long) (10 + Math.random() * 11));
                        } catch (Exception e) {
                            logger.error("log clean 1 sleep,err:{}", e.getMessage());
                        }
                    }
                    lastJobCleanLogTime = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            logger.error("log clean 1,err:{}", e.getMessage());
        }

        try {
            // 2、log-clean: switch open & once each day
            if (JobAdminConfig.getAdminConfig().getLogretentiondays() > 0
                    && System.currentTimeMillis() - lastCleanLogTime > FOUR_HOUR) {
                logger.info("log clean 2 begin,nowTime:{},lastTime:{}", System.currentTimeMillis(), lastJobCleanLogTime);
                // expire-time
                Calendar expiredDay = Calendar.getInstance();
                expiredDay.add(Calendar.DAY_OF_MONTH, -1 * JobAdminConfig.getAdminConfig().getLogretentiondays());
                expiredDay.set(Calendar.HOUR_OF_DAY, 0);
                expiredDay.set(Calendar.MINUTE, 0);
                expiredDay.set(Calendar.SECOND, 0);
                expiredDay.set(Calendar.MILLISECOND, 0);
                Date clearBeforeTime = expiredDay.getTime();

                // clean expired log
                List<Long> logIds;
                List<Long> logMetricsIds;
                int count = 0;
                List<JobInfo> jobInfoList = JobAdminConfig.getAdminConfig().getJobInfoDao().findAllRetentionEqualZero();
                if (CollectionUtils.isEmpty(jobInfoList)) {
                    return;
                }
                Collections.shuffle(jobInfoList);
                for (JobInfo jobInfo : jobInfoList) {
                    do {
                        // 清理log
                        logIds = JobAdminConfig.getAdminConfig().getJobLogDao().findClearLogIds(jobInfo.getJobGroup(), jobInfo.getId(), clearBeforeTime, 0, 500);
                        if (logIds != null && !logIds.isEmpty()) {
                            JobAdminConfig.getAdminConfig().getJobLogDao().clearLog(jobInfo.getJobGroup(), logIds);
                        }
                        // 清理log metrics
                        logMetricsIds = JobAdminConfig.getAdminConfig().getLogMetricDao().findClearLogMetricsIds(jobInfo.getId(), clearBeforeTime, 500);
                        if (logMetricsIds != null && !logMetricsIds.isEmpty()) {
                            JobAdminConfig.getAdminConfig().getLogMetricDao().clearLog(jobInfo.getId(), logMetricsIds);
                        }
                        // 循环次数大于1W次，则break，让其他任务执行，没有清理的数据等待下次调度
                        if (count++ > 10000) {
                            logger.error("log clean 2 too many in");
                            break;
                        }
                        try {
                            Thread.sleep((long) (2 + Math.random() * 2));
                        } catch (Exception e) {
                            logger.error("log clean 2 sleep,err:{}", e.getMessage());
                        }
                    } while (logIds != null && !logIds.isEmpty());
                    // 每个job完成要休眠
                    try {
                        Thread.sleep((long) (10 + Math.random() * 11));
                    } catch (Exception e) {
                        logger.error("log clean 2 sleep,err:{}", e.getMessage());
                    }
                }

                // clean logScriptList
                List<Long> logScriptList;
                do {
                    // 清理logScript
                    logScriptList = JobAdminConfig.getAdminConfig().getJobLogScriptDao().findClearLogIds(0, 0, clearBeforeTime, 0, 1000);
                    if (logScriptList != null && !logScriptList.isEmpty()) {
                        JobAdminConfig.getAdminConfig().getJobLogScriptDao().clearLog(logScriptList);
                    }
                    try {
                        Thread.sleep(2);
                    } catch (Exception e) {
                    }
                    // 循环次数大于1W次，则break，让其他任务执行，没有清理的数据等待下次调度
                    if (count++ > 10000) {
                        break;
                    }
                } while (logScriptList != null && !logScriptList.isEmpty());

                // update clean time
                lastCleanLogTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            logger.error("log clean 2,err:{}", e.getMessage());

        }

    }

    /**
     * 停止线程
     */
    public void toStop() {
        if (jobLogCleanPoolExecutor == null) {
            return;
        }
        jobLogCleanPoolExecutor.shutdown();
        logger.info("JobLogCleanHelper callbackThreadPool thread  stopped");
    }
}
