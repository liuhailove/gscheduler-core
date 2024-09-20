package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.route.ExecutorRouteStrategyEnum;
import com.tc.gschedulercore.core.scheduler.MisfireStrategyEnum;
import com.tc.gschedulercore.core.scheduler.ScheduleTypeEnum;
import com.tc.gschedulercore.core.thread.JobScheduleHelper;
import com.tc.gschedulercore.core.thread.JobTriggerPoolHelper;
import com.tc.gschedulercore.core.trigger.LogTypeEnum;
import com.tc.gschedulercore.core.trigger.TriggerTypeEnum;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.dao.JobGroupDao;
import com.tc.gschedulercore.dao.JobRegistryDao;
import com.tc.gschedulercore.enums.ExecutorBlockStrategyEnum;
import com.tc.gschedulercore.enums.GlueTypeEnum;
import com.tc.gschedulercore.service.JobService;
import com.tc.gschedulercore.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * index controller
 *
 * @author honggang.liu 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobinfo/openapi")
public class JobInfoOpenApiController {
    private static Logger logger = LoggerFactory.getLogger(JobInfoOpenApiController.class.getSimpleName());

    @Resource
    private JobGroupDao jobGroupDao;
    @Resource
    private JobService jobService;

    @Resource
    private JobRegistryDao xxlJobRegistryDao;

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


    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        @RequestParam(required = false) List<Integer> jobGroups,
                                        @RequestParam(required = false, defaultValue = "-1") Integer triggerStatus,
                                        @RequestParam(required = false) String jobDesc,
                                        @RequestParam(required = false) String jobName,
                                        @RequestParam(required = false) String executorHandler,
                                        @RequestParam(required = false) String author) {

        return jobService.pageList(start, length, jobGroups, triggerStatus, jobDesc, jobName, executorHandler, author);
    }

    @PostMapping("/add")
    @ResponseBody
    public ReturnT<String> add(@RequestBody JobInfo jobInfo, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
        if (!jobService.canHandleJobByGroup(openApiToken, jobInfo.getJobGroup())) {
            return ReturnT.FAIL_FOR_NOT_AUTH;
        }
        return jobService.add(jobInfo);
    }

    @PostMapping("/update")
    @ResponseBody
    public ReturnT<String> update(@RequestBody JobInfo jobInfo, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
        if (!jobService.canHandleJob(openApiToken, jobInfo.getId(), "job_common_update")) {
            return ReturnT.FAIL_FOR_NOT_AUTH;
        }
        return jobService.update(jobInfo);
    }

    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(int id, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
        if (!jobService.canHandleJob(openApiToken, id, "job_common_delete")) {
            return ReturnT.FAIL_FOR_NOT_AUTH;
        }
        return jobService.remove(id);
    }

    @GetMapping("/stop")
    @ResponseBody
    public ReturnT<String> pause(int id, @RequestParam(required = false) String updateBy, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
        if (!jobService.canHandleJob(openApiToken, id, "job_common_stop")) {
            return ReturnT.FAIL_FOR_NOT_AUTH;
        }
        return jobService.stop(id, updateBy);
    }

    @PostMapping("/batchPause")
    @ResponseBody
    public ReturnT<String> batchPause(@RequestBody Map<String, Object> requestMap, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
        logger.info("JobInfoController.batchPause，param:{}", requestMap);

        String ids = (String) requestMap.get("id");
        String updateBy = (String) requestMap.get("updateBy");

        List<Integer> list = Arrays.stream(ids.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        for (int id : list) {
            //如果有一个不符合鉴权，就返回失败
            if (!jobService.canHandleJob(openApiToken, id, "job_common_stop")) {
                return ReturnT.FAIL_FOR_NOT_AUTH;
            }
            jobService.stop(id, updateBy);
        }

        return new ReturnT<>(200, "batchPause success");
    }

    @GetMapping("/start")
    @ResponseBody
    public ReturnT<String> start(int id, @RequestParam(required = false) String updateBy, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
        if (!jobService.canHandleJob(openApiToken, id, "job_common_start")) {
            return ReturnT.FAIL_FOR_NOT_AUTH;
        }
        return jobService.start(id, updateBy);
    }

    @GetMapping("/queryBy")
    @ResponseBody
    public ReturnT<?> queryBy(@RequestParam("jobGroup") String jobGroup, @RequestParam("jobName") String jobName, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
        logger.info(">>open api queryBy, openApiToken:{}", openApiToken);
        if (!jobService.canHandleJobBy(openApiToken, "job_common_query", jobGroup, jobName)) {
            return ReturnT.FAIL_FOR_NOT_AUTH;
        }
        return jobService.queryBy(jobName, jobGroup);
    }


    @GetMapping("/loadById")
    @ResponseBody
    public ReturnT<JobInfo> loadById(int id) {
        return jobService.loadById(id);
    }

    @GetMapping("/loadAppIdByName")
    @ResponseBody
    public int loadAppIdByName(@RequestParam("appName") String appName) {
        JobGroup group = jobGroupDao.loadByName(appName);
        if (group == null) {
            return -1;
        }
        return jobGroupDao.loadByName(appName).getId();
    }

    @GetMapping("/loadByIdes")
    @ResponseBody
    public ReturnT<List<JobInfo>> loadByIdes(@RequestParam List<Integer> ides) {
        return jobService.loadByIdes(ides);
    }

    @GetMapping("/trigger")
    @ResponseBody
    public ReturnT<String> triggerJob(@RequestParam int id, @RequestParam(required = false) String executorParam, @RequestParam(required = false) String addressList, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken, @RequestParam(required = false) String additionalParams) {
        if (!jobService.canHandleJob(openApiToken, id, "job_common_exec_once")) {
            return ReturnT.FAIL_FOR_NOT_AUTH;
        }
        // force cover job param
        if (executorParam == null) {
            executorParam = "";
        }
        JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobInfoDao().loadById(id);
        String instanceId = UUID.randomUUID().toString();
        //这个接口先不动，不影响历史请求，想使用pfb走另外一个接口
        JobTriggerPoolHelper.trigger(jobInfo.getJobGroup(), id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList, 0L, LogTypeEnum.MAIN_LOG, instanceId, additionalParams, "");
        return new ReturnT<>(ReturnT.SUCCESS_CODE, ReturnT.SUCCESS_FLAG, instanceId);
    }

    @PostMapping("/triggerOnce")
    @ResponseBody
    public ReturnT<String> triggerJobOnce(@RequestBody Map<String, Object> requestMap, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
        int id = (int) requestMap.get("id");
        String executorParam = (String) requestMap.get("executorParam");
        String addressList = (String) requestMap.get("addressList");
        String additionalParams = (String) requestMap.get("additionalParams");
        String pfbParams = (String) requestMap.get("PFB_NAME");//调度基准使用的pfb：schedule_base_pfb。如果不传，默认是""，基准和pfb 都会调用
        logger.info(">>open api triggerJobOnce, id:{}", id);
        if (!jobService.canHandleJob(openApiToken, id, "job_common_exec_once")) {
            return ReturnT.FAIL_FOR_NOT_AUTH;
        }
        // force cover job param
        if (executorParam == null) {
            executorParam = "";
        }
        if (pfbParams == null) {
            pfbParams = "";
        }
        if (additionalParams == null) {
            additionalParams = "";
        }
        JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobInfoDao().loadById(id);
        String instanceId = UUID.randomUUID().toString();
        JobTriggerPoolHelper.trigger(jobInfo.getJobGroup(), id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList, 0L, LogTypeEnum.MAIN_LOG, instanceId, additionalParams, pfbParams);
        return new ReturnT<>(ReturnT.SUCCESS_CODE, ReturnT.SUCCESS_FLAG, instanceId);
    }

    @GetMapping("/loadRouterFlags")
    @ResponseBody
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

    @PostMapping("/updateRouterFlag")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<Integer> updateRouterFlag(@RequestBody JobInfo jobInfo, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
        if (!jobService.canHandleJob(openApiToken, jobInfo.getId(), "job_common_exec_once")) {
            return new ReturnT<>(ReturnT.FAIL_CODE);
        }
        return ReturnT.ofSuccess(jobService.updateRouterFlag(jobInfo));
    }

    @GetMapping("/nextTriggerTime")
    @ResponseBody
    public ReturnT<List<String>> nextTriggerTime(@RequestParam String scheduleType, @RequestParam String scheduleConf, @RequestHeader(name = "XXL_JOB_OPEN_API_TOKEN", defaultValue = "") String openApiToken) {
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

    @GetMapping("/nextTriggerTimeReport")
    @ResponseBody
    public ReturnT<List<Map<String, Object>>> nextTriggerTimeReport(@RequestParam(required = false) List<Integer> jobGroups) {
        logger.info("JobInfoController.nextTriggerTimeReport，param:{}", jobGroups);
        return jobService.nextTriggerTimeReport(jobGroups);
    }

}
