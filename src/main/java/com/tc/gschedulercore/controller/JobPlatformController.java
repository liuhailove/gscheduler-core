package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobPlatform;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.core.util.StringUtils;
import com.tc.gschedulercore.dao.JobPlatformDao;
import com.tc.gschedulercore.dao.JobUserGroupDao;
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
@RequestMapping("/jobplatform")
public class JobPlatformController {

    private Logger logger = LoggerFactory.getLogger(JobPlatformController.class.getSimpleName());

    /**
     * 平台管理
     */
    @Resource
    public JobPlatformDao jobPlatformDao;

    /**
     * 用户组管理
     */
    @Resource
    private JobUserGroupDao xxlJobUserGroupDao;

    @AuthAction()
    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "1") Integer page,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit,
                                        @RequestParam(required = false) String platformName,
                                        @RequestParam(required = false) String env,
                                        @RequestParam(required = false) String region,
                                        @RequestParam(required = false) List<String> platformNames
    ) {

        int start = 0;
        int length = 10;
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        // page query
        List<JobPlatform> list = jobPlatformDao.pageList(start, length, platformName, env, region, platformNames);
        int listCount = jobPlatformDao.pageListCount(start, length, platformName, env, region, platformNames);
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
    public ReturnT<JobPlatform> save(@RequestBody JobPlatform jobPlatform) {
        logger.info("JobPlatformController.save，param:{}", jobPlatform);
        // valid
        if (jobPlatform.getPlatformName() == null || jobPlatform.getPlatformName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "PlatformName"));
        }
        if (jobPlatform.getPlatformName().length() < 4 || jobPlatform.getPlatformName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobplatform_field_name_length"));
        }
        if (jobPlatform.getPlatformName().contains(">") || jobPlatform.getPlatformName().contains("<")) {
            return new ReturnT<>(500, "PlatformName" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobPlatform.getPlatformName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobplatform_field_name_contain_chinese"));
        }
        jobPlatform.setPlatformName(jobPlatform.getPlatformName().trim());
        jobPlatform.setPlatformDesc(org.springframework.util.StringUtils.isEmpty(jobPlatform.getPlatformDesc()) ? "" : jobPlatform.getPlatformDesc().trim());
        jobPlatform.setAddTime(new Date());
        jobPlatform.setUpdateTime(new Date());
        int ret = jobPlatformDao.save(jobPlatform);
        return (ret > 0) ? new ReturnT<>(jobPlatform) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @PostMapping("/update")
    @ResponseBody
    public ReturnT<String> update(@RequestBody JobPlatform jobPlatform) {
        logger.info("JobPlatformController.update，param:{}", jobPlatform);
        // valid
        if (jobPlatform.getPlatformName() == null || jobPlatform.getPlatformName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "PlatformName"));
        }
        if (jobPlatform.getPlatformName().length() < 4 || jobPlatform.getPlatformName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobplatform_field_name_length"));
        }
        if (jobPlatform.getPlatformName().contains(">") || jobPlatform.getPlatformName().contains("<")) {
            return new ReturnT<>(500, "PlatformName" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobPlatform.getPlatformName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobplatform_field_name_contain_chinese"));
        }
        JobPlatform oldJobPlatform = jobPlatformDao.loadByName(jobPlatform.getPlatformName());
        oldJobPlatform.setPlatformName(jobPlatform.getPlatformName().trim());
        oldJobPlatform.setPlatformAddress(jobPlatform.getPlatformAddress());
        oldJobPlatform.setEnv(jobPlatform.getEnv());
        oldJobPlatform.setRegion(jobPlatform.getRegion());
        oldJobPlatform.setPlatStatus(jobPlatform.getPlatStatus());
        oldJobPlatform.setUpdateTime(new Date());
        oldJobPlatform.setPlatformDesc(jobPlatform.getPlatformDesc());
        int ret = jobPlatformDao.update(oldJobPlatform);
//        if (oldPlatform != null && !oldPlatform.equals(jobPlatform.getPlatformName().trim())) {
//            // 修改分组中的平台数据
//            List<XxlJobUserGroup> jobUserGroups = xxlJobUserGroupDao.loadByPlatform(oldPlatform);
//            if (!CollectionUtils.isEmpty(jobUserGroups)) {
//                for (XxlJobUserGroup userGroup : jobUserGroups) {
//                    List<String> permissionPlatformList = userGroup.getPermissionPlatformList();
//                    permissionPlatformList.remove(oldPlatform);
//                    permissionPlatformList.add(jobPlatform.getPlatformName());
//                    userGroup.setPermissionPlatforms(org.apache.commons.lang3.StringUtils.join(permissionPlatformList));
//                    userGroup.setUpdateTime(new Date());
//                    userGroup.setAuthor(jobPlatform.getAuthor());
//                    xxlJobUserGroupDao.update(userGroup);
//                }
//            }
//            // 修改用户中的平台数据
//            JobUserPoolHelper.triggerPlatformChange(oldPlatform, jobPlatform.getPlatformName());
//        }
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(String platformName) {
        JobPlatform platform = jobPlatformDao.loadByName(platformName);
        // 修改分组中的平台数据
//        List<XxlJobUserGroup> jobUserGroups = xxlJobUserGroupDao.loadByPlatform(platform.getPlatformName());
//        if (!CollectionUtils.isEmpty(jobUserGroups)) {
//            for (XxlJobUserGroup userGroup : jobUserGroups) {
//                List<String> permissionPlatformList = userGroup.getPermissionPlatformList();
//                permissionPlatformList.remove(platform.getPlatformName());
//                userGroup.setPermissionPlatforms(org.apache.commons.lang3.StringUtils.join(permissionPlatformList));
//                userGroup.setUpdateTime(new Date());
//                xxlJobUserGroupDao.update(userGroup);
//            }
//        }
//        // 修改用户中的平台数据
//        JobUserPoolHelper.triggerPlatformRemove(platform.getPlatformName());
        if (xxlJobUserGroupDao.existByPlatform(platformName)) {
            return new ReturnT<>(500, I18nUtil.getString("jobplatform_remove_exist_relation_group"));
        }
        int ret = jobPlatformDao.delete(platform.getId());
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/stop")
    @ResponseBody
    public ReturnT<String> stop(String platformName) {
        JobPlatform platform = jobPlatformDao.loadByName(platformName);
        platform.setPlatStatus(0);
        platform.setUpdateTime(new Date());
        int ret = jobPlatformDao.update(platform);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/start")
    @ResponseBody
    public ReturnT<String> start(String platformName) {
        JobPlatform platform = jobPlatformDao.loadByName(platformName);
        platform.setPlatStatus(1);
        platform.setUpdateTime(new Date());
        int ret = jobPlatformDao.update(platform);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/loadById")
    @ResponseBody
    public ReturnT<JobPlatform> loadById(int id) {
        JobPlatform jobPlatform = jobPlatformDao.load(id);
        return jobPlatform != null ? new ReturnT<>(jobPlatform) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @GetMapping("/loadByPlatformName")
    @ResponseBody
    public ReturnT<JobPlatform> loadByPlatformName(String platformName) {
        JobPlatform jobPlatform = jobPlatformDao.loadByName(platformName);
        return jobPlatform != null ? new ReturnT<>(jobPlatform) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    /************* platform *******************/
    @GetMapping("/list")
    public String jobplatformList() {
        return "jobplatform/list";
    }

    @GetMapping("/add")
    public String jobplatformAdd() {
        return "jobplatform/add";
    }

    @GetMapping("/edit")
    public String jobplatformEdit(@RequestParam String platformName, RedirectAttributes attributes) {
        attributes.addAttribute("platformName", platformName);
        return "jobplatform/edit";
    }
}
