package com.tc.gschedulercore.controller;


import com.tc.gschedulercore.controller.annotation.PermissionLimit;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.HandleCallbackParam;
import com.tc.gschedulercore.core.dto.RegistryParam;
import com.tc.gschedulercore.core.dto.ReportDelayParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.service.AdminBiz;
import com.tc.gschedulercore.util.GsonTool;
import com.tc.gschedulercore.util.JobRemotingUtil;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author honggang.liu
 * @date 17/5/10
 */
@Controller
@RequestMapping("/api")
public class
JobApiController {

    @Resource
    private AdminBiz adminBiz;

    /**
     * api
     *
     * @param uri
     * @param data
     * @return
     */
    @Timed(percentiles = {0.5, 0.80, 0.90, 0.99, 0.999})
    @RequestMapping("/{uri}")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> api(HttpServletRequest request, @PathVariable("uri") String uri, @RequestBody(required = false) String data) {
        // valid
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        if (uri == null || uri.trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
        }
        if (JobAdminConfig.getAdminConfig().getAccessToken() != null
                && JobAdminConfig.getAdminConfig().getAccessToken().trim().length() > 0
                && !JobAdminConfig.getAdminConfig().getAccessToken().equals(request.getHeader(JobRemotingUtil.JOB_ACCESS_TOKEN))) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "The access token is wrong.");
        }

        // services mapping
        if ("callback".equals(uri)) {
            List<HandleCallbackParam> callbackParamList = GsonTool.fromJson(data, List.class, HandleCallbackParam.class);
            return adminBiz.callback(callbackParamList);
        } else if ("registry".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registry(registryParam);
        } else if ("registryRemove".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registryRemove(registryParam);
        } else {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping(" + uri + ") not found.");
        }
    }

    @PostMapping("/reportDelay")
    @ResponseBody
    public ReturnT<String> save(@RequestBody ReportDelayParam reportDelayParam) {
        // valid
        if (reportDelayParam == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, reportDelayParam is empty.");
        }
        if (reportDelayParam.getCurrentDateTim() - reportDelayParam.getLogDateTim() < 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, reportDelayParam currentDateTim=" + reportDelayParam.getCurrentDateTim() + " less than logDateTim=" + reportDelayParam.getLogDateTim() + ".");
        }
        return adminBiz.reportDelay(reportDelayParam);
    }

    /**
     * 日志metric处理
     *
     * @param metrics metric列表
     * @return 处理结果
     */
    @PostMapping("/metrics")
    @ResponseBody
    public ReturnT<String> metrics(@RequestBody String metrics) {
        if (StringUtils.isEmpty(metrics)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, metrics param is empty.");
        }
        return adminBiz.handleMetrics(metrics);
    }

}
