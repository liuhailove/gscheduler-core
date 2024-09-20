package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.model.OpenApi;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.dao.JobGroupDao;
import com.tc.gschedulercore.dao.OpenApiDao;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.*;

/**
 * 开放Api管理Controller
 *
 * @author honggang.liu
 */
@Controller
@RequestMapping("/openapi")
public class OpenApiController {

    /**
     * 平台管理
     */
    @Resource
    public OpenApiDao xxlOpenApiDao;

    /**
     * 执行器管理
     */
    @Resource
    public JobGroupDao jobGroupDao;

    @GetMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "1") Integer page,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit) {

        int start = 0;
        int length = 10;
        if (page != null && limit != null) {
            length = limit;
            start = (page - 1) * limit;
        }
        // page query
        List<OpenApi> list = xxlOpenApiDao.pageList(start, length);
        int listCount = xxlOpenApiDao.pageListCount(start, length);
        // package result
        Map<String, Object> maps = new HashMap<>(3);
        // 分页列表
        maps.put("data", list);
        // 总记录数
        maps.put("count", listCount);
        maps.put("code", ReturnT.SUCCESS_CODE);
        return maps;
    }

    @PostMapping("/save")
    @ResponseBody
    public ReturnT<OpenApi> save(@RequestBody OpenApi openApi) {
        // valid
        if (openApi.getApiName() == null || openApi.getApiName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "Api Name"));
        }
        if (openApi.getApiName().length() < 4 || openApi.getApiName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("openapi_field_name_length"));
        }
        if (openApi.getApiName().contains(">") || openApi.getApiName().contains("<")) {
            return new ReturnT<>(500, "Api Name" + I18nUtil.getString("system_unvalid"));
        }
        if (openApi.getUrls() == null || openApi.getUrls().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "Url"));
        }
        if (openApi.getUrls().length() < 4 || openApi.getApiName().length() > 2048) {
            return new ReturnT<>(500, I18nUtil.getString("openapi_field_url_length"));
        }
        if (openApi.getUrls().contains(">") || openApi.getUrls().contains("<")) {
            return new ReturnT<>(500, "Url" + I18nUtil.getString("system_unvalid"));
        }
        JobGroup group = jobGroupDao.load(openApi.getJobGroup());
        openApi.setJobGroupName(group.getAppname());
        openApi.setApiDesc(org.springframework.util.StringUtils.isEmpty(openApi.getApiDesc()) ? "" : openApi.getApiDesc().trim());
        openApi.setAccessToken(UUID.randomUUID().toString());
        openApi.setAddTime(new Date());
        openApi.setUpdateTime(new Date());
        int ret = xxlOpenApiDao.save(openApi);
        return (ret > 0) ? new ReturnT<>(openApi) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @PostMapping("/update")
    @ResponseBody
    public ReturnT<String> update(@RequestBody OpenApi openApi) {
        // valid
        if (openApi.getApiName() == null || openApi.getApiName().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "Api Name"));
        }
        if (openApi.getApiName().length() < 4 || openApi.getApiName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("openapi_field_name_length"));
        }
        if (openApi.getApiName().contains(">") || openApi.getApiName().contains("<")) {
            return new ReturnT<>(500, "Api Name" + I18nUtil.getString("system_unvalid"));
        }
        if (openApi.getUrls() == null || openApi.getUrls().trim().length() == 0) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "Url"));
        }
        if (openApi.getUrls().length() < 4 || openApi.getUrls().length() > 2048) {
            return new ReturnT<>(500, I18nUtil.getString("openapi_field_url_length"));
        }
        if (openApi.getUrls().contains(">") || openApi.getUrls().contains("<")) {
            return new ReturnT<>(500, "Url" + I18nUtil.getString("system_unvalid"));
        }
        JobGroup group = jobGroupDao.load(openApi.getJobGroup());
        openApi.setJobGroupName(group.getAppname());
        openApi.setApiDesc(org.springframework.util.StringUtils.isEmpty(openApi.getApiDesc()) ? "" : openApi.getApiDesc().trim());
        openApi.setUpdateTime(new Date());
        int ret = xxlOpenApiDao.update(openApi);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(Integer id) {
        int ret = xxlOpenApiDao.delete(id);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @GetMapping("/loadById")
    @ResponseBody
    public ReturnT<OpenApi> loadById(int id) {
        OpenApi openApi = xxlOpenApiDao.load(id);
        return openApi != null ? new ReturnT<>(openApi) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @GetMapping("/loadByIdes")
    @ResponseBody
    public ReturnT<List<OpenApi>> loadByIdes(@RequestParam List<Integer> ides) {
        return new ReturnT<>(xxlOpenApiDao.loadByIdes(ides));
    }

    @GetMapping("/list")
    public String openApiList() {
        return "openapi/list";
    }

    @GetMapping("/add")
    public String openApiAdd() {
        return "openapi/add";
    }

    @GetMapping("/edit")
    public String openApiEdit(@RequestParam Integer id, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        return "openapi/edit";
    }
}
