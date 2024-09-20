package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.dao.JobGroupDao;
import com.tc.gschedulercore.service.JobService;
import io.micrometer.core.annotation.Timed;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * index controller
 *
 * @author honggang.liu 2015-12-19 16:13:16
 */
@Controller
public class IndexController {
    private static Logger logger = LoggerFactory.getLogger(IndexController.class.getSimpleName());
    /**
     * job服务
     */
    @Resource
    private JobService jobService;

    /**
     * 执行器
     */
    @Resource
    public JobGroupDao jobGroupDao;

    @GetMapping("/")
    @ResponseBody
    public Map<String, Object> index(@RequestParam(required = false) List<Integer> jobGroups) {
        return jobService.dashboardInfo(jobGroups);
    }

    @Timed(percentiles = {0.5, 0.80, 0.90, 0.99, 0.999})
    @GetMapping("/chartInfo")
    @ResponseBody
    public ReturnT<List<Map<String, Object>>> chartInfo(@RequestParam(required = false) List<Integer> jobGroups, @RequestParam String startDate, @RequestParam String endDate) throws ParseException {
        return jobService.chartInfo(jobGroups, DateUtils.parseDate(startDate, "yyyy-MM-dd HH:mm:ss"), DateUtils.parseDate(endDate, "yyyy-MM-dd HH:mm:ss"));
    }

    @Timed(percentiles = {0.5, 0.80, 0.90, 0.99, 0.999})
    @GetMapping("/chartInfoByApp")
    @ResponseBody
    @Cacheable(value = "dashboardCache",key="'chartInfoByApp-'+#appNames+#startDate+#endDate")
    public ReturnT<List<Map<String, Object>>> chartInfoByApp(@RequestParam("appNames") String[] appNames, @RequestParam String startDate, @RequestParam String endDate) throws ParseException {
        if (appNames == null || appNames.length == 0) {
            return new ReturnT<>(Collections.emptyList());
        }
        return jobService.chartInfo(jobGroupDao.loadPkByNames(appNames), DateUtils.parseDate(startDate, "yyyy-MM-dd HH:mm:ss"), DateUtils.parseDate(endDate, "yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 待执行任务报表
     *
     * @param jobGroups 执行器集合
     * @return 待执行任务报表
     */
    @Timed(percentiles = {0.5, 0.80, 0.90, 0.99, 0.999})
    @GetMapping("/nextTriggerTimeReport")
    @ResponseBody
    @Cacheable(value = "triggerTimeCache",key="'nextTriggerTimeReport-'+#jobGroups")
    public ReturnT<List<Map<String, Object>>> nextTriggerTimeReport(@RequestParam(required = false) List<Integer> jobGroups) {
        logger.info("JobInfoController.nextTriggerTimeReport，param:{}", jobGroups);
        return jobService.nextTriggerTimeReport(jobGroups);
    }

    /**
     * 待执行任务报表
     *
     * @param appNames 执行器集合
     * @return 待执行任务报表
     */
    @Timed(percentiles = {0.5, 0.80, 0.90, 0.99, 0.999})
    @GetMapping("/nextTriggerTimeReportByApp")
    @ResponseBody
    @Cacheable(value = "triggerTimeCache",key="'nextTriggerTimeReport-'+#appNames")
    public Map<String, Object> nextTriggerTimeReportByApp(@RequestParam("appNames") List<String> appNames) {
        logger.info("JobInfoController.nextTriggerTimeReportByApp，param:{}", appNames);
        Map<String, Object> maps = new HashMap<>(6);
        // 总记录数
        maps.put("recordsTotal", 0);
        // 过滤后的总记录数
        maps.put("recordsFiltered", 0);
        // 分页列表
        maps.put("data", Collections.emptyList());
        // 消息
        maps.put("msg", "");
        maps.put("count", 0);
        maps.put("code", ReturnT.SUCCESS_CODE);
        if (appNames == null || appNames.isEmpty()) {
            return maps;
        }
        ReturnT<List<Map<String, Object>>> returnT = jobService.nextTriggerTimeReport(jobGroupDao.loadPkByNames(appNames.toArray(new String[0])));
        maps.put("recordsTotal", returnT.getContent().size());
        // 过滤后的总记录数
        maps.put("recordsFiltered", returnT.getContent().size());
        // 分页列表
        maps.put("data", returnT.getContent());
        // 消息
        maps.put("msg", returnT.getMsg());
        maps.put("count", returnT.getContent().size());
        return maps;
    }

    @GetMapping("env")
    @ResponseBody
    public Map<String, Object> env() {
        Map<String, Object> maps = new HashMap<>(2);
        // 消息
        maps.put("code", ReturnT.SUCCESS_CODE);
        maps.put("data", JobAdminConfig.getAdminConfig().getEnv());
        return maps;
    }


    @RequestMapping("/help")
    public String help() {
        return "help";
    }

    @RequestMapping("/index")
    public String toIndex() {
        return "index";
    }

    @GetMapping("/welcome")
    public String welcome() {
        return "welcome";
    }

    @RequestMapping("/platform")
    public String platform() {
        return "platform";
    }

}
