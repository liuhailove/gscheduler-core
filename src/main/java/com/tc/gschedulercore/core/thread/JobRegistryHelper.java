package com.tc.gschedulercore.core.thread;

import com.google.common.util.concurrent.RateLimiter;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.RegistryParam;
import com.tc.gschedulercore.core.dto.ReportDelayParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.enums.RegistryConfig;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.tc.gschedulercore.core.model.JobGroup.SCHEDULE_BASE_PFB;

/**
 * job registry instance
 *
 * @author honggang.liu 2016-10-02 19:10:24
 */
public class JobRegistryHelper {
    private static Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class.getSimpleName());

    private static JobRegistryHelper instance = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return instance;
    }

    private ThreadPoolExecutor registryOrRemoveThreadPool = null;
    private ThreadPoolExecutor reportDelayThreadPool = null;
    private Thread registryMonitorThread;
    private ThreadPoolExecutor metricHandlerThreadPool;
    private volatile boolean toStop = false;

    private static final String ALARM_ITEM_KEY = "alarmItem";
    private static final String MATCH_VAL_KEY = "matchValue";

    public void start() {
        // for registry or remove
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    logger.warn(">>registry or remove too fast, match threadpool rejected handler(run now).");
                });

        // 上报延迟执行线程池
        reportDelayThreadPool = new ThreadPoolExecutor(
                1,
                1,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "JobRegistryMonitorHelper-reportDelayThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    logger.warn(">>reportDelayThreadPool too fast, match threadpool rejected handler(run now).");
                });


        metricHandlerThreadPool = new ThreadPoolExecutor(
                2,
                5,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                r -> new Thread(r, "metricHandlerThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    logger.warn("metricHandlerThreadPool too fast, match threadpool rejected handler(run now).");
                });


        // for monitor
        registryMonitorThread = new Thread(() -> {
            while (!toStop) {
                try {
                    // auto registry group
                    List<JobGroup> groupList;
                    try (HintManager manager = HintManager.getInstance()) {
                        manager.setWriteRouteOnly();
                        groupList = JobAdminConfig.getAdminConfig().getJobGroupDao().findByAddressType(0);
                    }
                    if (groupList != null && !groupList.isEmpty()) {
                        // remove dead address (admin/executor)
                        List<Integer> ids;
                        try (HintManager manager = HintManager.getInstance()) {
                            manager.setWriteRouteOnly();
                            ids = JobAdminConfig.getAdminConfig().getJobRegistryDao().findDead(DateUtils.addSeconds(new Date(), RegistryConfig.DEAD_TIMEOUT * (-1)));
                        }
                        if (ids != null && !ids.isEmpty()) {
                            JobAdminConfig.getAdminConfig().getJobRegistryDao().removeDead(ids);
                        }

                        // fresh online address (admin/executor)
                        HashMap<String, List<String>> appAddressMap = new HashMap<>(4);
                        // 刷新flag
                        Map<String, List<String>> appAddressWithFlagMap = new HashMap<>(4);
                        List<JobRegistry> list;
                        try (HintManager manager = HintManager.getInstance()) {
                            manager.setWriteRouteOnly();
                            list = JobAdminConfig.getAdminConfig().getJobRegistryDao().findAll(DateUtils.addSeconds(new Date(), RegistryConfig.DEAD_TIMEOUT * (-1)));
                        }
                        if (list != null) {
                            for (JobRegistry item : list) {
                                if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                    String appname = item.getRegistryKey();
                                    List<String> registryList = appAddressMap.get(appname);
                                    if (registryList == null) {
                                        registryList = new ArrayList<>();
                                    }
                                    if (!registryList.contains(item.getRegistryValue())) {
                                        registryList.add(item.getRegistryValue());
                                    }
                                    appAddressMap.put(appname, registryList);
                                    // 有标签的注册列表
                                    List<String> registryWithFlagList = appAddressWithFlagMap.get(appname);
                                    if (registryWithFlagList == null) {
                                        registryWithFlagList = new ArrayList<>();
                                    }
                                    if (!registryWithFlagList.contains(item.getRegistryValue() + JobGroup.SPLIT_FLAG + item.getRouterFlag())) {
                                        //如果业务base环境没有上报标签，就设置一个base标签
                                        if (item.getRouterFlag().equals("")) {
                                            registryWithFlagList.add(item.getRegistryValue() + JobGroup.SPLIT_FLAG + SCHEDULE_BASE_PFB);
                                        } else {
                                            registryWithFlagList.add(item.getRegistryValue() + JobGroup.SPLIT_FLAG + item.getRouterFlag());
                                        }
                                    }
                                    appAddressWithFlagMap.put(appname, registryWithFlagList);
                                }
                            }
                        }

                        // fresh group address
                        for (JobGroup group : groupList) {
                            String appName = group.getAppname().replace("\t", "").replace("\n", "").replace("\r", "");
                            List<String> registryList = appAddressMap.get(appName);
                            String addressListStr = null;
                            if (!CollectionUtils.isEmpty(registryList)) {
                                Collections.sort(registryList);
                                StringBuilder addressListSb = new StringBuilder();
                                for (String item : registryList) {
                                    addressListSb.append(item).append(",");
                                }
                                addressListStr = addressListSb.toString();
                                addressListStr = addressListStr.substring(0, addressListStr.length() - 1);
                            }
                            group.setAddressList(addressListStr);

                            List<String> registryWithFlagList = appAddressWithFlagMap.get(appName);
                            String addressWithFlagListStr = null;
                            if (!CollectionUtils.isEmpty(registryWithFlagList)) {
                                Collections.sort(registryWithFlagList);
                                StringBuilder addressListSb = new StringBuilder();
                                for (String item : registryWithFlagList) {
                                    addressListSb.append(item).append(",");
                                }
                                addressWithFlagListStr = addressListSb.toString();
                                addressWithFlagListStr = addressWithFlagListStr.substring(0, addressWithFlagListStr.length() - 1);
                            }
                            group.setAddressListWithFlag(addressWithFlagListStr);

                            group.setUpdateTime(new Date());
                            JobAdminConfig.getAdminConfig().getJobGroupDao().update(group);
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(">>job registry monitor thread error:", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(">>job registry monitor thread error:", e);
                    }
                }
            }
            logger.info(">>job registry monitor thread stop");
        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();
    }

    public void toStop() {
        toStop = true;

        // stop registryOrRemoveThreadPool
        registryOrRemoveThreadPool.shutdownNow();

        // stop reportDelayThreadPool
        reportDelayThreadPool.shutdownNow();

        // stop monitir (interrupt and wait)
        registryMonitorThread.interrupt();
        try {
            registryMonitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }


    // ---------------------- helper ----------------------
    public ReturnT<String> registry(RegistryParam registryParam) {
        logger.info("job registry param,{}", registryParam);

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }
        if (!StringUtils.hasLength(registryParam.getRouterFlag())) {
            registryParam.setRouterFlag("");
        }
        if (registryParam.getRouterFlag().length() > 255) {
            registryParam.setRouterFlag(registryParam.getRouterFlag().substring(0, 255));
        }
        registryParam.setRouterFlag(registryParam.getRouterFlag().replace(",", "_"));
        // async execute
        registryOrRemoveThreadPool.execute(() -> {
            int ret = JobAdminConfig.getAdminConfig().getJobRegistryDao().registryUpdate(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue(), registryParam.getCpuStat(), registryParam.getMemoryStat(), registryParam.getLoadStat(), registryParam.getRouterFlag(), new Date());
            if (ret < 1) {
                JobAdminConfig.getAdminConfig().getJobRegistryDao().registrySave(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue(), registryParam.getCpuStat(), registryParam.getMemoryStat(), registryParam.getLoadStat(), registryParam.getRouterFlag(), new Date());
                // fresh
                freshGroupRegistryInfo(registryParam);
            }
        });
        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryRemove(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(() -> {
            int ret = JobAdminConfig.getAdminConfig().getJobRegistryDao().registryDelete(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
            if (ret > 0) {
                // fresh
                freshGroupRegistryInfo(registryParam);
            }
        });

        return ReturnT.SUCCESS;
    }

    public ReturnT<String> reportDelay(ReportDelayParam reportDelayParam) {
        // async execute
        reportDelayThreadPool.execute(() -> {
            JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached((int) reportDelayParam.getJobId());
            if (jobInfo == null) {
                logger.error("jobInfo not exist,request msg={}", reportDelayParam);
                return;
            }
            // 延迟消息记录
            DelayLog delayLog = new DelayLog();
            delayLog.setJobLogId(reportDelayParam.getLogId());
            delayLog.setLogDate(new Date(reportDelayParam.getLogDateTim()));
            delayLog.setStartExecutorDate(new Date(reportDelayParam.getCurrentDateTim()));
            delayLog.setJobId((int) reportDelayParam.getJobId());
            delayLog.setStartExecutorToleranceThresholdInMin(jobInfo.getStartExecutorToleranceThresholdInMin());
            delayLog.setJobGroup(jobInfo.getJobGroup());
            delayLog.setJobName(jobInfo.getJobName());
            delayLog.setTimeElapseInMs((int) (reportDelayParam.getCurrentDateTim() - reportDelayParam.getLogDateTim()));
            delayLog.setAddress(reportDelayParam.getAddress());
            delayLog.setAlarmStatus(DelayLog.ALARM_STATUS_DEFAULT);
            delayLog.setAddTime(new Date());
            delayLog.setUpdateTime(new Date());
            JobAdminConfig.getAdminConfig().getXxlDelayLogDao().save(delayLog);
            // 消息通知
            // 2、 alarm monitor
            // 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
            int newAlarmStatus;
            boolean alarmCheck = (!org.apache.commons.lang3.StringUtils.isEmpty(jobInfo.getAlarmEmail())) || !org.apache.commons.lang3.StringUtils.isEmpty(jobInfo.getAlarmSeatalk());
            if (alarmCheck) {
                boolean alarmResult = JobAdminConfig.getAdminConfig().getJobAlarmer().runTaskDelayAlarm(jobInfo, delayLog);
                newAlarmStatus = alarmResult ? DelayLog.ALARM_STATUS_SUCCESS : DelayLog.ALARM_STATUS_FAILED;
            } else {
                newAlarmStatus = DelayLog.ALARM_STATUS_NO_NEED;
            }
            JobAdminConfig.getAdminConfig().getXxlDelayLogDao().updateAlarmStatus(delayLog.getId(), delayLog.getJobGroup(), DelayLog.ALARM_STATUS_DEFAULT, newAlarmStatus);
        });

        return ReturnT.SUCCESS;
    }

    /**
     * 日志metric处理
     *
     * @param metrics metric数据
     * @return 处理结果
     */
    public ReturnT<String> handleMetrics(String metrics) {
        logger.info("handleMetrics,{}", metrics);
        metricHandlerThreadPool.submit(() -> {
            String metricsCp = String.valueOf(Arrays.copyOf(metrics.toCharArray(), metrics.length()));
            String[] lines = metricsCp.split("\n");
            for (String line : lines) {
                try {
                    // 增加限流，避免For循环过快
                    RateLimiter rateLimiter = RateLimiter.create(500);
                    rateLimiter.acquire();
                    LogMetric metric = LogMetric.fromThinString(line);
                    metric.setAddTime(new Date());
                    if (metric.getJobLogId().equals(0L)) {
                        continue;
                    }
                    JobLog jobLog;
                    if (metric.getJobId() == null || metric.getJobId().equals(0)) {
                        // 强制走主库，避免主从延迟
                        try (HintManager manager = HintManager.getInstance()) {
                            manager.setWriteRouteOnly();
                            jobLog = JobAdminConfig.getAdminConfig().getJobLogDao().loadBy(metric.getJobLogId());
                        }
                        if (jobLog == null) {
                            logger.warn("log={} not exists", metric.getJobLogId());
                            continue;
                        }
                        metric.setJobId(jobLog.getJobId());
                    } else {
                        JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(metric.getJobId());
                        jobLog = JobAdminConfig.getAdminConfig().getJobLogDao().load(jobInfo.getJobGroup(), metric.getJobLogId());
                    }
                    // 写入metric
                    JobAdminConfig.getAdminConfig().getLogMetricDao().save(metric);
                    // 处理告警
                    handleAlarm(jobLog, metric);
                } catch (Exception e) {
                    logger.info("handleMetrics exception,metrics={},error={}", metrics, e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 告警处理
     *
     * @param jobLog 执行日志
     * @param metric 过程日志
     */
    private void handleAlarm(JobLog jobLog, LogMetric metric) {
        List<AlarmRule> alarmRules = JobAdminConfig.getAdminConfig().getAlarmRuleService().findByJobGroupAndJobId(jobLog.getJobGroup(), jobLog.getJobId());
        if (CollectionUtils.isEmpty(alarmRules)) {
            return;
        }
        for (AlarmRule alarmRule : alarmRules) {
            Map<String, Object> matchMap = isMatch(alarmRule, metric);
            if (!CollectionUtils.isEmpty(matchMap)) {
                // 写入通知消息
                NotifyInfo notifyInfo = new NotifyInfo();
                notifyInfo.setApp(alarmRule.getJobGroupName());
                notifyInfo.setAlarmRuleId(alarmRule.getId());
                notifyInfo.setAlarmName(alarmRule.getAlarmName());
                notifyInfo.setAlarmLevel(alarmRule.getAlarmLevel());
                AlarmItem alarmItem = (AlarmItem) matchMap.get(ALARM_ITEM_KEY);
                notifyInfo.setAlarmType(alarmItem.getAlarmType());
                notifyInfo.setNotifyContent(generateNotifyContent((String) matchMap.get(MATCH_VAL_KEY), AlarmItem.queryEffectTypeDesc(alarmItem.getEffectType()), alarmItem.getObservationVal()));
                notifyInfo.setNotifyUrl(alarmRule.getNotifyUrl());
                notifyInfo.setNotifyUsers(alarmRule.getNotifyUsers());
                notifyInfo.setGmtCreate(new Date());
                JobAdminConfig.getAdminConfig().getNotifyInfoDao().save(notifyInfo);
                // 告警
                JobAdminConfig.getAdminConfig().getJobAlarmer().doNotifyAlarm(notifyInfo);
                // 跳出循环
                break;
            }
        }
    }

    /**
     * 生成告警内容
     *
     * @param val         触发告警的值
     * @param relation    生效关系
     * @param observation 观测值
     * @return 告警内容
     */
    private String generateNotifyContent(String val, String relation, String observation) {
        return "\n ----告警观测值：" + observation + "\n" +
                " ----告警生效关系：" + relation + "\n" +
                " ----告警触发值：" + val;
    }

    /**
     * 判断过程日志是否匹配规则
     *
     * @param alarmRule 告警规则
     * @param metric    执行日志
     * @return 匹配返回true, 否则返回false
     */
    private Map<String, Object> isMatch(AlarmRule alarmRule, LogMetric metric) {
        List<AlarmItem> alarmItems = JobAdminConfig.getAdminConfig().getAlarmRuleService().queryByAlarmRule(alarmRule.getId());
        if (CollectionUtils.isEmpty(alarmItems)) {
            return Collections.emptyMap();
        }
        Map<String, Object> matchValue = new HashMap<>(4);
        for (AlarmItem item : alarmItems) {
            switch (item.getAlarmType()) {
                case AlarmItem.LOG_PROCESS_MSG:
                    if (AlarmRule.TRIGGER_CONDITION_ANY.equals(alarmRule.getTriggerCondition())) {
                        if (isItemMatch(metric.getMsg(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getMsg());
                            return matchValue;
                        }
                    } else {
                        if (isItemMatch(metric.getMsg(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getMsg());

                        } else {
                            return Collections.emptyMap();
                        }
                    }
                    break;
                case AlarmItem.LOG_PROCESS_KEY1:
                    if (AlarmRule.TRIGGER_CONDITION_ANY.equals(alarmRule.getTriggerCondition())) {
                        if (isItemMatch(metric.getKey1(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getKey1());
                            return matchValue;
                        }
                    } else {
                        if (isItemMatch(metric.getKey1(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getKey1());

                        } else {
                            return Collections.emptyMap();
                        }
                    }
                    break;
                case AlarmItem.LOG_PROCESS_VAL1:
                    if (AlarmRule.TRIGGER_CONDITION_ANY.equals(alarmRule.getTriggerCondition())) {
                        if (isItemMatch(metric.getValue1(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getValue1());
                            return matchValue;
                        }
                    } else {
                        if (isItemMatch(metric.getValue1(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getValue1());

                        } else {
                            return Collections.emptyMap();
                        }
                    }
                    break;
                case AlarmItem.LOG_PROCESS_KEY2:
                    if (AlarmRule.TRIGGER_CONDITION_ANY.equals(alarmRule.getTriggerCondition())) {
                        if (isItemMatch(metric.getKey2(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getKey2());
                            return matchValue;
                        }
                    } else {
                        if (isItemMatch(metric.getKey2(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getKey2());

                        } else {
                            return Collections.emptyMap();
                        }
                    }
                    break;
                case AlarmItem.LOG_PROCESS_VAL2:
                    if (AlarmRule.TRIGGER_CONDITION_ANY.equals(alarmRule.getTriggerCondition())) {
                        if (isItemMatch(metric.getValue2(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getValue2());
                            return matchValue;
                        }
                    } else {
                        if (isItemMatch(metric.getValue2(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getValue2());

                        } else {
                            return Collections.emptyMap();
                        }
                    }
                    break;
                case AlarmItem.LOG_PROCESS_KEY3:
                    if (AlarmRule.TRIGGER_CONDITION_ANY.equals(alarmRule.getTriggerCondition())) {
                        if (isItemMatch(metric.getKey3(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getKey3());
                            return matchValue;
                        }
                    } else {
                        if (isItemMatch(metric.getKey3(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getKey3());

                        } else {
                            return Collections.emptyMap();
                        }
                    }
                    break;
                case AlarmItem.LOG_PROCESS_VAL3:
                    if (AlarmRule.TRIGGER_CONDITION_ANY.equals(alarmRule.getTriggerCondition())) {
                        if (isItemMatch(metric.getValue3(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getValue3());
                            return matchValue;
                        }
                    } else {
                        if (isItemMatch(metric.getValue3(), item.getObservationVal(), item.getEffectType())) {
                            matchValue.put(ALARM_ITEM_KEY, item);
                            matchValue.put(MATCH_VAL_KEY, metric.getValue3());

                        } else {
                            return Collections.emptyMap();
                        }
                    }
                    break;
                default:
                    return Collections.emptyMap();
            }
        }
        return matchValue;
    }

    /**
     * 判断是否匹配
     *
     * @param msg         匹配的值
     * @param observerVal 观测值
     * @param effectType  匹配类型
     */

    private boolean isItemMatch(String msg, String observerVal, Integer effectType) {
        // 如果需要匹配的值为空，或者观测值为空，则一定不匹配
        if (msg == null || observerVal == null) {
            return false;
        }
        switch (effectType) {
            case AlarmItem.EXACTLY_MATCH_TYPE:
                return msg.equals(observerVal);
            case AlarmItem.PREFIX_MATCH_TYPE:
                return msg.startsWith(observerVal);
            case AlarmItem.POSTFIX_MATCH_TYPE:
                return msg.endsWith(observerVal);
            case AlarmItem.CONTAIN_MATCH_TYPE:
                return msg.contains(observerVal);
            case AlarmItem.REGEXP_MATCH_TYPE:
                return msg.matches(observerVal);
            default:
                return false;
        }
    }


    private void freshGroupRegistryInfo(RegistryParam registryParam) {
        // 对于执行器摘除，并不需要立刻把对应的执行任务也标记为失败，
        // 此动作的完成，是通过com.xxl.job.admin.core.thread.JobCompleteHelper监控的，
        // 此处会把 @see findLostJobIds
        // 调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线。
        // 执行注册删除后，执行器中的注册地址需要立刻删除，避免再次下发时下发到无效的地址中，
        // 此处的实现原先是通过心跳调度实现的，然而这个时间太久了，所以修改为立刻移除
        if (RegistryConfig.RegistType.EXECUTOR.name().equals(registryParam.getRegistryGroup())) {
            JobGroup group;
            try (HintManager manager = HintManager.getInstance()) {
                manager.setWriteRouteOnly();
                group = JobAdminConfig.getAdminConfig().getJobGroupDao().loadByName(registryParam.getRegistryKey());
            }
            if (group == null || CollectionUtils.isEmpty(group.getRegistryList())) {
                return;
            }
            // addressType==1，为手动录入，不必更新
            if (group.getAddressType() == 1) {
                return;
            }
            // 注册地址更新
            List<String> registryList = new ArrayList<>();
            for (String registryValue : group.getRegistryList()) {
                if (!registryValue.equals(registryParam.getRegistryValue())) {
                    registryList.add(registryValue);
                }
            }
            group.setAddressList(org.apache.commons.lang3.StringUtils.join(registryList, ','));

            // flag更新
            List<String> registryWithFlagList = new ArrayList<>();
            for (String registryWithFlagValue : group.getRegistryWithFlagList()) {
                if (!registryWithFlagValue.equals(registryParam.getRegistryValue())) {
                    registryWithFlagList.add(registryWithFlagValue);
                }
            }
            group.setAddressListWithFlag(org.apache.commons.lang3.StringUtils.join(registryWithFlagList, ','));
            group.setUpdateTime(new Date());
            JobAdminConfig.getAdminConfig().getJobGroupDao().update(group);
        }
    }
}
