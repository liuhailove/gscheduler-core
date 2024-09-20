package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import com.tc.gschedulercore.core.route.ExecutorRouteStrategyEnum;
import com.tc.gschedulercore.core.trigger.JobTrigger;
import com.tc.gschedulercore.core.trigger.TriggerTypeEnum;
import com.tc.gschedulercore.enums.ExecutorBlockStrategyEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 延迟任务，
 * 在任务被配置延迟后，并不会立刻下发执行，而是需要到延迟对应的时间才开始执行，
 * 延迟任务每2s一次，因此和实际的执行时间有可能有1s的延迟
 *
 * @author honggang.liu
 */
public class JobDelayHelper {

    /**
     * logger
     */
    private static Logger logger = LoggerFactory.getLogger(JobDelayHelper.class.getSimpleName());

    private volatile boolean delayThreadToStop = false;

    private ScheduledExecutorService delayPoolExecutor = Executors.newSingleThreadScheduledExecutor();


    private static JobDelayHelper instance = new JobDelayHelper();

    public static JobDelayHelper getInstance() {
        return instance;
    }


    public void start() {
        delayPoolExecutor.scheduleAtFixedRate(this::handleDelayTask, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * 延迟任务处理
     */
    private void handleDelayTask() {
        try {
            List<Integer> jobGroupIdes = JobAdminConfig.getAdminConfig().getJobService().findAllGroupIdCached();
            for (Integer jobGroupId : jobGroupIdes) {
                // 1.拉取状态为处理中的最近6小时任务,
                List<JobLog> jobLogList;
                try (HintManager manager = HintManager.getInstance()) {
                    manager.setWriteRouteOnly();
                    jobLogList = JobAdminConfig.getAdminConfig().getJobLogDao().query10DelayExecute(jobGroupId, System.currentTimeMillis());
                }
                if (CollectionUtils.isEmpty(jobLogList)) {
                    continue;
                }
                for (JobLog jobLog : jobLogList) {
                    JobGroup jobGroup = JobAdminConfig.getAdminConfig().getJobService().loadGroupCached(jobLog.getJobGroup());
                    JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(jobLog.getJobId());
                    if (jobInfo == null) {
                        logger.info("log id's [{}] jobInfo is null,jobId={}", jobLog.getId(), jobLog.getJobId());
                        continue;
                    }
                    Connection conn = null;
                    Boolean connAutoCommit = null;
                    PreparedStatement preparedStatement = null;
                    try {
                        // 加锁
                        conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
                        connAutoCommit = conn.getAutoCommit();
                        conn.setAutoCommit(false);
                        preparedStatement = conn.prepareStatement("select * from gs_job_lock where lock_name = 'system_compensate_lock' for update");
                        preparedStatement.execute();
                        // 阻塞策略
                        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);
                        // route strategy
                        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);
                        int index = 0;
                        int total = 1;
                        if (!StringUtils.isEmpty(jobLog.getExecutorShardingParam())) {
                            String[] arr = jobLog.getExecutorShardingParam().split("/");
                            if (arr.length == 2) {
                                index = Integer.parseInt(arr[0]);
                                total = Integer.parseInt(arr[1]);
                            }
                        }
                        jobInfo.setExecutorParam(jobLog.getExecutorParam());
                        JobTrigger.postProcessing(jobGroup, jobInfo, jobLog.getExecutorFailRetryCount(), TriggerTypeEnum.DelayExecute, index, total, blockStrategy, executorRouteStrategyEnum, jobLog.getExecutorShardingParam(), jobLog, "", "");
                    } catch (Exception e) {
                        if (!delayThreadToStop) {
                            logger.error("JobDelayHelper#handleDelayTask error:", e);
                        }
                    } finally {
                        // commit
                        if (conn != null) {
                            try {
                                conn.commit();
                            } catch (SQLException e) {
                                logger.error("handleDelayTask commit:", e);
                            }
                            try {
                                conn.setAutoCommit(Boolean.TRUE.equals(connAutoCommit));
                            } catch (SQLException e) {
                                logger.error("handleDelayTask setAutoCommit:", e);
                            }
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                logger.error("handleDelayTask close:", e);
                            }
                        }
                        // close PreparedStatement
                        if (null != preparedStatement) {
                            try {
                                preparedStatement.close();
                            } catch (SQLException e) {
                                logger.error("handleCompensate close:", e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("handleCompensate close:", e);
        }
    }

    /**
     * 停止线程
     */
    public void toStop() {
        if (delayPoolExecutor == null) {
            return;
        }
        delayPoolExecutor.shutdown();
        logger.info("JobDelayHelper callbackThreadPool thread stopped");
    }

}
