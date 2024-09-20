package com.tc.gschedulercore.core.complete;

import com.google.common.util.concurrent.RateLimiter;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.context.GsJobContext;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import com.tc.gschedulercore.core.route.ExecutorRouteStrategyEnum;
import com.tc.gschedulercore.core.thread.JobTriggerPoolHelper;
import com.tc.gschedulercore.core.trigger.JobTrigger;
import com.tc.gschedulercore.core.trigger.LogTypeEnum;
import com.tc.gschedulercore.core.trigger.TriggerTypeEnum;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.core.util.JacksonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author honggang.liu 2020-10-30 20:43:10
 */
public class JobCompleter {
    private static final Logger logger = LoggerFactory.getLogger(JobCompleter.class.getSimpleName());
    /**
     * 消息处理的最大长度
     */
    private static final int MAX_HANDLE_MSG = 45000;

    private JobCompleter() {
    }

    /**
     * common fresh handle entrance (limit only once)
     *
     * @param jobLog 执行日志
     * @return 返回处理结果
     */
    public static void updateHandleInfoAndFinish(JobLog jobLog, boolean compensate) {
        // 初始化为不需要下发
        jobLog.setDispatchSub(JobLog.DISPATCH_SUB_NO_NEED);
        // finish
        finishJob(jobLog, compensate);
        // text最大64kb 避免长度过长
        if (!StringUtils.isEmpty(jobLog.getHandleMsg()) && jobLog.getHandleMsg().length() > MAX_HANDLE_MSG) {
            jobLog.setHandleMsg(jobLog.getHandleMsg().substring(0, MAX_HANDLE_MSG));
        }
        // 计算执行时间
        if (jobLog.getHandleTime() != null && jobLog.getTriggerTime() != null) {
            jobLog.setExecuteTime((jobLog.getHandleTime().getTime() - jobLog.getTriggerTime().getTime()) / 1000);
        }
        // fresh handle
        JobAdminConfig.getAdminConfig().getJobLogDao().updateHandleInfo(jobLog);
        // 处理等待父任务结束
        handleBeginAfterParent(jobLog, compensate);
    }

    public static void handleBeginAfterParent(JobLog xxlJobLog, boolean compensate) {
        if (GsJobContext.HANDLE_COCE_SUCCESS != xxlJobLog.getHandleCode()) {
            return;
        }
        JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(xxlJobLog.getJobId());
        if (jobInfo == null || jobInfo.getChildJobId() == null || jobInfo.getChildJobId().trim().length() == 0) {
            return;
        }
        String[] childJobIds = jobInfo.getChildJobId().split(",");

        List<JobInfo> beginAfterChildren = new ArrayList<>();
        for (String jobId : childJobIds) {
            if (jobId == null || jobId.trim().isEmpty()) {
                continue;
            }
            int childJobId = isNumeric(jobId) ? Integer.parseInt(jobId) : -1;
            if (childJobId <= 0) {
                continue;
            }
            JobInfo childJobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(childJobId);
            if (childJobInfo == null) {
                continue;
            }
            // 如果子任务配置是：需要在父任务结束后开始
            if (childJobInfo.isBeginAfterParent()) {
                beginAfterChildren.add(childJobInfo);
            }
        }
        boolean canStart = !beginAfterChildren.isEmpty();
        for (JobInfo beginAfterChild : beginAfterChildren) {
            if (!canHandleBeginAfterParents(xxlJobLog, beginAfterChild, jobInfo)) {
                canStart = false;
                break;
            }
        }
        if (canStart) {
            for (JobInfo beginAfterChild : beginAfterChildren) {
                handleBeginAfterParents(xxlJobLog, compensate, beginAfterChild, jobInfo);
            }
        }
    }

    /**
     * do somethind to finish job
     */
    private static void finishJob(JobLog jobLog, boolean compensate) {
        if (GsJobContext.HANDLE_COCE_SUCCESS != jobLog.getHandleCode()) {
            return;
        }
        JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(jobLog.getJobId());
        if (jobInfo == null || jobInfo.getChildJobId() == null || jobInfo.getChildJobId().trim().length() == 0) {
            return;
        }
        // 1、handle success, to trigger child job
        logger.info(">> simple log msg,logId={},jobGroup={},jobId={},jobName={},executorAddress={},executorParam={},executorShardingParam={}," +
                        "triggerCode={},triggerMsg={},handleCode={},handleTime={}",
                jobLog.getId(), jobLog.getJobGroup(), jobLog.getJobId(), jobLog.getJobName(), jobLog.getExecutorAddress(), jobLog.getExecutorParam(), jobLog.getExecutorShardingParam(),
                jobLog.getTriggerCode(), jobLog.getTriggerMsg(), jobLog.getHandleCode(), jobLog.getHandleTime());
        boolean hasSub = false;
        // 如果任务是分片任务，并且分片数大于1，并且当前调度的执行日志的分片参数以"0"开头，如(0/10),则说明是起一个任务，这样可以把此任务当成其他分片的父任务
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST.name().equals(jobInfo.getExecutorRouteStrategy())
                && jobInfo.getShardingNum() > 1
                && !StringUtils.isEmpty(jobLog.getExecutorShardingParam())
                && jobLog.getExecutorShardingParam().startsWith("0")) {
            hasSub = true;
            logger.info(">> first sharding handle，logId={}", jobLog.getId());
        }
        StringBuilder triggerChildMsg = new StringBuilder("<br><br><span style=\"color:#00c0ef;\" > >>" + I18nUtil.getString("jobconf_trigger_child_run") + "<<<<<<<<<<< </span><br>");
        String[] childJobIds = jobInfo.getChildJobId().split(",");
        for (int i = 0; i < childJobIds.length; i++) {
            int childJobId = (childJobIds[i] != null && !childJobIds[i].trim().isEmpty() && isNumeric(childJobIds[i])) ? Integer.parseInt(childJobIds[i]) : -1;
            if (childJobId <= 0) {
                triggerChildMsg.append(MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"),
                        (i + 1),
                        childJobIds.length,
                        childJobIds[i]));
                continue;
            }
            ReturnT<String> triggerChildResult = ReturnT.SUCCESS;
            JobInfo childJobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(childJobId);
            if (childJobInfo == null) {
                logger.error("error,child jobid={},not exits", childJobId);
                triggerChildResult = ReturnT.FAIL;
                triggerChildResult.setMsg("child jobid =" + childJobId + ", not exits");
                triggerChildMsg.append("child jobid =").append(childJobId).append(", not exits");
                continue;
            }
            // 此处主要为了兼容历史数据，历史数据没有parents
            if (childJobInfo.isBeginAfterParent() && childJobInfo.getParents().isEmpty()) {
                // 如果其没有父任务，此种不可能，因为到此处一定存在子任务，存在这种情况，应该是数据存在问题
                logger.error("error,child jobid={},has not parent", childJobId);
                triggerChildResult = ReturnT.FAIL;
                triggerChildResult.setMsg("child jobid=" + childJobId + ",has not parent");
                triggerChildMsg.append("child jobid=").append(childJobId).append(",has not parent");
                break;
            }
            if (!childJobInfo.isBeginAfterParent() && childJobInfo.getParamFromParent()) {
                if (!StringUtils.isEmpty(jobLog.getChildrenExecutorParams())) {
                    String[] childExecutorParams = null;
                    try {
                        childExecutorParams = JacksonUtil.readValue(jobLog.getChildrenExecutorParams(), String[].class);
                    } catch (Exception e) {
                        logger.error("paramFromParent log id [{}] params parser error", jobLog.getId(), e);
                    }
                    logger.info("handle paramFromParent sub task,logid={}", jobLog.getId());
                    if (childExecutorParams == null || childExecutorParams.length == 0) {
                        continue;
                    }
                    for (String executorParam : childExecutorParams) {
                        // 限流
                        long startTime = System.currentTimeMillis();
                        RateLimiter rateLimiter = JobTrigger.createRateLimiter(childJobInfo.getId(), childJobInfo.getDispatchThreshold());
                        double waitTime = rateLimiter.acquire();
                        long endTime = System.currentTimeMillis();
                        if (logger.isDebugEnabled()) {
                            logger.debug("parentLog={},executorParam={},waitTime={},elapse={}", jobLog.getId(), executorParam, waitTime, endTime - startTime);
                        }
                        JobTriggerPoolHelper.trigger(childJobInfo.getJobGroup(), childJobId, compensate ? TriggerTypeEnum.SYSTEM_COMPENSATE_RETRY : TriggerTypeEnum.PARENT, -1, null, executorParam, null, jobLog.getId(), LogTypeEnum.SUB_LOG, jobLog.getInstanceId(), "", "");
                    }
                    hasSub = true;
                }

            } else if (!childJobInfo.isBeginAfterParent()) {
                JobTriggerPoolHelper.trigger(childJobInfo.getJobGroup(), childJobId, compensate ? TriggerTypeEnum.SYSTEM_COMPENSATE_RETRY : TriggerTypeEnum.PARENT, -1, null, null, null, jobLog.getId(), LogTypeEnum.SUB_LOG, jobLog.getInstanceId(), "", "");
                hasSub = true;
            }

            // 子任务配置为等待父任务结束，那么设置为当前log有子任务
            if (childJobInfo.isBeginAfterParent()) {
                hasSub = true;
            }
            // add msg
            triggerChildMsg.append(MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"),
                    (i + 1),
                    childJobIds.length,
                    childJobIds[i],
                    (triggerChildResult.getCode() == ReturnT.SUCCESS_CODE ? I18nUtil.getString("system_success") : I18nUtil.getString("system_fail")),
                    triggerChildResult.getMsg()));

        }
        // child不为空，表明有子任务，所以更新父任务hashSub,如果jobLog已经设置为有子任务了，那就不用在设置了，不存在从"有子任务"到"无子任务的场景"
        if (!jobLog.isHasSub()) {
            jobLog.setHasSub(hasSub);
        }
        jobLog.setHandleMsg(jobLog.getHandleMsg() + triggerChildMsg);
        // 2、fix_delay trigger next
        // on the way
    }

    private static boolean canHandleBeginAfterParents(JobLog jobLog, JobInfo childJobInfo, JobInfo jobInfo) {
        // 大类
        // 只有一个父任务
        // -------场景1：如果父任务只有一个，并且没有父子任务传参数,并且父任务不是分片任务或者分片只有一个，那么直接开始子任务就可以了
        // -------场景2：如果父任务只有一个，并且是父子任务传参数,并且父任务不是分片任务或者分片只有一个，那么直接开始子任务就可以了
        // -------场景3：如果父任务只有一个，并且没有父子任务传参数，但是父任务是分片任务，并且分片数量>1,那么需要等待全部分片结束后才可以开始子任务
        // -------场景4：如果父任务只有一个，有父子任务传参数，父任务是分片任务，并且分片数量>1，那么需要等待全部分片结束后才可以开始子任务
        // 多于一个父任务
        // -------场景5：如果父任务有多个个，没有父子任务传参数,父任务不是分片任务或者父任务是分片任务，并且分片数量==1，那么需要等待全部父亲结束后才可以开始子任务
        // -------场景6：如果父任务有多个，没有父子任务传参数,父任务部分是分片任务并且分片数量>1,那么需要等待全部父亲结束后才可以开始子任务
        // -------场景7：如果父任务有多个，有父子任务传参数,直接拒绝，此中配置为错误配置
        // 父任务是否已经完成的标识，完成后相关的子任务不需要在判断
        if (childJobInfo.getParents().size() == 1) {
            // 场景1：如果父任务只有一个，并且没有父子任务传参数,并且父任务不是分片任务或者分片只有一个，那么直接开始子任务就可以了
            if (!childJobInfo.getParamFromParent()) {
                if (!ExecutorRouteStrategyEnum.SHARDING_BROADCAST.name().equals(jobInfo.getExecutorRouteStrategy()) ||
                        (ExecutorRouteStrategyEnum.SHARDING_BROADCAST.name().equals(jobInfo.getExecutorRouteStrategy()) && jobLog.getShardNum() == 1)) {
                    // 需要考虑当前任务是否为参数来自父任务，如果是，需要等待当前父任务全部结束
                    // 如果父任务没有配置参数来自父任务或者虽然配置了，但是根本没有父任务，则直接开始
                    int firstJobId = JobAdminConfig.getAdminConfig().getJobLogDao().findFirstJobByInstance(jobLog.getInstanceId());
                    if (!jobInfo.getParamFromParent() || firstJobId == jobLog.getJobId()) {
                        int ret = JobAdminConfig.getAdminConfig().getJobLogDao().updateDispatchSub(jobLog.getInstanceId(), Collections.singletonList(jobLog.getJobId()), jobLog.getJobGroup(), JobLog.DISPATCH_SUB_NO_NEED, JobLog.DISPATCH_SUB_SUCCESS);
                        logger.info("child job has not config paramFromParent and only one sharding, meet start child process. childJobId=[{}],parentJobId=[{}],logId={},instanceId={}", childJobInfo.getId(), jobInfo.getId(), jobLog.getId(), jobLog.getInstanceId());
                        return ret >= 1;
                    } else {
                        // 查询父亲的父亲，如果父亲的父亲是分片传参，需要取出全部分片分割求和，并和父亲任务对应的log的总数对比，相等，则可以开始
                        // 如果父亲的父亲是普通任务，则需要取出对应的log参数分割求和，并和父亲任务对应的log的总数对比，相等，则可以开始
                        // 此中情况下父亲任务只有一个，如果为多个，则没办法传参数，拒绝
                        JobInfo grandpaJobInfo = JobAdminConfig.getAdminConfig().getJobInfoDao().loadById(jobInfo.getParents().get(0));
                        List<JobLog> succeedJobLogList = JobAdminConfig.getAdminConfig().getJobLogDao().queryList(jobLog.getInstanceId(), grandpaJobInfo.getJobGroup(), grandpaJobInfo.getId(), Collections.singletonList(ReturnT.SUCCESS_CODE));
                        // 不可能为空，因为存在爷爷任务，爷爷任务一定执行过了
                        int expectedCount = 0;
                        // 分片传参
                        if (jobInfo.getParamFromParent()) {
                            for (JobLog grandpaLog : succeedJobLogList) {
                                String[] childExecutorParams = null;
                                try {
                                    childExecutorParams = JacksonUtil.readValue(grandpaLog.getChildrenExecutorParams(), String[].class);
                                } catch (Exception e) {
                                    logger.error("paramFromParent log id [{}] params parser error", grandpaLog.getId(), e);
                                }
                                logger.info("handle paramFromParent sub task, logId={}", grandpaLog.getId());
                                if (childExecutorParams != null) {
                                    expectedCount += childExecutorParams.length;
                                }
                            }
                        } else {
                            // 仅分片
                            expectedCount = jobLog.getShardNum();
                        }
                        // 统计父亲任务的执行个数
                        int count = JobAdminConfig.getAdminConfig().getJobLogDao().countStatusShardingBy(jobLog.getInstanceId(), jobInfo.getJobGroup(), jobInfo.getId(), Collections.singletonList(ReturnT.SUCCESS_CODE));
                        if (expectedCount == count) {
                            // 更新
                            int ret = JobAdminConfig.getAdminConfig().getJobLogDao().updateDispatchSub(jobLog.getInstanceId(), Collections.singletonList(jobLog.getJobId()), jobInfo.getJobGroup(), JobLog.DISPATCH_SUB_NO_NEED, JobLog.DISPATCH_SUB_SUCCESS);
                            if (ret >= 1) {
                                logger.info("child job has not config paramFromParent and only one sharding, but his grandpa config paramFromParent,meet start child process. childJobId=[{}],parentJobId=[{}],logId={},instanceId={}", childJobInfo.getId(), jobInfo.getId(), jobLog.getId(), jobLog.getInstanceId());
                                // 需要拿出全部子任务执行
                                return true;
                            }
                        }
                    }
                } else {
                    // 场景3：如果父任务只有一个，并且没有父子任务传参数，但是父任务是分片任务，并且分片数量>1,那么需要等待全部分片结束后才可以开始子任务
                    // 查看父任务对应的分片是否全部结束
                    // 统计的为SUCESS和Process（200，201）

                    // 如果父亲是分片，父亲的父亲是分片或者是传参数，都拒绝，过于复杂，不讨论
                    int count = JobAdminConfig.getAdminConfig().getJobLogDao().countStatusShardingBy(jobLog.getInstanceId(), jobLog.getJobGroup(), jobLog.getJobId(), Collections.singletonList(ReturnT.SUCCESS_CODE));
                    // 由于当前的log还没有更新为成功，因此此处只需要count==分片数量就可以启动子任务了
                    if (count == jobLog.getShardNum()) {
                        // 按照instanceId更新，一次运行只可能成功一次，可以保证只运行一次
                        int ret = JobAdminConfig.getAdminConfig().getJobLogDao().updateDispatchSub(jobLog.getInstanceId(), Collections.singletonList(jobLog.getJobId()), jobLog.getJobGroup(), JobLog.DISPATCH_SUB_NO_NEED, JobLog.DISPATCH_SUB_SUCCESS);
                        if (ret >= 1) {
                            logger.info("child job has not config paramFromParent and has multi sharding, meet start child process. childJobId=[{}],parentJobId=[{}],logId={},instanceId={}", childJobInfo.getId(), jobInfo.getId(), jobLog.getId(), jobLog.getInstanceId());
                            return true;
                        }
                    } else {
                        logger.info("child job has multi sharding log, waiting other job log process ok. childJobId=[{}], parentJobId=[{}], logId={}, instanceId={}", childJobInfo.getId(), jobInfo.getId(), jobLog.getId(), jobLog.getInstanceId());
                    }
                }
            } else {
                // 场景2：如果父任务只有一个，并且是父子任务传参数,并且父任务不是分片任务或者分片只有一个，那么直接开始子任务就可以了
                if (!ExecutorRouteStrategyEnum.SHARDING_BROADCAST.name().equals(jobInfo.getExecutorRouteStrategy()) ||
                        (ExecutorRouteStrategyEnum.SHARDING_BROADCAST.name().equals(jobInfo.getExecutorRouteStrategy()) && jobInfo.getShardingNum() == 1)) {
                    if (StringUtils.isEmpty(jobLog.getChildrenExecutorParams())) {
                        return true;
                    }
                    String[] childExecutorParams = null;
                    try {
                        childExecutorParams = JacksonUtil.readValue(jobLog.getChildrenExecutorParams(), String[].class);
                    } catch (Exception e) {
                        logger.error("paramFromParent log id [{}] params parser error", jobLog.getId(), e);
                    }
                    logger.info("handle paramFromParent sub task, logId={}", jobLog.getId());
                    if (childExecutorParams == null || childExecutorParams.length == 0) {
                        return true;
                    }
                    int ret = JobAdminConfig.getAdminConfig().getJobLogDao().updateDispatchSub(jobLog.getInstanceId(), Collections.singletonList(jobLog.getJobId()), jobLog.getJobGroup(), JobLog.DISPATCH_SUB_NO_NEED, JobLog.DISPATCH_SUB_SUCCESS);
                    return ret >= 1;
                } else {
                    // 场景4：如果父任务只有一个，有父子任务传参数，父任务是分片任务，并且分片数量>1，那么需要等待全部分片结束后才可以开始子任务
                    // 查看父任务对应的分片是否全部结束
                    int count = JobAdminConfig.getAdminConfig().getJobLogDao().countStatusShardingBy(jobLog.getInstanceId(), jobLog.getJobGroup(), jobLog.getJobId(), Collections.singletonList(ReturnT.SUCCESS_CODE));
                    // 由于当前的log还没有更新为成功，因此此处只需要count==分片数量就可以启动子任务了
                    if (count >= jobLog.getShardNum()) {
                        int ret = JobAdminConfig.getAdminConfig().getJobLogDao().updateDispatchSub(jobLog.getInstanceId(), Collections.singletonList(jobLog.getJobId()), jobLog.getJobGroup(), JobLog.DISPATCH_SUB_NO_NEED, JobLog.DISPATCH_SUB_SUCCESS);
                        return ret >= 1;
                    }
                }
            }
        } else if (!childJobInfo.getParamFromParent()) {
            // 多父任务下，必须要有共同的起点，如果没有，则拒绝此类任务配置
            // 场景5：如果父任务有多个，没有父子任务传参数，如果父任务是分片任务，那么需要等待全部的分片完成，如果是普通任务，那么需要等待普通任务完成
            // 剔除无效父任务，虽然当前任务可能有多个父任务，父子之间也可能配置了依赖关系，但是当前任务不一定都和父任务相关
            int firstJobId = JobAdminConfig.getAdminConfig().getJobLogDao().findFirstJobByInstance(jobLog.getInstanceId());
            List<JobInfo> usageJobList = new ArrayList<>();
            findUsageParentJob(firstJobId, childJobInfo.getId(), usageJobList);
            boolean scene5Cond = true;
            for (JobInfo parentJob : usageJobList) {
                if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST.name().equals(parentJob.getExecutorRouteStrategy())) {
                    // 查看父任务对应的分片是否全部结束
                    int count = JobAdminConfig.getAdminConfig().getJobLogDao().countStatusShardingBy(jobLog.getInstanceId(), parentJob.getJobGroup(), parentJob.getId(), Collections.singletonList(ReturnT.SUCCESS_CODE));
                    JobLog parentLog = JobAdminConfig.getAdminConfig().getJobLogDao().findOneJobLog(jobLog.getInstanceId(), parentJob.getJobGroup(), parentJob.getId(), Collections.singletonList(ReturnT.SUCCESS_CODE));
                    if (count > 0 && (parentLog == null || count != parentLog.getShardNum())) {
                        scene5Cond = false;
                        break;
                    }
                } else {
                    int count = JobAdminConfig.getAdminConfig().getJobLogDao().countStatusShardingBy(jobLog.getInstanceId(), parentJob.getJobGroup(), parentJob.getId(), Collections.singletonList(ReturnT.SUCCESS_CODE));
                    if (count != 1) {
                        scene5Cond = false;
                        break;
                    }
                }
            }
            if (scene5Cond) {
                List<Integer> usageParentIdList = usageJobList.stream().map(JobInfo::getId).collect(Collectors.toList());
                int ret = JobAdminConfig.getAdminConfig().getJobLogDao().updateDispatchSub(jobLog.getInstanceId(), usageParentIdList, -1, JobLog.DISPATCH_SUB_NO_NEED, JobLog.DISPATCH_SUB_SUCCESS);
                if (ret >= 1) {
                    logger.info("child job has multi parents, meet start child process. childJobId=[{}],parentJobId=[{}],logId={},instanceId={}", childJobInfo.getId(), jobInfo.getId(), jobLog.getId(), jobLog.getInstanceId());
                    return true;
                }
            } else {
                logger.info("child job has multi parents, waiting other job process ok. childJobId=[{}],parentJobId=[{}],logId={},instanceId={}", childJobInfo.getId(), jobInfo.getId(), jobLog.getId(), jobLog.getInstanceId());
            }
        } else {
            // 场景7：如果父任务有多个，有父子任务传参数，此中场景直接拒绝
            logger.error("child job has multi parents and has config param from parents. it's not allowed. childJobId=[{}],parentJobId=[{}],logId={},instanceId={}", childJobInfo.getId(), jobInfo.getId(), jobLog.getId(), jobLog.getInstanceId());
        }
        return false;
    }

    /**
     * 处理子任务在父任务结束后开始的任务
     *
     * @param xxlJobLog    调度日志
     * @param compensate   是否为补偿任务
     * @param childJobInfo 子任务
     * @param jobInfo      当前任务
     * @return 是否有子调度log
     */
    private static void handleBeginAfterParents(JobLog xxlJobLog, boolean compensate, JobInfo childJobInfo, JobInfo jobInfo) {
        // 大类
        // 只有一个父任务
        // -------场景1：如果父任务只有一个，并且没有父子任务传参数,并且父任务不是分片任务或者分片只有一个，那么直接开始子任务就可以了
        // -------场景2：如果父任务只有一个，并且是父子任务传参数,并且父任务不是分片任务或者分片只有一个，那么直接开始子任务就可以了
        // -------场景3：如果父任务只有一个，并且没有父子任务传参数，但是父任务是分片任务，并且分片数量>1,那么需要等待全部分片结束后才可以开始子任务
        // -------场景4：如果父任务只有一个，有父子任务传参数，父任务是分片任务，并且分片数量>1，那么需要等待全部分片结束后才可以开始子任务
        // 多于一个父任务
        // -------场景5：如果父任务有多个个，没有父子任务传参数,父任务不是分片任务或者父任务是分片任务，并且分片数量==1，那么需要等待全部父亲结束后才可以开始子任务
        // -------场景6：如果父任务有多个，没有父子任务传参数,父任务部分是分片任务并且分片数量>1,那么需要等待全部父亲结束后才可以开始子任务
        // -------场景7：如果父任务有多个，有父子任务传参数,直接拒绝，此中配置为错误配置
        // 父任务是否已经完成的标识，完成后相关的子任务不需要在判断
        // 场景1：如果父任务只有一个，并且没有父子任务传参数,并且父任务不是分片任务或者分片只有一个，那么直接开始子任务就可以了
        if (!childJobInfo.getParamFromParent()) {
            // 需要考虑当前任务是否为参数来自父任务，如果是，需要等待当前父任务全部结束
            // 如果父任务没有配置参数来自父任务或者虽然配置了，但是根本没有父任务，则直接开始
            logger.info("child job has not config paramFromParent and only one sharding, meet start child process. childJobId=[{}],parentJobId=[{}],logId={},instanceId={}", childJobInfo.getId(), jobInfo.getId(), xxlJobLog.getId(), xxlJobLog.getInstanceId());
            JobTriggerPoolHelper.trigger(childJobInfo.getJobGroup(), childJobInfo.getId(), compensate ? TriggerTypeEnum.SYSTEM_COMPENSATE_RETRY : TriggerTypeEnum.PARENT, -1, null, null, null, xxlJobLog.getId(), LogTypeEnum.SUB_LOG, xxlJobLog.getInstanceId(), "", "");
        } else {
            // 场景2：如果父任务只有一个，并且是父子任务传参数,并且父任务不是分片任务或者分片只有一个，那么直接开始子任务就可以了
            if (!ExecutorRouteStrategyEnum.SHARDING_BROADCAST.name().equals(jobInfo.getExecutorRouteStrategy()) ||
                    (ExecutorRouteStrategyEnum.SHARDING_BROADCAST.name().equals(jobInfo.getExecutorRouteStrategy()) && jobInfo.getShardingNum() == 1)) {
                if (StringUtils.isEmpty(xxlJobLog.getChildrenExecutorParams())) {
                    return;
                }
                String[] childExecutorParams = null;
                try {
                    childExecutorParams = JacksonUtil.readValue(xxlJobLog.getChildrenExecutorParams(), String[].class);
                } catch (Exception e) {
                    logger.error("paramFromParent log id [{}] params parser error", xxlJobLog.getId(), e);
                }
                logger.info("handle paramFromParent sub task, logId={}", xxlJobLog.getId());
                if (childExecutorParams == null || childExecutorParams.length == 0) {
                    return;
                }
                for (String executorParam : childExecutorParams) {
                    logger.info("child job has config paramFromParent, meet start child process. childJobId=[{}],parentJobId=[{}],logId={},instanceId={}", childJobInfo.getId(), jobInfo.getId(), xxlJobLog.getId(), xxlJobLog.getInstanceId());
                    // 限流
                    long startTime = System.currentTimeMillis();
                    RateLimiter rateLimiter = JobTrigger.createRateLimiter(childJobInfo.getId(), childJobInfo.getDispatchThreshold());
                    double waitTime = rateLimiter.acquire();
                    long endTime = System.currentTimeMillis();
                    if (logger.isDebugEnabled()) {
                        logger.debug("parentLog={},executorParam={},waitTime={},elapse={}", xxlJobLog.getId(), executorParam, waitTime, endTime - startTime);
                    }
                    JobTriggerPoolHelper.trigger(childJobInfo.getJobGroup(), childJobInfo.getId(), compensate ? TriggerTypeEnum.SYSTEM_COMPENSATE_RETRY : TriggerTypeEnum.PARENT, -1, null, executorParam, null, xxlJobLog.getId(), LogTypeEnum.SUB_LOG, xxlJobLog.getInstanceId(), "", "");
                }
            } else {
                // 场景4：如果父任务只有一个，有父子任务传参数，父任务是分片任务，并且分片数量>1，那么需要等待全部分片结束后才可以开始子任务
                // 查看父任务对应的分片是否全部结束
                // 查处全部分片对应的调度日志，逐个下发任务
                List<JobLog> succeedJobLogList = JobAdminConfig.getAdminConfig().getJobLogDao().queryList(xxlJobLog.getInstanceId(), xxlJobLog.getJobGroup(), xxlJobLog.getJobId(), Collections.singletonList(ReturnT.SUCCESS_CODE));
                if (CollectionUtils.isEmpty(succeedJobLogList)) {
                    succeedJobLogList = new ArrayList<>();
                }
                // 此处更新，保证只会下发一次
                // 如果在下发过程中重启了，有可能导致任务丢失
                for (JobLog jobLog : succeedJobLogList) {
                    if (!StringUtils.isEmpty(jobLog.getChildrenExecutorParams())) {
                        String[] childExecutorParams = null;
                        try {
                            childExecutorParams = JacksonUtil.readValue(jobLog.getChildrenExecutorParams(), String[].class);
                        } catch (Exception e) {
                            logger.error("paramFromParent logId [{}] params parser error", jobLog.getId(), e);
                        }
                        logger.info("handle paramFromParent sub task, logId [{}]", jobLog.getId());
                        if (childExecutorParams == null || childExecutorParams.length == 0) {
                            continue;
                        }
                        for (String executorParam : childExecutorParams) {
                            logger.info("child job has multi sharding, meet start child process. childJobId=[{}],parentJobId=[{}],logId={},instanceId={}", childJobInfo.getId(), jobInfo.getId(), xxlJobLog.getId(), xxlJobLog.getInstanceId());
                            // 限流
                            long startTime = System.currentTimeMillis();
                            RateLimiter rateLimiter = JobTrigger.createRateLimiter(childJobInfo.getId(), childJobInfo.getDispatchThreshold());
                            double waitTime = rateLimiter.acquire();
                            long endTime = System.currentTimeMillis();
                            if (logger.isDebugEnabled()) {
                                logger.debug("parentLog={},executorParam={},waitTime={},elapse={}", xxlJobLog.getId(), executorParam, waitTime, endTime - startTime);
                            }
                            JobTriggerPoolHelper.trigger(childJobInfo.getJobGroup(), childJobInfo.getId(), compensate ? TriggerTypeEnum.SYSTEM_COMPENSATE_RETRY : TriggerTypeEnum.PARENT, -1, null, executorParam, null, jobLog.getId(), LogTypeEnum.SUB_LOG, jobLog.getInstanceId(), "", "");
                        }
                    }
                }
            }
        }
    }

    /**
     * 根据起点任务ID查找jobLog使用到的父任务
     *
     * @param startJobId      起点任务ID
     * @param matchChildJobId 要匹配的任务
     * @param usageJobList    使用到的Job
     */
    public static void findUsageParentJob(int startJobId, int matchChildJobId, List<JobInfo> usageJobList) {
        JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobInfoDao().loadById(startJobId);
        if (jobInfo == null) {
            return;
        }
        if (jobInfo.getChildren().contains(matchChildJobId)) {
            usageJobList.add(jobInfo);
        }
        for (int childJobId : jobInfo.getChildren()) {
            findUsageParentJob(childJobId, matchChildJobId, usageJobList);
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
}
