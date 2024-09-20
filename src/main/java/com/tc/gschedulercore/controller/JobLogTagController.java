package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.model.JobLogTag;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.core.util.StringUtils;
import com.tc.gschedulercore.dao.JobGroupDao;
import com.tc.gschedulercore.dao.JobLogTagDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * job role controller
 *
 * @author honggang.liu
 */
@Controller
@RequestMapping("/joblogtag")
public class JobLogTagController {

    private Logger logger = LoggerFactory.getLogger(JobLogTagController.class.getSimpleName());

    /**
     * 日志tag管理
     */
    @Resource
    public JobLogTagDao jobLogTagDao;

    /**
     * 执行器管理
     */
    @Resource
    public JobGroupDao jobGroupDao;

    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        @RequestParam(required = false, defaultValue = "") String appName,
                                        @RequestParam(required = false, defaultValue = "") String tagName,
                                        @RequestParam List<String> appNames) {

        // page query
        List<JobLogTag> list = jobLogTagDao.pageList(start, length, appName, tagName, appNames);
        int listCount = jobLogTagDao.pageListCount(start, length, appName, tagName, appNames);
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

    @PostMapping("/save")
    @ResponseBody
    public ReturnT<JobLogTag> save(@RequestBody JobLogTag jobLogTag) {
        logger.info("JobLogTagController.save，param:{}", jobLogTag);
        // valid
        if (jobLogTag.getTagName() == null || jobLogTag.getTagName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "TagName"));
        }
        if (jobLogTag.getTagName().length() < 4 || jobLogTag.getTagName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_tagname_length"));
        }
        if (jobLogTag.getTagName().contains(">") || jobLogTag.getTagName().contains("<")) {
            return new ReturnT<>(500, "TagName" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobLogTag.getTagName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_tagname_contain_chinese"));
        }
        if (jobLogTag.getAppName() == null || jobLogTag.getAppName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        JobGroup jobGroup = jobGroupDao.loadByName(jobLogTag.getAppName());
        if (jobGroup == null) {
            return new ReturnT<>(500, (I18nUtil.getString("jobconf_name_app_name_not_exist")));
        }
        jobLogTag.setAddTime(new Date());
        jobLogTag.setUpdateTime(new Date());
        int ret = jobLogTagDao.save(jobLogTag);
        return (ret > 0) ? new ReturnT<>(jobLogTag) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @PostMapping("/update")
    @ResponseBody
    public ReturnT<String> update(@RequestBody JobLogTag jobLogTag) {
        logger.info("JobLogTagController.update，param:{}", jobLogTag);
        // valid
        if (jobLogTag.getTagName() == null || jobLogTag.getTagName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "TagName"));
        }
        if (jobLogTag.getTagName().length() < 4 || jobLogTag.getTagName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_tagname_length"));
        }
        if (jobLogTag.getTagName().contains(">") || jobLogTag.getTagName().contains("<")) {
            return new ReturnT<>(500, "TagName" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobLogTag.getTagName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_tagname_contain_chinese"));
        }
        if (jobLogTag.getAppName() == null || jobLogTag.getAppName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        JobGroup jobGroup = jobGroupDao.loadByName(jobLogTag.getAppName());
        if (jobGroup == null) {
            return new ReturnT<>(500, (I18nUtil.getString("jobconf_name_app_name_not_exist")));
        }
        JobLogTag oldJobLogTag = jobLogTagDao.load(jobLogTag.getId());
        // 检查重复
        if (!oldJobLogTag.getTagName().equals(jobLogTag.getTagName()) || !oldJobLogTag.getAppName().equals(jobLogTag.getAppName())) {
            if (Boolean.TRUE.equals(jobLogTagDao.exist(jobLogTag.getAppName(), jobLogTag.getTagName()))) {
                return new ReturnT<>(500, (I18nUtil.getString("joblogtag_appnae_tagname_exist")));
            }
        }
        oldJobLogTag.setAuthor(jobLogTag.getAuthor());
        oldJobLogTag.setTagName(jobLogTag.getTagName());
        oldJobLogTag.setAppName(jobLogTag.getAppName());
        oldJobLogTag.setTagDesc(jobLogTag.getTagDesc());
        oldJobLogTag.setUpdateTime(new Date());
        int ret = jobLogTagDao.update(oldJobLogTag);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(int id) {
        int ret = jobLogTagDao.delete(id);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @GetMapping("/loadById")
    @ResponseBody
    public ReturnT<JobLogTag> loadById(int id) {
        JobLogTag jobLogTag = jobLogTagDao.load(id);
        return jobLogTag != null ? new ReturnT<>(jobLogTag) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    /**
     * list请求转发
     *
     * @return 转发到list
     */
    @GetMapping("/list")
    public String list() {
        return "joblogtag/list";
    }

    /**
     * add 请求转发
     *
     * @return add页面
     */
    @GetMapping("/add")
    public String add() {
        return "joblogtag/add";
    }

    /**
     * add 请求转发
     *
     * @return add页面
     */
    @GetMapping("/edit")
    public String edit(Integer id, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        return "joblogtag/edit";
    }
}
