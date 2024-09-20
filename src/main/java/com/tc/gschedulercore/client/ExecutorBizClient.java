package com.tc.gschedulercore.client;

import com.tc.gschedulercore.core.dto.*;
import com.tc.gschedulercore.service.ExecutorBiz;
import com.tc.gschedulercore.util.JobRemotingUtil;

/**
 * admin api test
 *
 * @author honggang.liu 2017-07-28 22:14:52
 */
public class ExecutorBizClient implements ExecutorBiz {

    public ExecutorBizClient() {
    }

    public ExecutorBizClient(String addressUrl, String accessToken) {
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
    public ReturnT<String> beat() {
        return JobRemotingUtil.postBody(addressUrl + "beat", accessToken, null, timeout, "", String.class);
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {
        return JobRemotingUtil.postBody(addressUrl + "idleBeat", accessToken, null, timeout, idleBeatParam, String.class);
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return JobRemotingUtil.postBody(addressUrl + "run", accessToken, null, timeout, triggerParam, String.class);
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return JobRemotingUtil.postBody(addressUrl + "kill", accessToken, null, timeout, killParam, String.class);
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return JobRemotingUtil.postBody(addressUrl + "log", accessToken, null, timeout, logParam, LogResult.class);
    }

    @Override
    public ReturnT<String> checkResult(TriggerParam triggerParam) {
        return JobRemotingUtil.postBody(addressUrl + "checkResult", accessToken, null, timeout, triggerParam, String.class);
    }
}
