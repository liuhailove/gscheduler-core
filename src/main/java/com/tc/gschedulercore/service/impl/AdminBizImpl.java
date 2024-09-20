package com.tc.gschedulercore.service.impl;

import com.tc.gschedulercore.core.dto.HandleCallbackParam;
import com.tc.gschedulercore.core.dto.RegistryParam;
import com.tc.gschedulercore.core.dto.ReportDelayParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.thread.JobCompleteHelper;
import com.tc.gschedulercore.core.thread.JobRegistryHelper;
import com.tc.gschedulercore.service.AdminBiz;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author honggang.liu 2017-07-27 21:54:20
 */
@Service
public class AdminBizImpl implements AdminBiz {


    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return JobCompleteHelper.getInstance().callback(callbackParamList, false);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }

    /**
     * 延迟上报
     *
     * @param reportDelayParam 延迟上报参数
     * @return 返回处理消息
     */
    @Override
    public ReturnT<String> reportDelay(ReportDelayParam reportDelayParam) {
        return JobRegistryHelper.getInstance().reportDelay(reportDelayParam);
    }

    /**
     * 日志metric处理
     *
     * @param metrics metric数据
     * @return 处理结果
     */
    @Override
    public ReturnT<String> handleMetrics(String metrics) {
        return JobRegistryHelper.getInstance().handleMetrics(metrics);
    }
}
