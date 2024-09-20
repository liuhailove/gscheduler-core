package com.tc.gschedulercore.service;


import com.tc.gschedulercore.core.dto.*;

/**
 * Created by honggang.liu on 17/3/1.
 */
public interface ExecutorBiz {

    /**
     * beat
     *
     * @return
     */
    ReturnT<String> beat();

    /**
     * idle beat
     *
     * @param idleBeatParam
     * @return
     */
    ReturnT<String> idleBeat(IdleBeatParam idleBeatParam);

    /**
     * run
     *
     * @param triggerParam
     * @return
     */
    ReturnT<String> run(TriggerParam triggerParam);

    /**
     * kill
     *
     * @param killParam
     * @return
     */
    ReturnT<String> kill(KillParam killParam);

    /**
     * log
     *
     * @param logParam
     * @return
     */
    ReturnT<LogResult> log(LogParam logParam);


    /**
     * checkResult 结果检查
     *
     * @param triggerParam 触发时的参数
     * @return code==200，表示执行成功，否则为失败
     */
    ReturnT<String> checkResult(TriggerParam triggerParam);

}
