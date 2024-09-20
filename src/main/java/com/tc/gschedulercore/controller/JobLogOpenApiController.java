package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.core.complete.JobCompleter;
import com.tc.gschedulercore.core.dto.KillParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import com.tc.gschedulercore.core.scheduler.JobScheduler;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.dao.JobGroupDao;
import com.tc.gschedulercore.dao.JobInfoDao;
import com.tc.gschedulercore.dao.JobLogDao;
import com.tc.gschedulercore.service.ExecutorBiz;
import com.tc.gschedulercore.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * index controller
 *
 * @author honggang.liu 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/joblog/openapi")
public class JobLogOpenApiController {
    private static Logger logger = LoggerFactory.getLogger(JobLogOpenApiController.class.getSimpleName());

    @Resource
    private JobGroupDao jobGroupDao;
    @Resource
    public JobInfoDao jobInfoDao;
    @Resource
    public JobLogDao jobLogDao;

    @GetMapping
    @ResponseBody
    public Map<String, Object> index(@RequestParam(required = false, defaultValue = "0") Integer jobId) {
        // 执行器列表
        Map<String, Object> dataMap = new HashMap<>(2);
        dataMap.put("JobGroupList", jobGroupDao.findAll());
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

    @GetMapping("/getJobsByGroup")
    @ResponseBody
    public ReturnT<List<JobInfo>> getJobsByGroup(@RequestParam int jobGroup) {
        return new ReturnT<>(jobInfoDao.getJobsByGroup(jobGroup));
    }

    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        @RequestParam(required = false) List<Integer> jobGroups,
                                        @RequestParam(required = false, defaultValue = "-1") int jobId,
                                        @RequestParam(required = false, defaultValue = "0") int logStatus,
                                        @RequestParam(required = false, defaultValue = "-1") long logId,
                                        @RequestParam(required = false, defaultValue = " - ") String filterTime,
                                        @RequestParam(required = false, defaultValue = "-1") Long parentLog,
                                        @RequestParam(required = false, defaultValue = "") String handleMsg,
                                        @RequestParam(required = false, defaultValue = "") String instanceId) {
        // parse param
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime != null && filterTime.trim().length() > 0) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }
        JobInfo jobInfo = null;
        if (jobId != 0) {
            jobInfo = jobInfoDao.loadById(jobId);
        }
        // page query
        List<JobLog> list = jobLogDao.pageList(start, length, jobGroups, jobInfo == null ? 0 : jobInfo.getJobGroup(), jobId, triggerTimeStart, triggerTimeEnd, logStatus, logId, parentLog, null, null, null, StringUtils.trim(handleMsg), instanceId, null);
        int listCount = jobLogDao.pageListCount(start, length, jobGroups, jobInfo == null ? 0 : jobInfo.getJobGroup(), jobId, triggerTimeStart, triggerTimeEnd, logStatus, logId, parentLog, null, null, null, StringUtils.trim(handleMsg), instanceId, null);
        // package result
        Map<String, Object> maps = new HashMap<>(3);
        // 总记录数
        maps.put("recordsTotal", listCount);
        // 过滤后的总记录数
        maps.put("recordsFiltered", listCount);
        // 分页列表
        maps.put("data", list);
        return maps;
    }

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
            if (logIds != null && !logIds.isEmpty()) {
                jobLogDao.clearLog(jobGroup, logIds);
            }
        } while (logIds != null && !logIds.isEmpty());

        return ReturnT.SUCCESS;
    }

}
