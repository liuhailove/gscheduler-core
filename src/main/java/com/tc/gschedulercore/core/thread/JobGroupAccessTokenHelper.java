package com.tc.gschedulercore.core.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * job access token 辅助类
 *
 * @author honggang.liu
 */
public class JobGroupAccessTokenHelper {
    private static Logger logger = LoggerFactory.getLogger(JobGroupAccessTokenHelper.class.getSimpleName());

    private static JobGroupAccessTokenHelper instance = new JobGroupAccessTokenHelper();

    public static JobGroupAccessTokenHelper getInstance() {
        return instance;
    }

    private ScheduledExecutorService accessTokenThreadExecutor = Executors.newSingleThreadScheduledExecutor();


    public void start() {
//        accessTokenThreadExecutor.scheduleAtFixedRate(() -> {
//            // 1.查找当前时间>生效时间，且有效token!=当前token的执行器
//            List<JobGroup> jobGroups = JobAdminConfig.getAdminConfig().getJobGroupDao().findNeedUpdateAccessToken(new Date());
//            if (CollectionUtils.isEmpty(jobGroups)) {
//                return;
//            }
//            // 2.更新有效token=当前token
//            for (JobGroup jobGroup : jobGroups) {
//                jobGroup.setTokenEffective(jobGroup.getCurrentAccessToken());
//                jobGroup.setUpdateTime(new Date());
//                JobAdminConfig.getAdminConfig().getJobGroupDao().update(jobGroup);
//            }
//        }, 60, 60, TimeUnit.SECONDS);
    }

    public void toStop() {
        accessTokenThreadExecutor.shutdown();
    }
}
