package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobUser;
import com.tc.gschedulercore.core.model.JobUserGroup;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.dao.JobUserDao;
import com.tc.gschedulercore.dao.JobUserGroupDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
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
@RequestMapping("/jobuser")
public class JobUserController {

    private Logger logger = LoggerFactory.getLogger(JobUserController.class.getSimpleName());


    @Resource
    private JobUserDao xxlJobUserDao;

//    /**
//     * 角色管理DAO
//     */
//    @Resource
//    private XxlJobRoleDao xxlJobRoleDao;

    /**
     * 用户组管理DAO
     */
    @Resource
    private JobUserGroupDao xxlJobUserGroupDao;

    @AuthAction()
    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "1") Integer page,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit,
                                        @RequestParam(required = false) String username,
                                        @RequestParam(required = false) String roleName) {
        int start = 0;
        int length = 10;
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        // page list
        List<JobUser> list = xxlJobUserDao.pageList(start, length, username, roleName);
        int listCount = xxlJobUserDao.pageListCount(start, length, username, roleName);
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
    public ReturnT<String> save(@RequestBody JobUser jobUser) {
        // valid username
        if (!StringUtils.hasText(jobUser.getUsername())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_username"));
        }
        jobUser.setUsername(jobUser.getUsername().trim());
        if (!(jobUser.getUsername().length() >= 4 && jobUser.getUsername().length() <= 60)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }
        // valid password
        if (StringUtils.hasText(jobUser.getPwd())) {
            jobUser.setPwd(jobUser.getPwd().trim());
            if (!(jobUser.getPwd().length() >= 4 && jobUser.getPwd().length() <= 60)) {
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
            }
            // md5 password
            jobUser.setPwd(DigestUtils.md5DigestAsHex(jobUser.getPwd().getBytes()));
        }
        // check repeat
        JobUser existUser = xxlJobUserDao.loadByUserName(jobUser.getUsername());
        if (existUser != null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("user_username_repeat"));
        }
        if (!StringUtils.isEmpty(jobUser.getPermissionGroups())) {
            List<JobUserGroup> jobUserGroups = xxlJobUserGroupDao.loadByNames(Arrays.asList(jobUser.getPermissionGroups().split(",")));
            Set<String> permissionPlatformSet = new HashSet<>();
            if (!CollectionUtils.isEmpty(jobUserGroups)) {
                for (JobUserGroup userGroup : jobUserGroups) {
                    if (!StringUtils.isEmpty(userGroup.getPermissionPlatforms())) {
                        permissionPlatformSet.addAll(Arrays.asList(userGroup.getPermissionPlatforms().split(",")));
                    }
                }
            }
            jobUser.setPermissionPlatforms(org.apache.commons.lang3.StringUtils.join(permissionPlatformSet, ","));
        }
        jobUser.setAddTime(new Date());
        jobUser.setUpdateTime(new Date());
        // write
        xxlJobUserDao.save(jobUser);
        return ReturnT.SUCCESS;
    }

    @AuthAction()
    @PostMapping("/update")
    @ResponseBody
    public ReturnT<String> update(@RequestBody JobUser jobUser) {
        // valid username
        if (!StringUtils.hasText(jobUser.getUsername())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_username"));
        }
        jobUser.setUsername(jobUser.getUsername().trim());
        if (!(jobUser.getUsername().length() >= 4 && jobUser.getUsername().length() <= 60)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }
        if (!StringUtils.isEmpty(jobUser.getPermissionGroups())) {
            List<JobUserGroup> jobUserGroups = xxlJobUserGroupDao.loadByNames(Arrays.asList(jobUser.getPermissionGroups().split(",")));
            Set<String> permissionPlatformSet = new HashSet<>();
            if (!CollectionUtils.isEmpty(jobUserGroups)) {
                for (JobUserGroup userGroup : jobUserGroups) {
                    if (!StringUtils.isEmpty(userGroup.getPermissionPlatforms())) {
                        permissionPlatformSet.addAll(Arrays.asList(userGroup.getPermissionPlatforms().split(",")));
                    }
                }
            }
            jobUser.setPermissionPlatforms(org.apache.commons.lang3.StringUtils.join(permissionPlatformSet, ","));
        }
        jobUser.setAddTime(new Date());
        jobUser.setUpdateTime(new Date());
        JobUser oldUser = xxlJobUserDao.loadByUserName(jobUser.getUsername());
        jobUser.setId(oldUser.getId());
        // write
        xxlJobUserDao.update(jobUser);
        return ReturnT.SUCCESS;
    }

    @AuthAction()
    @GetMapping("/loadByUserName")
    @ResponseBody
    public ReturnT<JobUser> loadByUserName(String userName) {
        return new ReturnT<>(xxlJobUserDao.loadByUserName(userName));
    }

    @AuthAction()
    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(String userName) {
        JobUser user = xxlJobUserDao.loadByUserName(userName);
        xxlJobUserDao.delete(user.getId());
        return ReturnT.SUCCESS;
    }

    @AuthAction()
    @PostMapping("/updatePwd")
    @ResponseBody
    public ReturnT<String> updatePwd(String password) {
        // valid password
        if (password == null || password.trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), "密码不可为空");
        }
        password = password.trim();
        if (!(password.length() >= 4 && password.length() <= 60)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }

        // md5 password
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

//        // update pwd
//        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
//
//        // do write
//        XxlJobUser existUser = xxlJobUserDao.loadByUserName(loginUser.getUsername());
//        existUser.setPassword(md5Password);
//        xxlJobUserDao.update(existUser);

        return ReturnT.SUCCESS;
    }

    /************* user *******************/
    @GetMapping("/list")
    public String jobuserList() {
        return "jobuser/list";
    }

    @GetMapping("/add")
    public String jobuserAdd() {
        return "jobuser/add";
    }

    @GetMapping("/edit")
    public String jobuserEdit(@RequestParam String userName, RedirectAttributes attributes) {
        attributes.addAttribute("userName", userName);
        return "jobuser/edit";
    }


}
