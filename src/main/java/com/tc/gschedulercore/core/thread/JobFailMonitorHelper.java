package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import com.tc.gschedulercore.core.trigger.LogTypeEnum;
import com.tc.gschedulercore.core.trigger.TriggerTypeEnum;
import com.tc.gschedulercore.core.util.I18nUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.tc.gschedulercore.controller.JobLogController.TASK_TERMINATION_FLAG_FALSE;
import static com.tc.gschedulercore.controller.JobLogController.TASK_TERMINATION_FLAG_TRUE;
import static com.tc.gschedulercore.core.thread.JobESHelper.PRE_READ_MS;

/**
 * 失败重试
 *
 * @author honggang.liu
 */
public class JobFailMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobFailMonitorHelper.class.getSimpleName());

    /**
     * 运行中错误
     */
    private static final String RUNNING_ERROR_MSG = "There are tasks running";

    /**
     * ringDat数据，是预加载数据
     */
    private static final Map<Integer, List<String>> ringData = new ConcurrentHashMap<>();


    /**
     * 7天时间
     */
    private static final int SEVEN_DAYS_IN_MILLIS = 7 * 24 * 60 * 60 * 1000;

    private static JobFailMonitorHelper instance = new JobFailMonitorHelper();

    public static JobFailMonitorHelper getInstance() {
        return instance;
    }

    // ---------------------- monitor ----------------------

    private Thread monitorThread;
    private Thread ringThread;
    private volatile boolean toStop = false;
    private volatile boolean ringThreadToStop = false;

    /**
     * 预处理个数
     */
    private static final int PRE_READ_COUNT = 1000;


    public void start() {
        monitorThread = new Thread(() -> {
            long nowTime = 0;
            long minExecutorFailTriggerTime = 0;
            // monitor
            while (!toStop) {
                try {
                    // 预加载5s的数据
                    // 1、pre read
                    nowTime = System.currentTimeMillis();
                    minExecutorFailTriggerTime = nowTime - SEVEN_DAYS_IN_MILLIS;
                    List<Integer> jobGroupIdes = JobAdminConfig.getAdminConfig().getJobService().findAllGroupIdCached();
                    for (int jobGroup : jobGroupIdes) {
                        List<Long> failLogIds;
                        try (HintManager manager = HintManager.getInstance()) {
                            manager.setWriteRouteOnly();
                            // 查找下发到执行器后执行失败的任务
                            failLogIds = JobAdminConfig.getAdminConfig().getJobLogDao().findFailJobLogIds(jobGroup, minExecutorFailTriggerTime, nowTime + PRE_READ_MS, PRE_READ_COUNT);
                        }
                        if (failLogIds != null && !failLogIds.isEmpty()) {
                            for (long failLogId : failLogIds) {
                                logger.info("JobFailMonitorHelper failLogId={}", failLogId);
                                JobLog log;
                                try (HintManager manager = HintManager.getInstance()) {
                                    manager.setWriteRouteOnly();
                                    log = JobAdminConfig.getAdminConfig().getJobLogDao().load(jobGroup, failLogId);
                                }
                                // 如果执行时间为null，那么就是立刻执行
                                if (log.getExecutorFailTriggerTime() == 0) {
                                    log.setExecutorFailTriggerTime(nowTime);
                                }
                                // 当前时间>要执行的时间+预读时间或者>要执行的时间，说明已经错过，那么立刻执行
                                if (nowTime > log.getExecutorFailTriggerTime() + PRE_READ_MS || nowTime >= log.getExecutorFailTriggerTime()) {
                                    fireAlarm(log);
                                } else {
                                    // 存放到另外一个ring中，注意，虽然ring中的数据和findFailJobLogIds的数据有可能重复，但是log执行时会加锁，所以只可能成功一个
                                    // 1、make ring second
                                    int ringSecond = (int) ((log.getExecutorFailTriggerTime() / 1000) % 60);
                                    // 2、push time ring
                                    pushTimeRing(ringSecond, jobGroup, log.getId());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error("job fail monitor thread error:", e);
                    }
                }
                try {
                    long cost = System.currentTimeMillis() - nowTime;
                    // Wait seconds, align second
                    if (cost < 2000) {
                        TimeUnit.MILLISECONDS.sleep(2000 - System.currentTimeMillis() % 1000);
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info("job fail monitor thread stop");

        });
        monitorThread.setDaemon(true);
        monitorThread.setName("JobFailMonitorHelper");
        monitorThread.start();


        // ring thread
        ringThread = new Thread(() -> {
            // align second
            try {
                TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
            } catch (InterruptedException e) {
                if (!ringThreadToStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            while (!ringThreadToStop) {
                try {
                    // second data
                    List<String> ringItemData = new ArrayList<>();
                    // 避免处理耗时太长，跨过刻度，向前校验一个刻度；
                    int nowSecond = Calendar.getInstance().get(Calendar.SECOND);
                    for (int i = 0; i < 2; i++) {
                        List<String> tmpData = ringData.remove((nowSecond + 60 - i) % 60);
                        if (tmpData != null) {
                            ringItemData.addAll(tmpData);
                        }
                    }
                    // ring trigger
                    logger.debug(">>fail retry time-ring beat : {} = {}", nowSecond, Arrays.asList(ringItemData));
                    if (!ringItemData.isEmpty()) {
                        // do trigger
                        for (String failJobGroupLogId : ringItemData) {
                            String[] jobGroupAndLog = failJobGroupLogId.split(":", 2);
                            // do trigger
                            JobLog log = JobAdminConfig.getAdminConfig().getJobLogDao().load(Integer.parseInt(jobGroupAndLog[0]), Long.parseLong(jobGroupAndLog[1]));
                            fireAlarm(log);
                        }
                        // clear
                        ringItemData.clear();
                    }
                } catch (Exception e) {
                    if (!ringThreadToStop) {
                        logger.error("JobScheduleHelper#ringThread error:", e);
                    }
                }

                // next second, align second
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (InterruptedException e) {
                    if (!ringThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info("JobScheduleHelper#ringThread stop");
        });
        ringThread.setDaemon(true);
        ringThread.setName("JobScheduleHelper#ringThread");
        ringThread.start();
    }

    /**
     * 触发告警
     *
     * @param log log
     * @return
     */
    private void fireAlarm(JobLog log) {
        logger.info("fireAlarm,{}", log);
        // lock log
        int lockRet = JobAdminConfig.getAdminConfig().getJobLogDao().updateAlarmStatus(log.getId(), log.getJobGroup(), 0, -1);
        if (lockRet < 1) {
            return;
        }
        JobInfo info = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(log.getJobId());
        if (info == null) {
            logger.error("fireAlarm get job id[{}] is null", log.getJobId());
            return;
        }
        // 1、fail retry monitor 手动终止的任务不再重试
        if (log.getExecutorFailRetryCount() > 0 && log.getTaskTerminationFlag() == TASK_TERMINATION_FLAG_FALSE) {

            // 父logid <=0，说明是第一次重拾，这说明当前log.Id就是父ID，否则就是第一次之后的重试，则log.parentId就是父logId
            long parentLog;
            if (log.getTriggerType() == TriggerTypeEnum.RETRY.getValue()) {
                // 说明已经重试过
                parentLog = log.getParentLog();
            } else {
                // log.getExecutorFailRetryCount() == info.getExecutorFailRetryCount(),说明一定是第一次重试
                parentLog = log.getId();
                // 说明父任务有子任务
                log.setHasSub(true);
            }
            JobTriggerPoolHelper.trigger(info.getJobGroup(), log.getJobId(), TriggerTypeEnum.RETRY, (log.getExecutorFailRetryCount() - 1), log.getExecutorShardingParam(), log.getExecutorParam(), null, parentLog, LogTypeEnum.SUB_LOG, log.getInstanceId(), "", "");
            String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>" + I18nUtil.getString("jobconf_trigger_type_retry") + "<<<<<<<<<<< </span><br>";
            log.setTriggerMsg(log.getTriggerMsg() + retryMsg);
            JobAdminConfig.getAdminConfig().getJobLogDao().updateTriggerInfo(log);
        }
        if (log.getTaskTerminationFlag() == TASK_TERMINATION_FLAG_TRUE) {
            logger.info("user manual click stop task button,not need retry run task,{}", log);
        }
        // 2、fail alarm monitor
        // 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
        int newAlarmStatus;
        boolean alarmCheck = (!StringUtils.isEmpty(info.getAlarmEmail())) || !StringUtils.isEmpty(info.getAlarmSeatalk());
        boolean finalSendCheck = (!info.isFinalFailedSendAlarm()) || (info.isFinalFailedSendAlarm() && log.getExecutorFailRetryCount() == 0);
        boolean alarmSilenceCheck = System.currentTimeMillis() > info.getAlarmSilenceTo();
        // 如果仅最后一次发送告警为真，则只有重试次数==0时才发送告警；为假时，每次都告警
        if (alarmCheck && finalSendCheck && alarmSilenceCheck) {
            //目的：单机串行报的错误可以被关闭
            logger.info("begin trigger,JobName:{},isTaskRunningAlarm:{} ", info.getJobName(), info.getTaskRunningAlarm());
            boolean containRunningErr = !StringUtils.isEmpty(log.getTriggerMsg()) && log.getTriggerMsg().contains(RUNNING_ERROR_MSG) && Boolean.TRUE.equals(info.getTaskRunningAlarm());
            boolean notContainRunningErr = StringUtils.isEmpty(log.getTriggerMsg()) || (!log.getTriggerMsg().contains(RUNNING_ERROR_MSG));

            if (containRunningErr || notContainRunningErr) {
                logger.info("trigger job fail alarm email send error, JobName:{}", info.getJobName());
                boolean alarmResult = JobAdminConfig.getAdminConfig().getJobAlarmer().alarm(info, log);
                newAlarmStatus = alarmResult ? 2 : 3;
            } else {
                newAlarmStatus = 1;
            }
        } else {
            newAlarmStatus = 1;
        }
        JobAdminConfig.getAdminConfig().getJobLogDao().updateAlarmStatus(log.getId(), log.getJobGroup(), -1, newAlarmStatus);
        // 更新告警静默到
        if (newAlarmStatus != JobLog.ALARM_STATUS_NO_NEED) {
            JobAdminConfig.getAdminConfig().getJobInfoDao().updateAlarmSilenceTo(log.getJobId(), System.currentTimeMillis() + (long) info.getAlarmSilence() * 60 * 1000);
        }
    }

    /**
     * 添加到时间轮对接
     *
     * @param ringSecond 时间轮秒
     * @param logId      日志ID
     */
    private void pushTimeRing(int ringSecond, long jobGroup, long logId) {
        // push async ring
        List<String> ringItemData = ringData.computeIfAbsent(ringSecond, k -> new ArrayList<>());
        ringItemData.add(jobGroup + ":" + logId);
        logger.debug(">>schedule fail retry push time-ring : {}= {}", ringSecond, Collections.singletonList(ringItemData));
    }

    public void toStop() {
        toStop = true;
        // interrupt and wait
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        // if has ring data
        boolean hasRingData = false;
        if (!ringData.isEmpty()) {
            for (int second : ringData.keySet()) {
                List<String> tmpData = ringData.get(second);
                if (tmpData != null && !tmpData.isEmpty()) {
                    hasRingData = true;
                    break;
                }
            }
        }
        if (hasRingData) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // stop ring (wait job-in-memory stop)
        ringThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (ringThread.getState() != Thread.State.TERMINATED) {
            // interrupt and wait
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
