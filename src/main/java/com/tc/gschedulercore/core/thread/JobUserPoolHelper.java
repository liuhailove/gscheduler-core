package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * job trigger thread pool helper
 *
 * @author honggang.liu 2018-07-03 21:08:07
 */
public class JobUserPoolHelper {
    private static Logger logger = LoggerFactory.getLogger(JobUserPoolHelper.class.getSimpleName());

    private static JobUserPoolHelper instance = new JobUserPoolHelper();

    public static JobUserPoolHelper getInstance() {
        return instance;
    }

    /**
     * fast/slow thread pool
     */
    private static ExecutorService executorService;

    /**
     * 分页大小
     */
    private static int pageSize = 10;

    /**
     * 开启线程
     */
    public void start() {
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 关闭线程
     */
    public void stop() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * 平台名称修改
     */
    public void platformChange(String oldPlatform, String newPlatform) {
        logger.info("platformChange，oldPlatform={},newPlatform={}", oldPlatform, newPlatform);
        // 修改用户中的平台数据
//        int count = XxlJobAdminConfig.getAdminConfig().getXxlJobUserDao().loadByPlatformCount(oldPlatform);
//        int batch = count % pageSize == 0 ? count / pageSize : count / pageSize + 1;
//        for (int i = 0; i < batch; i++) {
//            List<XxlJobUser> jobUsers = XxlJobAdminConfig.getAdminConfig().getXxlJobUserDao().loadByPlatform(i * pageSize, pageSize, oldPlatform);
//            if (!CollectionUtils.isEmpty(jobUsers)) {
//                for (XxlJobUser user : jobUsers) {
//                    List<String> permissionPlatformList = user.getPermissionPlatformList();
//                    permissionPlatformList.remove(oldPlatform);
//                    permissionPlatformList.add(newPlatform);
//                    user.setPermissionPlatforms(org.apache.commons.lang3.StringUtils.join(permissionPlatformList));
//                    user.setUpdateTime(new Date());
//                    XxlJobAdminConfig.getAdminConfig().getXxlJobUserDao().update(user);
//                }
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                }
//            }
//        }
    }

    /**
     * 平台移除
     */
    public void platformRemove(String oldPlatformName) {
        logger.info("platformRemove，oldPlatform={}", oldPlatformName);
//        int count = XxlJobAdminConfig.getAdminConfig().getXxlJobUserDao().loadByPlatformCount(oldPlatformName);
//        int batch = count % pageSize == 0 ? count / pageSize : count / pageSize + 1;
//        for (int i = 0; i < batch; i++) {
//            List<XxlJobUser> jobUsers = XxlJobAdminConfig.getAdminConfig().getXxlJobUserDao().loadByPlatform(i * pageSize, pageSize, oldPlatformName);
//            if (!CollectionUtils.isEmpty(jobUsers)) {
//                for (XxlJobUser user : jobUsers) {
//                    List<String> permissionPlatformList = user.getPermissionPlatformList();
//                    permissionPlatformList.remove(oldPlatformName);
//                    user.setPermissionPlatforms(org.apache.commons.lang3.StringUtils.join(permissionPlatformList,","));
//                    user.setUpdateTime(new Date());
//                    XxlJobAdminConfig.getAdminConfig().getXxlJobUserDao().update(user);
//                }
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                }
//            }
//        }
    }

    /**
     * 角色名称修改
     */
    public void roleNameChange(String oldRoleName, String newRoleName) {
        logger.info("roleNameChange，oldRoleName={},newRoleName={}", oldRoleName, newRoleName);
        // 修改用户中的平台数据
//        int count = XxlJobAdminConfig.getAdminConfig().getXxlJobUserDao().loadByRoleNameCount(oldRoleName);
//        int batch = count % pageSize == 0 ? count / pageSize : count / pageSize + 1;
//        for (int i = 0; i < batch; i++) {
//            List<XxlJobUser> jobUsers = XxlJobAdminConfig.getAdminConfig().getXxlJobUserDao().loadByRoleName(i * pageSize, pageSize, oldRoleName);
//            if (!CollectionUtils.isEmpty(jobUsers)) {
//                for (XxlJobUser user : jobUsers) {
//                    user.setRoleName(newRoleName);
//                    user.setUpdateTime(new Date());
//                    XxlJobAdminConfig.getAdminConfig().getXxlJobUserDao().update(user);
//                }
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                }
//            }
//        }
    }

    /**
     * 平台移除
     */
    public void roleNameRemove(String oldRoleName) {
        logger.info("platformRemove，oldRoleName={}", oldRoleName);
        int count = JobAdminConfig.getAdminConfig().getXxlJobUserDao().loadByRoleNameCount(oldRoleName);
        int batch = count % pageSize == 0 ? count / pageSize : count / pageSize + 1;
        for (int i = 0; i < batch; i++) {
            List<JobUser> jobUsers = JobAdminConfig.getAdminConfig().getXxlJobUserDao().loadByRoleName(i * pageSize, pageSize, oldRoleName);
            if (!CollectionUtils.isEmpty(jobUsers)) {
                for (JobUser user : jobUsers) {
                    user.setRoleName("");
                    user.setUpdateTime(new Date());
                    JobAdminConfig.getAdminConfig().getXxlJobUserDao().update(user);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * helper
     */
    private static JobUserPoolHelper helper = new JobUserPoolHelper();

    /**
     * 平台名称修改
     */
    public static void triggerPlatformChange(String oldPlatform, String newPlatform) {
        executorService.execute(() -> helper.platformChange(oldPlatform, newPlatform));
    }

    /**
     * 平台Remove
     */
    public static void triggerPlatformRemove(String oldPlatformName) {
        executorService.execute(() -> helper.platformRemove(oldPlatformName));
    }

    /**
     * 角色名称修改
     */
    public static void triggerRoleNameChange(String oldRoleName, String newRoleName) {
        executorService.execute(() -> helper.roleNameChange(oldRoleName, newRoleName));
    }

    /**
     * 角色Remove
     */
    public static void triggerRoleNameRemove(String oldRoleName) {
        executorService.execute(() -> helper.roleNameRemove(oldRoleName));
    }


}
