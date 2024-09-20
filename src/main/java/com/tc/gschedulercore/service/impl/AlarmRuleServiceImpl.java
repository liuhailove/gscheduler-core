package com.tc.gschedulercore.service.impl;

import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.core.util.TelUtils;
import com.tc.gschedulercore.dao.*;
import com.tc.gschedulercore.enums.RetryType;
import com.tc.gschedulercore.service.AlarmRuleService;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.tc.gschedulercore.core.cron.CronExpression;

import javax.annotation.Resource;
import javax.script.Invocable;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tc.gschedulercore.core.thread.JobCheckHelper.*;
import static com.tc.gschedulercore.core.thread.JobESHelper.*;


/**
 * 告警规则服务
 *
 * @author honggang.liu
 */
@Service
public class AlarmRuleServiceImpl implements AlarmRuleService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmRuleServiceImpl.class.getSimpleName());

    /**
     * 告警规则DAO
     */
    @Resource
    private AlarmRuleDao alarmRuleDao;

    /**
     * 告警项DAO
     */
    @Resource
    private AlarmItemDao alarmItemDao;

    /**
     * 执行器DAO
     */
    @Resource
    private JobGroupDao jobGroupDao;

    /**
     * job检查Dao
     */
    @Resource
    private JobCheckDao jobCheckDao;

    /**
     * 告警脚本DAO
     */
    @Resource
    private AlarmScriptDao alarmScriptDao;

    /**
     * 脚本Item Dao
     */
    @Resource
    private AlarmScriptItemDao alarmScriptItemDao;

    /**
     * 规则保存
     *
     * @param alarmRule 告警规则
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<Integer> add(AlarmRule alarmRule) {
        ReturnT<Integer> basicCheck = checkEntityInternal(alarmRule);
        if (basicCheck != null) {
            return basicCheck;
        }
        // valid base
        JobGroup group = jobGroupDao.load(alarmRule.getJobGroupId());
        if (group == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_field_jobgroup")));
        }
        alarmRule.setAlarmName(alarmRule.getAlarmName().trim());
        alarmRule.setJobGroupId(alarmRule.getJobGroupId());
        alarmRule.setJobGroupName(group.getAppname());
        alarmRule.setNotifyUrl(alarmRule.getNotifyUrl().trim());
        Boolean exist = alarmRuleDao.existBy(alarmRule.getJobGroupId(), alarmRule.getAlarmName());
        if (Boolean.TRUE.equals(exist)) {
            return ReturnT.ofFail(-1, I18nUtil.getString("alarm_name_has_set"));
        }
        if (AlarmRule.SEVERITY_LEVEL.equals(alarmRule.getAlarmLevel())) {
            //校验电话是否合法，不合法返回错误信息
            if (!org.springframework.util.StringUtils.hasLength(alarmRule.getVoiceAlarmTels())) {
                return ReturnT.ofFail(-1, I18nUtil.getString("tel_not_invalid"));
            }
            // 使用正则表达式匹配手机号码格式
            String[] phoneNumbers = alarmRule.getVoiceAlarmTels().split(",");
            //提示框写：填写大陆境内电话，不需要写86前缀，使用,分割，一个电话错误全部拨打失败
            for (String phoneNumber : phoneNumbers) {
                //如果有一个电话不满足正则表达式，就不打电话
                if (!TelUtils.isValidPhoneNumber(phoneNumber)) {
                    return ReturnT.ofFail(-1, I18nUtil.getString("tel not invalid"));
                }
            }
            alarmRule.setVoiceAlarmTels(alarmRule.getVoiceAlarmTels());
        }
        alarmRule.setGmtCreate(new Date());
        alarmRule.setGmtModified(new Date());
        int ret = alarmRuleDao.save(alarmRule);
        return ReturnT.ofSuccess(ret);
    }

    /**
     * 规则更新
     *
     * @param alarmRule 告警规则
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<Integer> update(AlarmRule alarmRule) {
        ReturnT<Integer> basicCheck = checkEntityInternal(alarmRule);
        if (basicCheck != null) {
            return basicCheck;
        }
        alarmRule.setAlarmName(alarmRule.getAlarmName().trim());
        alarmRule.setNotifyUrl(alarmRule.getNotifyUrl().trim());
        alarmRule.setGmtModified(new Date());
        // valid base
        JobGroup group = jobGroupDao.load(alarmRule.getJobGroupId());
        if (group == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_field_jobgroup")));
        }
        if (AlarmRule.SEVERITY_LEVEL.equals(alarmRule.getAlarmLevel())) {
            //校验电话是否合法，不合法返回错误信息
            if (!org.springframework.util.StringUtils.hasLength(alarmRule.getVoiceAlarmTels())) {
                return ReturnT.ofFail(-1, I18nUtil.getString("tel_not_invalid"));
            }
            // 使用正则表达式匹配手机号码格式
            String[] phoneNumbers = alarmRule.getVoiceAlarmTels().split(",");
            //提示框写：填写大陆境内电话，不需要写86前缀，使用,分割，一个电话错误全部拨打失败
            for (String phoneNumber : phoneNumbers) {
                //如果有一个电话不满足正则表达式，就不打电话
                if (!TelUtils.isValidPhoneNumber(phoneNumber)) {
                    return ReturnT.ofFail(-1, I18nUtil.getString("tel not invalid"));
                }
            }
            alarmRule.setVoiceAlarmTels(alarmRule.getVoiceAlarmTels());
        }
        alarmRule.setJobGroupId(alarmRule.getJobGroupId());
        alarmRule.setJobGroupName(group.getAppname());
        AlarmRule oldAlarmRule = alarmRuleDao.load(alarmRule.getId());
        if (oldAlarmRule == null) {
            return ReturnT.ofFail(-1, "【" + alarmRule.getId() + "】" + I18nUtil.getString("resource_not_exist"));
        }
        if (!oldAlarmRule.getJobGroupId().equals(alarmRule.getJobGroupId()) || !oldAlarmRule.getAlarmName().equals(alarmRule.getAlarmName())) {
            Boolean exist = alarmRuleDao.existBy(alarmRule.getJobGroupId(), alarmRule.getAlarmName());
            if (Boolean.TRUE.equals(exist)) {
                return ReturnT.ofFail(-1, I18nUtil.getString("resource_has_set"));
            }
        }
        int ret = alarmRuleDao.update(alarmRule);
        return ReturnT.ofSuccess(ret);
    }

    /**
     * 移除规则
     *
     * @param id 规则ID
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<Integer> remove(Long id) {
        // 删除jobCheck
        jobCheckDao.deleteByAlarm(id);
        // 删除配置项
        alarmItemDao.removeBy(id);
        // 删除script
        alarmScriptDao.removeBy(id);
        // 删除script item
        alarmScriptItemDao.removeBy(id);
        // 删除规则
        int ret = alarmRuleDao.remove(id);
        return ReturnT.ofSuccess(ret);
    }

    /**
     * 分页查找
     *
     * @param offset         偏移量
     * @param pageSize       分页大小
     * @param resourceType   资源类型
     * @param alarmLevel     告警级别
     * @param alarmName      告警名称
     * @param permissionApps 授权应用
     * @return 记录数
     */
    @Override
    public List<AlarmRule> pageList(int offset, int pageSize, Integer resourceType, Integer alarmLevel, String alarmName, List<Integer> permissionApps) {
        return alarmRuleDao.pageList(offset, pageSize, resourceType, alarmLevel, alarmName, permissionApps);
    }

    /**
     * 分页查找
     *
     * @param offset         偏移量
     * @param pageSize       分页大小
     * @param resourceType   资源类型
     * @param alarmLevel     告警级别
     * @param alarmName      告警名称
     * @param permissionApps 授权应用
     * @return 记录数
     */
    @Override
    public int pageListCount(int offset, int pageSize, Integer resourceType, Integer alarmLevel, String alarmName, List<Integer> permissionApps) {
        return alarmRuleDao.pageListCount(offset, pageSize, resourceType, alarmLevel, alarmName, permissionApps);
    }

    /**
     * 根据应用名称查询告警规则
     *
     * @param app 应用名称
     * @return 规则集合
     */
    @Override
    public List<AlarmRule> queryByApp(String app) {
        return alarmRuleDao.queryByApp(app);
    }

    /**
     * 查询规则集合
     *
     * @param jobGroupId 执行器ID
     * @param jobId      任务ID
     * @return 规则集合
     */
    @Cacheable(value = "alarmRuleCache", key = "'alarm-'+#jobGroupId+'-'+#jobId")
    @Override
    public List<AlarmRule> findByJobGroupAndJobId(@Param("jobGroupId") Integer jobGroupId, @Param("jobId") Integer jobId) {
        return alarmRuleDao.findByJobGroupAndJobId(jobGroupId, jobId);
    }

    /**
     * 判断app是否存在告警规则
     *
     * @param app 应用名称
     * @return 存在返回true，否则返回false或者NULL
     */
    @Override
    public Boolean exist(String app) {
        return alarmRuleDao.exist(app);
    }

    /**
     * 规则ID
     *
     * @param id 主键ID
     * @return 告警规则
     */
    @Override
    public AlarmRule load(Long id) {
        return alarmRuleDao.load(id);
    }

    /**
     * 规则保存
     *
     * @param alarmItem 告警规则条目
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<Integer> addItem(AlarmItem alarmItem) {
        if (alarmItem.getResourceType() != null && alarmItem.getResourceType().equals(AlarmRule.EXECUTOR_LOG) && alarmItem.getCheckPeriodInMin() == null) {
            return ReturnT.ofFail(-1, I18nUtil.getString("alarm_check_period_can_not_be_null"));
        }
        Boolean exist = alarmItemDao.existBy(alarmItem.getAlarmRuleId(), alarmItem.getResourceType(), alarmItem.getAlarmType());
        if (Boolean.TRUE.equals(exist)) {
            return ReturnT.ofFail(-1, I18nUtil.getString("alarm_item_type_has_set"));
        }
        alarmItem.setGmtCreate(new Date());
        alarmItem.setGmtModified(new Date());
        int ret = alarmItemDao.save(alarmItem);
        // 如果是执行日志，则需要同步关联check
        if (AlarmRule.PROCESS_LOG.equals(alarmItem.getResourceType())) {
            return ReturnT.ofSuccess(ret);
        }
        AlarmRule alarmRule = alarmRuleDao.load(alarmItem.getAlarmRuleId());
        String jobIdes = alarmRule.getJobIdes();
        if ("".equals(StringUtils.trim(jobIdes)) || jobIdes == null) {
            return ReturnT.ofSuccess(ret);
        }
        for (String jobIdStr : jobIdes.split(",")) {
            int jobId = Integer.parseInt(jobIdStr);
            // 先查询
            JobCheck jobCheck = jobCheckDao.queryByJob(alarmRule.getJobGroupId(), jobId);
            if (jobCheck == null) {
                jobCheck = new JobCheck();
                jobCheck.setJobGroup(alarmRule.getJobGroupId());
                jobCheck.setJobId(jobId);
                jobCheck.setAlarmRuleId(alarmRule.getId());
                jobCheck.setTriggerFixedRateInMin(alarmItem.getCheckPeriodInMin());
                jobCheck.setTriggerLastTime(System.currentTimeMillis());
                jobCheck.setTriggerNextTime(System.currentTimeMillis() + alarmItem.getCheckPeriodInMin() * 60 * 1000);
                jobCheck.setGmtCreate(new Date());
                jobCheckDao.save(jobCheck);
            } else {
                jobCheck.setTriggerFixedRateInMin(alarmItem.getCheckPeriodInMin());
                jobCheck.setTriggerNextTime(System.currentTimeMillis() + alarmItem.getCheckPeriodInMin() * 60 * 1000);
                jobCheckDao.update(jobCheck);
            }
        }
        return ReturnT.ofSuccess(ret);
    }

    /**
     * 流控规则更新
     *
     * @param alarmItem 告警规则条目
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<Integer> updateItem(AlarmItem alarmItem) {
        alarmItem.setGmtModified(new Date());
        AlarmItem oldAlarmItem = alarmItemDao.load(alarmItem.getId());
        if (oldAlarmItem == null) {
            return ReturnT.ofFail(-1, "【" + alarmItem.getId() + "】" + I18nUtil.getString("resource_not_exist"));
        }
        if (!oldAlarmItem.getAlarmType().equals(alarmItem.getAlarmType()) || !oldAlarmItem.getResourceType().equals(alarmItem.getResourceType())) {
            Boolean exist = alarmItemDao.existBy(alarmItem.getAlarmRuleId(), alarmItem.getResourceType(), alarmItem.getAlarmType());
            if (Boolean.TRUE.equals(exist)) {
                return ReturnT.ofFail(-1, I18nUtil.getString("resource_has_set"));
            }
        }
        // 执行日志，需要同步更新jobCheck
        if (AlarmRule.EXECUTOR_LOG.equals(alarmItem.getResourceType()) &&
                alarmItem.getCheckPeriodInMin() != null &&
                !alarmItem.getCheckPeriodInMin().equals(oldAlarmItem.getCheckPeriodInMin())) {
            List<JobCheck> jobChecks = jobCheckDao.queryByAlarm(alarmItem.getAlarmRuleId());
            if (!CollectionUtils.isEmpty(jobChecks)) {
                for (JobCheck jobCheck : jobChecks) {
                    jobCheck.setTriggerFixedRateInMin(alarmItem.getCheckPeriodInMin());
                    jobCheck.setTriggerNextTime(jobCheck.getTriggerLastTime() + alarmItem.getCheckPeriodInMin() * 60 * 1000);
                    jobCheckDao.update(jobCheck);
                }
            }
        }
        return ReturnT.ofSuccess(alarmItemDao.update(alarmItem));
    }

    /**
     * 移除规则
     *
     * @param ruleItemId 规则项ID
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<Integer> removeItem(Long ruleItemId) {
        AlarmItem alarmItem = alarmItemDao.load(ruleItemId);
        // 执行日志，只有一条记录，因此删除全部的jobCheck
        if (AlarmRule.EXECUTOR_LOG.equals(alarmItem.getResourceType())) {
            jobCheckDao.deleteByAlarm(alarmItem.getAlarmRuleId());
        }
        return ReturnT.ofSuccess(alarmItemDao.remove(ruleItemId));
    }

    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    @Override
    public List<AlarmItem> itemsPageList(int offset, int pageSize, Long alarmRuleId) {
        return alarmItemDao.pageList(offset, pageSize, alarmRuleId);
    }

    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    @Override
    public int itemsPageListCount(int offset, int pageSize, Long alarmRuleId) {
        return alarmItemDao.pageListCount(offset, pageSize, alarmRuleId);
    }

    /**
     * 规则条目ID
     *
     * @param ruleItemId 主键ID
     * @return 告警规则条目
     */
    @Override
    public AlarmItem loadItem(Long ruleItemId) {
        return alarmItemDao.load(ruleItemId);
    }

    /**
     * 关联资源集合
     *
     * @param ruleId  规则ID
     * @param jobIdes 任务集合
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<Integer> createRelation(Long ruleId, Integer[] jobIdes) throws ParseException {
        if (jobIdes == null || jobIdes.length == 0) {
            jobIdes = new Integer[]{};
        }
        List<Integer> filterJobIdList = Arrays.stream(jobIdes).filter(Objects::nonNull).collect(Collectors.toList());
        AlarmRule alarmRule = alarmRuleDao.load(ruleId);
        if (AlarmRule.EXECUTOR_LOG.equals(alarmRule.getResourceType())) {
            for (Integer jobId : filterJobIdList) {
                // 先查询，目前是如果之前已经设置了，就用之前的最后一次触发时间
                Boolean exist = jobCheckDao.exist(alarmRule.getJobGroupId(), jobId);
                if (Boolean.TRUE.equals(exist)) {
                    continue;
                }
                JobCheck jobCheck = new JobCheck();
                jobCheck.setJobGroup(alarmRule.getJobGroupId());
                jobCheck.setJobId(jobId);
                jobCheck.setAlarmRuleId(alarmRule.getId());
                List<AlarmItem> alarmItemList = alarmItemDao.queryByAlarmRule(alarmRule.getId());
                if (!CollectionUtils.isEmpty(alarmItemList)) {
                    // 执行日志下，只会有一条记录
                    AlarmItem alarmItem = alarmItemList.get(0);
                    jobCheck.setTriggerFixedRateInMin(alarmItem.getCheckPeriodInMin());
                    jobCheck.setTriggerLastTime(System.currentTimeMillis());
                    jobCheck.setTriggerNextTime(System.currentTimeMillis() + alarmItem.getCheckPeriodInMin() * 60 * 1000);
                    jobCheck.setGmtCreate(new Date());
                    jobCheckDao.save(jobCheck);
                }
            }
        } else {
            for (Integer jobId : filterJobIdList) {
                // 设置alarmScriptItem
                List<AlarmScript> alarmScriptList = alarmScriptDao.queryBy(alarmRule.getId());
                if (CollectionUtils.isEmpty(alarmScriptList)) {
                    continue;
                }
                for (AlarmScript alarmScript : alarmScriptList) {
                    AlarmScriptItem alarmScriptItem = alarmScriptItemDao.queryBy(alarmRule.getId(), alarmScript.getId(), alarmRule.getJobGroupId(), jobId);
                    if (alarmScriptItem == null) {
                        alarmScriptItem = new AlarmScriptItem();
                        alarmScriptItem.setAlarmRuleId(alarmRule.getId());
                        alarmScriptItem.setAlarmScriptId(alarmScript.getId());
                        alarmScriptItem.setAlarmCheckExp(alarmScript.getAlarmCheckExp());
                        alarmScriptItem.setAlarmMsgExp(alarmScript.getAlarmMsgExp());
                        alarmScriptItem.setCronExp(alarmScript.getCronExp());
                        alarmScriptItem.setJobGroup(alarmRule.getJobGroupId());
                        alarmScriptItem.setJobId(jobId);
                        alarmScriptItem.setTriggerLastTime(System.currentTimeMillis());
                        alarmScriptItem.setRetryConf(alarmScript.getScriptRetryConf());
                        alarmScriptItem.setScriptRetryCount(alarmScript.getScriptRetryCount());
                        alarmScriptItem.setRetryType(alarmScript.getRetryType());
                        alarmScriptItem.setTriggerNextTime(0);
                        alarmScriptItem.setGmtCreate(new Date());
                        alarmScriptItemDao.save(alarmScriptItem);
                    }
                }
            }
        }
        // 移除jobCheck
        List<Integer> jobIdList = jobCheckDao.queryJobIdByAlarm(ruleId);
        if (!CollectionUtils.isEmpty(jobIdList)) {
            jobIdList.removeAll(filterJobIdList);
            for (Integer jobId : jobIdList) {
                jobCheckDao.deleteBy(jobId);
            }
        }
        // 移除script item
        List<Integer> jobIdListBy = alarmScriptItemDao.queryJobIdByAlarm(ruleId);
        if (!CollectionUtils.isEmpty(jobIdListBy)) {
            jobIdListBy.removeAll(filterJobIdList);
            for (Integer jobId : jobIdListBy) {
                alarmScriptItemDao.removeByJob(ruleId, jobId);
            }
        }
        int ret = alarmRuleDao.createRelation(ruleId, StringUtils.join(filterJobIdList, ","), new Date());
        return ReturnT.ofSuccess(ret);
    }

    /**
     * 根据告警规则ID查告警规则条目
     *
     * @param alarmRuleId 告警规则ID
     * @return 规则集合
     */
    @Override
    public List<AlarmItem> queryByAlarmRule(Long alarmRuleId) {
        return alarmItemDao.queryByAlarmRule(alarmRuleId);
    }

    /**
     * 加载脚本
     *
     * @param alarmScriptId 脚本ID
     * @return 告警脚本
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AlarmScript loadScript(Long alarmScriptId) {
        return alarmScriptDao.load(alarmScriptId);
    }

    /**
     * 增加告警监控脚本
     *
     * @param alarmScript 告警监控脚本
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<Integer> addScript(AlarmScript alarmScript) throws ParseException {
        ReturnT<Integer> basicCheck = checkScript(alarmScript);
        if (basicCheck != null) {
            return basicCheck;
        }
        //判断是否有恶意代码
        String removeComment = StringUtils.replacePattern(alarmScript.getAlarmCheckExp(), "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
        String finalCode = StringUtils.replacePattern(removeComment, "\\s+", " ");
        Set<String> insecure = BLACKLIST.stream().filter(s -> StringUtils.containsIgnoreCase(finalCode, s))
                .collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(insecure)) {
            LOGGER.error("Warning! input string is insecure,alarmScriptId:{},jobGroupId:{}", alarmScript.getId(), alarmScript.getJobGroup());
            return ReturnT.ofFail(-1, "Warning! input string is insecure");
        }

        alarmScript.setScriptName(alarmScript.getScriptName().trim());
        alarmScript.setAlarmCheckExp(alarmScript.getAlarmCheckExp().trim());
        alarmScript.setAlarmMsgExp(alarmScript.getAlarmMsgExp().trim());
        alarmScript.setCronExp(alarmScript.getCronExp().trim());
//        // cron表达式校验
        if (alarmScript.getRetryType() == RetryType.CRON_TYPE) {
            if (alarmScript.getCronExp() == null || !CronExpression.isValidExpression(alarmScript.getCronExp())) {
                return ReturnT.ofFail(-1, "cron parser error");
            }
        }
        alarmScript.setScriptRetryConf(alarmScript.getScriptRetryConf().trim());
        // 脚本校验
        ReturnT<Integer> alarmCheck = checkScript(alarmScript.getAlarmCheckExp(), "alarmCheck");
        if (alarmCheck != null) {
            return alarmCheck;
        }
        ReturnT<Integer> alarmMsCheck = checkScript(alarmScript.getAlarmMsgExp(), "alarmMsg");
        if (alarmMsCheck != null) {
            return alarmMsCheck;
        }
        if (Boolean.TRUE.equals(alarmScriptDao.existBy(alarmScript.getAlarmRuleId(), alarmScript.getScriptName()))) {
            return ReturnT.ofFail(-1, "script name has exist");
        }
        AlarmRule alarmRule = alarmRuleDao.load(alarmScript.getAlarmRuleId());
        alarmScript.setJobGroup(alarmRule.getJobGroupId());
        alarmScript.setGmtCreate(new Date());
        alarmScript.setGmtModified(new Date());
        int result = alarmScriptDao.save(alarmScript);
        // 更新关联script item
        String jobIdes = alarmRule.getJobIdes();
        if (StringUtils.isEmpty(jobIdes)) {
            return ReturnT.ofSuccess(result);
        }
        jobIdes = jobIdes.trim();
        for (String jobIdStr : jobIdes.split(",")) {
            if (StringUtils.isEmpty(jobIdStr)) {
                continue;
            }
            int jobId = Integer.parseInt(jobIdStr);
            AlarmScriptItem alarmScriptItem = new AlarmScriptItem();
            alarmScriptItem.setAlarmRuleId(alarmRule.getId());
            alarmScriptItem.setAlarmScriptId(alarmScript.getId());
            alarmScriptItem.setAlarmCheckExp(alarmScript.getAlarmCheckExp());
            alarmScriptItem.setAlarmMsgExp(alarmScript.getAlarmMsgExp());
            alarmScriptItem.setScriptRetryCount(alarmScript.getScriptRetryCount());
            alarmScriptItem.setRetryType(alarmScript.getRetryType());
//            alarmScriptItem.setCronExp(alarmScript.getCronExp());
            alarmScriptItem.setRetryConf(alarmScript.getScriptRetryConf());
            alarmScriptItem.setJobGroup(alarmRule.getJobGroupId());
            alarmScriptItem.setJobId(jobId);
            alarmScriptItem.setTriggerLastTime(System.currentTimeMillis());
//            alarmScriptItem.setTriggerNextTime(new CronExpression(alarmScript.getCronExp()).getNextValidTimeAfter(new Date()).getTime());
            alarmScriptItem.setTriggerNextTime(0);
            alarmScriptItem.setCronExp("");
            alarmScriptItem.setGmtCreate(new Date());
            alarmScriptItemDao.save(alarmScriptItem);
        }
        return ReturnT.ofSuccess(result);
    }

    /**
     * 更新告警监控脚本
     *
     * @param alarmScript 告警监控脚本
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<Integer> updateScript(AlarmScript alarmScript) throws ParseException {
        if (alarmScript.getId() == null) {
            return ReturnT.ofFail(-1, "id can not be null");
        }

        ReturnT<Integer> basicCheck = checkScript(alarmScript);
        if (basicCheck != null) {
            return basicCheck;
        }

        //判断是否有恶意代码
        String removeComment = StringUtils.replacePattern(alarmScript.getAlarmCheckExp(), "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
        String finalCode = StringUtils.replacePattern(removeComment, "\\s+", " ");
        Set<String> insecure = BLACKLIST.stream().filter(s -> StringUtils.containsIgnoreCase(finalCode, s))
                .collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(insecure)) {
            LOGGER.error("Warning! input string is insecure,alarmScriptId:{},jobGroupId:{}", alarmScript.getId(), alarmScript.getJobGroup());
            return ReturnT.ofFail(-1, "Warning! input string is insecure");
        }

        alarmScript.setScriptName(alarmScript.getScriptName().trim());
        alarmScript.setAlarmCheckExp(alarmScript.getAlarmCheckExp().trim());
        alarmScript.setAlarmMsgExp(alarmScript.getAlarmMsgExp().trim());
//        alarmScript.setCronExp("");
        alarmScript.setGmtModified(new Date());
        // cron表达式校验
        if (alarmScript.getRetryType() == RetryType.CRON_TYPE) {
            if (alarmScript.getCronExp() == null || !CronExpression.isValidExpression(alarmScript.getCronExp())) {
                return ReturnT.ofFail(-1, "cron parser error");
            }
        } else {
            alarmScript.setCronExp("");
        }
        alarmScript.setScriptRetryConf(alarmScript.getScriptRetryConf().trim());
        // 脚本校验
        ReturnT<Integer> alarmCheck = checkScript(alarmScript.getAlarmCheckExp(), "alarmCheck");
        if (alarmCheck != null) {
            return alarmCheck;
        }
        ReturnT<Integer> alarmMsCheck = checkScript(alarmScript.getAlarmMsgExp(), "alarmMsg");
        if (alarmMsCheck != null) {
            return alarmMsCheck;
        }
        // 数据唯一校验
        AlarmScript oldAlarmScript = alarmScriptDao.load(alarmScript.getId());
        if (oldAlarmScript == null) {
            return ReturnT.ofFail(-1, "【" + alarmScript.getId() + "】not exist");
        }
        alarmScript.setAlarmRuleId(oldAlarmScript.getAlarmRuleId());
        AlarmRule alarmRule = alarmRuleDao.load(alarmScript.getAlarmRuleId());
        alarmScript.setJobGroup(alarmRule.getJobGroupId());
        if (!oldAlarmScript.getScriptName().equals(alarmScript.getScriptName()) && Boolean.TRUE.equals(alarmScriptDao.existBy(alarmScript.getAlarmRuleId(), alarmScript.getScriptName()))) {
            return ReturnT.ofFail(-1, "script name has exist");
        }
        int result = alarmScriptDao.update(alarmScript);
        // 更新关联script item
        String jobIdes = alarmRule.getJobIdes();
        if (StringUtils.isEmpty(jobIdes)) {
            return ReturnT.ofSuccess(result);
        }
        jobIdes = jobIdes.trim();
        for (String jobIdStr : jobIdes.split(",")) {
            int jobId = Integer.parseInt(jobIdStr);
            // 先查询
            AlarmScriptItem alarmScriptItem = alarmScriptItemDao.queryBy(alarmScript.getAlarmRuleId(), alarmScript.getId(), alarmRule.getJobGroupId(), jobId);
            alarmScriptItem.setAlarmCheckExp(alarmScript.getAlarmCheckExp());
            alarmScriptItem.setAlarmMsgExp(alarmScript.getAlarmMsgExp());
            alarmScriptItem.setCronExp(alarmScript.getCronExp());
            alarmScriptItem.setRetryConf(alarmScript.getScriptRetryConf());
            alarmScriptItem.setScriptRetryCount(alarmScript.getScriptRetryCount());
            alarmScriptItem.setRetryType(alarmScript.getRetryType());
            //alarmScriptItem.setTriggerNextTime(new CronExpression(alarmScript.getCronExp()).getNextValidTimeAfter(new Date(alarmScriptItem.getTriggerLastTime())).getTime());
            alarmScriptItem.setTriggerNextTime(0);
            alarmScriptItemDao.update(alarmScriptItem);
        }
        return ReturnT.ofSuccess(result);
    }

    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    @Override
    public List<AlarmScript> scriptsPageList(int offset, int pageSize, Long alarmRuleId) {
        return alarmScriptDao.pageList(offset, pageSize, alarmRuleId);
    }

    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    @Override
    public int scriptsPageListCount(int offset, int pageSize, Long alarmRuleId) {
        return alarmScriptDao.pageListCount(offset, pageSize, alarmRuleId);
    }

    /**
     * 移除脚本
     *
     * @param id 规则ID
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ReturnT<Integer> removeScript(Long id) {
        alarmScriptDao.remove(id);
        alarmScriptItemDao.removeByScript(id);
        return ReturnT.ofSuccess();
    }

    /**
     * 查询script item
     *
     * @param alarmRuleId 规则ID
     * @param jobId       任务ID
     * @return 列表信息
     */
    @Cacheable(value = "alarmRuleScriptItemCache", key = "'alarmItem-'+#alarmRuleId+'-'+#jobId")
    @Override
    public List<AlarmScriptItem> queryItemsByAlarmRuleAndJob(Long alarmRuleId, Integer jobId) {
        return alarmScriptItemDao.queryByAlarmRuleAndJob(alarmRuleId, jobId);
    }

    private ReturnT<Integer> checkEntityInternal(AlarmRule entity) {
        if (StringUtils.isBlank(entity.getAlarmName())) {
            return ReturnT.ofFail(-1, "alarm name can't be null or empty");
        }
        if (entity.getAlarmLevel() == null) {
            return ReturnT.ofFail(-1, "alarm level can't be null");
        }
        if (entity.getTriggerCondition() == null) {
            return ReturnT.ofFail(-1, "trigger condition can't be null");
        }
        if (entity.getOpen() == null) {
            return ReturnT.ofFail(-1, "open can't be null");
        }
        return null;
    }

    private ReturnT<Integer> checkScript(AlarmScript alarmScript) {
        if (StringUtils.isEmpty(alarmScript.getScriptName())) {
            return ReturnT.ofFail(-1, "script name can't be null or empty");
        }
        if (StringUtils.isEmpty(alarmScript.getAlarmCheckExp())) {
            return ReturnT.ofFail(-1, "alarm check exp  can't be null");
        }
        if (StringUtils.isEmpty(alarmScript.getAlarmMsgExp())) {
            return ReturnT.ofFail(-1, "alarm check msg exp  can't be null");
        }
//        if (StringUtils.isEmpty(alarmScript.getCronExp())) {
//            return ReturnT.ofFail(-1, "cron exp  can't be null");
//        }
        if (alarmScript.getRetryType() == RetryType.CRON_TYPE) {
            if (alarmScript.getCronExp() == null || !CronExpression.isValidExpression(alarmScript.getCronExp())) {
                return ReturnT.ofFail(-1, "cron parser error");
            }
        } else {
            if (alarmScript.getRetryType() == RetryType.CUSTOMER_TYPE) {
                String[] retryArr = alarmScript.getScriptRetryConf().split(",");
                for (String retryInterval : retryArr) {
                    try {
                        Integer.parseInt(retryInterval);
                    } catch (Exception e) {
                        return ReturnT.ofFail(-1, "script retry conf must be number");
                    }
                }
            } else {
                try {
                    Integer.parseInt(alarmScript.getScriptRetryConf());
                } catch (Exception e) {
                    return ReturnT.ofFail(-1, "script retry conf must be number");
                }
            }
        }

        if (alarmScript.getJobGroup() > 0) {
            return ReturnT.ofFail(-1, "job group should grater than zero");
        }
        return null;
    }

    /**
     * checkResult 结果检查
     */
    private ReturnT<Integer> checkScript(String script, String jsFuncName) {
        // gsSum匹配
        Matcher gsSumMatcher = GS_SUM_PATTERN.matcher(script);

        Matcher gsCountMatcher = GS_COUNT_PATTERN.matcher(script);

        Matcher gsESFuncMatcher = GS_GET_DATA_FROM_ES_PATTERN.matcher(script);

        Matcher gsTimeESMatcher = GS_NOW_TIME_PATTERN.matcher(script);

        Matcher gsTimeStampESMatcher = GS_NOW_TIME_STAMP_PATTERN.matcher(script);


        //判断是否有恶意代码
        String removeComment = StringUtils.replacePattern(script, "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
        String finalCode = StringUtils.replacePattern(removeComment, "\\s+", " ");
        Set<String> insecure = BLACKLIST.stream().filter(s -> StringUtils.containsIgnoreCase(finalCode, s))
                .collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(insecure)) {
            LOGGER.error("Warning! input string is insecure,script:{}", script);
            return ReturnT.ofFail(-1, "Warning! input string is insecure");
        }
        //通过安全校验
        while (gsSumMatcher.find()) {
            String group = gsSumMatcher.group();
            String[] parts = group.split(",");//约定数据第一个元素是Field_list中一员
            if (!FIELD_LIST.contains(parts[0])) {
                LOGGER.error("gsSum()第一个入参只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}]", group);
                return ReturnT.ofFail(-1, "gsSum() can only operate[msg,key1,value1,key2,value2,key3,value3] param");
            }
            long sum = 1;
            script = script.replaceAll("gsSum\\s{0,20}\\(" + group + "\\)", sum + "");
        }

//        while (gsTimeESMatcher.find()) {
//            String group = gsTimeESMatcher.group();
//            script=script.replaceAll("\\bgsNowTime\\(\\)",System.currentTimeMillis()/1000+"");
//            String replaceData=script.getAlarmCheckExp().replace("\\\\bgsTime\\\\(\\\\)",  System.currentTimeMillis()/1000+"");
//            alarmScript.setAlarmCheckExp(replaceData);
//        }

        while (gsCountMatcher.find()) {
            String group = gsCountMatcher.group();
            String[] parts = group.split("=");//约定数据第一个元素是Field_list中一员
            if (!FIELD_LIST.contains(parts[0])) {
                LOGGER.error("gsCount()入参=左边只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}]", group);
                return ReturnT.ofFail(-1, "gsCount() can only operate[msg,key1,value1,key2,value2,key3,value3] param");
            }
            long count = 1;
            script = script.replaceAll("gsCount\\s{0,20}\\(" + group + "\\)", count + "");
        }

        while (gsESFuncMatcher.find()) {
            String group = gsESFuncMatcher.group();//获取的字符串还有)

            String[] parts = group.split("\",\"");//用",做分隔符，用, ;都会有无法完全分隔的可能
            if (!validateSQL(parts[0])) {
                LOGGER.error("gsGetDataFromES() sql不合法:{}", group);
                return ReturnT.ofFail(-1, "gsGetDataFromES() sql invalid");
            }
            long count = 1;
            script = script.replaceAll("gsGetDataFromES\\(\".*?\",\".*?\"\\)", count + "");
        }

        LocalDateTime now = LocalDateTime.now();
        long currentTimeMillis = System.currentTimeMillis();

        //提供时间方法，供业务使用
        while (gsTimeESMatcher.find()) {
            String group = gsTimeESMatcher.group().trim();
            int num = Integer.parseInt(group);
            LocalDateTime adjustedDate = now.plusMinutes(num);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String date=adjustedDate.format(formatter);
            script=script.replaceAll("gsNowTime\\s{0,20}\\("+ group + "\\s{0,20}\\)",  date);
        }

        while (gsTimeStampESMatcher.find()) {
            String group = gsTimeStampESMatcher.group().trim();
            long number = Long.parseLong(group);
            long currentLong= currentTimeMillis/1000-number;
            script=script.replaceAll("gsNowTimeStamp\\s{0,20}\\("+ group + "\\s{0,20}\\)",  currentLong+"");
        }

        try {
            //是否有必要执行，为什么要执行？==》保存的时候执行一次，及时爆出问题？===》 但是也没有真正执行具体数据？
            SCRIPT_ENGINE.eval(script);
            // 使用1作为默认参数，避免除0错误
            // 计算结果
            ((Invocable) SCRIPT_ENGINE).invokeFunction(jsFuncName, 1, 1, 1, 1, 1, 1, 1);
            return null;
        } catch (Exception e) {
            LOGGER.info("parser script error,jsFuncName={},script={}", jsFuncName, script);
            return ReturnT.ofFail(-1, "parser script error,error=" + e.getMessage());
        }
    }

    public boolean validateSQL(String sql) {
        // 正则表达式匹配模式
        String pattern = "(?i).*\\bLIMIT\\s+(\\d+)\\b.*";

        // 编译正则表达式
        Pattern p = Pattern.compile(pattern);

        // 匹配输入的SQL
        Matcher m = p.matcher(sql);

        // 如果匹配成功，进一步校验LIMIT后的数字
        if (m.find()) {
            String limitValue = m.group(1);  // 获取LIMIT后的数字
            int limit = Integer.parseInt(limitValue);  // 转换为整数
            return limit <= 10000;  // 判断数字是否小于等于10000
        }

        return false;  // 不包含LIMIT关键字或数字不符合条件
    }
}
