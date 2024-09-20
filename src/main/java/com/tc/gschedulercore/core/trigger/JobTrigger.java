package com.tc.gschedulercore.core.trigger;

import com.google.common.util.concurrent.RateLimiter;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.TriggerParam;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.core.route.ExecutorRouteStrategyEnum;
import com.tc.gschedulercore.core.scheduler.JobScheduler;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.enums.ExecutorBlockStrategyEnum;
import com.tc.gschedulercore.enums.RetryType;
import com.tc.gschedulercore.enums.ShardingType;
import com.tc.gschedulercore.service.ExecutorBiz;
import com.tc.gschedulercore.util.IpUtil;
import com.tc.gschedulercore.util.ThrowableUtil;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * go-scheduler trigger
 * Created by honggang.liu on 17/7/13.
 */
public class JobTrigger {
    private static Logger logger = LoggerFactory.getLogger(JobTrigger.class.getSimpleName());

    /**
     * 连接拒绝固定字符串，如果trigger消息包含此数据，需要再次调度，
     * 此种问题产生的原因，往往是因为业务SDK的端口号没有及时回收导致的
     */
    private static final String CONNECT_REFUSED = "Connection refused (Connection refused)";

    /**
     * 任务未注册mgs，此处主要是为了应对'JobFailMonitorHelper'优化，
     * 当出现此错误msg时，让 executor_fail_trigger_time 赋值为当前值，以便可以被失败监控拉取出来
     */
    private static final String TASK_NOT_REGISTERED = "Task not registered";

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY = 3;

    /**
     * 最大分发阈值
     */
    private static final int MAX_DISPATCH_THRESHOLD = 5000;

    /**
     * job->ratelimiter map
     */
    private static final Map<Integer, RateLimiter> jobRateLimiterMap = new ConcurrentHashMap<>();

    /**
     * trigger job
     *
     * @param jobId
     * @param triggerType
     * @param failRetryCount        >=0: use this param
     *                              <0: use param from job info config
     * @param executorShardingParam
     * @param executorParam         null: use job param
     *                              not null: cover job param
     * @param addressList           null: use executor addressList
     *                              not null: cover
     * @param logTypeEnum           日志类型
     * @param instanceId            实例ID
     * @param parentLog             父任务ID
     */
    public static void trigger(int jobId,
                               TriggerTypeEnum triggerType,
                               int failRetryCount,
                               String executorShardingParam,
                               String executorParam,
                               String addressList,
                               Long parentLog,
                               LogTypeEnum logTypeEnum,
                               String instanceId,
                               String additionalParams,
                               String routeFlag) {

        logger.info("trigger jobId={},failRetryCount={},executorShardingParam={},executorParam={},addressList={},instanceId={}", jobId, failRetryCount, executorShardingParam, executorParam, addressList, instanceId);
        // load data
        JobInfo jobInfoCached = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(jobId);
        if (jobInfoCached == null) {
            logger.warn(">>> trigger fail, jobId invalid，jobId={}", jobId);
            return;
        }
        JobInfo jobInfo = new JobInfo();
        BeanUtils.copyProperties(jobInfoCached, jobInfo);
        if (executorParam != null) {
            jobInfo.setExecutorParam(executorParam);
        }
        int finalFailRetryCount = failRetryCount >= 0 ? failRetryCount : jobInfo.getExecutorFailRetryCount();
        JobGroup group = JobAdminConfig.getAdminConfig().getJobGroupDao().load(jobInfo.getJobGroup());
        // cover addressList
        if (addressList != null && !addressList.trim().isEmpty()) {
            group.setAddressType(1);
            group.setAddressList(addressList.trim());
        }
        // sharding param
        int[] shardingParam = null;
        if (!StringUtils.isEmpty(executorShardingParam)) {
            String[] shardingArr = executorShardingParam.split("/");
            if (shardingArr.length == 2 && isNumeric(shardingArr[0]) && isNumeric(shardingArr[1])) {
                shardingParam = new int[2];
                shardingParam[0] = Integer.parseInt(shardingArr[0]);
                shardingParam[1] = Integer.parseInt(shardingArr[1]);
            }
        }
        //路由标签优先级：group>jobInfo>openapi
        if (!org.springframework.util.StringUtils.isEmpty(jobInfo.getRouterFlag())) {
            routeFlag = jobInfo.getRouterFlag();
        }
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null)
                && group.getRegistryWithFlagByJobFlag(routeFlag) != null && !group.getRegistryWithFlagByJobFlag(routeFlag).isEmpty()
                && shardingParam == null) {
            // 第一个sharding分片的ID作为其他sharding的父亲，同时第一个分片需要继承父亲的id
            Long shardingParentId = parentLog;
            if (jobInfo.getShardingType() == ShardingType.CUSTOMER_DEFINE_NUM_TYPE) {
                for (int i = 0; i < jobInfo.getShardingNum(); i++) {
                    // 限流
                    RateLimiter rateLimiter = JobTrigger.createRateLimiter(jobInfo.getId(), jobInfo.getDispatchThreshold());
                    rateLimiter.acquire();
                    if (i == 0) {
                        shardingParentId = processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, jobInfo.getShardingNum(), shardingParentId, logTypeEnum, true, instanceId, additionalParams, routeFlag);
                    } else {
                        processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, jobInfo.getShardingNum(), shardingParentId, LogTypeEnum.SUB_LOG, false, instanceId, additionalParams, routeFlag);
                    }
                }
            } else {
                for (int i = 0; i < group.getRegistryWithFlagByJobFlag(routeFlag).size(); i++) {
                    // 限流
                    RateLimiter rateLimiter = JobTrigger.createRateLimiter(jobInfo.getId(), jobInfo.getDispatchThreshold());
                    rateLimiter.acquire();
                    if (i == 0) {
                        shardingParentId = processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, group.getRegistryWithFlagByJobFlag(routeFlag).size(), shardingParentId, logTypeEnum, true, instanceId, additionalParams, routeFlag);
                    } else {
                        processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, group.getRegistryWithFlagByJobFlag(routeFlag).size(), shardingParentId, LogTypeEnum.SUB_LOG, false, instanceId, additionalParams, routeFlag);
                    }
                }
            }
        } else {
            if (shardingParam == null) {
                shardingParam = new int[]{0, 1};
            }
            processTrigger(group, jobInfo, finalFailRetryCount, triggerType, shardingParam[0], shardingParam[1], parentLog, logTypeEnum, false, instanceId, additionalParams, routeFlag);
        }
    }

    private static boolean isNumeric(String str) {
        try {
            Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param group               job group, registry list may be empty
     * @param jobInfo             job信息
     * @param finalFailRetryCount 重试次数
     * @param triggerType         触发类型
     * @param index               sharding index
     * @param total               sharding index
     * @param parentLog           父logId
     * @param hasSub              是否有子任务
     * @param instanceId          实例ID
     * @param logTypeEnum         日志类型
     */
    private static Long processTrigger(JobGroup group, JobInfo jobInfo, int finalFailRetryCount, TriggerTypeEnum triggerType, int index, int total, long parentLog, LogTypeEnum logTypeEnum, boolean hasSub, String instanceId, String additionalParams, String routeFlag) {
        // 先数据check
        if (TriggerTypeEnum.SYSTEM_COMPENSATE_RETRY == triggerType) {
            // 备注：此处并不一定返回的是一个，比如如果一次分片中的的返回处理参数包含重复的，此处旧有可能是多个，此处取limit 1
            // 此处理逻辑，仅仅是为处理在项目重启时，发生正在分片处理的数据没有及时更新的特殊场景，所以一般不会走到这个逻辑
            JobLog existLog = JobAdminConfig.getAdminConfig().getJobLogDao().loadLog(group.getId(), jobInfo.getId(), instanceId, parentLog, jobInfo.getExecutorParam());
            if (null != existLog) {
                // 补偿任务需要发送seatalk告警
                JobAdminConfig.getAdminConfig().getJobAlarmer().compensateAlarm(jobInfo, existLog);
                logger.info("logId={},jobId={},SYSTEM_COMPENSATE_RETRY", existLog.getId(), existLog.getJobId());
                return existLog.getId();
            }
        }
        // param
        // block strategy
        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);
        // route strategy
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);
        String shardingParam = (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) ? String.valueOf(index).concat("/").concat(String.valueOf(total)) : null;

        // 1、save log-id
        JobLog jobLog = new JobLog();
        jobLog.setJobGroup(jobInfo.getJobGroup());
        jobLog.setJobId(jobInfo.getId());
        jobLog.setTriggerTime(new Date());
        jobLog.setParentLog(parentLog);
        jobLog.setLogType(logTypeEnum.getValue());
        jobLog.setTriggerType(triggerType.getValue());
        jobLog.setHasSub(hasSub);
        jobLog.setInstanceId(instanceId);
        jobLog.setJobName(jobInfo.getJobName());
        jobLog.setExecutorParam(jobInfo.getExecutorParam());
        if (jobInfo.isDelay()) {
            jobLog.setStartExecuteTimeInMs(System.currentTimeMillis() + jobInfo.getDelayInMs());
            // 提前写入
            jobLog.setExecutorShardingParam(shardingParam);
        } else {
            jobLog.setStartExecuteTimeInMs(0L);
        }
        // 设置重试次数
        jobLog.setExecutorFailRetryCount(finalFailRetryCount);

        // 此处增加重试，原因是jobLog的生成是根据snow算法生成的，
        // 而这种算法有可能重复，在重复的情况下，多个事务的插入有可能导致事务回滚
        // 通过重试+sleep尽最大努力重试成功
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                JobAdminConfig.getAdminConfig().getJobLogDao().save(jobLog);
                break;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("jobLog={},error={}", jobLog, e.getMessage());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                }
                // 尝试后最终失败，则发seatalk告警
                if (i == MAX_RETRY - 1) {
                    JobAdminConfig.getAdminConfig().getJobAlarmer().saveLogFailAlarm(jobInfo, jobLog, e.getMessage());
                }
            }
        }
        if (!(jobInfo.isDelay() && triggerType == TriggerTypeEnum.PARENT)) {
            postProcessing(group, jobInfo, finalFailRetryCount, triggerType, index, total, blockStrategy, executorRouteStrategyEnum, shardingParam, jobLog, additionalParams, routeFlag);
        }
        return jobLog.getId();
    }

    /**
     * postProcessing log写入后的后置处理
     *
     * @param group                     执行期
     * @param jobInfo                   任务信息
     * @param finalFailRetryCount       最终失败重试次数
     * @param triggerType               触发类型
     * @param index                     下标
     * @param total                     全部分片
     * @param blockStrategy             阻塞策略
     * @param executorRouteStrategyEnum 路由策略
     * @param shardingParam             分配参数
     * @param jobLog                    log信息
     */
    public static void postProcessing(JobGroup group, JobInfo jobInfo, int finalFailRetryCount, TriggerTypeEnum triggerType, int index, int total, ExecutorBlockStrategyEnum blockStrategy, ExecutorRouteStrategyEnum executorRouteStrategyEnum, String shardingParam, JobLog jobLog, String additionalParams, String routeFlag) {
        logger.info(">>trigger start, jobId={},jobLogId:{}", jobLog.getJobId(), jobLog.getId());
        // 2、init trigger-param
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId());
        triggerParam.setAppname(jobInfo.getAppName());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        triggerParam.setAdditionalParams(jobInfo.getAdditionalParams());
        if (additionalParams != null && !additionalParams.isEmpty()) {
            triggerParam.setAdditionalParams(additionalParams);
        }
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(jobLog.getId());
        triggerParam.setParentLog(jobLog.getParentLog());
        triggerParam.setInstanceId(jobLog.getInstanceId());
        triggerParam.setLogDateTime(jobLog.getTriggerTime().getTime());
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
        triggerParam.setBroadcastIndex(index);
        triggerParam.setBroadcastTotal(total);
        triggerParam.setBusinessStartExecutorToleranceThresholdInMin(jobInfo.getStartExecutorToleranceThresholdInMin());
        // 3、init address
        String address = null;
        ReturnT<String> routeAddressResult = null;
        //保险起见，在比较一次routeflag
        if (!org.springframework.util.StringUtils.isEmpty(jobInfo.getRouterFlag())) {
            routeFlag = jobInfo.getRouterFlag();
        }
        if (group.getRegistryWithFlagByJobFlag(routeFlag) != null && !group.getRegistryWithFlagByJobFlag(routeFlag).isEmpty()) {
            if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
                if (index < 0) {
                    index = 0;
                }
                address = group.getRegistryWithFlagByJobFlag(routeFlag).get(index % group.getRegistryWithFlagByJobFlag(routeFlag).size());
            } else {
                routeAddressResult = executorRouteStrategyEnum.getRouter().route(triggerParam, group.getRegistryWithFlagByJobFlag(routeFlag));
                if (routeAddressResult.getCode() == ReturnT.SUCCESS_CODE) {
                    address = routeAddressResult.getContent();
                }
            }
        } else {
            routeAddressResult = new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobconf_trigger_address_empty"));
        }

        // 4、trigger remote executor
        ReturnT<String> triggerResult;
        if (address != null) {
            triggerResult = runExecutor(triggerParam, address);
        } else {
            triggerResult = new ReturnT<>(ReturnT.FAIL_CODE, null);
        }

        // 在请求拒绝时，主动增加失败转移
        if (triggerResult.getCode() == ReturnT.FAIL_CODE &&
                !StringUtils.isEmpty(triggerResult.getMsg()) &&
                triggerResult.getMsg().contains(CONNECT_REFUSED)) {
            // 采用失败转移策略
            routeAddressResult = ExecutorRouteStrategyEnum.FAILOVER.getRouter().route(triggerParam, group.getRegistryWithFlagByJobFlag(routeFlag));
            if (routeAddressResult.getCode() == ReturnT.SUCCESS_CODE) {
                address = routeAddressResult.getContent();
                triggerResult = runExecutor(triggerParam, address);
            }
        }

        // 5、collection trigger info
        StringBuilder triggerMsgSb = new StringBuilder();
        triggerMsgSb.append(I18nUtil.getString("jobconf_trigger_type")).append("：").append(triggerType.getTitle());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_admin_adress")).append("：").append(IpUtil.getIp());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regtype")).append("：").append((group.getAddressType() == 0) ? I18nUtil.getString("jobgroup_field_addressType_0") : I18nUtil.getString("jobgroup_field_addressType_1"));
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regaddress")).append("：").append(group.getRegistryWithFlagByJobFlag(jobInfo.getRouterFlag()));
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorRouteStrategy")).append("：").append(executorRouteStrategyEnum.getTitle());
        if (shardingParam != null) {
            triggerMsgSb.append("(").append(shardingParam).append(")");
        }
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorBlockStrategy")).append("：").append(blockStrategy.getTitle());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_timeout")).append("：").append(jobInfo.getExecutorTimeout());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorFailRetryCount")).append("：").append(finalFailRetryCount);
        triggerMsgSb.append("<br>").append(I18nUtil.getString("joblog_field_executorParams")).append("：").append(triggerParam.getExecutorParams());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("joblog_field_additionalParams")).append("：").append(triggerParam.getAdditionalParams());
        triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>").append(I18nUtil.getString("jobconf_trigger_run")).
                append("<<<<<<<<<<< </span><br>").
                append((routeAddressResult != null && routeAddressResult.getMsg() != null) ? routeAddressResult.getMsg() + "<br><br>" : "").
                append(triggerResult.getMsg() != null ? triggerResult.getMsg() : "");

        // 6、save log trigger-info
        jobLog.setExecutorAddress(address);
        jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
        jobLog.setExecutorParam(jobInfo.getExecutorParam());
        jobLog.setExecutorShardingParam(shardingParam);

        // 如果下发就失败了，那么就立刻重试
        if (triggerResult.getCode() == ReturnT.FAIL_CODE) {
            // 设置执行失败重试时间
            jobLog.setExecutorFailTriggerTime(System.currentTimeMillis());
        }

        // 在请求拒绝时，主动增加失败重试
        if (finalFailRetryCount == 0 &&
                triggerType != TriggerTypeEnum.RETRY &&
                triggerResult.getCode() == ReturnT.FAIL_CODE &&
                !StringUtils.isEmpty(triggerResult.getMsg()) &&
                triggerResult.getMsg().contains(CONNECT_REFUSED)) {
            // 设置重试次数
            jobLog.setExecutorFailRetryCount(1);
            jobLog.setExecutorFailTriggerTime(System.currentTimeMillis() + 10 * 1000);
        } else {
            // 设置重试次数
            jobLog.setExecutorFailRetryCount(finalFailRetryCount);
        }
        jobLog.setTriggerCode(triggerResult.getCode());
        jobLog.setTriggerMsg(triggerMsgSb.toString());
        // 设置执行阈值
        if (jobInfo.getExecutorThreshold() > 0) {
            jobLog.setExecutorThresholdTimeout(System.currentTimeMillis() + jobInfo.getExecutorThreshold() * 1000L);
        } else {
            jobLog.setExecutorThresholdTimeout(0);
        }
        JobAdminConfig.getAdminConfig().getJobLogDao().updateTriggerInfo(jobLog);
        logger.debug(">>trigger end, jobId:{}", jobLog.getId());
        // 增加脚本告警日志相关
        List<AlarmRule> alarmRuleList = JobAdminConfig.getAdminConfig().getAlarmRuleService().findByJobGroupAndJobId(jobLog.getJobGroup(), jobLog.getJobId());
        if (!CollectionUtils.isEmpty(alarmRuleList)) {
            for (AlarmRule alarmRule : alarmRuleList) {
                try {
                    List<AlarmScriptItem> alarmScriptItemList = JobAdminConfig.getAdminConfig().getAlarmRuleService().queryItemsByAlarmRuleAndJob(alarmRule.getId(), jobLog.getJobId());
                    if (!CollectionUtils.isEmpty(alarmScriptItemList)) {
                        for (AlarmScriptItem alarmScriptItem : alarmScriptItemList) {
                            if (!StringUtils.isEmpty(alarmScriptItem.getRetryConf())) {
                                long retryInterval = 0;
                                if (alarmScriptItem.getRetryType() == RetryType.FIX_RATE_TYPE) {
                                    try {
                                        retryInterval = Long.parseLong(alarmScriptItem.getRetryConf());
                                    } catch (NumberFormatException e) {
                                        logger.error("job={}，配置{}重试配置不能转换为数字", alarmScriptItem.getId(), alarmScriptItem.getRetryConf());
                                        // 设置为默认值
                                        retryInterval = 10;
                                    }
                                } else if (alarmScriptItem.getRetryType() == RetryType.CUSTOMER_TYPE) {
                                    // 客户自定义
                                    if (StringUtils.isEmpty(alarmScriptItem.getRetryConf())) {
                                        retryInterval = 10;
                                    } else {
                                        String[] confArr = alarmScriptItem.getRetryConf().split(",");
                                        // 配置的数组长度<重试次数，全部设置为默认10s
                                        if (confArr.length < alarmScriptItem.getScriptRetryCount()) {
                                            retryInterval = 10;
                                        } else {
                                            try {
                                                String conf = confArr[0];
                                                retryInterval = Long.parseLong(conf);
                                            } catch (Exception e) {
                                                logger.error("job={}，配置{}重试配置不能转换为数字", jobInfo.getId(), jobInfo.getRetryConf(), e);
                                                // 设置为默认值
                                                retryInterval = 10;
                                            }
                                        }
                                    }
                                }
                                JobLogScript jobLogScript = new JobLogScript();
                                jobLogScript.setJobId(jobLog.getJobId());
                                jobLogScript.setJobGroup(jobLog.getJobGroup());
                                jobLogScript.setJobLogId(jobLog.getId());
                                jobLogScript.setAlarmRuleId(alarmRule.getId());
                                jobLogScript.setAlarmScriptId(alarmScriptItem.getId());
                                jobLogScript.setScriptRetryCount(alarmScriptItem.getScriptRetryCount());
                                jobLogScript.setScriptRetryConf(alarmScriptItem.getRetryConf());
                                jobLogScript.setRetryType(alarmScriptItem.getRetryType());
                                // 计算第一次检查时间
                                jobLogScript.setScriptCheckTriggerTime(System.currentTimeMillis() + retryInterval * 1000L);
                                JobAdminConfig.getAdminConfig().getJobLogScriptDao().save(jobLogScript);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.error("parse script conf error,jobId={},logId={}", jobLog.getJobId(), jobLog.getId(), e);
                }
            }
        }
    }

    /**
     * run executor
     *
     * @param triggerParam 触发参数
     * @param address      地址
     * @return 执行结果
     */
//    @Timed(percentiles = {0.5, 0.80, 0.90, 0.99, 0.999})
    public static ReturnT<String> runExecutor(TriggerParam triggerParam, String address) {
        //统计请求次数
        JobAdminConfig.getAdminConfig().getRegistry().counter("scheduleRunExecutor").increment();
        Timer timer = Timer.builder("runExecutorTimeCost")
                .tag("uri", "runExecutor")
                .tag("region", JobAdminConfig.getAdminConfig().getEnv())
                .tag("appname", triggerParam.getAppname())
                .tag("executorHandler", triggerParam.getExecutorHandler())
                .register(JobAdminConfig.getAdminConfig().getRegistry());

        Timer.Sample sample = Timer.start(JobAdminConfig.getAdminConfig().getRegistry());


        ReturnT<String> runResult;
        try {
            ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(address);
            runResult = executorBiz.run(triggerParam);
        } catch (Exception e) {
            logger.error(">>trigger error, please check if the executor[{}] is running.", address, e);
            runResult = new ReturnT<>(ReturnT.FAIL_CODE, ThrowableUtil.toString(e));
        }
        String runResultSB = I18nUtil.getString("jobconf_trigger_run") + "：" + "<br>address：" + address +
                "<br>code：" + runResult.getCode() +
                "<br>msg：" + runResult.getMsg();
        runResult.setMsg(runResultSB);
        sample.stop(timer);

        return runResult;
    }

    /**
     * 创建限流器
     *
     * @param jobId             下发阈值
     * @param dispatchThreshold 下发阈值
     * @return 限流器
     */
    public static RateLimiter createRateLimiter(Integer jobId, double dispatchThreshold) {
        RateLimiter rateLimiter = jobRateLimiterMap.get(jobId);
        if (rateLimiter == null || rateLimiter.getRate() != dispatchThreshold) {
            synchronized (JobTrigger.class) {
                rateLimiter = jobRateLimiterMap.get(jobId);
                if (rateLimiter == null) {
                    rateLimiter = RateLimiter.create(dispatchThreshold > 0 ? dispatchThreshold : MAX_DISPATCH_THRESHOLD);
                    jobRateLimiterMap.put(jobId, rateLimiter);
                }
            }
        }
        return rateLimiter;
    }

}
