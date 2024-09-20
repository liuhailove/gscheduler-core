package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.TriggerInfo;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobInfoDetail;
import com.tc.gschedulercore.core.model.JobInfoHistory;
import com.tc.gschedulercore.core.route.ExecutorRouteStrategyEnum;
import com.tc.gschedulercore.core.scheduler.MisfireStrategyEnum;
import com.tc.gschedulercore.core.scheduler.ScheduleTypeEnum;
import com.tc.gschedulercore.core.thread.JobScheduleHelper;
import com.tc.gschedulercore.core.thread.JobTriggerPoolHelper;
import com.tc.gschedulercore.core.trigger.LogTypeEnum;
import com.tc.gschedulercore.core.trigger.TriggerTypeEnum;
import com.tc.gschedulercore.core.util.ExcelUtils;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.dao.JobGroupDao;
import com.tc.gschedulercore.enums.ExecutorBlockStrategyEnum;
import com.tc.gschedulercore.enums.GlueTypeEnum;
import com.tc.gschedulercore.service.JobService;
import com.tc.gschedulercore.util.DateUtil;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * index controller
 *
 * @author honggang.liu 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {
    private static Logger logger = LoggerFactory.getLogger(JobInfoController.class.getSimpleName());

    @Resource
    private JobGroupDao jobGroupDao;

    @Resource
    private JobService jobService;

    @GetMapping
    @ResponseBody
    public Map<String, Object> index(@RequestParam(required = false, defaultValue = "-1") int jobGroup) {
        Map<String, Object> map = new HashMap<>(7);
        // 枚举-字典
        // 路由策略-列表
        map.put("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values());
        // Glue类型-字典
        map.put("GlueTypeEnum", GlueTypeEnum.values());
        // 阻塞处理策略-字典
        map.put("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());
        // 调度类型
        map.put("ScheduleTypeEnum", ScheduleTypeEnum.values());
        // 调度过期策略
        map.put("MisfireStrategyEnum", MisfireStrategyEnum.values());

        // 执行器列表
        map.put("JobGroupList", jobGroupDao.findAll());
        map.put("jobGroup", jobGroup);
        return map;
    }

    @AuthAction()
    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit,
                                        @RequestParam(required = false) List<Integer> jobGroups,
                                        @RequestParam(required = false, defaultValue = "-1") Integer triggerStatus,
                                        @RequestParam(required = false) String jobDesc,
                                        @RequestParam(required = false) String jobName,
                                        @RequestParam(required = false) String executorHandler,
                                        @RequestParam(required = false) String author) {
        // 此处主要为了兼容新版UI
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        return jobService.pageList(start, length, jobGroups, triggerStatus, jobDesc, jobName, executorHandler, author);
    }

    //    @AuthAction()
    @GetMapping("/export")
//    @ResponseBody
    public void export(@RequestParam(required = false) List<Integer> jobGroups,
                       @RequestParam(required = false, defaultValue = "-1") Integer triggerStatus,
                       @RequestParam(required = false) String jobDesc,
                       @RequestParam(required = false) String jobName,
                       @RequestParam(required = false) String executorHandler,
                       @RequestParam(required = false) String author,
                       HttpServletResponse response) throws IOException {

        List<JobInfo> jobInfoList = JobAdminConfig.getAdminConfig().getJobInfoDao().pageList(0, Integer.MAX_VALUE, jobGroups, triggerStatus, jobDesc, jobName, executorHandler, author);
        List<JobInfoDetail> jobInfoDetails = new ArrayList<>();

        if (!CollectionUtils.isEmpty(jobInfoList)) {
            for (JobInfo jobInfo : jobInfoList) {
                JobInfoDetail jobInfoDetail = new JobInfoDetail();
                String groupName = jobGroupDao.load(jobInfo.getJobGroup()).getTitle();
                jobInfoDetail.setAppname(jobInfo.getAppName());
                jobInfoDetail.setAlarmSeatalk(jobInfoDetail.getAlarmSeatalk());
                jobInfoDetail.setJobId(jobInfo.getId());
                jobInfoDetail.setJobName(jobInfo.getJobName());
                jobInfoDetail.setJobDesc(jobInfo.getJobDesc());
                jobInfoDetail.setTitle(groupName);
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
        }
        ExcelUtils.exportExcel(jobInfoDetails, "调度任务详细信息", "调度任务详细信息", JobInfoDetail.class, "调度任务详细信息", response);
    }

    @AuthAction()
    @GetMapping("/triggerDelayList")
    @ResponseBody
    public Map<String, Object> triggerDelayList(@RequestParam(required = false, defaultValue = "0") int start,
                                                @RequestParam(required = false, defaultValue = "10") int length,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                @RequestParam(required = false) List<Integer> jobGroups) {
        // 此处主要为了兼容新版UI
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        return jobService.triggerDelayList(start, length, jobGroups);
    }

    @AuthAction()
    @PostMapping("/add")
    @ResponseBody
    public ReturnT<String> add(@RequestBody JobInfo jobInfo) {
        logger.info("JobInfoController.add，param:{}", jobInfo);
        return jobService.add(jobInfo);
    }

    @AuthAction()
    @PostMapping("/update")
    @ResponseBody
    public ReturnT<String> update(@RequestBody JobInfo jobInfo) {
        logger.info("JobInfoController.update，param:{}", jobInfo);
        return jobService.update(jobInfo);
    }

    @AuthAction()
    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(int id) {
        logger.info("JobInfoController.remove，param:{}", id);
        return jobService.remove(id);
    }

    @AuthAction()
    @GetMapping("/stop")
    @ResponseBody
    public ReturnT<String> pause(int id, @RequestParam(required = false) String updateBy) {
        logger.info("JobInfoController.pause，param:{}", id);
        return jobService.stop(id, updateBy);
    }

    @AuthAction()
    @PostMapping("/batchPause")
    @ResponseBody
    public ReturnT<String> batchPause(@RequestBody Map<String, Object> requestMap) {
        logger.info("JobInfoController.batchPause，param:{}", requestMap);

        String ids = (String) requestMap.get("id");
        String updateBy = (String) requestMap.get("updateBy");

        List<Integer> list = Arrays.stream(ids.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        for (int id : list) {
            jobService.stop(id, updateBy);
        }

        return new ReturnT<>(200, "batchPause success");
    }

    @AuthAction()
    @GetMapping("/start")
    @ResponseBody
    public ReturnT<String> start(int id, @RequestParam(required = false) String updateBy) {
        logger.info("JobInfoController.start，param:{}", id);
        return jobService.start(id, updateBy);
    }

    @AuthAction()
    @GetMapping("/loadById")
    @ResponseBody
    public ReturnT<JobInfo> loadById(int id) {
        return jobService.loadById(id);
    }

    @AuthAction()
    @GetMapping("/loadByIdes")
    @ResponseBody
    public ReturnT<List<JobInfo>> loadByIdes(@RequestParam List<Integer> ides) {
        return jobService.loadByIdes(ides);
    }

    @AuthAction()
    @GetMapping("/loadByJobGroup")
    @ResponseBody
    public ReturnT<List<JobInfo>> loadByJobGroup(int jobGroup) {
        return jobService.loadByJobGroup(jobGroup);
    }


    /**
     * 加载Job变更历史
     *
     * @param id 主键ID
     * @return Job History
     */
    @AuthAction()
    @GetMapping("/loadHistoryById")
    @ResponseBody
    public ReturnT<List<JobInfoHistory>> loadHistoryById(int id) {
        return jobService.loadHistoryById(id);
    }

    @AuthAction()
    @GetMapping("/trigger")
    @ResponseBody
    public ReturnT<String> triggerJob(@RequestParam int id, @RequestParam(required = false) String executorParam, @RequestParam(required = false) String addressList) {
        logger.info("JobInfoController.triggerJob，param:{},{},{}", id, executorParam, addressList);
        // force cover job param
        if (executorParam == null) {
            executorParam = "";
        }
        try (HintManager manager = HintManager.getInstance()) {
            manager.setWriteRouteOnly();
            JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobInfoDao().loadById(id);
            JobTriggerPoolHelper.trigger(jobInfo.getJobGroup(), id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList, 0L, LogTypeEnum.MAIN_LOG, UUID.randomUUID().toString(), "", "");
        }
        return ReturnT.SUCCESS;
    }

    @AuthAction()
    @PostMapping("/triggerNew")
    @ResponseBody
    public ReturnT<String> triggerJobNew(@RequestBody TriggerInfo triggerInfo) {
        logger.info("JobInfoController.triggerNew,{}", triggerInfo);
        // force cover job param
        String executorParam = "";
        if (triggerInfo.getExecutorParam() == null) {
            executorParam = "";
        } else {
            executorParam = triggerInfo.getExecutorParam();
        }
        try (HintManager manager = HintManager.getInstance()) {
            manager.setWriteRouteOnly();
            JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobInfoDao().loadById(triggerInfo.getId());
            JobTriggerPoolHelper.trigger(jobInfo.getJobGroup(), triggerInfo.getId(), TriggerTypeEnum.MANUAL, -1, null, executorParam, triggerInfo.getAddressList(), 0L, LogTypeEnum.MAIN_LOG, UUID.randomUUID().toString(), "", "");
        }
        return ReturnT.SUCCESS;
    }

    @AuthAction()
    @GetMapping("/nextTriggerTime")
    @ResponseBody
    public ReturnT<List<String>> nextTriggerTime(@RequestParam String scheduleType, @RequestParam String scheduleConf) {
        logger.info("JobInfoController.nextTriggerTime，param:{},{}", scheduleType, scheduleConf);
        JobInfo paramJobInfo = new JobInfo();
        paramJobInfo.setScheduleType(scheduleType);
        paramJobInfo.setScheduleConf(scheduleConf);
        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(paramJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")) + e.getMessage());
        }
        return new ReturnT<>(result);
    }

    @AuthAction()
    @GetMapping("/setTriggerDelay")
    @ResponseBody
    public ReturnT<String> setTriggerDelay(@RequestParam(required = false) List<Integer> jobIdes, @RequestParam Long delayTimeLength) {
        logger.info("JobInfoController.setTriggerDelay，param:jobIdes={},delayTimeLength={}", jobIdes, delayTimeLength);
        return jobService.setTriggerDelay(jobIdes, delayTimeLength);
    }

    @AuthAction()
    @PostMapping("/updateRouterFlag")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<Integer> updateRouterFlag(@RequestBody JobInfo jobInfo) {
        return ReturnT.ofSuccess(jobService.updateRouterFlag(jobInfo));
    }

    /************* info *******************/
    @GetMapping("/list")
    public String jobinfoList() {
        return "jobinfo/list";
    }

    @GetMapping("/add")
    public String jobinfoAdd() {
        return "jobinfo/add";
    }

    @GetMapping("/edit")
    public String jobinfoEdit(@RequestParam Integer id, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        return "jobinfo/edit";
    }

    @GetMapping("/execOnce")
    public String jobinfoExecOnce(@RequestParam Integer id, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        return "jobinfo/exec_once";
    }

    @GetMapping("/detail")
    public String jobinfoDetail(@RequestParam Integer id, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        return "jobinfo/detail";
    }

    @GetMapping("/triggerDelayRedirect")
    public String triggerDelayRedirect(@RequestParam Integer[] ids, RedirectAttributes attributes) {
        attributes.addAttribute("ids", ids);
        return "jobinfo/trigger_delay_list";
    }

    @GetMapping("/setDelayRedirect")
    public String setDelayRedirect(@RequestParam Integer[] jobIdes, RedirectAttributes attributes) {
        attributes.addAttribute("jobIdes", jobIdes);
        return "jobinfo/set_delay";
    }

    @GetMapping("/routerFlag")
    public String routerFlag(@RequestParam Integer jobId, @RequestParam Integer jobGroup, RedirectAttributes attributes) {
        attributes.addAttribute("jobId", jobId);
        attributes.addAttribute("jobGroup", jobGroup);
        return "jobinfo/router_flag";
    }
}
