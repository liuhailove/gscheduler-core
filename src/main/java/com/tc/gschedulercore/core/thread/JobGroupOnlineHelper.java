package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.enums.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * job monitor instance
 *
 * @author honggang.liu 2015-9-1 18:05:56
 */
public class JobGroupOnlineHelper {
    private static Logger logger = LoggerFactory.getLogger(JobGroupOnlineHelper.class.getSimpleName());

    private static JobGroupOnlineHelper instance = new JobGroupOnlineHelper();

    public static JobGroupOnlineHelper getInstance() {
        return instance;
    }

    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();


    // ---------------------- monitor ----------------------
    private volatile boolean toStop = false;

    public void start() {
        singleThreadExecutor.submit(() -> {
            // monitor
            while (!toStop) {
                try {
                    List<JobGroup> jobGroupList = JobAdminConfig.getAdminConfig().getJobGroupDao().findAll();
                    for (JobGroup jobGroup : jobGroupList) {
                        if (jobGroup.getAddressType() == 0) {
                            // 自动注册任务,只需要判断注册地址是否为null
                            jobGroup.setOnlineStatus(!CollectionUtils.isEmpty(jobGroup.getRegistryList()));
                        } else {
                            // 非自动注册，注册信息中有，说明在线，否在不在线
                            int count = JobAdminConfig.getAdminConfig().getJobRegistryDao().exist("EXECUTOR", jobGroup.getRegistryList());
                            jobGroup.setOnlineStatus(count > 0);
                        }
                        jobGroup.setUpdateTime(new Date());
                        JobAdminConfig.getAdminConfig().getJobGroupDao().updateOnlineStatus(jobGroup);
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(">>JobGroupOnlineHelper thread error:", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.DEAD_TIMEOUT);
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>JobGroupOnlineHelper thread stop");
        });
    }

    public void toStop() {
        toStop = true;
        singleThreadExecutor.shutdown();
    }
}
