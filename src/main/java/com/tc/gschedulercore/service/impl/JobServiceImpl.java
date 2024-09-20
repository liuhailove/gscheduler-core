package com.tc.gschedulercore.service.impl;


import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.cron.CronExpression;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.core.route.ExecutorRouteStrategyEnum;
import com.tc.gschedulercore.core.scheduler.MisfireStrategyEnum;
import com.tc.gschedulercore.core.scheduler.ScheduleTypeEnum;
import com.tc.gschedulercore.core.thread.JobScheduleHelper;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.core.util.JacksonUtil;
import com.tc.gschedulercore.dao.*;
import com.tc.gschedulercore.enums.ExecutorBlockStrategyEnum;
import com.tc.gschedulercore.enums.GlueTypeEnum;
import com.tc.gschedulercore.enums.RetryType;
import com.tc.gschedulercore.service.JobService;
import com.tc.gschedulercore.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * core job action for go-scheduler
 *
 * @author honggang.liu
 */
@Service
public class JobServiceImpl implements JobService {
    private static Logger logger = LoggerFactory.getLogger(JobServiceImpl.class.getSimpleName());

    @Resource
    private JobGroupDao jobGroupDao;
    @Resource
    private JobInfoDao jobInfoDao;
    @Resource
    private JobLogGlueDao jobLogGlueDao;
    @Resource
    private JobLogReportDao jobLogReportDao;

    /**
     * job变更历史dao
     */
    @Resource
    private JobInfoHistoryDao jobInfoHistoryDao;

    /**
     * 开放平台配置
     */
    @Resource
    private OpenApiDao xxlOpenApiDao;

    /**
     * 最大分发阈值
     */
    private static final int MAX_DISPATCH_THRESHOLD = 5000;

    @Override
    public Map<String, Object> pageList(int start, int length, List<Integer> jobGroups, int triggerStatus, String jobDesc, String jobName, String executorHandler, String author) {
        if (jobName != null) {
            jobName = jobName.replace("_", "\\_");
        }
        if (jobDesc != null) {
            jobDesc = jobDesc.replace("_", "\\_");
        }
        List<JobInfo> list;
        int listCount;
        try (HintManager manager = HintManager.getInstance()) {
            manager.setWriteRouteOnly();
            // page list
            list = jobInfoDao.pageList(start, length, jobGroups, triggerStatus, jobDesc, jobName, executorHandler, author);
            list = list.stream().filter(bean -> {
                if (bean.getTriggerNextTime() > 0) {
                    bean.setTriggerNextDateTime(new Date(bean.getTriggerNextTime()));
                }
                return true;
            }).collect(Collectors.toList());
            listCount = jobInfoDao.pageListCount(start, length, jobGroups, triggerStatus, jobDesc, jobName, executorHandler, author);
        }
        // package result
        Map<String, Object> maps = new HashMap<>(6);
        maps.put("recordsTotal", listCount);        // 总记录数
        maps.put("recordsFiltered", listCount);    // 过滤后的总记录数
        maps.put("data", list);                    // 分页列表
        maps.put("msg", "");                    // 消息
        maps.put("count", listCount);
        maps.put("code", ReturnT.SUCCESS_CODE);
        return maps;
    }

    /**
     * page list
     *
     * @param start     开始下标
     * @param length    长度
     * @param jobGroups 执行器ID
     * @return 待延迟列表
     */
    @Override
    public Map<String, Object> triggerDelayList(int start, int length, List<Integer> jobGroups) {
        List<JobInfo> list;
        int listCount;
        try (HintManager manager = HintManager.getInstance()) {
            manager.setWriteRouteOnly();
            if (!CollectionUtils.isEmpty(jobGroups)) {
                // page list
                list = jobInfoDao.triggerDelayList(start, length, jobGroups);
                list = list.stream().filter(bean -> {
                    if (bean.getTriggerNextTime() > 0) {
                        bean.setTriggerNextDateTime(new Date(bean.getTriggerNextTime()));
                    }
                    return true;
                }).collect(Collectors.toList());
                listCount = jobInfoDao.triggerDelayListCount(start, length, jobGroups);
            } else {
                list = Collections.emptyList();
                listCount = 0;
            }
        }
        // package result
        Map<String, Object> maps = new HashMap<>(6);
        maps.put("recordsTotal", listCount);        // 总记录数
        maps.put("recordsFiltered", listCount);    // 过滤后的总记录数
        maps.put("data", list);                    // 分页列表
        maps.put("msg", "");                    // 消息
        maps.put("count", listCount);
        maps.put("code", ReturnT.SUCCESS_CODE);
        return maps;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<String> add(JobInfo jobInfo) {
        // valid base
        JobGroup group = jobGroupDao.load(jobInfo.getJobGroup());
        if (group == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_field_jobgroup")));
        }
        if (jobInfo.getJobName() == null || jobInfo.getJobName().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobname")));
        }
        if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }
        if (jobInfo.getAuthor() == null || jobInfo.getAuthor().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }
        if (jobInfo.getAdditionalParams() != null && jobInfo.getAdditionalParams().length() > 2048) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_length_too_long") + I18nUtil.getString("joblog_field_additionalParams")));
        }
        jobInfo.setScheduleConf(jobInfo.getScheduleConf() == null ? null : StringUtils.trim(jobInfo.getScheduleConf()));
        // valid trigger
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (jobInfo.getScheduleConf() == null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "Cron" + I18nUtil.getString("system_unvalid"));
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE/* || scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
            if (jobInfo.getScheduleConf() == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")));
            }
            try {
                int fixSecond = Integer.parseInt(jobInfo.getScheduleConf());
                if (fixSecond < 1) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
                }
            } catch (Exception e) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
        }

        // valid job
        if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_gluetype") + I18nUtil.getString("system_unvalid")));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType()) && (jobInfo.getExecutorHandler() == null || jobInfo.getExecutorHandler().trim().length() == 0)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + "JobHandler"));
        }
        // 》fix "\r" in shell
        if (GlueTypeEnum.GLUE_SHELL == GlueTypeEnum.match(jobInfo.getGlueType()) && jobInfo.getGlueSource() != null) {
            jobInfo.setGlueSource(jobInfo.getGlueSource().replaceAll("\r", ""));
        }

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid")));
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
        }
        List<Integer> childJobInfos = new ArrayList<>();
        // 》ChildJobId valid
        if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(childJobIdItem)) {
                    JobInfo childJobInfo = jobInfoDao.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                    // 如果当前任务配置了"参数来自父任务"，且子任务的父任务还有其他任务，则提示"子任务[id]有多个父任务，不能配置参数来自父任务"
                    if (jobInfo.getParamFromParent() && !childJobInfo.getParents().isEmpty()) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format(I18nUtil.getString("jobinfo_paramFromParent_but_has_other_parent"), childJobIdItem));
                    }
                    // 如果当前任务没有配置了"参数来自父任务"，但是子任务的父任务还有其他任务，并且其他父任务存在配置"参数来自父任务"，则提示"子任务[id]有多个父任务，需修改其他父任务，关闭"参数来自父任务"配置"
                    if (!jobInfo.getParamFromParent() && !childJobInfo.getParents().isEmpty() && jobInfoDao.existParamFromParent(childJobInfo.getParents())) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format(I18nUtil.getString("jobinfo_not_paramFromParent_but_has_other_parent"), childJobIdItem));
                    }
                    childJobInfos.add(childJobInfo.getId());
                    jobInfoDao.update(childJobInfo);
                } else {
                    return new ReturnT<>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }
            // join , avoid "xxx,,"
            StringBuilder temp = new StringBuilder();
            for (String item : childJobIds) {
                temp.append(item).append(",");
            }
            temp.deleteCharAt(temp.length() - 1);
            jobInfo.setChildJobId(temp.toString());
        }
        if (jobInfoDao.exist(jobInfo.getJobName(), group.getAppname()) > 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_name_app_name_exist"));
        }
        if (jobInfo.getRetryType() > 0) {
            if (StringUtils.isEmpty(jobInfo.getRetryConf())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_not_null"));
            }
            if (jobInfo.getRetryType() == RetryType.FIX_RATE_TYPE && !isNumeric(jobInfo.getRetryConf())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_fixed_not_number"));
            } else if (jobInfo.getRetryType() == RetryType.CUSTOMER_TYPE) {
                int failRetryCount = jobInfo.getExecutorFailRetryCount();
                if (StringUtils.isEmpty(jobInfo.getRetryConf()) || jobInfo.getRetryConf().split(",").length != failRetryCount) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_customer_retry_count_not_match_conf"));
                }
                for (String item : jobInfo.getRetryConf().split(",")) {
                    if (!isNumeric(item)) {
                        return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_customer_retry_count_not_cov_num"));
                    }
                }
            } else if (jobInfo.getRetryType() == RetryType.EXPONENTIAL_BACK_OFF_TYPE) {
                String[] confArr = jobInfo.getRetryConf().split(",");
                if (confArr.length != 2) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_exp_length"));
                }
                for (String item : confArr) {
                    if (!isDouble(item)) {
                        return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_exp_not_number"));
                    }
                }
            }
        }
        if (jobInfo.getDispatchThreshold() > MAX_DISPATCH_THRESHOLD) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_dispatch_threshold_exceed_max_threshold"));
        }
        if (jobInfo.getLogRetentionDays() > JobAdminConfig.getAdminConfig().getLogretentiondays()) {
            return new ReturnT<>(ReturnT.FAIL_CODE, MessageFormat.format(I18nUtil.getString("jobinfo_log_retention_days_exceed_max_threshold"), JobAdminConfig.getAdminConfig().getLogretentiondays()));
        }
        if (jobInfo.getStartExecutorToleranceThresholdInMin() < 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_start_executor_tolerance_threshold_in_min_less_than_zero"));
        }
        if (jobInfo.getAlarmSilence() < 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_alarm_silence_less_than_zero"));
        }
        // add in db
        jobInfo.setAddTime(new Date());
        jobInfo.setUpdateTime(new Date());
        jobInfo.setGlueUpdatetime(new Date());
        if (StringUtils.isEmpty(jobInfo.getAlarmSeatalk())) {
            jobInfo.setAlarmSeatalk(group.getAlarmSeatalk());
        }
        jobInfo.setAppName(group.getAppname());
        // 默认为0，也就是有告警第一次立刻告警
        jobInfo.setAlarmSilenceTo(0);
        if (jobInfo.getTaskRunningAlarm() == null) {
            jobInfo.setTaskRunningAlarm(Boolean.TRUE);
        }
        jobInfoDao.save(jobInfo);
        if (jobInfo.getId() < 1) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_add") + I18nUtil.getString("system_fail")));
        }
        // 记录变更历史
        JobInfoHistory xxlJobInfoHistory = new JobInfoHistory();
        xxlJobInfoHistory.setJobId(jobInfo.getId());
        xxlJobInfoHistory.setJobSource(JacksonUtil.writeValueAsString(jobInfo));
        xxlJobInfoHistory.setJobRemark(jobInfo.getJobDesc());
        xxlJobInfoHistory.setAddTime(new Date());
        jobInfoHistoryDao.save(xxlJobInfoHistory);
        // 更新父子关系
        handleParent(jobInfo.getId(), new ArrayList<>(), childJobInfos);
        // 处理等待父任务
        boolean rollback = isRollback(jobInfo);
        if (rollback) {
            // 手动设置事务回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_child_root_not_same"));
        }
        return new ReturnT<>(String.valueOf(jobInfo.getId()));
    }

    private boolean isNumeric(String str) {
        try {
            Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDouble(String str) {
        try {
            Double.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<String> update(JobInfo jobInfo) {
        // valid base
        if (jobInfo.getJobName() == null || jobInfo.getJobName().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobname")));
        }
        if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }
        if (jobInfo.getAuthor() == null || jobInfo.getAuthor().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }
        if (jobInfo.getAdditionalParams() != null && jobInfo.getAdditionalParams().length() > 2048) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_length_too_long") + I18nUtil.getString("joblog_field_additionalParams")));
        }
        if (jobInfo.getExecutorHandler() != null && jobInfo.getExecutorHandler().length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_length_too_long") + I18nUtil.getString("jobinfo_field_executorhandler")));
        }
        if (jobInfo.getExecutorParam() != null && jobInfo.getExecutorParam().length() > 512) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_length_too_long") + I18nUtil.getString("joblog_field_executorParams")));
        }
        if (jobInfo.getJobName() != null && jobInfo.getJobName().length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_length_too_long") + I18nUtil.getString("jobinfo_name")));
        }
        // valid trigger
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (jobInfo.getScheduleConf() == null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "Cron" + I18nUtil.getString("system_unvalid"));
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE /*|| scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
            if (jobInfo.getScheduleConf() == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
            try {
                int fixSecond = Integer.parseInt(jobInfo.getScheduleConf());
                if (fixSecond < 1) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
                }
            } catch (Exception e) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
        }

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid")));
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (jobInfo.getRetryType() > 0) {
            if (StringUtils.isEmpty(jobInfo.getRetryConf())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_not_null"));
            }
            if (jobInfo.getRetryType() == RetryType.FIX_RATE_TYPE && !isNumeric(jobInfo.getRetryConf())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_fixed_not_number"));
            } else if (jobInfo.getRetryType() == RetryType.CUSTOMER_TYPE) {
                int failRetryCount = jobInfo.getExecutorFailRetryCount();
                if (StringUtils.isEmpty(jobInfo.getRetryConf()) || jobInfo.getRetryConf().split(",").length != failRetryCount) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_customer_retry_count_not_match_conf"));
                }
                for (String item : jobInfo.getRetryConf().split(",")) {
                    if (!isNumeric(item)) {
                        return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_customer_retry_count_not_cov_num"));
                    }
                }
            } else if (jobInfo.getRetryType() == RetryType.EXPONENTIAL_BACK_OFF_TYPE) {
                String[] confArr = jobInfo.getRetryConf().split(",");
                if (confArr.length != 2) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_exp_length"));
                }
                for (String item : confArr) {
                    if (!isDouble(item)) {
                        return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_retry_conf_exp_not_number"));
                    }
                }
            }
        }
        if (jobInfo.getDispatchThreshold() > MAX_DISPATCH_THRESHOLD) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_dispatch_threshold_exceed_max_threshold"));
        }
        if (jobInfo.getLogRetentionDays() > JobAdminConfig.getAdminConfig().getLogretentiondays()) {
            return new ReturnT<>(ReturnT.FAIL_CODE, MessageFormat.format(I18nUtil.getString("jobinfo_log_retention_days_exceed_max_threshold"), JobAdminConfig.getAdminConfig().getLogretentiondays()));
        }
        // group valid
        JobGroup jobGroup = jobGroupDao.load(jobInfo.getJobGroup());
        if (jobGroup == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_jobgroup") + I18nUtil.getString("system_unvalid")));
        }
        // stage job info
        JobInfo existsJobInfo = jobInfoDao.loadById(jobInfo.getId());
        if (existsJobInfo == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_not_found")));
        }
        if (!existsJobInfo.getJobName().equals(jobInfo.getJobName()) && jobInfoDao.exist(jobInfo.getJobName(), jobGroup.getAppname()) > 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_name_app_name_exist"));
        }
        // 如果当前任务配置了"参数来自父任务"，且有多个父任务，则提示"有多个父任务时，不能开启"参数来自父任务""
        if (jobInfo.getParamFromParent() && existsJobInfo.getParents().size() > 1) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_have_more_than_one_parent_can_not_paramFromParent"));
        }
        if (jobInfo.getStartExecutorToleranceThresholdInMin() < 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_start_executor_tolerance_threshold_in_min_less_than_zero"));
        }
        if (jobInfo.getAlarmSilence() < 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_child_job_can_not_be_self"));
        }
        List<Integer> childJobList = new ArrayList<>();
        // 》ChildJobId valid
        if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(childJobIdItem)) {
                    // 子任务不能包含自身
                    if (Integer.parseInt(childJobIdItem) == jobInfo.getId()) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                    JobInfo childJobInfo = jobInfoDao.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                    // 如果当前任务配置了"参数来自父任务"，且子任务的父任务还有其他任务，则提示"子任务[id]有多个父任务，不能配置参数来自父任务"
                    // 父任务需要移除当前任务id
                    List<Integer> parents = childJobInfo.getParents();
                    parents.remove(Integer.valueOf(jobInfo.getId()));
                    if (jobInfo.getParamFromParent() && !parents.isEmpty()) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format(I18nUtil.getString("jobinfo_paramFromParent_but_has_other_parent"), childJobIdItem));
                    }
                    // 如果当前任务没有配置了"参数来自父任务"，但是子任务的父任务还有其他任务，并且其他父任务存在配置"参数来自父任务"，则提示"子任务[id]有多个父任务，需修改其他父任务，关闭"参数来自父任务"配置"
                    if (!jobInfo.getParamFromParent() && !parents.isEmpty() && jobInfoDao.existParamFromParent(parents)) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format(I18nUtil.getString("jobinfo_not_paramFromParent_but_has_other_parent"), childJobIdItem));
                    }
                    childJobList.add(childJobInfo.getId());
                    // 更新子任务的父亲
                    parents.add(jobInfo.getId());
                    childJobInfo.setParentJobId(StringUtils.join(parents, ","));
                    jobInfoDao.update(childJobInfo);
                } else {
                    return new ReturnT<>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }
            // join , avoid "xxx,,"
            StringBuilder temp = new StringBuilder();
            for (String item : childJobIds) {
                temp.append(item).append(",");
            }
            temp.deleteCharAt(temp.length() - 1);
            jobInfo.setChildJobId(temp.toString());
        }
        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = existsJobInfo.getTriggerNextTime();
        boolean scheduleDataNotChanged = jobInfo.getScheduleType().equals(existsJobInfo.getScheduleType()) && (
                (jobInfo.getScheduleConf() == null && existsJobInfo.getScheduleConf() == null) ||
                        (jobInfo.getScheduleConf() != null && jobInfo.getScheduleConf().equals(existsJobInfo.getScheduleConf())));
        if (existsJobInfo.getTriggerStatus() == 1 && !scheduleDataNotChanged) {
            try {
                Date nextValidTime = JobScheduleHelper.generateNextValidTime(jobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime == null) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
                }
                nextTriggerTime = nextValidTime.getTime();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
        }
        // 如果任务设置了延迟，则必须要求其有父任务
        if (jobInfo.isDelay() && CollectionUtils.isEmpty(existsJobInfo.getParents())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_delay_task_no_parents"));
        }
        // 先记录变更历史
        JobInfoHistory jobInfoHistory = new JobInfoHistory();
        jobInfoHistory.setJobId(existsJobInfo.getId());
        jobInfoHistory.setJobSource(JacksonUtil.writeValueAsString(existsJobInfo));
        jobInfoHistory.setJobRemark(jobInfo.getJobDesc());
        jobInfoHistory.setAddTime(new Date());
        jobInfoHistoryDao.save(jobInfoHistory);
        // remove  backup more than 30
        jobInfoHistoryDao.removeOld(existsJobInfo.getId(), 30);
        List<Integer> existChildJob = existsJobInfo.getChildren();
        if (StringUtils.isEmpty(jobInfo.getAlarmSeatalk())) {
            existsJobInfo.setAlarmSeatalk(jobGroup.getAlarmSeatalk());
        } else {
            existsJobInfo.setAlarmSeatalk(jobInfo.getAlarmSeatalk());
        }
        // 再记录变更
        existsJobInfo.setAppName(jobGroup.getAppname());
        existsJobInfo.setJobGroup(jobInfo.getJobGroup());
        existsJobInfo.setJobName(jobInfo.getJobName());
        existsJobInfo.setJobDesc(jobInfo.getJobDesc());
        existsJobInfo.setAuthor(jobInfo.getAuthor());
        existsJobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
        existsJobInfo.setScheduleType(jobInfo.getScheduleType());
        existsJobInfo.setScheduleConf(jobInfo.getScheduleConf() == null ? null : StringUtils.trim(jobInfo.getScheduleConf()));
        existsJobInfo.setMisfireStrategy(jobInfo.getMisfireStrategy());
        existsJobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
        existsJobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
        existsJobInfo.setExecutorParam(jobInfo.getExecutorParam());
        existsJobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        existsJobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
        existsJobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
        existsJobInfo.setChildJobId(jobInfo.getChildJobId());
        existsJobInfo.setTriggerNextTime(nextTriggerTime);
        existsJobInfo.setShardingType(jobInfo.getShardingType());
        existsJobInfo.setShardingNum(jobInfo.getShardingNum());
        existsJobInfo.setRetryType(jobInfo.getRetryType());
        existsJobInfo.setRetryConf(jobInfo.getRetryConf());
        existsJobInfo.setExecutorThreshold(jobInfo.getExecutorThreshold());
        existsJobInfo.setParamFromParent(jobInfo.getParamFromParent());
        existsJobInfo.setResultCheck(jobInfo.isResultCheck());
        existsJobInfo.setFinalFailedSendAlarm(jobInfo.isFinalFailedSendAlarm());
        existsJobInfo.setBeginAfterParent(jobInfo.isBeginAfterParent());
        existsJobInfo.setUpdateBy(jobInfo.getUpdateBy());
        existsJobInfo.setUpdateTime(new Date());
        existsJobInfo.setDispatchThreshold(jobInfo.getDispatchThreshold());
        existsJobInfo.setLogRetentionDays(jobInfo.getLogRetentionDays());
        // 修改恢复为0
        existsJobInfo.setAlarmSilenceTo(0);
        existsJobInfo.setStartExecutorToleranceThresholdInMin(jobInfo.getStartExecutorToleranceThresholdInMin());
        existsJobInfo.setAlarmSilence(jobInfo.getAlarmSilence());
        existsJobInfo.setTaskRunningAlarm(Boolean.TRUE.equals(jobInfo.getTaskRunningAlarm()));
        existsJobInfo.setVoiceAlarmTels(jobInfo.getVoiceAlarmTels());
        existsJobInfo.setAdditionalParams(jobInfo.getAdditionalParams());
        existsJobInfo.setDelay(jobInfo.isDelay());
        if (jobInfo.isDelay()) {
            existsJobInfo.setDelayInMs(jobInfo.getDelayInMs());
        } else {
            existsJobInfo.setDelayInMs(0L);
        }
        jobInfoDao.update(existsJobInfo);

        JobInfo jobInfoUpdater = jobInfoDao.loadById(jobInfo.getId());
        JobInfoHistory xxlJobInfoHistoryUpdate = new JobInfoHistory();
        xxlJobInfoHistoryUpdate.setJobId(jobInfoUpdater.getId());
        xxlJobInfoHistoryUpdate.setJobSource(JacksonUtil.writeValueAsString(jobInfoUpdater));
        xxlJobInfoHistoryUpdate.setJobRemark(jobInfo.getJobDesc());
        xxlJobInfoHistoryUpdate.setAddTime(new Date());
        jobInfoHistoryDao.save(xxlJobInfoHistoryUpdate);
        // 更新父子关系
        handleParent(existsJobInfo.getId(), existChildJob, childJobList);
        // 处理等待父任务，重新加载，因为父子任务更新后需要再次加载
        jobInfo = jobInfoDao.loadById(jobInfo.getId());
        boolean rollback = isRollback(jobInfo);
        if (rollback) {
            // 手动设置事务回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_child_root_not_same"));
        }
        return ReturnT.SUCCESS;
    }


    private boolean isRollback(JobInfo jobInfo) {
        int rootJobId = jobInfo.getId();
        int curJobRootId = -1;
        boolean rollback = false;
        boolean isChildBeginAfterParent = true;
        for (Integer childJobIdItem : jobInfo.getChildren()) {
            JobInfo childJobInfo = jobInfoDao.loadById(childJobIdItem);
            // 参数来自父任务时，父亲节点只能有一个
            if (childJobInfo.getParamFromParent() && childJobInfo.getParents().size() > 1) {
                return true;
            }
            if (!CollectionUtils.isEmpty(childJobInfo.getChildren())) {
                return isRollback(childJobInfo);
            }
            if (childJobInfo.isBeginAfterParent()) {
                rootJobId = findRootJob(childJobInfo);
                if (rootJobId == -1) {
                    rollback = true;
                    break;
                }
                isChildBeginAfterParent = true;
            } else {
                isChildBeginAfterParent = false;
            }
        }
        if (!rollback && isChildBeginAfterParent && jobInfo.isBeginAfterParent()) {
            curJobRootId = findRootJob(jobInfo);
            if (rootJobId != curJobRootId) {
                // 二次校验
                JobInfo rootJobInfo = jobInfoDao.loadById(rootJobId);
                rootJobId = findRootJob(rootJobInfo);
                if (rootJobId != curJobRootId || rootJobId == -1) {
                    rollback = true;
                }
            }
        }
        return rollback;
    }

    //
    // 查找任务的根任务，如果当前任务的父任务大于1，那么需要父任务的父任务有共同的根任务，否则校验不通过
    //
    //          -  child1
    //       -
    // root1                  ---合法
    //       -
    //          - child2
    //
    // root1
    //        -
    //           - child      ---非法，根任务ID不同
    //        -
    // root2
    //
    //
    // @param jobInfo 校验任务
    // @return 合法返回rootID，否则返回-1
    //
    private int findRootJob(JobInfo jobInfo) {
        if (CollectionUtils.isEmpty(jobInfo.getParents())) {
            return jobInfo.getId();
        }
        if (jobInfo.getParents().size() == 1) {
            return findRootJob(jobInfoDao.loadById(jobInfo.getParents().get(0)));
        }
        int expectedRootId = findRootJob(jobInfoDao.loadById(jobInfo.getParents().get(0)));
        for (int i = 1; i < jobInfo.getParents().size(); i++) {
            int otherRootId = findRootJob(jobInfoDao.loadById(jobInfo.getParents().get(i)));
            if (expectedRootId != otherRootId) {
                return -1;
            }
        }
        return expectedRootId;
    }

    /**
     * 处理孩子节点与父亲节点的关系
     *
     * @param existChildJob DB中任务
     * @param childJobList  当前子任务
     */
    private void handleParent(Integer jobId, List<Integer> existChildJob, List<Integer> childJobList) {
        List<Integer> existChildren = new ArrayList<>(existChildJob);
        // 获取要移除的数据
        existChildren.removeAll(childJobList);
        // 要增加的
        List<Integer> currentChildren = new ArrayList<>(childJobList);
        currentChildren.removeAll(existChildJob);
        for (Integer removeId : existChildren) {
            JobInfo jobInfo = jobInfoDao.loadById(removeId);
            List<Integer> parents = jobInfo.getParents();
            parents.removeAll(Collections.singletonList(jobId));
            jobInfo.setParentJobId(StringUtils.join(parents, ","));
            jobInfo.setUpdateTime(new Date());
            jobInfoDao.update(jobInfo);
        }
        for (Integer addJobId : currentChildren) {
            JobInfo jobInfo = jobInfoDao.loadById(addJobId);
            List<Integer> parents = jobInfo.getParents();
            if (!parents.contains(jobId)) {
                parents.add(jobId);
            }
            jobInfo.setParentJobId(StringUtils.join(parents, ","));
            jobInfo.setUpdateTime(new Date());
            jobInfoDao.update(jobInfo);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<String> remove(int id) {
        JobInfo xxlJobInfo = jobInfoDao.loadById(id);
        if (xxlJobInfo == null) {
            return ReturnT.SUCCESS;
        }
        long startTime = System.currentTimeMillis();
        logger.info("remove jobInfo，begin: startTime:{}，jobId:{}", startTime, id);
        // 如果任务是执行中，则不允许删除
        if (xxlJobInfo.getTriggerStatus() == 1) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_del_limit_0"));
        }
        jobInfoDao.delete(id);
        jobLogGlueDao.deleteByJobId(id);
        // 删除job变更历史
        jobInfoHistoryDao.deleteByJobId(id);
        // 删除父子关联关系
        // 1.更新以此ID为父亲的job
        List<JobInfo> useParentJobInfos = jobInfoDao.getJobsByParent(id);
        if (!CollectionUtils.isEmpty(useParentJobInfos)) {
            for (JobInfo jobInfo : useParentJobInfos) {
                List<Integer> parents = jobInfo.getParents();
                parents.remove(Integer.valueOf(id));
                jobInfo.setParentJobId(StringUtils.join(parents, ","));
                jobInfo.setUpdateTime(new Date());
                jobInfoDao.update(jobInfo);
            }
        }
        long updateTime = System.currentTimeMillis();
        logger.info("remove jobInfo,update job startTime:{}，updateTime:{}", startTime, updateTime);
        // 2.更新以此ID为孩子的job
        List<JobInfo> useChildJobInfos = jobInfoDao.getJobsByChild(id);
        if (!CollectionUtils.isEmpty(useChildJobInfos)) {
            for (JobInfo jobInfo : useChildJobInfos) {
                List<Integer> children = jobInfo.getChildren();
                children.remove(Integer.valueOf(id));
                jobInfo.setChildJobId(StringUtils.join(children, ","));
                jobInfo.setUpdateTime(new Date());
                jobInfoDao.update(jobInfo);
            }
        }
        long endTime = System.currentTimeMillis();
        logger.info("remove job success,endTime:{},elapsedTime:{}", endTime, endTime - startTime);
        return ReturnT.SUCCESS;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<String> start(int id, String updateBy) {
        logger.info("user start Job,id:{},updateBy:{}", id, updateBy);
        JobInfo jobInfo = jobInfoDao.loadById(id);
        // valid
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
        if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type_none_limit_start")));
        }
        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = 0;
        try {
            Date nextValidTime = JobScheduleHelper.generateNextValidTime(jobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
            nextTriggerTime = nextValidTime.getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }

        // 先记录变更历史
        JobInfoHistory xxlJobInfoHistory = new JobInfoHistory();
        xxlJobInfoHistory.setJobId(jobInfo.getId());
        xxlJobInfoHistory.setJobSource(JacksonUtil.writeValueAsString(jobInfo));
        xxlJobInfoHistory.setJobRemark(jobInfo.getJobDesc());
        xxlJobInfoHistory.setAddTime(new Date());
        jobInfoHistoryDao.save(xxlJobInfoHistory);
        // remove  backup more than 30
        jobInfoHistoryDao.removeOld(jobInfo.getId(), 30);

        // 在执行状态变更
        jobInfo.setTriggerStatus(1);
        jobInfo.setTriggerLastTime(0);
        jobInfo.setTriggerNextTime(nextTriggerTime);
        if (updateBy != null) {
            jobInfo.setUpdateBy(updateBy);
        }
        jobInfo.setUpdateTime(new Date());
        jobInfoDao.update(jobInfo);

        return ReturnT.SUCCESS;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<String> stop(int id, String updateBy) {
        logger.info("user stop Job,id:{},updateBy:{}", id, updateBy);

        JobInfo jobInfo = jobInfoDao.loadById(id);

        // 先记录变更历史
        JobInfoHistory jobInfoHistory = new JobInfoHistory();
        jobInfoHistory.setJobId(jobInfo.getId());
        jobInfoHistory.setJobSource(JacksonUtil.writeValueAsString(jobInfo));
        jobInfoHistory.setJobRemark(jobInfo.getJobDesc());
        jobInfoHistory.setAddTime(new Date());
        jobInfoHistoryDao.save(jobInfoHistory);
        // remove  backup more than 30
        jobInfoHistoryDao.removeOld(jobInfo.getId(), 30);
        // 在执行数据变更
        jobInfo.setTriggerStatus(0);
        jobInfo.setTriggerLastTime(0);
        jobInfo.setTriggerNextTime(0);
        if (updateBy != null) {
            jobInfo.setUpdateBy(updateBy);
        }
        jobInfo.setUpdateTime(new Date());
        jobInfoDao.update(jobInfo);
        return ReturnT.SUCCESS;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<JobInfo> loadById(int id) {
        return new ReturnT<>(jobInfoDao.loadById(id));
    }

    /**
     * 加载Job
     *
     * @param ides 主键ID
     * @return Job
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<List<JobInfo>> loadByIdes(List<Integer> ides) {
        return new ReturnT<>(jobInfoDao.loadByIdes(ides));
    }

    /**
     * 加载Job列表
     *
     * @param jobGroup 执行器
     * @return Job列表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<List<JobInfo>> loadByJobGroup(int jobGroup) {
        return new ReturnT<>(jobInfoDao.getJobsByGroup(jobGroup));
    }

    /**
     * 加载Job变更历史
     *
     * @param id 主键ID
     * @return Job History
     */
    @Override
    public ReturnT<List<JobInfoHistory>> loadHistoryById(int id) {
        return new ReturnT<>(jobInfoHistoryDao.findByJobId(id));
    }

    @Cacheable(value = "dashboardCache", key = "'dashboardInfo-'+#jobGroups")
    @Override
    public Map<String, Object> dashboardInfo(List<Integer> jobGroups) {
        if (CollectionUtils.isEmpty(jobGroups)) {
            jobGroups = jobGroupDao.findAllId();
        }
        int jobLogCount = 0;
        int jobLogSuccessCount = 0;
        int jobLogFailedCount = 0;
        int jobLogRunningCount = 0;
        JobLogReport jobLogReport = jobLogReportDao.queryLogReportTotalByGroups(jobGroups);
        if (jobLogReport != null) {
            jobLogCount = jobLogReport.getRunningCount() + jobLogReport.getSucCount() + jobLogReport.getFailCount();
            jobLogSuccessCount = jobLogReport.getSucCount();
            jobLogFailedCount = jobLogReport.getFailCount();
            jobLogRunningCount = jobLogReport.getRunningCount();
        }

        // executor count
        Set<String> executorAddressSet = new HashSet<>();
        List<JobGroup> groupList = jobGroupDao.pageList(0, Integer.MAX_VALUE, null, null, jobGroups);

        if (!CollectionUtils.isEmpty(groupList)) {
            for (JobGroup group : groupList) {
                if (group.getRegistryList() != null && !group.getRegistryList().isEmpty()) {
                    executorAddressSet.addAll(group.getRegistryList());
                }
            }
        }
        Map<String, Integer> jobInfoCountMap = jobInfoDao.triggerStatusCountByGroups(jobGroups);
        int executorCount = executorAddressSet.size();
        Map<String, Object> dashboardMap = new HashMap<>();
        if (CollectionUtils.isEmpty(jobInfoCountMap)) {
            dashboardMap.put("jobInfoCount", 0);
            dashboardMap.put("jobInfoRunningCount", 0);
            dashboardMap.put("jobInfoStoppedCount", 0);
        } else {
            dashboardMap.put("jobInfoCount", jobInfoCountMap.get("totalSum"));
            dashboardMap.put("jobInfoRunningCount", jobInfoCountMap.get("runningSum"));
            dashboardMap.put("jobInfoStoppedCount", jobInfoCountMap.get("stoppedSum"));
        }
        dashboardMap.put("jobLogCount", jobLogCount);
        dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
        dashboardMap.put("jobLogFailedCount", jobLogFailedCount);
        dashboardMap.put("jobLogRunningCount", jobLogRunningCount);

        dashboardMap.put("executorCount", executorCount);
        return dashboardMap;
    }

    @Override
    @Cacheable(value = "dashboardCache", key = "'chartInfo-'+#jobGroups+#startDate+#endDate")
    public ReturnT<List<Map<String, Object>>> chartInfo(List<Integer> jobGroups, Date startDate, Date endDate) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<JobGroup> groupList;
        if (CollectionUtils.isEmpty(jobGroups)) {
            groupList = jobGroupDao.findAll();
        } else {
            groupList = jobGroupDao.loadByIds(jobGroups);
        }
        if (!CollectionUtils.isEmpty(groupList)) {
            // 构造全部的triggerDay
            int betweenDays = (int) (endDate.getTime() - startDate.getTime()) / (1000 * 3600 * 24);
            Set<String> triggerDaySet = new HashSet<>();
            for (int i = -betweenDays; i <= 0; i++) {
                triggerDaySet.add(DateUtil.formatDate(DateUtil.addDays(new Date(), i)));
            }
            for (JobGroup group : groupList) {
                Map<String, Integer> triggerStatusCountMap = jobInfoDao.triggerStatusCount(group.getId());
                Map<String, HashMap<String, Object>> triggerDayMapMap = new TreeMap<>();
                int triggerCountRunningTotal = 0;
                int triggerCountSucTotal = 0;
                int triggerCountFailTotal = 0;
                List<JobLogReport> logReportList = jobLogReportDao.queryLogReport(group.getId(), startDate, endDate);
                if (!CollectionUtils.isEmpty(logReportList)) {
                    for (JobLogReport item : logReportList) {
                        String day = DateUtil.formatDate(item.getTriggerDay());
                        int triggerDayCountRunning = item.getRunningCount();
                        int triggerDayCountSuc = item.getSucCount();
                        int triggerDayCountFail = item.getFailCount();
                        triggerCountRunningTotal += triggerDayCountRunning;
                        triggerCountSucTotal += triggerDayCountSuc;
                        triggerCountFailTotal += triggerDayCountFail;
                        HashMap<String, Object> triggerDayMap = triggerDayMapMap.get(day);
                        if (triggerDayMap == null) {
                            triggerDayMap = new HashMap<>(5);
                            triggerDayMap.put("triggerDay", day);
                            triggerDayMap.put("triggerDayCountRunning", triggerDayCountRunning);
                            triggerDayMap.put("triggerDayCountSuc", triggerDayCountSuc);
                            triggerDayMap.put("triggerDayCountFail", triggerDayCountFail);
                            triggerDayMap.put("triggerDayTotal", triggerDayCountRunning + triggerDayCountSuc + triggerDayCountFail);
                            triggerDayMapMap.put(day, triggerDayMap);
                        } else {
                            triggerDayMap.put("triggerDayCountRunning", triggerDayCountRunning + (int) triggerDayMap.get("triggerDayCountRunning"));
                            triggerDayMap.put("triggerDayCountSuc", triggerDayCountSuc + (int) triggerDayMap.get("triggerDayCountSuc"));
                            triggerDayMap.put("triggerDayCountFail", triggerDayCountFail + (int) triggerDayMap.get("triggerDayCountFail"));
                            triggerDayMap.put("triggerDayTotal", triggerDayCountRunning + triggerDayCountSuc + triggerDayCountFail + (int) triggerDayMap.get("triggerDayTotal"));
                        }
                    }
                    Set<String> otherDaySet = new HashSet<>(triggerDaySet);
                    otherDaySet.removeAll(triggerDayMapMap.keySet());
                    for (String otherDay : otherDaySet) {
                        HashMap<String, Object> triggerDayMap = new HashMap<>(5);
                        triggerDayMap.put("triggerDay", otherDay);
                        triggerDayMap.put("triggerDayCountRunning", 0);
                        triggerDayMap.put("triggerDayCountSuc", 0);
                        triggerDayMap.put("triggerDayCountFail", 0);
                        triggerDayMap.put("triggerDayTotal", 0);
                        triggerDayMapMap.put(otherDay, triggerDayMap);
                    }
                } else {
                    for (int i = -betweenDays; i <= 0; i++) {
                        HashMap<String, Object> triggerDayMap = new HashMap<>(5);
                        triggerDayMap.put("triggerDay", DateUtil.formatDate(DateUtil.addDays(new Date(), i)));
                        triggerDayMap.put("triggerDayCountRunning", 0);
                        triggerDayMap.put("triggerDayCountSuc", 0);
                        triggerDayMap.put("triggerDayCountFail", 0);
                        triggerDayMap.put("triggerDayTotal", 0);
                        triggerDayMapMap.put(DateUtil.formatDate(DateUtil.addDays(new Date(), i)), triggerDayMap);
                    }
                }
                Map<String, Object> result = new HashMap<>();
                result.put("triggerDayMapList", triggerDayMapMap.values());
                result.put("triggerCountRunningTotal", triggerCountRunningTotal);
                result.put("triggerCountSucTotal", triggerCountSucTotal);
                result.put("triggerCountFailTotal", triggerCountFailTotal);
                result.put("triggerCountTotal", triggerCountRunningTotal + triggerCountSucTotal + triggerCountFailTotal);
                if (triggerStatusCountMap == null) {
                    result.put("triggerJobTotal", 0);
                    result.put("triggerJobRunningTotal", 0);
                    result.put("triggerJobStoppedTotal", 0);
                } else {
                    result.put("triggerJobTotal", triggerStatusCountMap.get("totalSum"));
                    result.put("triggerJobRunningTotal", triggerStatusCountMap.get("runningSum"));
                    result.put("triggerJobStoppedTotal", triggerStatusCountMap.get("stoppedSum"));
                }
                result.put("jobGroup", group.getId());
                result.put("appname", group.getAppname());
                resultList.add(result);
            }
        }
        return new ReturnT<>(resultList);
    }

    /**
     * 待执行任务报表
     *
     * @param jobGroups 执行器集合
     * @return 待执行任务报表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<List<Map<String, Object>>> nextTriggerTimeReport(List<Integer> jobGroups) {
        List<Map<String, Object>> reportList = new ArrayList<>();
        List<JobGroup> xxlJobGroups;
        if (CollectionUtils.isEmpty(jobGroups)) {
            xxlJobGroups = jobGroupDao.findAll();
        } else {
            xxlJobGroups = jobGroupDao.loadByIds(jobGroups);
        }
        JobInfo paramJobInfo = new JobInfo();
        for (JobGroup group : xxlJobGroups) {
            List<JobInfo> jobInfos = jobInfoDao.getJobsByGroup(group.getId());
            if (!CollectionUtils.isEmpty(jobInfos)) {
                for (JobInfo jobInfo : jobInfos) {
                    if (!StringUtils.isEmpty(jobInfo.getScheduleConf())) {
                        try {
                            paramJobInfo.setScheduleType(jobInfo.getScheduleType());
                            paramJobInfo.setScheduleConf(jobInfo.getScheduleConf());
                            Date nextDate = JobScheduleHelper.generateNextValidTime(paramJobInfo, new Date());
                            if (nextDate != null && jobInfo.getTriggerStatus() == JobInfo.TRIGGER_STATUS_RUNNING) {
                                //只插入有执行时间的数据
                                Map<String, Object> reportMap = new HashMap<>(6);
                                reportMap.put("groupId", group.getId());
                                reportMap.put("appName", group.getAppname());
                                reportMap.put("jobName", jobInfo.getJobName());
                                reportMap.put("jobDesc", jobInfo.getJobDesc());
                                reportMap.put("author", jobInfo.getAuthor());

                                reportMap.put("nextTriggerTime", DateUtil.formatDateTime(nextDate));
                                //更新list
                                reportList.add(reportMap);
                            }
                        } catch (Exception e) {
                            logger.error("nextTriggerTimeReport error:", e);
                        }
                    }
                }
            }

        }
        //排序
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 按照"nextTriggerTime"字段升序排序
//        reportList.sort(Comparator.comparing(reportMap -> {
//            String nextTriggerTime = (String) reportMap.get("nextTriggerTime");
//            try {
//                return dateFormat.parse(nextTriggerTime);
//            } catch (Exception e) {
//                throw new RuntimeException("Invalid date format: " + nextTriggerTime, e);
//            }
//        }));
        return new ReturnT<>(reportList);
    }

    /**
     * 此accessToken能否处理对应的任务
     *
     * @param accessToken 访问token
     * @param jobId       任务id
     * @return 可以返回true, 否则返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean canHandleJob(String accessToken, int jobId, String permissionUrl) {
        OpenApi openApi = xxlOpenApiDao.loadByAccessToken(accessToken);
        if (openApi == null) {
            return false;
        }
        List<JobInfo> jobInfos = jobInfoDao.getJobsByGroup(openApi.getJobGroup());
        if (jobInfos == null) {
            return false;
        }
        JobInfo handleJobInfo = null;
        for (JobInfo jobInfo : jobInfos) {
            if (jobInfo.getId() == jobId) {
                handleJobInfo = jobInfo;
            }
        }
        if (handleJobInfo == null) {
            return false;
        }
        return canHandleJobBy(accessToken, permissionUrl, handleJobInfo.getAppName(), handleJobInfo.getJobName());
    }

    /**
     * 此accessToken能否处理对应的任务
     *
     * @param accessToken   访问token
     * @param jobGroup      任务组
     * @param jobName       job名称
     * @param permissionUrl 被允许的URL
     * @return 可以返回true, 否则返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean canHandleJobBy(String accessToken, String permissionUrl, String jobGroup, String jobName) {
        OpenApi openApi = xxlOpenApiDao.loadByAccessToken(accessToken);
        if (openApi == null) {
            return false;
        }
        if (Strings.isEmpty(openApi.getUrls())) {
            return false;
        }
        List<String> urls = Arrays.asList(openApi.getUrls().split(","));
        if (!urls.contains(permissionUrl)) {
            return false;
        }
        List<JobInfo> jobInfos = jobInfoDao.getJobsByGroup(openApi.getJobGroup());
        if (jobInfos == null) {
            return false;
        }
        JobGroup xxlJobGroup = jobGroupDao.loadByName(jobGroup);
        if (xxlJobGroup == null) {
            return false;
        }
        for (JobInfo jobInfo : jobInfos) {
            if (jobInfo.getJobName().equals(jobName) && xxlJobGroup.getId() == jobInfo.getJobGroup()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 此accessToken能否处理对应的任务
     *
     * @param accessToken 访问token
     * @param jobGroup    组ID
     * @return 可以返回true, 否则返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean canHandleJobByGroup(String accessToken, int jobGroup) {
        OpenApi openApi = xxlOpenApiDao.loadByAccessToken(accessToken);
        if (openApi == null) {
            return false;
        }
        JobGroup xxlJobGroup = jobGroupDao.load(jobGroup);
        if (xxlJobGroup == null) {
            return false;
        }
        if (!openApi.getJobGroup().equals(jobGroup)) {
            return false;
        }
        return true;
    }

    /**
     * 设置延迟
     *
     * @param jobIdes         jobId集合
     * @param delayTimeLength 延迟时长
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<String> setTriggerDelay(List<Integer> jobIdes, Long delayTimeLength) {
        if (CollectionUtils.isEmpty(jobIdes)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_ides_not_null")));
        }
        List<JobInfo> jobInfoList = jobInfoDao.loadByIdes(jobIdes);
        if (CollectionUtils.isEmpty(jobInfoList)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_not_exist")));
        }
        for (JobInfo jobInfo : jobInfoList) {
            if (jobInfo.getTriggerNextTime() <= 0) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_next_trigger_time_should_gt_zero")));
            }
            if ("NONE".equals(jobInfo.getScheduleType())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_schedule_type_not_none")));
            }
        }
        for (JobInfo jobInfo : jobInfoList) {
            jobInfoDao.triggerNextTime(jobInfo.getId(), jobInfo.getTriggerNextTime() + delayTimeLength);
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 查询Job信息，job按照jobName和appName唯一
     *
     * @param jobName job名称
     * @param appName 执行器名称
     * @return 存在返回job，不存在返回null
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<JobInfo> queryBy(String jobName, String appName) {
        return ReturnT.ofSuccess(jobInfoDao.queryBy(jobName, appName));
    }

    /**
     * 更新路由标签
     *
     * @param jobInfo 任务
     * @return 更新成功返回1
     */
    @Override
    public int updateRouterFlag(JobInfo jobInfo) {
        return jobInfoDao.updateRouterFlag(jobInfo);
    }

    /**
     * 根据JobId获取group信息
     *
     * @param jobId 任务ID
     * @return group ID
     */
    @Cacheable(value = "jobInfoCache", key = "'loadGroupBy-'+#jobId")
    @Override
    public Integer loadGroupBy(Long jobId) {
        return jobInfoDao.loadGroupBy(jobId);
    }

    @Cacheable(value = "jobInfoCache", key = "'loadByIdCached-'+#id")
    @Override
    public JobInfo loadByIdCached(Integer id) {
        return jobInfoDao.loadById(id);
    }

    /**
     * 获取缓存后的group
     *
     * @param groupId groupId
     * @return 缓存后的group
     */
    @Cacheable(value = "jobGroupCache", key = "'loadByIdCached-'+#groupId")
    @Override
    public JobGroup loadGroupCached(Integer groupId) {
        return jobGroupDao.load(groupId);
    }

    @Cacheable(value = "jobGroupCache", key = "'findAllId'")
    @Override
    public List<Integer> findAllGroupIdCached() {
        return jobGroupDao.findAllId();
    }
}
