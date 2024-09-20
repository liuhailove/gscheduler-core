package com.tc.gschedulercore.core.scheduler;


import com.tc.gschedulercore.client.ExecutorBizClient;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.etcd.EtcdRegisterCenter;
import com.tc.gschedulercore.core.etcd.JetcdOperationClient;
import com.tc.gschedulercore.core.thread.*;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.enums.ExecutorBlockStrategyEnum;
import com.tc.gschedulercore.service.ExecutorBiz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author honggang.liu 2018-10-28 00:18:17
 */

public class JobScheduler {
    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class.getSimpleName());


    public void init() throws Exception {
        // init i18n
        initI18n();

        // admin trigger pool start
        JobTriggerPoolHelper.toStart();//开启触发（调度）线程池

        // admin registry monitor run
        JobRegistryHelper.getInstance().start();//开启注册线程，主要是根据register表去维护group执行器中address的有效性（只维护自动录入的）

        // admin fail-monitor run
        JobFailMonitorHelper.getInstance().start();//开启监控线程，根据jobId，对失败（调度失败或执行失败）的任务进行重试和告警（前提是有配置重试和告警）

        // admin lose-monitor run ( depend on JobTriggerPoolHelper )
        JobCompleteHelper.getInstance().start();//开启回调线程池，和监控线程，对丢失任务结果的log进行结果回查

        // admin log report start
        JobLogReportHelper.getInstance().start();//开启线程，定时查看是否需要发送job报表并发送 以及定时清理过期的job log

        // start-schedule  ( depend on JobTriggerPoolHelper )
        JobScheduleHelper.getInstance().start();//开启调度线程和环形调度线程

        JobESHelper.getInstance().start();//开启ES线程，后台扫描es 告警数据

        // start 在线状态统计
        JobGroupOnlineHelper.getInstance().start();//统计执行器在线情况

        // etcd 初始化
        JetcdOperationClient.getInstance().init();

        // etcd注册与发现初始化
        EtcdRegisterCenter.getInstance().init();

        // 阈值告警start
        JobThresholdMonitorHelper.getInstance().start();//任务执行超时告警

        // 系统告警start
        JobSystemReportHelper.getInstance().start();//执行器下线系统告警&任务执行失败系统告警

        // access生效Start
        JobGroupAccessTokenHelper.getInstance().start();

        // 用户编辑Start
        JobUserPoolHelper.getInstance().start();

        // 任务补偿Start
        JobCompensateHelper.getInstance().start();

//        // 等待父任务完成Start
//        JobWaitParentHelper.getInstance().start();

        // 日志清理Start
        JobLogCleanHelper.getInstance().start();

        // 执行日志丢失监控Start
        JobLossMonitorHelper.getInstance().start();

        // 任务检查check
        JobCheckHelper.getInstance().start();

        // 脚本告警检查
        JobScriptRetryMonitorHelper.getInstance().start();

        // 延迟任务
        JobDelayHelper.getInstance().start();

        // 自身server注册发现处理
        ServerRegistryHelper.getInstance().start();

        logger.info(" init go-scheduler admin success.");
    }

    public void destroy() throws Exception {

        // 自身server注册发现处理
        ServerRegistryHelper.getInstance().toStop();

        // 延迟任务
        JobDelayHelper.getInstance().toStop();

        // 脚本告警检查
        JobScriptRetryMonitorHelper.getInstance().toStop();

        // 任务检查stop
        JobCheckHelper.getInstance().toStop();

        // 执行日志丢失监控Stop
        JobLossMonitorHelper.getInstance().toStop();

        // 日志清理Stop
        JobLogCleanHelper.getInstance().toStop();

//        // 等待父任务完成Stop
//        JobWaitParentHelper.getInstance().toStop();

        // 任务补偿Stop
        JobCompensateHelper.getInstance().toStop();

        // 用户编辑Stop
        JobUserPoolHelper.getInstance().stop();

        // access生效stop
        JobGroupAccessTokenHelper.getInstance().toStop();

        // 系统告警stop
        JobSystemReportHelper.getInstance().toStop();

        // 阈值告警stop
        JobThresholdMonitorHelper.getInstance().toStop();

        // etcd注册与发现销毁
        EtcdRegisterCenter.getInstance().destroy();

        // etcd 销毁
        JetcdOperationClient.getInstance().destroy();

        // stop 在线状态统计
        JobGroupOnlineHelper.getInstance().toStop();

        // stop-schedule
        JobScheduleHelper.getInstance().toStop();

        // admin log report stop
        JobLogReportHelper.getInstance().toStop();

        // admin lose-monitor stop
        JobCompleteHelper.getInstance().toStop();

        // admin fail-monitor stop
        JobFailMonitorHelper.getInstance().toStop();

        // admin registry stop
        JobRegistryHelper.getInstance().toStop();

        // admin trigger pool stop
        JobTriggerPoolHelper.toStop();

    }

    // ---------------------- I18n ----------------------

    private void initI18n() {
        for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }

    // ---------------------- executor-client ----------------------
    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();

    public static ExecutorBiz getExecutorBiz(String address) throws Exception {
        // valid
        if (address == null || address.trim().length() == 0) {
            return null;
        }

        // load-cache
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }

        // set-cache
        executorBiz = new ExecutorBizClient(address, JobAdminConfig.getAdminConfig().getAccessToken());
        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }

}
