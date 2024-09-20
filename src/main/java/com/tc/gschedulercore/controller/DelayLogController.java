package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.DelayLog;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.dao.DelayLogDao;
import com.tc.gschedulercore.dao.JobInfoDao;
import com.tc.gschedulercore.util.DateUtil;
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
@RequestMapping("/delay")
public class DelayLogController {

    /**
     * 延迟日志DAO
     */
    @Resource
    private DelayLogDao delayLogDao;

    /**
     * 任务Dao
     */
    @Resource
    public JobInfoDao jobInfoDao;


    /**
     * @param start       分页开始
     * @param length      分页长度
     * @param jobGroups   执行器组
     * @param jobId       job id
     * @param logId       日志ID
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
                                        @RequestParam(required = false, defaultValue = "0") Integer jobId,
                                        @RequestParam(required = false, defaultValue = "0") Long logId,
                                        @RequestParam(required = false, defaultValue = "0") String filterTime) {
        // 此处主要为了兼容新版UI
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
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
        if (!CollectionUtils.isEmpty(jobGroups)) {
            jobGroups = jobGroups.stream().filter(id -> id > 0).collect(Collectors.toList());
        }
        JobInfo jobInfo = null;
        if (jobId > 0) {
            jobInfo = jobInfoDao.loadById(jobId);
        }
        // page query
        List<DelayLog> list = delayLogDao.pageList(start, length, jobGroups, jobInfo == null ? 0 : jobInfo.getJobGroup(), jobId, triggerTimeStart, triggerTimeEnd, logId);
        int listCount = delayLogDao.pageListCount(start, length, jobGroups, jobInfo == null ? 0 : jobInfo.getJobGroup(), jobId, triggerTimeStart, triggerTimeEnd, logId);
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

    /************* list *******************/
    @GetMapping("/list")
    public String joblogList(@RequestParam(required = false) Long jobId, RedirectAttributes attributes) {
        if (jobId != null) {
            attributes.addAttribute("jobId", jobId);
        }
        return "delay/list";
    }

}
