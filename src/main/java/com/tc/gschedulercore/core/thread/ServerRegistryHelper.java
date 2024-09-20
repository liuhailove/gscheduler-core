package com.tc.gschedulercore.core.thread;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.enums.RegistryConfig;
import com.tc.gschedulercore.util.IpUtil;
import net.sf.ehcache.util.NamedThreadFactory;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerRegistryHelper {

    /**
     * logger
     */
    private static Logger LOGGER = LoggerFactory.getLogger(JobCompensateHelper.class.getSimpleName());

    @SuppressWarnings("PMD.JobWaitParentHelper")
    private ScheduledExecutorService serverRegistryPoolExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("server-registry-helper", true));

    @SuppressWarnings("PMD.JobWaitParentHelper")
    private ScheduledExecutorService serverRemoveDeadPoolExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("server-remove-dead-helper", true));
    /**
     * 单例
     */
    private static final ServerRegistryHelper instance = new ServerRegistryHelper();

    public static ServerRegistryHelper getInstance() {
        return instance;
    }

    public void start() {
        // 每10s执行一次
        serverRegistryPoolExecutor.scheduleWithFixedDelay(this::handleRegistry, 0, RegistryConfig.BEAT_TIMEOUT, TimeUnit.SECONDS);
        // 每30s执行一次
        serverRemoveDeadPoolExecutor.scheduleWithFixedDelay(this::handleRemoveDead, 0, RegistryConfig.DEAD_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 处理服务节点的注册
     */
    public void handleRegistry() {
        try {
            // 服务端ip
            String serverIp = IpUtil.getIp();
            int ret = JobAdminConfig.getAdminConfig().getServerRegistryDao().registryUpdate(serverIp, new Date());
            if (ret < 1) {
                JobAdminConfig.getAdminConfig().getServerRegistryDao().registrySave(serverIp, new Date());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 处理服务节点的移除
     */
    public void handleRemoveDead() {
        try {
            List<Integer> ids;
            try (HintManager manager = HintManager.getInstance()) {
                manager.setWriteRouteOnly();
                ids = JobAdminConfig.getAdminConfig().getServerRegistryDao().findDead(DateUtils.addSeconds(new Date(), RegistryConfig.DEAD_TIMEOUT * (-1)));
            }
            if (!CollectionUtils.isEmpty(ids)) {
                JobAdminConfig.getAdminConfig().getServerRegistryDao().removeDead(ids);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    /**
     * 停止线程
     */
    public void toStop() {
        if (serverRegistryPoolExecutor != null) {
            serverRegistryPoolExecutor.shutdown();
        }
        if (serverRemoveDeadPoolExecutor != null) {
            serverRemoveDeadPoolExecutor.shutdown();
        }
        LOGGER.info("ServerRegistryHelper callbackThreadPool thread  stopped");
    }
}
