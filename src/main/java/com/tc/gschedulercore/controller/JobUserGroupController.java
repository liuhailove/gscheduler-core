package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobUser;
import com.tc.gschedulercore.core.model.JobUserGroup;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.core.util.StringUtils;
import com.tc.gschedulercore.dao.JobGroupDao;
import com.tc.gschedulercore.dao.JobUserDao;
import com.tc.gschedulercore.dao.JobUserGroupDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.*;

/**
 * job role controller
 *
 * @author honggang.liu
 */
@Controller
@RequestMapping("/jobusergroup")
public class JobUserGroupController {

    private Logger logger = LoggerFactory.getLogger(JobUserGroupController.class.getSimpleName());

    /**
     * 组管理
     */
    @Resource
    public JobUserGroupDao jobUserGroupDao;

    /**
     * 用户管理
     */
    @Resource
    private JobUserDao xxlJobUserDao;

    /**
     * 执行器管理
     */
    @Resource
    private JobGroupDao jobGroupDao;

    @AuthAction()
    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "1") Integer page,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit,
                                        @RequestParam(required = false) String groupName,
                                        @RequestParam(required = false) List<String> groupNames) {
        int start = 0;
        int length = 10;
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        // page query
        List<JobUserGroup> list = jobUserGroupDao.pageList(start, length, groupName, groupNames);
        int listCount = jobUserGroupDao.pageListCount(start, length, groupName, groupNames);
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
    public ReturnT<JobUserGroup> save(@RequestBody JobUserGroup jobUserGroup) {
        logger.info("JobUserGroupController.save，param:{}", jobUserGroup);
        // valid
        if (jobUserGroup.getGroupName() == null || jobUserGroup.getGroupName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "GroupName"));
        }
        if (jobUserGroup.getGroupName().length() < 4 || jobUserGroup.getGroupName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobugroup_field_name_length"));
        }
        if (jobUserGroup.getGroupName().contains(">") || jobUserGroup.getGroupName().contains("<")) {
            return new ReturnT<>(500, "GroupName" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobUserGroup.getGroupName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobugroup_field_name_contain_chinese"));
        }
        jobUserGroup.setGroupName(jobUserGroup.getGroupName().trim());
        jobUserGroup.setAddTime(new Date());
        jobUserGroup.setUpdateTime(new Date());
        int ret = jobUserGroupDao.save(jobUserGroup);
        return (ret > 0) ? new ReturnT<>(jobUserGroup) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @PostMapping("/update")
    @ResponseBody
    public ReturnT<String> update(@RequestBody JobUserGroup jobUserGroup) {
        logger.info("JobUserGroupController.update，param:{}", jobUserGroup);
        // valid
        if (jobUserGroup.getGroupName() == null || jobUserGroup.getGroupName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "GroupName"));
        }
        if (jobUserGroup.getGroupName().length() < 4 || jobUserGroup.getGroupName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobugroup_field_name_length"));
        }
        if (jobUserGroup.getGroupName().contains(">") || jobUserGroup.getGroupName().contains("<")) {
            return new ReturnT<>(500, "GroupName" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobUserGroup.getGroupName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobugroup_field_name_contain_chinese"));
        }
        JobUserGroup oldJobUserGroup = jobUserGroupDao.load(jobUserGroup.getId());
        String oldGroupName = oldJobUserGroup.getGroupName();
        oldJobUserGroup.setAuthor(jobUserGroup.getAuthor());
        oldJobUserGroup.setGroupName(jobUserGroup.getGroupName());
        oldJobUserGroup.setGroupDesc(jobUserGroup.getGroupDesc());
        oldJobUserGroup.setPermissionPlatforms(jobUserGroup.getPermissionPlatforms());
        oldJobUserGroup.setPermissionJobGroups(jobUserGroup.getPermissionJobGroups());
        oldJobUserGroup.setUpdateTime(new Date());
        int ret = jobUserGroupDao.update(oldJobUserGroup);
        if (!jobUserGroup.getGroupName().trim().equals(oldGroupName)) {
            List<JobUser> jobUsers = xxlJobUserDao.loadByUGroupName(oldGroupName);
            if (!CollectionUtils.isEmpty(jobUsers)) {
                for (JobUser user : jobUsers) {
                    List<String> permissionGroupList = user.getPermissionGroupList();
                    permissionGroupList.remove(oldGroupName);
                    permissionGroupList.add(jobUserGroup.getGroupName().trim());
                    user.setPermissionGroups(org.apache.commons.lang3.StringUtils.join(permissionGroupList, ","));
                    user.setUpdateTime(new Date());
                    xxlJobUserDao.update(user);
                }
            }
        }
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(int id) {
        JobUserGroup jobUserGroup = jobUserGroupDao.load(id);
        List<JobUser> jobUsers = xxlJobUserDao.loadByUGroupName(jobUserGroup.getGroupName());
        if (!CollectionUtils.isEmpty(jobUsers)) {
            for (JobUser user : jobUsers) {
                List<String> permissionGroupList = user.getPermissionGroupList();
                permissionGroupList.remove(jobUserGroup.getGroupName());
                user.setPermissionGroups(org.apache.commons.lang3.StringUtils.join(permissionGroupList, ","));
                user.setUpdateTime(new Date());
                xxlJobUserDao.update(user);
            }
        }
        int ret = jobUserGroupDao.delete(id);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/loadById")
    @ResponseBody
    public ReturnT<JobUserGroup> loadById(int id) {
        JobUserGroup jobUserGroup = jobUserGroupDao.load(id);
        return jobUserGroup != null ? new ReturnT<>(jobUserGroup) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @GetMapping("/loadByUGroupName")
    @ResponseBody
    public ReturnT<JobUserGroup> loadByUGroupName(String groupName) {
        JobUserGroup jobUserGroup = jobUserGroupDao.loadByUGroupName(groupName);
        return jobUserGroup != null ? new ReturnT<>(jobUserGroup) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @GetMapping("/removeByUGroupName")
    @ResponseBody
    public ReturnT<String> removeByUGroupName(String groupName) {
        JobUserGroup jobUserGroup = jobUserGroupDao.loadByUGroupName(groupName);
        if (Boolean.TRUE.equals(jobGroupDao.existUGroup(groupName))) {
            return new ReturnT<>(500, I18nUtil.getString("jobguroup_remove_exist_relation_group"));
        }
        List<JobUser> jobUsers = xxlJobUserDao.loadByUGroupName(jobUserGroup.getGroupName());
        if (!CollectionUtils.isEmpty(jobUsers)) {
            for (JobUser user : jobUsers) {
                List<String> permissionGroupList = user.getPermissionGroupList();
                permissionGroupList.remove(jobUserGroup.getGroupName());
                user.setPermissionGroups(org.apache.commons.lang3.StringUtils.join(permissionGroupList, ","));
                user.setUpdateTime(new Date());
                xxlJobUserDao.update(user);
            }
        }
        int ret = jobUserGroupDao.delete(jobUserGroup.getId());
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/loadByUGroupNames")
    @ResponseBody
    public ReturnT<List<JobUserGroup>> loadByNames(String[] groupNames) {
        if (groupNames == null || groupNames.length == 0) {
            return new ReturnT<>(Collections.emptyList());
        }
        return new ReturnT<>(jobUserGroupDao.loadByNames(Arrays.asList(groupNames)));
    }

    @AuthAction()
    @GetMapping("/loadPermissionJobGroupsByNames")
    @ResponseBody
    public ReturnT<List<String>> loadPermissionJobGroupsByNames(String[] groupNames) {
        if (groupNames == null || groupNames.length == 0) {
            return new ReturnT<>(Collections.emptyList());
        }
        return new ReturnT<>(jobUserGroupDao.loadPermissionJobGroupsByNames(Arrays.asList(groupNames)));
    }


    @AuthAction()
    @GetMapping("/loadPermissionPlatformsByNames")
    @ResponseBody
    public ReturnT<List<String>> loadPermissionPlatformsByNames(String[] groupNames) {
        if (groupNames == null || groupNames.length == 0) {
            return new ReturnT<>(Collections.emptyList());
        }
        return new ReturnT<>(jobUserGroupDao.loadPermissionPlatformsByNames(Arrays.asList(groupNames)));
    }

    /************* ugroup *******************/
    @GetMapping("/list")
    public String jobugroupList() {
        return "jobugroup/list";
    }

    @GetMapping("/add")
    public String jobugroupAdd() {
        return "jobugroup/add";
    }

    @GetMapping("/edit")
    public String jobugroupEdit(@RequestParam String groupName, RedirectAttributes attributes) {
        attributes.addAttribute("groupName", groupName);
        return "jobugroup/edit";
    }

}
