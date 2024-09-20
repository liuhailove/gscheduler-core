package com.tc.gschedulercore.client;

import com.tc.gschedulercore.core.dto.HandleCallbackParam;
import com.tc.gschedulercore.core.dto.RegistryParam;
import com.tc.gschedulercore.core.dto.ReportDelayParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.service.AdminBiz;
import com.tc.gschedulercore.util.JobRemotingUtil;

import java.util.List;

/**
 * admin api test
 *
 * @author xuxueli 2017-07-28 22:14:52
 */
public class AdminBizClient implements AdminBiz {

    public AdminBizClient() {
    }

    public AdminBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;

        // valid
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
    }

    private String addressUrl;
    private String accessToken;
    private int timeout = 30;


    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return JobRemotingUtil.postBody(addressUrl + "api/callback", accessToken, null, timeout, callbackParamList, String.class);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return JobRemotingUtil.postBody(addressUrl + "api/registry", accessToken, null, timeout, registryParam, String.class);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, null, timeout, registryParam, String.class);
    }

    /**
     * 延迟上报
     *
     * @param reportDelayParam 延迟上报参数
     * @return 返回处理消息
     */
    @Override
    public ReturnT<String> reportDelay(ReportDelayParam reportDelayParam) {
        return null;
    }

    /**
     * 日志metric处理
     *
     * @param metrics metric数据
     * @return 处理结果
     */
    @Override
    public ReturnT<String> handleMetrics(String metrics) {
        return null;
    }
}
