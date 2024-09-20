package com.tc.gschedulercore.core.route.strategy;

import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.TriggerParam;
import com.tc.gschedulercore.core.route.ExecutorRouter;

import java.util.List;

/**
 * Created by honggang.liu on 17/3/10.
 */
public class ExecutorRouteLast extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<>(addressList.get(addressList.size()-1));
    }

}
