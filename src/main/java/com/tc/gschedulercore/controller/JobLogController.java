package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.complete.JobCompleter;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.KillParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import com.tc.gschedulercore.core.model.LogMetric;
import com.tc.gschedulercore.core.scheduler.JobScheduler;
import com.tc.gschedulercore.core.thread.JobTriggerPoolHelper;
import com.tc.gschedulercore.core.trigger.LogTypeEnum;
import com.tc.gschedulercore.core.trigger.TriggerTypeEnum;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.dao.JobGroupDao;
import com.tc.gschedulercore.dao.JobInfoDao;
import com.tc.gschedulercore.dao.JobLogDao;
import com.tc.gschedulercore.dao.LogMetricDao;
import com.tc.gschedulercore.service.ExecutorBiz;
import com.tc.gschedulercore.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * index controller
 *
 * @author honggang.liu 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/joblog")
public class JobLogController {
    private static Logger logger = LoggerFactory.getLogger(JobLogController.class.getSimpleName());

    @Resource
    private JobGroupDao jobGroupDao;
    @Resource
    public JobInfoDao jobInfoDao;
    @Resource
    public JobLogDao jobLogDao;

    /**
     * 日志metric dao
     */
    @Resource
    private LogMetricDao logMetricDao;


    //默认值是0
    public static final int TASK_TERMINATION_FLAG_FALSE = 0;
    //用户点击终止任务时，会设置为true
    public static final int TASK_TERMINATION_FLAG_TRUE = 1;


    @AuthAction()
    @GetMapping
    @ResponseBody
    public Map<String, Object> index(@RequestParam(required = false, defaultValue = "0") Integer jobId) {
        // 执行器列表
        List<JobGroup> jobGroupListAll = jobGroupDao.findAll();
        Map<String, Object> dataMap = new HashMap<>(2);
        dataMap.put("JobGroupList", jobGroupListAll);

        // 任务
        if (jobId > 0) {
            JobInfo jobInfo = jobInfoDao.loadById(jobId);
            if (jobInfo == null) {
                throw new RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
            }
            dataMap.put("jobInfo", jobInfo);
        }

        return dataMap;
    }

    @AuthAction()
    @GetMapping("/getJobsByGroup")
    @ResponseBody
    public ReturnT<List<JobInfo>> getJobsByGroup(@RequestParam int jobGroup) {
        List<JobInfo> list = jobInfoDao.getJobsByGroup(jobGroup);
        return new ReturnT<>(list);
    }

    /**
     * @param start          分页开始
     * @param length         分页长度
     * @param jobGroups      执行器组
     * @param jobId          job id
     * @param logStatus      日志状态
     * @param logId          日志ID
     * @param filterTime     起始时间
     * @param parentLog      父日志
     * @param excludeTime    排除时间过滤
     * @param executeTimeAsc 是否按照执行时间升序排叙
     * @return 分页数据
     */
    @AuthAction()
    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit,
                                        @RequestParam(required = false) List<Integer> jobGroups,
                                        @RequestParam(required = false, defaultValue = "0") int jobId,
                                        @RequestParam(required = false, defaultValue = "0") int logStatus,
                                        @RequestParam(required = false, defaultValue = "0") Long logId,
                                        @RequestParam(required = false, defaultValue = "0") String filterTime,
                                        @RequestParam(required = false, defaultValue = "-1") Long parentLog,
                                        @RequestParam(required = false, defaultValue = "0") String excludeTime,
                                        @RequestParam(required = false) Boolean executeTimeAsc,
                                        @RequestParam(required = false, defaultValue = "") String handleMsg,
                                        @RequestParam(required = false, defaultValue = "") String instanceId,
                                        @RequestParam(required = false, defaultValue = "") String tagName) {
        // 此处主要为了兼容新版UI
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        // parse param
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime != null && !filterTime.trim().isEmpty()) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }
        Date excludeTimeStart = null;
        Date excludeTimeEnd = null;
        if (!StringUtils.isEmpty(excludeTime)) {
            String[] temp = excludeTime.split(" - ");
            if (temp.length == 2 && !StringUtils.isEmpty(temp[0]) && !StringUtils.isEmpty(temp[1])) {
                excludeTimeStart = DateUtil.parseDateTime(temp[0]);
                excludeTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }
        if (!CollectionUtils.isEmpty(jobGroups)) {
            jobGroups = jobGroups.stream().filter(id -> id > 0).collect(Collectors.toList());
        }
        JobInfo jobInfo = null;
        if (jobId != 0) {
            jobInfo = jobInfoDao.loadById(jobId);
        }
        // page query
        List<JobLog> list = jobLogDao.pageList(start, length, jobGroups, jobInfo == null ? 0 : jobInfo.getJobGroup(), jobId, triggerTimeStart, triggerTimeEnd, logStatus, logId, parentLog, excludeTimeStart, excludeTimeEnd, executeTimeAsc, StringUtils.trim(handleMsg), StringUtils.trim(instanceId), tagName);
        int listCount = jobLogDao.pageListCount(start, length, jobGroups, jobInfo == null ? 0 : jobInfo.getJobGroup(), jobId, triggerTimeStart, triggerTimeEnd, logStatus, logId, parentLog, excludeTimeStart, excludeTimeEnd, executeTimeAsc, StringUtils.trim(handleMsg), StringUtils.trim(instanceId), tagName);
        // package result
        Map<String, Object> maps = new HashMap<>(6);
        // 总记录数
        maps.put("recordsTotal", listCount);
        // 过滤后的总记录数
        maps.put("recordsFiltered", listCount);
        // 分页列表
        maps.put("data", list);
        // 消息
        maps.put("msg", "");
        maps.put("count", listCount);
        maps.put("code", ReturnT.SUCCESS_CODE);
        return maps;
    }

    @AuthAction()
    @GetMapping("/logDetailPage")
    @ResponseBody
    public Map<String, Object> logDetailPage(@RequestParam long id) {
        JobLog jobLog = jobLogDao.loadBy(id);
        if (jobLog == null) {
            throw new RuntimeException(I18nUtil.getString("joblog_logid_unvalid"));
        }
        Map<String, Object> dataMap = new HashMap<>(5);
        dataMap.put("triggerCode", jobLog.getTriggerCode());
        dataMap.put("handleCode", jobLog.getHandleCode());
        dataMap.put("executorAddress", jobLog.getExecutorAddress());
        dataMap.put("triggerTime", jobLog.getTriggerTime().getTime());
        dataMap.put("logId", jobLog.getId());
        return dataMap;
    }

//    @AuthAction()
//    @GetMapping("/logDetailCat")
//    @ResponseBody
//    public ReturnT<LogResult> logDetailCat(@RequestParam String executorAddress, @RequestParam long triggerTime, @RequestParam long logId, @RequestParam int fromLineNum) {
//        try {
//            ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(executorAddress);
//            ReturnT<LogResult> logResult = executorBiz.log(new LogParam(triggerTime, logId, fromLineNum));
//
//            // is end
//            if (logResult.getContent() != null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
//                JobLog jobLog = jobLogDao.loadBy(logId);
//                if (jobLog.getHandleCode() > 0) {
//                    logResult.getContent().setEnd(true);
//                }
//            }
//
//            return logResult;
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//            return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
//        }
//    }

    @AuthAction()
    @GetMapping("/logKill")
    @ResponseBody
    public ReturnT<String> logKill(@RequestParam long id) {
        // base check
        JobLog log = jobLogDao.loadBy(id);
        JobInfo jobInfo = jobInfoDao.loadById(log.getJobId());
        if (jobInfo == null) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
            return new ReturnT<>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }
        //当用户点击终止任务时，设置字段标识为true。
        JobAdminConfig.getAdminConfig().getJobLogDao().updateTaskTerminationFlag(id, TASK_TERMINATION_FLAG_TRUE);

        // request of kill
        ReturnT<String> runResult = null;
        try {
            ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(log.getExecutorAddress());
            runResult = executorBiz.kill(new KillParam(jobInfo.getId()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            runResult = new ReturnT<>(500, e.getMessage());
        }

        if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
            log.setHandleCode(ReturnT.FAIL_CODE);
            log.setHandleMsg(I18nUtil.getString("joblog_kill_log_byman") + ":" + (runResult.getMsg() != null ? runResult.getMsg() : ""));
            log.setHandleTime(new Date());
            JobCompleter.updateHandleInfoAndFinish(log, false);
            return new ReturnT<>(runResult.getMsg());
        } else {
            return new ReturnT<>(500, runResult.getMsg());
        }
    }

    @AuthAction()
    @GetMapping("/clearLog")
    @ResponseBody
    public ReturnT<String> clearLog(@RequestParam int jobGroup, @RequestParam int jobId, @RequestParam int type) {

        Date clearBeforeTime = null;
        int clearBeforeNum = 0;
        if (type == 1) {
            // 清理一个月之前日志数据
            clearBeforeTime = DateUtil.addMonths(new Date(), -1);
        } else if (type == 2) {
            // 清理三个月之前日志数据
            clearBeforeTime = DateUtil.addMonths(new Date(), -3);
        } else if (type == 3) {
            // 清理六个月之前日志数据
            clearBeforeTime = DateUtil.addMonths(new Date(), -6);
        } else if (type == 4) {
            // 清理一年之前日志数据
            clearBeforeTime = DateUtil.addYears(new Date(), -1);
        } else if (type == 5) {
            // 清理一千条以前日志数据
            clearBeforeNum = 1000;
        } else if (type == 6) {
            // 清理一万条以前日志数据
            clearBeforeNum = 10000;
        } else if (type == 7) {
            // 清理三万条以前日志数据
            clearBeforeNum = 30000;
        } else if (type == 8) {
            // 清理十万条以前日志数据
            clearBeforeNum = 100000;
        } else if (type == 9) {
            // 清理所有日志数据
            clearBeforeNum = 0;
        } else {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_clean_type_unvalid"));
        }

        List<Long> logIds;
        do {
            logIds = jobLogDao.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (!CollectionUtils.isEmpty(logIds)) {
                jobLogDao.clearLog(jobGroup, logIds);
            }
        } while (!CollectionUtils.isEmpty(logIds));

        return ReturnT.SUCCESS;
    }

    @AuthAction()
    @GetMapping("/triggerFailed")
    @ResponseBody
    public ReturnT<String> triggerFailed(@RequestParam Long id) {
        logger.info("JobLogController.triggerFailed，param:{}", id);
        JobLog log = jobLogDao.loadBy(id);
        if (log == null) {
            return new ReturnT<>(500, I18nUtil.getString("joblog_logid_unvalid"));
        }
        // 如果触发成功并且[执行成功或者待执行]，则不允许重试
        if (log.getTriggerCode() == ReturnT.SUCCESS_CODE && (log.getHandleCode() == ReturnT.SUCCESS_CODE || log.getHandleCode() == 0)) {
            return new ReturnT<>(500, I18nUtil.getString("joblog_logid_unvalid_trigger_status"));
        }
        // 重试后，一定有子任务
        log.setHasSub(true);
        jobLogDao.updateHandleInfo(log);
        JobTriggerPoolHelper.trigger(log.getJobGroup(), log.getJobId(), TriggerTypeEnum.MANUAL_RETRY, 0, log.getExecutorShardingParam(), log.getExecutorParam(), null, id, LogTypeEnum.SUB_LOG, log.getInstanceId(), "", "");
        return ReturnT.SUCCESS;
    }

    /**
     * @param logId 日志ID
     * @return 分页数据
     */
    @AuthAction()
    @GetMapping("/processPageList")
    @ResponseBody
    public Map<String, Object> processPageList(@RequestParam Long logId) {
        // page query
        JobLog jobLog = jobLogDao.loadBy(logId);
        List<LogMetric> list = logMetricDao.findBy(logId, jobLog.getJobId());
        // package result
        Map<String, Object> maps = new HashMap<>(6);
        // 总记录数
        maps.put("recordsTotal", list.size());
        // 过滤后的总记录数
        maps.put("recordsFiltered", list.size());
        // 分页列表
        maps.put("data", list);
        // 消息
        maps.put("msg", "");
        maps.put("count", list.size());
        maps.put("code", ReturnT.SUCCESS_CODE);
        return maps;
    }


    /************* log *******************/
    @GetMapping("/list")
    public String joblogList(@RequestParam(required = false) Long jobId, RedirectAttributes attributes) {
        if (jobId != null) {
            attributes.addAttribute("jobId", jobId);
        }
        return "joblog/list";
    }

    @GetMapping("/childLogList")
    public String joblogchildLogList(@RequestParam Long logId, RedirectAttributes attributes) {
        attributes.addAttribute("logId", logId);
        return "joblog/child_log_list";
    }

    @GetMapping("/processLogList")
    public String processLogList(@RequestParam Long logId, RedirectAttributes attributes) {
        attributes.addAttribute("logId", logId);
        return "joblog/process_log_list";
    }

}
