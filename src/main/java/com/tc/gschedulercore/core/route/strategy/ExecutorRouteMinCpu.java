package com.tc.gschedulercore.core.route.strategy;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.TriggerParam;
import com.tc.gschedulercore.core.model.JobRegistry;
import com.tc.gschedulercore.core.route.ExecutorRouter;

import java.util.List;
import java.util.Random;

/**
 * 内存最小优先
 *
 * @author honggang.liu
 */
public class ExecutorRouteMinCpu extends ExecutorRouter {

    private static Random localRandom = new Random();

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        JobRegistry registry = JobAdminConfig.getAdminConfig().getJobRegistryDao().findMinCpuAddress(triggerParam.getAppname());
        if (registry != null && registry.getRegistryValue() != null) {
            return new ReturnT<>(registry.getRegistryValue());
        }
        return new ReturnT<>(addressList.get(localRandom.nextInt(addressList.size())));
    }
}
