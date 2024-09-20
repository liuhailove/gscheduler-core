package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.complete.JobCompleter;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.TriggerParam;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import com.tc.gschedulercore.core.scheduler.JobScheduler;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.service.ExecutorBiz;
import com.tc.gschedulercore.util.DateUtil;
import com.tc.gschedulercore.util.ThrowableUtil;
import net.sf.ehcache.util.NamedThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务丢失监控
 *
 * @author honggang.liu
 */
public class JobLossMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobLossMonitorHelper.class.getSimpleName());

    /**
     * 随机
     */
    private static Random localRandom = new Random();

    @SuppressWarnings("PMD.JobWaitParentHelper")
    private ScheduledExecutorService jobLossMonitorPoolExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("JobLossMonitorHelper", true));

    private static JobLossMonitorHelper instance = new JobLossMonitorHelper();

    public static JobLossMonitorHelper getInstance() {
        return instance;
    }

    public void start() {
        jobLossMonitorPoolExecutor.scheduleWithFixedDelay(this::jobLossMonitor, 5, 60, TimeUnit.SECONDS);
    }

    public void jobLossMonitor() {
        try {
            List<Integer> jobGroupIdes = JobAdminConfig.getAdminConfig().getJobService().findAllGroupIdCached();
            for (int jobGroup : jobGroupIdes) {
                // 任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；
                Date lostTime = DateUtil.addMinutes(new Date(), -10);
                List<Long> lostJobIds;
                try (HintManager manager = HintManager.getInstance()) {
                    manager.setWriteRouteOnly();
                    lostJobIds = JobAdminConfig.getAdminConfig().getJobLogDao().findLostJobIds(jobGroup, lostTime);
                }
                if (CollectionUtils.isEmpty(lostJobIds)) {
                    continue;
                }
                for (Long logId : lostJobIds) {
                    // 最后一次自救
                    JobLog jobLog;
                    try (HintManager manager = HintManager.getInstance()) {
                        manager.setWriteRouteOnly();
                        jobLog = JobAdminConfig.getAdminConfig().getJobLogDao().load(jobGroup, logId);
                    }
                    logger.info("log lost,logId={},jobId={},jogGroup={}", jobLog.getId(), jobLog.getJobId(), jobLog.getJobGroup());
                    jobLog.setHandleTime(new Date());
                    // 优化，以便失败监控可以走到索引
                    jobLog.setExecutorFailTriggerTime(System.currentTimeMillis());
                    JobGroup group = JobAdminConfig.getAdminConfig().getJobService().loadGroupCached(jobLog.getJobGroup());
                    if (group == null || CollectionUtils.isEmpty(group.getRegistryList())) {
                        jobLog.setHandleCode(ReturnT.FAIL_CODE);
                        jobLog.setHandleMsg(I18nUtil.getString("joblog_lost_fail") + ": registryList is null");
                    } else {
                        JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(jobLog.getJobId());
                        if (jobInfo == null) {
                            logger.error("[{}] job info not exist", jobLog.getJobId());
                            jobLog.setHandleCode(ReturnT.FAIL_CODE);
                            jobLog.setHandleMsg(I18nUtil.getString("joblog_lost_fail") + ": job info not exist");
                        } else if (!jobInfo.isResultCheck()) {
                            jobLog.setHandleCode(ReturnT.FAIL_CODE);
                            jobLog.setHandleMsg(I18nUtil.getString("joblog_lost_fail"));
                        } else {
                            TriggerParam triggerParam = new TriggerParam();
                            triggerParam.setJobId(jobLog.getJobId());
                            triggerParam.setExecutorHandler(jobLog.getExecutorHandler());
                            triggerParam.setExecutorParams(jobLog.getExecutorParam());
                            triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
                            triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
                            triggerParam.setLogId(jobLog.getId());
                            triggerParam.setLogDateTime(jobLog.getTriggerTime().getTime());
                            triggerParam.setGlueType(jobInfo.getGlueType());
                            triggerParam.setGlueSource(jobInfo.getGlueSource());
                            triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
                            if (StringUtils.isEmpty(jobLog.getExecutorShardingParam())) {
                                triggerParam.setBroadcastIndex(0);
                                triggerParam.setBroadcastTotal(1);
                            } else {
                                String[] shardingArr = jobLog.getExecutorShardingParam().split("/");
                                try {
                                    triggerParam.setBroadcastIndex(Integer.parseInt(shardingArr[0]));
                                } catch (NumberFormatException e) {
                                    triggerParam.setBroadcastIndex(0);
                                }
                                try {
                                    triggerParam.setBroadcastTotal(Integer.parseInt(shardingArr[1]));
                                } catch (NumberFormatException e) {
                                    triggerParam.setBroadcastIndex(1);
                                }
                            }
                            ReturnT<String> runResult;
                            String address = "";
                            try {
                                address = group.getRegistryList().get(localRandom.nextInt(group.getRegistryList().size()));
                                ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(address);
                                runResult = executorBiz.checkResult(triggerParam);
                            } catch (Exception e) {
                                logger.error(">>trigger error, please check if the executor[{}] is running.", address, e);
                                runResult = new ReturnT<>(ReturnT.FAIL_CODE, ThrowableUtil.toString(e));
                            }
                            if (runResult.getCode() == ReturnT.SUCCESS_CODE) {
                                jobLog.setHandleCode(ReturnT.SUCCESS_CODE);
                                jobLog.setHandleMsg(I18nUtil.getString("job_log_check_return_success"));
                            } else {
                                jobLog.setHandleCode(ReturnT.FAIL_CODE);
                                jobLog.setHandleMsg(I18nUtil.getString("joblog_lost_fail") + "-msg:" + runResult.getMsg());
                            }
                        }
                    }
                    JobCompleter.updateHandleInfoAndFinish(jobLog, false);
                }
            }
        } catch (Exception e) {
            logger.error("job fail monitor thread error:", e);
        }
    }

    /**
     * 停止线程
     */
    public void toStop() {
        if (jobLossMonitorPoolExecutor == null) {
            return;
        }
        jobLossMonitorPoolExecutor.shutdown();
        logger.info("JobLossMonitorHelper callbackThreadPool thread  stopped");
    }
}
