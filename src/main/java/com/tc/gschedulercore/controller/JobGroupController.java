package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.cron.CronExpression;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.core.thread.JobScheduleHelper;
import com.tc.gschedulercore.core.util.ExcelUtils;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.dao.JobGroupDao;
import com.tc.gschedulercore.dao.JobInfoDao;
import com.tc.gschedulercore.dao.JobRegistryDao;
import com.tc.gschedulercore.dao.JobUserGroupDao;
import com.tc.gschedulercore.enums.RegistryConfig;
import com.tc.gschedulercore.util.DateUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * job group controller
 *
 * @author honggang.liu 2016-10-02 20:52:56
 */
@Controller
@RequestMapping("/jobgroup")
public class JobGroupController {

    private Logger logger = LoggerFactory.getLogger(JobGroupController.class.getSimpleName());
    /**
     * 固定每天10：30发送报表
     */
    private static final String REPORT_FIX_CRON = "0 30 10 * * ?";

    @Resource
    public JobInfoDao jobInfoDao;
    @Resource
    public JobGroupDao jobGroupDao;
    @Resource
    private JobRegistryDao xxlJobRegistryDao;

    /**
     * 用户组管理
     */
    @Resource
    private JobUserGroupDao xxlJobUserGroupDao;

    @AuthAction()
    @GetMapping("/findAll")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public List<JobGroup> findAll() {
        return jobGroupDao.findAll();
    }

    @AuthAction()
    @GetMapping("/loadByIds")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public List<JobGroup> loadByIds(@RequestParam(required = false) List<Integer> ids) {
        return jobGroupDao.loadByIds(ids);
    }

    @AuthAction()
    @GetMapping("/pageList")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit,
                                        @RequestParam(required = false) String appname, @RequestParam(required = false) String title,
                                        @RequestParam(required = false) List<Integer> ids) {

        // 此处主要为了兼容新版UI
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        // page query
        List<JobGroup> list = jobGroupDao.pageList(start, length, appname, title, ids);
        int listCount = jobGroupDao.pageListCount(start, length, appname, title, ids);
        // package result
        Map<String, Object> maps = new HashMap<>(6);
        // 总记录数
        maps.put("recordsTotal", listCount);
        // 过滤后的总记录数
        maps.put("recordsFiltered", listCount);
        // 分页列表
        maps.put("data", list);
        maps.put("msg", "");
        // 总记录数
        maps.put("count", listCount);
        maps.put("code", ReturnT.SUCCESS_CODE);
        return maps;
    }

    @AuthAction()
    @PostMapping("/save")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<JobGroup> save(@RequestBody JobGroup jobGroup) {
        logger.info("JobGroupController.save，param:{}", jobGroup);
        // valid
        if (jobGroup.getAppname() == null || jobGroup.getAppname().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        if (jobGroup.getAppname().length() < 4 || jobGroup.getAppname().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_appname_length"));
        }
        if (jobGroup.getAppname().contains(">") || jobGroup.getAppname().contains("<")) {
            return new ReturnT<>(500, "AppName" + I18nUtil.getString("system_unvalid"));
        }
        if (jobGroup.getTitle() == null || jobGroup.getTitle().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
        }
        if (jobGroup.getTitle().contains(">") || jobGroup.getTitle().contains("<")) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_title") + I18nUtil.getString("system_unvalid"));
        }
        if (jobGroup.getAddressType() != 0) {
            if (jobGroup.getAddressList() == null || jobGroup.getAddressList().trim().length() == 0) {
                return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_addressType_limit"));
            }
            if (jobGroup.getAddressList().contains(">") || jobGroup.getAddressList().contains("<")) {
                return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_registryList") + I18nUtil.getString("system_unvalid"));
            }
            String[] addresss = jobGroup.getAddressList().split(",");
            for (String item : addresss) {
                if (item == null || item.trim().length() == 0) {
                    return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid"));
                }
            }
        }
        if (jobGroup.getScheduleConf() != null && !CronExpression.isValidExpression(jobGroup.getScheduleConf())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Cron" + I18nUtil.getString("system_unvalid"));
        }
        long nextTriggerTime;
        try {
            logger.info("JobGroupController2.save，param:{}", jobGroup);

            if (!StringUtils.hasLength(jobGroup.getScheduleConf())) {
                jobGroup.setScheduleConf(REPORT_FIX_CRON);
            }
            Date nextValidTime = JobScheduleHelper.generateNextValidTime(jobGroup, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
            nextTriggerTime = nextValidTime.getTime();
        } catch (Exception e) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
        // process
        jobGroup.setUpdateTime(new Date());
        jobGroup.setAlarmSeatalk(jobGroup.getAlarmSeatalk());
        logger.info("JobGroupController3.save，param:{}", jobGroup);

        jobGroup.setBlackAddressList(jobGroup.getBlackAddressList());
        logger.info("JobGroupController4.save，param:{}", jobGroup);

        jobGroup.setTriggerNextTime(nextTriggerTime);
        jobGroup.setTriggerLastTime(0);
        jobGroup.setTriggerStatus(true);
        if (jobGroup.getAlarmSeatalk() == null) {
            jobGroup.setAlarmSeatalk("");
        }
        if (jobGroup.getBlackAddressList() == null) {
            jobGroup.setBlackAddressList("");
        }
        if (jobGroup.getTokenEffectiveDate() != null) {
            jobGroup.setCurrentAccessToken(UUID.randomUUID().toString());
        }
        int ret = jobGroupDao.save(jobGroup);
        // 得到添加的组
        for (String ugroup : jobGroup.getUgroupList()) {
            JobUserGroup userGroup = xxlJobUserGroupDao.loadByUGroupName(ugroup);
            if (userGroup != null) {
                List<String> permissionJobGroups = userGroup.getPermissionJobGroupList();
                if (!permissionJobGroups.contains(jobGroup.getAppname())) {
                    permissionJobGroups.add(jobGroup.getAppname());
                    userGroup.setPermissionJobGroups(org.apache.commons.lang3.StringUtils.join(permissionJobGroups, ","));
                    xxlJobUserGroupDao.update(userGroup);
                }
            }
        }
        return (ret > 0) ? new ReturnT<>(jobGroup) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @PostMapping("/update")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> update(@RequestBody JobGroup jobGroup) {
        logger.info("JobGroupController.update，param:{}", jobGroup);
        // valid
        if (jobGroup.getAppname() == null || jobGroup.getAppname().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        if (jobGroup.getAppname().length() < 4 || jobGroup.getAppname().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_appname_length"));
        }
        if (jobGroup.getTitle() == null || jobGroup.getTitle().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
        }
        if (jobGroup.getAddressType() == 0) {
            // 0=自动注册
            List<String> registryList = findRegistryByAppName(jobGroup.getAppname());
            String addressListStr = null;
            if (registryList != null && !registryList.isEmpty()) {
                Collections.sort(registryList);
                addressListStr = "";
                for (String item : registryList) {
                    addressListStr += item + ",";
                }
                addressListStr = addressListStr.substring(0, addressListStr.length() - 1);
            }
            jobGroup.setAddressList(addressListStr);
        } else {
            // 1=手动录入
            if (jobGroup.getAddressList() == null || jobGroup.getAddressList().trim().length() == 0) {
                return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_addressType_limit"));
            }
            String[] addresss = jobGroup.getAddressList().split(",");
            for (String item : addresss) {
                if (item == null || item.trim().length() == 0) {
                    return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid"));
                }
            }
        }
        if (jobGroup.getScheduleConf() != null && !CronExpression.isValidExpression(jobGroup.getScheduleConf())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Cron" + I18nUtil.getString("system_unvalid"));
        }
        // next trigger time (5s后生效，避开预读周期)
        try {
            if (!StringUtils.hasLength(jobGroup.getScheduleConf())) {
                jobGroup.setScheduleConf(REPORT_FIX_CRON);
            }
            Date nextValidTime = JobScheduleHelper.generateNextValidTime(jobGroup, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
            jobGroup.setTriggerNextTime(nextValidTime.getTime());
        } catch (Exception e) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
        // process
        jobGroup.setUpdateTime(new Date());
        jobGroup.setBlackAddressList(jobGroup.getBlackAddressList());
        if (jobGroup.getBlackAddressList() == null) {
            jobGroup.setBlackAddressList("");
        }
        if (jobGroup.getAlarmSeatalk() == null) {
            jobGroup.setAlarmSeatalk("");
        }
        jobGroup.setTriggerStatus(true);
        JobGroup oldJobGroup = jobGroupDao.load(jobGroup.getId());
        jobGroup.setScheduleConf(oldJobGroup.getScheduleConf());
        jobGroup.setTriggerLastTime(oldJobGroup.getTriggerLastTime());
        jobGroup.setTriggerNextTime(oldJobGroup.getTriggerNextTime());
        if (jobGroup.getTokenEffectiveDate() == null) {
            jobGroup.setCurrentAccessToken("");
        } else if (jobGroup.getTokenEffectiveDate() != null && !StringUtils.hasLength(oldJobGroup.getCurrentAccessToken())) {
            jobGroup.setCurrentAccessToken(UUID.randomUUID().toString());
        }
        jobGroup.setRouterFlag(oldJobGroup.getRouterFlag());
        jobGroup.setAddressListWithFlag(oldJobGroup.getAddressListWithFlag());
        int ret = jobGroupDao.update(jobGroup);
        List<JobInfo> jobInfos = jobInfoDao.getJobsByGroup(jobGroup.getId());
        if (!CollectionUtils.isEmpty(jobInfos)) {
            for (JobInfo jobInfo : jobInfos) {
                jobInfo.setAppName(jobGroup.getAppname());
                jobInfo.setUpdateTime(new Date());
                jobInfoDao.update(jobInfo);
            }
        }
        jobInfoDao.updateSeatalk(jobGroup.getId(), jobGroup.getAlarmSeatalk());
        if (!jobGroup.getAppname().equals(oldJobGroup.getAppname())) {
            // 更新用户组
            List<JobUserGroup> userGroups = xxlJobUserGroupDao.loadByAppName(oldJobGroup.getAppname());
            if (!CollectionUtils.isEmpty(userGroups)) {
                for (JobUserGroup userGroup : userGroups) {
                    List<String> jobGroups = userGroup.getPermissionJobGroupList();
                    jobGroups.remove(oldJobGroup.getAppname());
                    jobGroups.add(jobGroup.getAppname());
                    userGroup.setPermissionJobGroups(org.apache.commons.lang3.StringUtils.join(jobGroups, ","));
                    xxlJobUserGroupDao.update(userGroup);
                }
            }
        }
        // 判断归属分组和旧分组是否一致，如果不一致，则需要更新分组数据
        if (!oldJobGroup.getUgroupList().containsAll(jobGroup.getUgroupList()) || !jobGroup.getUgroupList().containsAll(oldJobGroup.getUgroupList())) {
            // 得到被移除的组
            List<String> oldUgroupList = oldJobGroup.getUgroupList();
            oldUgroupList.removeAll(jobGroup.getUgroupList());
            for (String ugroup : oldUgroupList) {
                JobUserGroup userGroup = xxlJobUserGroupDao.loadByUGroupName(ugroup);
                if (userGroup != null) {
                    List<String> permissionJobGroups = userGroup.getPermissionJobGroupList();
                    permissionJobGroups.removeAll(Collections.singletonList(jobGroup.getAppname()));
                    userGroup.setPermissionJobGroups(org.apache.commons.lang3.StringUtils.join(permissionJobGroups, ","));
                    xxlJobUserGroupDao.update(userGroup);
                }
            }
            // 得到新添加的组
            List<String> newUgroupList = jobGroup.getUgroupList();
            newUgroupList.removeAll(oldJobGroup.getUgroupList());
            for (String ugroup : newUgroupList) {
                JobUserGroup userGroup = xxlJobUserGroupDao.loadByUGroupName(ugroup);
                if (userGroup != null) {
                    List<String> permissionJobGroups = userGroup.getPermissionJobGroupList();
                    if (!permissionJobGroups.contains(jobGroup.getAppname())) {
                        permissionJobGroups.add(jobGroup.getAppname());
                        userGroup.setPermissionJobGroups(org.apache.commons.lang3.StringUtils.join(permissionJobGroups, ","));
                        xxlJobUserGroupDao.update(userGroup);
                    }
                }
            }
        }
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    private List<String> findRegistryByAppName(String appnameParam) {
        HashMap<String, List<String>> appAddressMap = new HashMap<>();
        List<JobRegistry> list = xxlJobRegistryDao.findAll(DateUtils.addSeconds(new Date(), RegistryConfig.DEAD_TIMEOUT * (-1)));
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
                }
            }
        }
        return appAddressMap.get(appnameParam);
    }

    @AuthAction()
    @GetMapping("/remove")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> remove(int id) {
        // valid
        long startTime = System.currentTimeMillis();
        logger.info("JobGroupController.remove，startTime:{}", startTime);
        int count = jobInfoDao.pageListCount(0, 10, Collections.singletonList(id), -1, null, null, null, null);
        if (count > 0) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_del_limit_0"));
        }
        // 更新用户组
        JobGroup jobGroup = jobGroupDao.load(id);
        int ret = jobGroupDao.remove(id);
        // 更新用户组
        List<JobUserGroup> userGroups = xxlJobUserGroupDao.loadByAppName(jobGroup.getAppname());
        if (!CollectionUtils.isEmpty(userGroups)) {
            for (JobUserGroup userGroup : userGroups) {
                List<String> jobGroups = userGroup.getPermissionJobGroupList();
                jobGroups.remove(jobGroup.getAppname());
                userGroup.setPermissionJobGroups(org.apache.commons.lang3.StringUtils.join(jobGroups, ","));
                xxlJobUserGroupDao.update(userGroup);
            }
        }
        long endTime = System.currentTimeMillis();
        logger.info("JobGroupController.remove，endTime:{},elapsedTime:{}", endTime, endTime - startTime);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/loadById")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<JobGroup> loadById(int id) {
        JobGroup jobGroup = jobGroupDao.load(id);
        return jobGroup != null ? new ReturnT<>(jobGroup) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @GetMapping("/loadByName")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<JobGroup> loadByName(String appName) {
        JobGroup jobGroup = jobGroupDao.loadByName(appName);
        return jobGroup != null ? new ReturnT<>(jobGroup) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @GetMapping("/loadByAppNames")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<List<Integer>> loadByAppNames(@RequestParam("appNames") String[] appNames) {
        if (appNames == null || appNames.length == 0) {
            return new ReturnT<>(Collections.emptyList());
        }
        List<Integer> jobGroups = jobGroupDao.loadPkByNames(appNames);
        return jobGroups != null ? new ReturnT<>(jobGroups) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }


    @GetMapping("/loadFullByAppNames")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> loadFullByAppNames(@RequestParam("appNames") String[] appNames, @RequestParam(required = false, defaultValue = "1") Integer page,
                                                  @RequestParam(required = false, defaultValue = "10") Integer limit) {
        int start = 0;
        int length = 10;
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        Map<String, Object> maps = new HashMap<>(6);
        if (appNames == null || appNames.length == 0) {
            // 总记录数
            maps.put("recordsTotal", 0);
            // 过滤后的总记录数
            maps.put("recordsFiltered", 0);
            // 分页列表
            maps.put("data", Collections.emptyList());
            maps.put("msg", "");
            // 总记录数
            maps.put("count", 0);
            maps.put("code", ReturnT.SUCCESS_CODE);
        } else {
            List<JobGroup> jobGroups = jobGroupDao.loadFullByAppNames(start, length, appNames);
            int listCount = jobGroupDao.loadFullByAppNamesCount(start, length, appNames);
            // 总记录数
            maps.put("recordsTotal", listCount);
            // 过滤后的总记录数
            maps.put("recordsFiltered", listCount);
            // 分页列表
            maps.put("data", jobGroups);
            maps.put("msg", "");
            // 总记录数
            maps.put("count", listCount);
            maps.put("code", ReturnT.SUCCESS_CODE);
        }
        return maps;
    }

    @AuthAction()
    @GetMapping("/loadRouterFlags")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> loadRouterFlags(@RequestParam("jobGroup") Integer jobGroup) {
        Map<String, Object> maps = new HashMap<>(6);
        JobGroup jobGroupInfo = jobGroupDao.load(jobGroup);
        List<String> routerFlags = xxlJobRegistryDao.loadRouterFlags(jobGroupInfo.getAppname());
        if (CollectionUtils.isEmpty(routerFlags)) {
            routerFlags = new ArrayList<>(0);
        }
        //加一个默认值，代表基准环境。
        routerFlags.add("schedule_base_pfb");
        // 总记录数
        maps.put("recordsTotal", routerFlags.size());
        // 过滤后的总记录数
        maps.put("recordsFiltered", routerFlags.size());
        // 分页列表
        maps.put("data", routerFlags);
        maps.put("msg", "");
        // 总记录数
        maps.put("count", routerFlags.size());
        maps.put("code", ReturnT.SUCCESS_CODE);
        return maps;
    }

    @AuthAction()
    @PostMapping("/updateRouterFlag")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<Integer> updateRouterFlag(@RequestBody JobGroup jobGroup) {
        return ReturnT.ofSuccess(jobGroupDao.updateRouterFlag(jobGroup));
    }

    @PostMapping("/genAccessToken")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> genAccessToken(int id, String tokenEffectiveDate) {
        Date tokenEffectiveDt = DateUtil.parseDateTime(tokenEffectiveDate);
        String newAccessToken = UUID.randomUUID().toString();
        JobGroup jobGroup = jobGroupDao.load(id);
        jobGroup.setLastAccessToken(jobGroup.getCurrentAccessToken());
        jobGroup.setCurrentAccessToken(newAccessToken);
        jobGroup.setTokenEffectiveDate(tokenEffectiveDt);
        jobGroup.setUpdateTime(new Date());
        int ret = jobGroupDao.update(jobGroup);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    /**
     * 导出应用及任务
     */
    @GetMapping(value = "/export")
    public void exportExcel(@RequestParam("ids") List<Integer> ids, HttpServletResponse response) throws IOException {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        List<JobGroup> jobGroupList = jobGroupDao.loadByIds(ids);
        List<JobInfoDetail> jobInfoDetails = new ArrayList<>();
        for (JobGroup jobGroup : jobGroupList) {
            List<JobInfo> jobInfoList = jobInfoDao.getJobsByGroup(jobGroup.getId());
            if (!CollectionUtils.isEmpty(jobInfoList)) {
                for (JobInfo jobInfo : jobInfoList) {
                    JobInfoDetail jobInfoDetail = new JobInfoDetail();
                    jobInfoDetail.setAppname(jobGroup.getAppname());
                    jobInfoDetail.setTitle(jobGroup.getTitle());
                    jobInfoDetail.setAddressType(jobGroup.getAddressType());
                    jobInfoDetail.setAlarmSeatalk(jobInfoDetail.getAlarmSeatalk());
                    jobInfoDetail.setJobId(jobInfo.getId());
                    jobInfoDetail.setJobName(jobInfo.getJobName());
                    jobInfoDetail.setJobDesc(jobInfo.getJobDesc());
                    jobInfoDetail.setAuthor(jobInfo.getAuthor());
                    jobInfoDetail.setAlarmEmail(jobInfo.getAlarmEmail());
                    jobInfoDetail.setScheduleType(jobInfo.getScheduleType());
                    jobInfoDetail.setScheduleConf(jobInfo.getScheduleConf());
                    jobInfoDetail.setMisfireStrategy(jobInfo.getMisfireStrategy());
                    jobInfoDetail.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
                    jobInfoDetail.setExecutorHandler(jobInfo.getExecutorHandler());
                    jobInfoDetail.setExecutorParam(jobInfo.getExecutorParam());
                    jobInfoDetail.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
                    jobInfoDetail.setExecutorTimeout(jobInfo.getExecutorTimeout());
                    jobInfoDetail.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
                    jobInfoDetail.setGlueType(jobInfo.getGlueType());
                    jobInfoDetail.setGlueSource(jobInfo.getGlueSource());
                    jobInfoDetail.setGlueRemark(jobInfo.getGlueRemark());
                    jobInfoDetail.setChildJobId(jobInfo.getChildJobId());
                    jobInfoDetail.setAlarmSeatalk(jobInfo.getAlarmSeatalk());
                    jobInfoDetail.setExecutorThreshold(jobInfo.getExecutorThreshold());
                    jobInfoDetail.setShardingType(jobInfo.getShardingType());
                    jobInfoDetail.setShardingNum(jobInfo.getShardingNum());
                    jobInfoDetail.setRetryType(jobInfo.getRetryType());
                    jobInfoDetail.setRetryConf(jobInfo.getRetryConf());
                    jobInfoDetail.setParamFromParent(jobInfo.getParamFromParent());
                    jobInfoDetail.setResultCheck(jobInfo.isResultCheck());
                    jobInfoDetail.setFinalFailedSendAlarm(jobInfo.isFinalFailedSendAlarm());
                    jobInfoDetail.setBeginAfterParent(jobInfo.isBeginAfterParent());
                    jobInfoDetail.setParentJobId(jobInfo.getParentJobId());
                    jobInfoDetail.setTriggerStatus(jobInfo.getTriggerStatus());
                    jobInfoDetails.add(jobInfoDetail);
                }
            } else {
                JobInfoDetail jobInfoDetail = new JobInfoDetail();
                jobInfoDetail.setAppname(jobGroup.getAppname());
                jobInfoDetail.setTitle(jobGroup.getTitle());
                jobInfoDetail.setAddressType(jobGroup.getAddressType());
                jobInfoDetail.setAlarmSeatalk(jobInfoDetail.getAlarmSeatalk());
                jobInfoDetails.add(jobInfoDetail);
            }
        }
        ExcelUtils.exportExcel(jobInfoDetails, "执行器及调度任务详细信息", "执行器及调度任务详细信息", JobInfoDetail.class, "执行期及调度任务详细信息", response);
    }

    /**
     * 导出应用及任务
     */
    @PostMapping(value = "/upload")
    @ResponseBody
    public ReturnT<String> uploadExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "文件为空");
        }
        String ext = "";
        String originalFileName = file.getOriginalFilename();
        if (originalFileName != null) {
            ext = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
        }
        if (!ext.equalsIgnoreCase(ExcelUtils.ExcelTypeEnum.XLSX.getValue()) && !ext.equalsIgnoreCase(ExcelUtils.ExcelTypeEnum.XLS.getValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "不是excel格式");
        }
        List<JobInfoDetail> jobInfoDetailList = ExcelUtils.importExcel(file, JobInfoDetail.class);
        for (JobInfoDetail jobInfoDetail : jobInfoDetailList) {
            if (!StringUtils.hasLength(jobInfoDetail.getAppname())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "执行器名称不能为空");
            }
            JobGroup jobGroup = jobGroupDao.loadByName(jobInfoDetail.getAppname());
            if (jobGroup == null) {
                jobGroup = new JobGroup();
                jobGroup.setAppname(jobInfoDetail.getAppname());
                jobGroup.setTitle(jobInfoDetail.getTitle());
                jobGroup.setAddressType(jobInfoDetail.getAddressType());
                jobGroup.setUpdateTime(new Date());
                jobGroup.setAlarmSeatalk(jobInfoDetail.getAlarmSeatalk());
                jobGroup.setOnlineStatus(false);
                jobGroupDao.save(jobGroup);
            }
            // 不存在时才插入
            int exist = jobInfoDao.exist(jobInfoDetail.getJobName(), jobGroup.getAppname());
            if (exist <= 0) {
                JobInfo jobInfo = new JobInfo();
                jobInfo.setJobGroup(jobGroup.getId());
                jobInfo.setJobName(jobInfoDetail.getJobName());
                jobInfo.setAppName(jobGroup.getAppname());
                jobInfo.setJobDesc(!StringUtils.hasLength(jobInfoDetail.getJobDesc()) ? "" : jobInfoDetail.getJobDesc());
                jobInfo.setAddTime(new Date());
                jobInfo.setUpdateTime(new Date());
                jobInfo.setAuthor(jobInfoDetail.getAuthor());
                jobInfo.setAlarmEmail(jobInfoDetail.getAlarmEmail());
                jobInfo.setScheduleType(jobInfoDetail.getScheduleType());
                jobInfo.setScheduleConf(jobInfoDetail.getScheduleConf());
                jobInfo.setMisfireStrategy(jobInfoDetail.getMisfireStrategy());
                jobInfo.setExecutorRouteStrategy(jobInfoDetail.getExecutorRouteStrategy());
                jobInfo.setExecutorHandler(jobInfoDetail.getExecutorHandler());
                jobInfo.setExecutorParam(jobInfoDetail.getExecutorParam());
                jobInfo.setExecutorBlockStrategy(jobInfoDetail.getExecutorBlockStrategy());
                jobInfo.setExecutorTimeout(jobInfoDetail.getExecutorTimeout());
                jobInfo.setExecutorFailRetryCount(jobInfoDetail.getExecutorFailRetryCount());
                jobInfo.setGlueType(jobInfoDetail.getGlueType());
                jobInfo.setGlueRemark("");
                jobInfo.setGlueSource("");
                jobInfo.setUpdateTime(new Date());
                jobInfo.setAlarmSeatalk(jobGroup.getAlarmSeatalk());
                jobInfoDao.save(jobInfo);
            }
        }
        return ReturnT.SUCCESS;
    }

    /************* group *******************/
    @GetMapping("/list")
    public String jobgroupList() {
        return "jobgroup/list";
    }

    @GetMapping("/add")
    public String jobgroupAdd() {
        return "jobgroup/add";
    }

    @GetMapping("/edit")
    public String jobgroupEdit(@RequestParam Integer id, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        return "jobgroup/edit";
    }

    @GetMapping("/detail")
    public String jobgroupDetail(@RequestParam Integer id, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        return "jobgroup/detail";
    }

    @GetMapping("/routerFlag")
    public String routerFlag(@RequestParam Integer jobGroup, RedirectAttributes attributes) {
        attributes.addAttribute("jobGroup", jobGroup);
        return "jobgroup/router_flag";
    }
}
