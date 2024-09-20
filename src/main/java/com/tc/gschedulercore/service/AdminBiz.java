package com.tc.gschedulercore.service;


import com.tc.gschedulercore.core.dto.HandleCallbackParam;
import com.tc.gschedulercore.core.dto.RegistryParam;
import com.tc.gschedulercore.core.dto.ReportDelayParam;
import com.tc.gschedulercore.core.dto.ReturnT;

import java.util.List;

/**
 * @author honggang.liu 2017-07-27 21:52:49
 */
public interface AdminBiz {


    // ---------------------- callback ----------------------

    /**
     * callback
     *
     * @param callbackParamList
     * @return
     */
    ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);


    // ---------------------- registry ----------------------

    /**
     * registry
     *
     * @param registryParam
     * @return
     */
    ReturnT<String> registry(RegistryParam registryParam);

    /**
     * registry remove
     *
     * @param registryParam
     * @return
     */
    ReturnT<String> registryRemove(RegistryParam registryParam);

    /**
     * 延迟上报
     *
     * @param reportDelayParam 延迟上报参数
     * @return 返回处理消息
     */
    ReturnT<String> reportDelay(ReportDelayParam reportDelayParam);


    /**
     * 日志metric处理
     *
     * @param metrics metric数据
     * @return 处理结果
     */
    ReturnT<String> handleMetrics(String metrics);


    // ---------------------- biz (custome) ----------------------
    // group、job ... manage

}
