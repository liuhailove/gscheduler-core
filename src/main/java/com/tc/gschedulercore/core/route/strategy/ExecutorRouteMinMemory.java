package com.tc.gschedulercore.core.route.strategy;


import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.TriggerParam;
import com.tc.gschedulercore.core.model.JobRegistry;
import com.tc.gschedulercore.core.route.ExecutorRouter;

import java.util.List;
import java.util.Random;

/**
 * Created by honggang.liu on 17/3/10.
 */
public class ExecutorRouteMinMemory extends ExecutorRouter {

    private static Random localRandom = new Random();

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        JobRegistry registry = JobAdminConfig.getAdminConfig().getJobRegistryDao().findMinMemoryAddress(triggerParam.getAppname());
        if (registry != null && registry.getRegistryValue() != null) {
            return new ReturnT<>(registry.getRegistryValue());
        }
        return new ReturnT<>(addressList.get(localRandom.nextInt(addressList.size())));
    }

}
