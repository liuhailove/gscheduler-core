package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.MenuInfo;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.SystemInfo;
import com.tc.gschedulercore.core.model.JobMenu;
import com.tc.gschedulercore.core.model.JobRole;
import com.tc.gschedulercore.core.thread.JobUserPoolHelper;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.core.util.StringUtils;
import com.tc.gschedulercore.dao.JobMenuDao;
import com.tc.gschedulercore.dao.JobRoleDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
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
@RequestMapping("/jobrole")
public class JobRoleController {

    private Logger logger = LoggerFactory.getLogger(JobRoleController.class.getSimpleName());

    /**
     * 角色管理
     */
    @Resource
    public JobRoleDao jobRoleDao;

    /**
     * 菜单管理
     */
    @Resource
    private JobMenuDao jobMenuDao;

    /**
     * 系统管理员角色
     */
    private static final String SYSTEM_MANAGER_ROLE = "SYSTEM_MANAGER_ROLE";

    @AuthAction()
    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit) {

        // 此处主要为了兼容新版UI
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        // page query
        List<JobRole> list = jobRoleDao.pageList(start, length);
        int listCount = jobRoleDao.pageListCount(start, length);
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
    public ReturnT<JobRole> save(@RequestBody JobRole jobRole) {
        logger.info("JobRoleController.save，param:{}", jobRole);
        // valid
        if (jobRole.getRoleName() == null || jobRole.getRoleName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "RoleName"));
        }
        if (jobRole.getRoleName().length() < 4 || jobRole.getRoleName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_rolename_length"));
        }
        if (jobRole.getRoleName().contains(">") || jobRole.getRoleName().contains("<")) {
            return new ReturnT<>(500, "RoleName" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobRole.getRoleName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_rolename_contain_chinese"));
        }
        jobRole.setRoleName(jobRole.getRoleName().trim());
        jobRole.setAddTime(new Date());
        jobRole.setUpdateTime(new Date());
        int ret = jobRoleDao.save(jobRole);
        return (ret > 0) ? new ReturnT<>(jobRole) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @PostMapping("/update")
    @ResponseBody
    public ReturnT<String> update(@RequestBody JobRole jobRole) {
        logger.info("JobRoleController.update，param:{}", jobRole);
        // valid
        if (jobRole.getRoleName() == null || jobRole.getRoleName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "RoleName"));
        }
        if (jobRole.getRoleName().length() < 4 || jobRole.getRoleName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_rolename_length"));
        }
        if (jobRole.getRoleName().contains(">") || jobRole.getRoleName().contains("<")) {
            return new ReturnT<>(500, "RoleName" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobRole.getRoleName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_rolename_contain_chinese"));
        }
        JobRole oldJobRole = jobRoleDao.loadByName(jobRole.getRoleName());
        oldJobRole.setAuthor(jobRole.getAuthor());
        oldJobRole.setRoleName(jobRole.getRoleName());
        oldJobRole.setRoleDesc(jobRole.getRoleDesc());
        oldJobRole.setPermissionUrls(jobRole.getPermissionUrls());
        oldJobRole.setPermissionMenus(jobRole.getPermissionMenus());
        oldJobRole.setUpdateTime(new Date());
        int ret = jobRoleDao.update(oldJobRole);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(String roleName) {
        JobRole jobRole = jobRoleDao.loadByName(roleName);
        // 修改关联用户
        JobUserPoolHelper.triggerRoleNameRemove(jobRole.getRoleName());
        int ret = jobRoleDao.delete(jobRole.getId());
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/loadById")
    @ResponseBody
    public ReturnT<JobRole> loadById(int id) {
        JobRole jobRole = jobRoleDao.load(id);
        return jobRole != null ? new ReturnT<>(jobRole) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @GetMapping("/loadByRoleName")
    @ResponseBody
    public ReturnT<JobRole> loadByRoleName(@RequestParam String roleName) {
        JobRole jobRole = jobRoleDao.loadByName(roleName);
        return jobRole != null ? new ReturnT<>(jobRole) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @GetMapping("/loadSystemMenu")
    @ResponseBody
    @Cacheable(value = "menuCache", key = "'loadSystemMenu-'+#roleName")
    public SystemInfo loadSystemMenu(@RequestParam String roleName) {
        List<MenuInfo> menuInfos = new ArrayList<>();
        List<JobMenu> jobMenus;
        if (SYSTEM_MANAGER_ROLE.equals(roleName)) {
            jobMenus = jobMenuDao.loadsAll();
        } else {
            // 构造菜单
            JobRole jobRole = jobRoleDao.loadByName(roleName);
            jobMenus = jobMenuDao.loads(Arrays.asList(jobRole.getPermissionMenus().split(",")));
        }
        if (!CollectionUtils.isEmpty(jobMenus)) {
            for (JobMenu jobMenu : jobMenus) {
                MenuInfo menuInfo = new MenuInfo();
                menuInfo.setName(jobMenu.getMenuName());
                menuInfo.setHref(jobMenu.getHref());
                menuInfo.setTitle(jobMenu.getTitle());
                menuInfo.setIcon(jobMenu.getIcon());
                menuInfo.setImage(jobMenu.getImage());
                menuInfo.setTarget(jobMenu.getTarget());
                menuInfo.setParent(jobMenu.getParent());
                menuInfos.add(menuInfo);
            }
            menuInfos = MenuInfo.buildTree(menuInfos);
        }
        return new SystemInfo(menuInfos);
    }

    @AuthAction()
    @GetMapping("/loadOp")
    @ResponseBody
    @Cacheable(value = "menuCache", key = "'loadOp-'+#roleName+#parent")
    public ReturnT<List<JobMenu>> loadOp(@RequestParam String roleName, @RequestParam String parent) {
        if (SYSTEM_MANAGER_ROLE.equals(roleName)) {
            return new ReturnT<>(jobMenuDao.loadAllOps());
        }
        JobRole jobRole = jobRoleDao.loadByName(roleName);
        return new ReturnT<>(jobMenuDao.loadOps(Arrays.asList(jobRole.getPermissionMenus().split(",")), parent));
    }

    @GetMapping("/invalidLoadOpCache")
    @ResponseBody
    public ReturnT<String> invalidLoadOpCache(@RequestParam String roleName, @RequestParam String parent) {
        Cache menuCache = JobAdminConfig.getAdminConfig().getCacheManager().getCache("menuCache");
        if (menuCache == null) {
            return ReturnT.SUCCESS;
        }
        menuCache.evictIfPresent("loadOp-" + roleName + parent);
        return ReturnT.SUCCESS;
    }

    /************* role *******************/
    @GetMapping("/list")
    public String jobroleList() {
        return "jobrole/list";
    }

    @GetMapping("/add")
    public String jobroleAdd() {
        return "jobrole/add";
    }

    @GetMapping("/edit")
    public String jobroleEdit(@RequestParam String roleName, RedirectAttributes attributes) {
        attributes.addAttribute("roleName", roleName);
        return "jobrole/edit";
    }
}
