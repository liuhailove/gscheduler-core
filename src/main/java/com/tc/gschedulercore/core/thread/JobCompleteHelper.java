package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.complete.JobCompleter;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.HandleCallbackParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import com.tc.gschedulercore.core.model.JobLogTag;
import com.tc.gschedulercore.core.util.JacksonUtil;
import com.tc.gschedulercore.enums.RetryType;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * job lose-monitor instance
 *
 * @author honggang.liu
 */
public class JobCompleteHelper {
    private static Logger logger = LoggerFactory.getLogger(JobCompleteHelper.class.getSimpleName());

    /**
     * 10s
     */
    private static long DEFAULT_RETRY_TIME = 10000;

    /**
     * 使用快速队列的最先子任务数量
     */
    private static final int MIN_FAST_THRESHOLD = 100;

    private static JobCompleteHelper instance = new JobCompleteHelper();

    public static JobCompleteHelper getInstance() {
        return instance;
    }

    // ---------------------- monitor ----------------------
    private ThreadPoolExecutor callbackThreadPool = null;
    /**
     * 此队列主要用于子任务传参类型的父任务，目的是使此类父任务尽快运行，以便数据入库，
     * 避免父任务没有机会执行导致的任务丢失
     */
    private ThreadPoolExecutor fastCallbackThreadPool = null;

    public void start() {
        // for fast callback
        fastCallbackThreadPool = new ThreadPoolExecutor(
                10,
                25,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000000),
                r -> new Thread(r, "jobCompleteHelper-fast-callbackThreadPool-" + r.hashCode()),
                new ThreadPoolExecutor.CallerRunsPolicy());
        fastCallbackThreadPool.allowCoreThreadTimeOut(false);

        // for callback
        callbackThreadPool = new ThreadPoolExecutor(
                20,
                25,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000000),
                r -> new Thread(r, "jobCompleteHelper-callbackThreadPool-" + r.hashCode()),
                new ThreadPoolExecutor.CallerRunsPolicy());
        callbackThreadPool.allowCoreThreadTimeOut(false);
    }

    public void toStop() {
        callbackThreadPool.shutdown();
        try {
            // 100毫秒轮训一次
            while (!callbackThreadPool.awaitTermination(50, TimeUnit.MILLISECONDS)) {
                logger.info("JobCompleteHelper callbackThreadPool thread wait stopping");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("JobCompleteHelper callbackThreadPool thread  stoppped");

        fastCallbackThreadPool.shutdown();
        try {
            // 100毫秒轮训一次
            while (!fastCallbackThreadPool.awaitTermination(50, TimeUnit.MILLISECONDS)) {
                logger.info("JobCompleteHelper fast-callbackThreadPool thread wait stopping");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("JobCompleteHelper fast-callbackThreadPool thread  stoppped");
    }


    // ---------------------- helper ----------------------

    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList, boolean compensate) {
        // 此中任务较大概率是参数来自父任务类型的任务，所有使用快速线程优先执行
        if (!CollectionUtils.isEmpty(callbackParamList) &&
                callbackParamList.get(0).getHandleMsg() != null &&
                callbackParamList.get(0).getHandleMsg().length > MIN_FAST_THRESHOLD) {
            fastCallbackThreadPool.execute(() -> {
                try {
                    for (HandleCallbackParam handleCallbackParam : callbackParamList) {
                        ReturnT<String> callbackResult = callback(handleCallbackParam, compensate);
                        logger.info("JobApiController.callback {}, handleCallback logId={}, callbackResult={}",
                                (callbackResult.getCode() == ReturnT.SUCCESS_CODE ? "success" : "fail"), handleCallbackParam.getLogId(), callbackResult);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("callback-error-in-run: ", e);
                }
            });
        } else {
            callbackThreadPool.execute(() -> {
                try {
                    for (HandleCallbackParam handleCallbackParam : callbackParamList) {
                        ReturnT<String> callbackResult = callback(handleCallbackParam, compensate);
                        logger.info("JobApiController.callback {},handleCallback logId={}, callbackResult={}",
                                (callbackResult.getCode() == ReturnT.SUCCESS_CODE ? "success" : "fail"), handleCallbackParam.getLogId(), callbackResult);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("callback-error-in-run: ", e);
                }
            });
        }

        return ReturnT.SUCCESS;
    }

    private ReturnT<String> callback(HandleCallbackParam handleCallbackParam, boolean compensate) {
        // valid log item
        JobLog log;
        try (HintManager manager = HintManager.getInstance()) {
            manager.setWriteRouteOnly();
            if (handleCallbackParam.getJobId() != null && handleCallbackParam.getJobId() > 0) {
                logger.info("callback logId={},jobId={}", handleCallbackParam.getLogId(), handleCallbackParam.getJobId());
                Integer jobGroup = JobAdminConfig.getAdminConfig().getJobInfoDao().loadGroupBy(handleCallbackParam.getJobId());
                log = JobAdminConfig.getAdminConfig().getJobLogDao().load(jobGroup, handleCallbackParam.getLogId());
            } else {
                logger.info("callback logId={},jobId is zero", handleCallbackParam.getLogId());
                log = JobAdminConfig.getAdminConfig().getJobLogDao().loadBy(handleCallbackParam.getLogId());
            }
        }
        if (log == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "log item not found.");
        }
        if (log.getHandleCode() > 0) {
            // avoid repeat callback, trigger child job etc
            return new ReturnT<>(ReturnT.FAIL_CODE, "log repeate callback.");
        }
        if (handleCallbackParam.getHandleCode() == ReturnT.SUCCESS_CODE) {
            // 设置为处理中,同时把childrenExecutorParams写入，主要是为了避免处理子任务突然中断，会导致父任务状态没有及时更新，同时也可能因此而遗漏子任务下发
            int ret = JobAdminConfig.getAdminConfig().getJobLogDao().update2Processing(log.getId(), log.getJobGroup(), 0, ReturnT.PROCESSING_CODE,
                    JacksonUtil.writeValueAsString(handleCallbackParam.getHandleMsg()), new Date());
            if (ret <= 0) {
                return ReturnT.SUCCESS;
            }
        }
        // handle msg
        StringBuilder handleMsg = new StringBuilder();
        if (log.getHandleMsg() != null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (handleCallbackParam.getHandleMsg() != null) {
            int length = handleCallbackParam.getHandleMsg().length;
            for (int idx = 0; idx < length; idx++) {
                handleMsg.append(handleCallbackParam.getHandleMsg()[idx]);
                if (idx < length - 1) {
                    handleMsg.append(",");
                }
            }
            log.setChildrenExecutorParams(JacksonUtil.writeValueAsString(handleCallbackParam.getHandleMsg()));
        }
        // success, save log
        log.setHandleTime(new Date());
        log.setHandleCode(handleCallbackParam.getHandleCode());
        log.setHandleMsg(handleMsg.toString());
        // 失败设置重试时间
        if (log.getHandleCode() != ReturnT.SUCCESS_CODE) {
            // load data
            JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(log.getJobId());

            // 失败时，无论是否需要重试，都写上此时间
            log.setExecutorFailTriggerTime(System.currentTimeMillis());
            if (jobInfo == null) {
                logger.warn(">>> trigger fail, jobId invalid，jobId={}", log.getJobId());
            } else if (log.getExecutorFailRetryCount() > 0) {
                // 重试次数>0 进行如下配置
                // 历史数据处理
                if (jobInfo.getRetryType() == RetryType.HISTORY_TYPE) {
                    // 历史数据，默认10s后重试
                    log.setExecutorFailTriggerTime(System.currentTimeMillis() + DEFAULT_RETRY_TIME);
                } else if (jobInfo.getRetryType() == RetryType.FIX_RATE_TYPE) {
                    long retryInterval;
                    try {
                        retryInterval = Long.parseLong(jobInfo.getRetryConf());
                    } catch (NumberFormatException e) {
                        logger.error("job={}，配置{}重试配置不能转换为数字", jobInfo.getId(), jobInfo.getRetryConf());
                        // 设置为默认值
                        retryInterval = 10;
                    }
                    log.setExecutorFailTriggerTime(System.currentTimeMillis() + retryInterval * 1000);
                } else if (jobInfo.getRetryType() == RetryType.CUSTOMER_TYPE) {
                    // 客户自定义
                    long retryInterval;
                    if (StringUtils.isEmpty(jobInfo.getRetryConf())) {
                        retryInterval = 10;
                    } else {
                        String[] confArr = jobInfo.getRetryConf().split(",");
                        // 配置的数组长度<重试次数，全部设置为默认10s
                        if (confArr.length < jobInfo.getExecutorFailRetryCount()) {
                            retryInterval = 10;
                        } else {
                            try {
                                // 此处有可能出现索引越界，比如中途修改了jobInfo的失败重试次数，加入原先jobInfo配置为4，生成日志为log的retry也是4，
                                // 如果重试开始前修改为2，则2-4索引越界，所以会出错，此种场景只需要按照默认10s重试就可以了
                                String conf = confArr[jobInfo.getExecutorFailRetryCount() - log.getExecutorFailRetryCount()];
                                retryInterval = Long.parseLong(conf);
                            } catch (Exception e) {
                                logger.error("job={}，配置{}重试配置不能转换为数字", jobInfo.getId(), jobInfo.getRetryConf(), e);
                                // 设置为默认值
                                retryInterval = 10;
                            }
                        }
                    }
                    log.setExecutorFailTriggerTime(System.currentTimeMillis() + retryInterval * 1000);
                } else if (jobInfo.getRetryType() == RetryType.EXPONENTIAL_BACK_OFF_TYPE) {
                    long retryInterval;
                    if (StringUtils.isEmpty(jobInfo.getRetryConf())) {
                        retryInterval = 10;
                    } else {
                        String[] confArr = jobInfo.getRetryConf().split(",");
                        // 配置的数组长度<重试次数，全部设置为默认10s
                        if (confArr.length != 2) {
                            retryInterval = 10;
                        } else {
                            try {
                                // 初始间隔
                                long initialInterval = Long.parseLong(confArr[0]);
                                //最大间隔
                                long maxInterval = Long.parseLong(confArr[1]);
                                int exp = jobInfo.getExecutorFailRetryCount() - log.getExecutorFailRetryCount() + 1;
                                // 指数运算
                                double retryIntervalEx = Math.pow(initialInterval, exp);
                                if (retryIntervalEx > maxInterval) {
                                    retryIntervalEx = maxInterval;
                                }
                                retryInterval = (long) retryIntervalEx;
                            } catch (NumberFormatException e) {
                                logger.error("job={}，配置EXPONENTIAL_BACK_OFF_TYPE{}重试配置不能转换为数字", jobInfo.getId(), jobInfo.getRetryConf());
                                // 设置为默认值
                                retryInterval = 10;
                            }
                        }
                    }
                    log.setExecutorFailTriggerTime(System.currentTimeMillis() + retryInterval * 1000);
                }
                if (!StringUtils.isEmpty(jobInfo.getAppName())) {
                    List<JobLogTag> jobLogTags;
                    try (HintManager manager = HintManager.getInstance()) {
                        manager.setWriteRouteOnly();
                        jobLogTags = JobAdminConfig.getAdminConfig().getXxlJobLogTagDao().loadAll(jobInfo.getAppName());
                    }
                    if (!CollectionUtils.isEmpty(jobLogTags)) {
                        for (JobLogTag jobLogTag : jobLogTags) {
                            if (!StringUtils.isEmpty(log.getHandleMsg()) && (log.getHandleMsg().toUpperCase().contains(jobLogTag.getTagName().toUpperCase()))) {
                                log.setTagName(jobLogTag.getTagName());
                            }
                        }
                    }
                }
            }
        }
        JobCompleter.updateHandleInfoAndFinish(log, compensate);
        return ReturnT.SUCCESS;
    }
}
