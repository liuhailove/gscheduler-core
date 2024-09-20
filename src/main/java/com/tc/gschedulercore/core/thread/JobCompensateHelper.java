package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.dto.HandleCallbackParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobCompensate;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLog;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.core.util.JacksonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务补偿，
 * 主要处理状态长时间处于执行中的（201）的父子任务，
 * 通过把此中任务从库中拉出来重新trigger，以避免
 * 因为down机或者重启导致的子任务没有下发而导致的任务
 * 丢失。
 * 重新trigger时，通过执行参数+instanceId来标记任务
 * 是否已经执行过了，避免同一个任务下发多次
 *
 * @author honggang.liu
 */
public class JobCompensateHelper {

    /**
     * logger
     */
    private static Logger logger = LoggerFactory.getLogger(JobCompensateHelper.class.getSimpleName());

    private volatile boolean compensateThreadToStop = false;

    /**
     * 6h过期
     */
    private static final int EXPIRE_HOUR = 6;

    /**
     * 已经处理的时间
     */
    private static final int PROCESS_ELAPSE_MINUTE = 60;

    private ScheduledExecutorService compensatePoolExecutor = Executors.newSingleThreadScheduledExecutor();


    private static JobCompensateHelper instance = new JobCompensateHelper();

    public static JobCompensateHelper getInstance() {
        return instance;
    }


    public void start() {
        compensatePoolExecutor.scheduleAtFixedRate(this::handleCompensate, 30, 60, TimeUnit.SECONDS);
    }

    /**
     * 任务补偿
     */
    private void handleCompensate() {
        try {
            // 1.拉取状态为【处理中】的超过1h,小于6h的任务，
            // 处理超过1h的原因：
            // （1）由于限流原因，导致子任务下发过慢，是状态为【处理中】超过了1h，此种任务不应该补偿
            // （2）由于停机或者部署，任务没有在单位时间内下发完，导致状态长时间处于【处理中】，此种不需要补偿
            // 对于不应该补偿的任务，通过以下方式避免
            // （1）判断父任务的server下发IP是不是已经不存在了，如果已经不存在了，说明Server已经发生了重启，是由于重启导致状态卡在【处理中】，因此需要补偿，否则不应该补偿，等待Server继续处理，直到【处理中】-》【success】或者【failed】
            // （2）判断是否补偿过，如果已经补偿过，则不需要在补偿
            // （3）判断子任务对应的执行参数是否已经补偿过，补偿过，则不需要在补偿
            // 说明，如果一个任务下发超过6h，则QPS=1/3600/6，这个值过分小了，因此可以认为这是一个错误，这个错误有可能是由于系统BUG导致的，这个没必要在补偿了，直接舍弃掉
            List<JobLog> jobLogList;
            try (HintManager manager = HintManager.getInstance()) {
                manager.setWriteRouteOnly();
                jobLogList = JobAdminConfig.getAdminConfig().getJobLogDao().query10Process(DateUtils.addMinutes(new Date(), PROCESS_ELAPSE_MINUTE * (-6)), DateUtils.addMinutes(new Date(), PROCESS_ELAPSE_MINUTE * (-1)));
            }
            if (CollectionUtils.isEmpty(jobLogList)) {
                return;
            }
            for (JobLog jobLog : jobLogList) {
                if (StringUtils.isEmpty(jobLog.getChildrenExecutorParams())) {
                    return;
                }
                // 判断下发IP已经不在线了
                // 例子：
                // 任务触发类型：Cron触发<br>调度机器：10.91.142.112<br>执行器-注册方式：
                // 自动注册<br>执行器-地址列表：[http://10.91.138.154:41081, http://10.91.141.26:35581, http://10.91.155.71:37847, ]<br>路由策略：分片广播(1/5)<br>阻塞处理策略：丢弃后续调度<br>任务超时时间：0<br>失败重试次数：0<br>执行参数：{"biz_type":1}
                String triggerMsg = jobLog.getTriggerMsg();
                String addressMsg = I18nUtil.getString("jobconf_trigger_admin_adress") + "：";
                if (StringUtils.isEmpty(triggerMsg) || !triggerMsg.contains(addressMsg)) {
                    return;
                }
                String subStr = triggerMsg.substring(triggerMsg.indexOf(addressMsg) + addressMsg.length());
                String serverAddress = subStr.substring(0, subStr.indexOf("<br>"));
                try (HintManager manager = HintManager.getInstance()) {
                    manager.setWriteRouteOnly();
                    if (JobAdminConfig.getAdminConfig().getServerRegistryDao().exist(serverAddress) > 0) {
                        return;
                    }
                }
                // 如果不是sharding任务，直接开启子任务
                String[] childExecutorParams;
                try {
                    childExecutorParams = JacksonUtil.readValue(jobLog.getChildrenExecutorParams(), String[].class);
                } catch (Exception e) {
                    logger.error("log id [{}] params parser error", jobLog.getId(), e);
                    return;
                }
                Connection conn = null;
                Boolean connAutoCommit = null;
                PreparedStatement preparedStatement = null;
                try {
                    // 加锁
                    conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
                    connAutoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);
                    preparedStatement = conn.prepareStatement("select * from gs_job_lock where lock_name = 'system_compensate_lock' for update");
                    preparedStatement.execute();
                    // 是否补偿过判断
                    if (Boolean.TRUE.equals(JobAdminConfig.getAdminConfig().getJobCompensateDao().exist(jobLog.getId(), jobLog.getJobGroup(), jobLog.getJobId()))) {
                        return;
                    }
                    // 状态回更，设置为处初始,同时把childrenExecutorParams写入，主要是为了避免处理子任务突然中断，会导致父任务状态没有及时更新，同时也可能因此而遗漏子任务下发
                    int ret = JobAdminConfig.getAdminConfig().getJobLogDao().update2Processing(jobLog.getId(), jobLog.getJobGroup(), ReturnT.PROCESSING_CODE, 0, jobLog.getChildrenExecutorParams(), jobLog.getHandleTime());
                    if (ret <= 0) {
                        return;
                    }
                    // 插入任务补偿队列
                    JobCompensate jobCompensate = new JobCompensate();
                    jobCompensate.setJobLogId(jobLog.getId());
                    jobCompensate.setJobId(jobLog.getJobId());
                    jobCompensate.setJobGroup(jobLog.getJobGroup());
                    jobCompensate.setExpireTime(DateUtils.addHours(new Date(), EXPIRE_HOUR));
                    jobCompensate.setAddTime(new Date());
                    JobAdminConfig.getAdminConfig().getJobCompensateDao().save(jobCompensate);
                    HandleCallbackParam handleCallbackParam = new HandleCallbackParam();
                    handleCallbackParam.setHandleCode(ReturnT.SUCCESS_CODE);
                    handleCallbackParam.setHandleMsg(childExecutorParams);
                    handleCallbackParam.setLogDateTim(jobLog.getHandleTime().getTime());
                    handleCallbackParam.setLogId(jobLog.getId());
                    logger.info("log id [{}] will compensate", jobLog.getId());
                    // 补偿任务需要发送seatalk告警
                    JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(jobLog.getJobId());
                    JobAdminConfig.getAdminConfig().getJobAlarmer().compensateAlarm(jobInfo, jobLog);
                    // 回放
                    JobCompleteHelper.getInstance().callback(Collections.singletonList(handleCallbackParam), true);
                } catch (Exception e) {
                    if (!compensateThreadToStop) {
                        logger.error("JobCompensateHelper#handleCompensate error:", e);
                    }
                } finally {
                    // commit
                    if (conn != null) {
                        try {
                            conn.commit();
                        } catch (SQLException e) {
                            logger.error("handleCompensate commit:", e);
                        }
                        try {
                            conn.setAutoCommit(Boolean.TRUE.equals(connAutoCommit));
                        } catch (SQLException e) {
                            logger.error("handleCompensate setAutoCommit:", e);
                        }
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            logger.error("handleCompensate close:", e);
                        }
                    }
                    // close PreparedStatement
                    if (null != preparedStatement) {
                        try {
                            preparedStatement.close();
                        } catch (SQLException e) {
                            logger.error("handleCompensate close:", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("handleCompensate close:", e);
        }
    }

    /**
     * 停止线程
     */
    public void toStop() {
        if (compensatePoolExecutor == null) {
            return;
        }
        compensatePoolExecutor.shutdown();
        logger.info("JobCompensateHelper callbackThreadPool thread  stoppped");
    }

}
