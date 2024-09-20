package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobMenu;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.core.util.StringUtils;
import com.tc.gschedulercore.dao.JobMenuDao;
import com.tc.gschedulercore.dao.JobRoleDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * job role controller
 *
 * @author honggang.liu
 */
@Controller
@RequestMapping("/jobmenu")
public class JobMenuController {

    private Logger logger = LoggerFactory.getLogger(JobMenuController.class.getSimpleName());

    /**
     * 菜单管理
     */
    @Resource
    public JobMenuDao jobMenuDao;

    /**
     * 角色管理
     */
    @Resource
    public JobRoleDao jobRoleDao;

    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> findPage(@RequestParam(required = false, defaultValue = "1") Integer page,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit) {
        int start = 0;
        int length = 10;
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        // page query
        List<JobMenu> list = jobMenuDao.findPage(start, length);
        int listCount = jobMenuDao.findAllCount();
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
    public ReturnT<JobMenu> save(@RequestBody JobMenu jobMenu) {
        logger.info("JobUrlController.save，param:{}", jobMenu);
        // valid
        if (jobMenu.getMenuName() == null || jobMenu.getMenuName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "Name"));
        }
        if (jobMenu.getMenuName().length() < 4 || jobMenu.getMenuName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_urlname_length"));
        }
        if (jobMenu.getMenuName().contains(">") || jobMenu.getMenuName().contains("<")) {
            return new ReturnT<>(500, "Name" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobMenu.getMenuName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_urlname_contain_chinese"));
        }
        int ret = jobMenuDao.save(jobMenu);
        return (ret > 0) ? new ReturnT<>(jobMenu) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @PostMapping("/update")
    @ResponseBody
    public ReturnT<String> update(@RequestBody JobMenu jobMenu) {
        logger.info("JobUrlController.update，param:{}", jobMenu);
        // valid
        if (jobMenu.getMenuName() == null || jobMenu.getMenuName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "Name"));
        }
        if (jobMenu.getMenuName().length() < 4 || jobMenu.getMenuName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_urlname_length"));
        }
        if (jobMenu.getMenuName().contains(">") || jobMenu.getMenuName().contains("<")) {
            return new ReturnT<>(500, "Name" + I18nUtil.getString("system_unvalid"));
        }
        if (StringUtils.isContainChinese(jobMenu.getMenuName())) {
            return new ReturnT<>(500, I18nUtil.getString("jobconf_field_urlname_contain_chinese"));
        }
        JobMenu oldJobUrl = jobMenuDao.loadByName(jobMenu.getMenuName());
        oldJobUrl.setHref(jobMenu.getHref());
        oldJobUrl.setMenuName(jobMenu.getMenuName());
        oldJobUrl.setTitle(jobMenu.getTitle());
        int ret = jobMenuDao.update(oldJobUrl);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/removeByName")
    @ResponseBody
    public ReturnT<String> removeByName(String name) {
        JobMenu jobMenu = jobMenuDao.loadByName(name);
        // 判断角色中包含此name的选项
        if (Boolean.TRUE.equals(jobRoleDao.existByMenu(name))) {
            return new ReturnT<>(500, I18nUtil.getString("jobmenu_remove_exist_relation_role"));
        }
        // 删除用户中包含此name的选项
        int ret = jobMenuDao.delete(jobMenu.getId());
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @AuthAction()
    @GetMapping("/loadById")
    @ResponseBody
    public ReturnT<JobMenu> loadById(int id) {
        JobMenu jobUrl = jobMenuDao.load(id);
        return jobUrl != null ? new ReturnT<>(jobUrl) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @GetMapping("/loadByName")
    @ResponseBody
    public ReturnT<JobMenu> loadByName(String name) {
        JobMenu jobMenu = jobMenuDao.loadByName(name);
        return jobMenu != null ? new ReturnT<>(jobMenu) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @AuthAction()
    @GetMapping("/openApiList")
    @ResponseBody
    public Map<String, Object> openApiList(@RequestParam(required = false, defaultValue = "1") Integer page,
                                           @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<JobMenu> apiList = new ArrayList<>();
        apiList.add(new JobMenu("任务-添加", "job_common_add"));
        apiList.add(new JobMenu("任务-导出", "job_common_export"));
        apiList.add(new JobMenu("任务-同步", "job_common_syn"));
        apiList.add(new JobMenu("任务-启动", "job_common_start"));
        apiList.add(new JobMenu("任务-停止", "job_common_stop"));
        apiList.add(new JobMenu("任务-执行一次", "job_common_exec_once"));
        apiList.add(new JobMenu("任务-删除", "job_common_delete"));
        apiList.add(new JobMenu("任务-查找", "job_common_query"));
        apiList.add(new JobMenu("任务-更新", "job_common_update"));
        // page query
        List<JobMenu> list = apiList;
        int listCount = apiList.size();
        // package result
        Map<String, Object> maps = new HashMap<>(6);
        // 总记录数
        maps.put("recordsTotal", listCount);
        // 分页列表
        maps.put("data", list);
        maps.put("msg", "");
        // 总记录数
        maps.put("count", listCount);
        maps.put("code", ReturnT.SUCCESS_CODE);
        return maps;
    }

    /************* menu *******************/
    @GetMapping("/list")
    public String jobmenuList() {
        return "jobmenu/list";
    }

    @GetMapping("/add")
    public String jobmenuAdd() {
        return "jobmenu/add";
    }

    @GetMapping("/edit")
    public String jobmenuEdit(@RequestParam String menuName, RedirectAttributes attributes) {
        attributes.addAttribute("name", menuName);
        return "jobmenu/edit";
    }
}
