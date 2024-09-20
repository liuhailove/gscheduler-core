package com.tc.gschedulercore.core.route.strategy;


import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.dto.TriggerParam;
import com.tc.gschedulercore.core.route.ExecutorRouter;
import com.tc.gschedulercore.core.scheduler.JobScheduler;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.service.ExecutorBiz;

import java.util.List;

/**
 * Created by honggang.liu on 17/3/10.
 */
public class ExecutorRouteFailover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {

        StringBuffer beatResultSB = new StringBuffer();
        for (String address : addressList) {
            // beat
            ReturnT<String> beatResult;
            try {
                ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(address);
                beatResult = executorBiz.beat();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                beatResult = new ReturnT<>(ReturnT.FAIL_CODE, "" + e);
            }
            beatResultSB.append((beatResultSB.length() > 0) ? "<br><br>" : "")
                    .append(I18nUtil.getString("jobconf_beat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.getCode())
                    .append("<br>msg：").append(beatResult.getMsg());

            // beat success
            if (beatResult.getCode() == ReturnT.SUCCESS_CODE) {

                beatResult.setMsg(beatResultSB.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return new ReturnT<>(ReturnT.FAIL_CODE, beatResultSB.toString());

    }
}
