package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.trigger.JobTrigger;
import com.tc.gschedulercore.core.trigger.LogTypeEnum;
import com.tc.gschedulercore.core.trigger.TriggerTypeEnum;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * job trigger thread pool helper
 *
 * @author honggang.liu 2018-07-03 21:08:07
 */
public class JobTriggerPoolHelper {
    private static Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class.getSimpleName());


    // ---------------------- trigger pool ----------------------

    /**
     * fast/slow thread pool
     */
    private ThreadPoolExecutor fastTriggerPool = null;
    private ThreadPoolExecutor slowTriggerPool = null;
    /**
     * 业务线程池
     */
    private static Map<Integer, ThreadPoolExecutor> executorThreadPoolMap = new ConcurrentHashMap<>();

    public void start() {
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                JobAdminConfig.getAdminConfig().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50000),
                r -> new Thread(r, "JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode()),
                new ThreadPoolExecutor.CallerRunsPolicy());

        slowTriggerPool = new ThreadPoolExecutor(
                10,
                JobAdminConfig.getAdminConfig().getTriggerPoolSlowMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100000),
                r -> new Thread(r, "JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        List<JobGroup> jobGroupList;
        try (HintManager manager = HintManager.getInstance()) {
            manager.setWriteRouteOnly();
            jobGroupList = JobAdminConfig.getAdminConfig().getJobGroupDao().findAll();
        }
        if (!CollectionUtils.isEmpty(jobGroupList)) {
            for (JobGroup jobGroup : jobGroupList) {
                executorThreadPoolMap.put(jobGroup.getId(), new ThreadPoolExecutor(
                        5,
                        20,
                        60L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(100000),
                        r -> new Thread(r, "executorThreadPool-callbackThreadPool-" + r.hashCode()),
                        new ThreadPoolExecutor.CallerRunsPolicy()));
            }
        }
    }


    public void stop() {
        fastTriggerPool.shutdown();
        slowTriggerPool.shutdown();
        try {
            // 50毫秒轮训一次
            while (!fastTriggerPool.awaitTermination(50, TimeUnit.MILLISECONDS)) {
                logger.info("JobTriggerPoolHelper fastTriggerPool thread wait stopping");
            }
            // 50毫秒轮训一次
            while (!slowTriggerPool.awaitTermination(50, TimeUnit.MILLISECONDS)) {
                logger.info("JobTriggerPoolHelper slowTriggerPool thread wait stopping");
            }
            // 业务线程池释放
            for (Integer executorId : executorThreadPoolMap.keySet()) {
                ThreadPoolExecutor executor = executorThreadPoolMap.get(executorId);
                executor.shutdown();
                while (!executor.awaitTermination(50, TimeUnit.MILLISECONDS)) {
                    logger.info("go-scheduler-executorId-{} thread wait stopping", executorId);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("trigger thread pool shutdown success.");
    }


    /**
     * job timeout count
     * ms > min
     */
    private volatile long minTim = System.currentTimeMillis() / 60000;
    private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();


    /**
     * add trigger
     */
    public void addTrigger(
            final int jobGroup,
            final int jobId,
            final TriggerTypeEnum triggerType,
            final int failRetryCount,
            final String executorShardingParam,
            final String executorParam,
            final String addressList,
            final Long parentLog,
            LogTypeEnum logTypeEnum,
            String instanceId,
            String additionalParams,
            String routeFlag) {
        // 优先走业务队列
        if (executorThreadPoolMap.containsKey(jobGroup)) {
            addExecutorTrigger(jobGroup, jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList, parentLog, logTypeEnum, instanceId, additionalParams, routeFlag);
            return;
        }
        // choose thread pool
        ThreadPoolExecutor triggerPool = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        // job-timeout 10 times in 1 min
        if (jobTimeoutCount != null && jobTimeoutCount.get() > 10) {
            triggerPool = slowTriggerPool;
        }
        // trigger
        triggerPool.execute(() -> {
            long start = System.currentTimeMillis();
            try {
                // do trigger
                JobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList, parentLog, logTypeEnum, instanceId, additionalParams, routeFlag);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                // check timeout-count-map
                long minTimNow = System.currentTimeMillis() / 60000;
                if (minTim != minTimNow) {
                    minTim = minTimNow;
                    jobTimeoutCountMap.clear();
                }
                // incr timeout-count-map
                long cost = System.currentTimeMillis() - start;
                if (cost > 500) {       // ob-timeout threshold 500ms
                    AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                    if (timeoutCount != null) {
                        timeoutCount.incrementAndGet();
                    }
                }
            }
        });
    }

    /**
     * add addExecutorTrigger
     */
    public void addExecutorTrigger(
            final int jobGroup,
            final int jobId,
            final TriggerTypeEnum triggerType,
            final int failRetryCount,
            final String executorShardingParam,
            final String executorParam,
            final String addressList,
            final Long parentLog,
            LogTypeEnum logTypeEnum,
            String instanceId,
            String additionalParams,
            String routeFlag) {


        // trigger
        executorThreadPoolMap.get(jobGroup).execute(() -> {
            try {
                // do trigger
                JobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList, parentLog, logTypeEnum, instanceId, additionalParams, routeFlag);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }


    // ---------------------- helper ----------------------

    private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    public static void toStart() {
        helper.start();
    }

    public static void toStop() {
        helper.stop();
    }

    /**
     * @param jobId                 任务ID
     * @param triggerType           触发类型
     * @param failRetryCount        >=0: use this param
     *                              <0: use param from job info config
     * @param executorShardingParam sharing参数
     * @param executorParam         null: use job param
     *                              not null: cover job param
     * @param instanceId            实例ID
     * @param addressList           执行地址列表
     * @param logTypeEnum           日志类型
     * @param parentLog             父logId
     */
    public static void trigger(int jobGroup, int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam, String addressList, Long parentLog, LogTypeEnum logTypeEnum, String instanceId, String additionalParams, String routeFlag) {
        helper.addTrigger(jobGroup, jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList, parentLog, logTypeEnum, instanceId, additionalParams, routeFlag);
    }
}
