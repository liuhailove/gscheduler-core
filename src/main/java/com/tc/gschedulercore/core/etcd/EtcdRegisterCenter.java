package com.tc.gschedulercore.core.etcd;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.util.IpUtil;
import io.etcd.jetcd.*;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 注册中心
 *
 * @author honggang.liu
 */
@Service
public class EtcdRegisterCenter {
    private static Logger logger = LoggerFactory.getLogger(EtcdRegisterCenter.class.getSimpleName());
    /**
     * 注册中心目录
     */
    private static final String ETCD_REGISTER_DIR = "go_scheduler_root";

    /**
     * 命名空间
     */
    private static final String NAMESPACE = "go_scheduler_namespace";

    /**
     * 组
     */
    private static final String GROUP = "go_scheduler_group";

    /**
     * 服务注册路径
     */
    private String SERVER_REGISTER_PATH;

    /**
     * 组注册路径
     */
    private String GROUP_REGISTER_PATH;

    /**
     * /
     */
    private static final String SPLASH = "/";

    /**
     * http协议
     */
    private static final String HTTP_PROTOCOL = "http://";

    /**
     * 实例
     */
    private static EtcdRegisterCenter instance = new EtcdRegisterCenter();

    /**
     * 单线程的调度任务，用于health检查
     */
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


    /**
     * 单线程的调度任务，用于清理
     */
    private ScheduledExecutorService clearExecutorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * 单例
     *
     * @return 返回etcd操作对象
     */
    public static EtcdRegisterCenter getInstance() {
        return instance;
    }


    public void init() {
        if (!Boolean.TRUE.equals(JobAdminConfig.getAdminConfig().getUseEtcd())) {
            return;
        }
        logger.info(" EtcdRegisterCenter init");
        GROUP_REGISTER_PATH = ETCD_REGISTER_DIR + SPLASH + NAMESPACE + SPLASH + GROUP + SPLASH;
        SERVER_REGISTER_PATH = GROUP_REGISTER_PATH + JobAdminConfig.getAdminConfig().getServerName() + SPLASH + UUID.randomUUID();
        // 服务注册
        registerInstance(SERVER_REGISTER_PATH, HTTP_PROTOCOL + IpUtil.getLocalHostLANAddress() + ":" + JobAdminConfig.getAdminConfig().getPort() + JobAdminConfig.getAdminConfig().getContextPath(), null);
        // 服务监听
        watchInstance(GROUP_REGISTER_PATH, onNext -> {
            onNext.getEvents().forEach(action -> {
                switch (action.getEventType()) {
                    case PUT:
                        logger.info("EtcdRegisterCenter server register:key={},value={}", action.getKeyValue().getKey().toString(StandardCharsets.UTF_8), action.getKeyValue().getValue().toString(StandardCharsets.UTF_8));
                        break;
                    case DELETE:
                        logger.info("EtcdRegisterCenter server remove:key={},value={}", action.getKeyValue().getKey().toString(StandardCharsets.UTF_8), action.getKeyValue().getValue().toString(StandardCharsets.UTF_8));
                        break;
                    default:
                        break;

                }
            });
        });

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            KeyValue keyValue = getInstance(SERVER_REGISTER_PATH);
            if (keyValue == null) {
                logger.warn("EtcdRegisterCenter service has down,execute register again,SERVER_REGISTER_PATH={}", SERVER_REGISTER_PATH);
                // 服务注册
                registerInstance(SERVER_REGISTER_PATH, HTTP_PROTOCOL + IpUtil.getLocalHostLANAddress() + ":" + JobAdminConfig.getAdminConfig().getPort() + JobAdminConfig.getAdminConfig().getContextPath(), null);
            }
        }, 60, 120, TimeUnit.SECONDS);

        clearExecutorService.scheduleAtFixedRate(() -> {
            List<KeyValue> keyValues = getGroupALlInstance(GROUP_REGISTER_PATH);
            for (KeyValue keyValue : keyValues) {
                if (keyValue.getLease() == 0) {
                    deregisterInstance(keyValue.getKey().toString());
                }
            }
        }, 60, 180, TimeUnit.SECONDS);
    }

    public void destroy() {
        if (!Boolean.TRUE.equals(JobAdminConfig.getAdminConfig().getUseEtcd())) {
            return;
        }
        logger.info("EtcdRegisterCenter destroy");
        deregisterInstance(SERVER_REGISTER_PATH);
        scheduledExecutorService.shutdown();
    }

    /**
     * 注册
     *
     * @param serverNamePath 服务记录key
     * @param metadata       原数据
     * @param electionName   选举范围定义
     */
    public void registerInstance(String serverNamePath, String metadata, String electionName) {
        logger.info(" EtcdRegisterCenter register:serverNamePath={},metadata={},electionName={}", serverNamePath, metadata, electionName);
        ByteSequence serverKey = ByteSequence.from(serverNamePath, StandardCharsets.UTF_8);
        ByteSequence metadataValue = ByteSequence.from(metadata, StandardCharsets.UTF_8);
        ByteSequence electionNameKey = ByteSequence.from(electionName == null ? "" : electionName, StandardCharsets.UTF_8);
        Lease lease = JetcdOperationClient.getInstance().getLease();
        long leaseId = 0;
        try {
            // 租约徐租
            leaseId = lease.grant(60, 30, TimeUnit.SECONDS).get().getID();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        StreamObserver<LeaseKeepAliveResponse> observer = new StreamObserver<LeaseKeepAliveResponse>() {
            @Override
            public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
                if (logger.isDebugEnabled()) {
                    List<KeyValue> keyValues = getGroupALlInstance(GROUP_REGISTER_PATH);
                    for (KeyValue kv : keyValues) {
                        logger.debug("heart beat:key={},value={}", kv.getKey(), kv.getValue());
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
            }
        };
        // 续约
        lease.keepAlive(leaseId, observer);
        KV kv = JetcdOperationClient.getInstance().getKv();
        // 续约绑定
        PutOption option = PutOption.newBuilder().withLeaseId(leaseId).build();
        //注册
        kv.put(serverKey, metadataValue, option);
        // 参与选举
        if (!StringUtils.isEmpty(electionName)) {
            Election election = JetcdOperationClient.getInstance().getElection();
            election.campaign(electionNameKey, leaseId, serverKey);
        }
    }

    /**
     * 服务注销
     *
     * @param serverNamePath 服务记录key
     */
    public void deregisterInstance(String serverNamePath) {
        logger.info("  EtcdRegisterCenter deregisterInstance:serverNamePath={}", serverNamePath);
        ByteSequence serverKey = ByteSequence.from(serverNamePath, StandardCharsets.UTF_8);
        KV kv = JetcdOperationClient.getInstance().getKv();
        Lease lease = JetcdOperationClient.getInstance().getLease();
        DeleteOption option = DeleteOption.newBuilder().withPrevKV(true).build();
        CompletableFuture<DeleteResponse> deleteFuture = kv.delete(serverKey, option);
        try {
            long leaseId = deleteFuture.get(30, TimeUnit.SECONDS).getPrevKvs().get(0).getLease();
            lease.revoke(leaseId);
            logger.info("  EtcdRegisterCenter deregisterInstance,leaseId={}", leaseId);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取分组下的全部实例
     *
     * @param groupRegisterPath 分组记录key
     * @return 获取分组下的全部实例
     */
    public List<KeyValue> getGroupALlInstance(String groupRegisterPath) {
        KV kv = JetcdOperationClient.getInstance().getKv();
        ByteSequence groupKey = ByteSequence.from(groupRegisterPath, StandardCharsets.UTF_8);
        GetOption option = GetOption.newBuilder().withPrefix(groupKey).build();
        CompletableFuture<GetResponse> getFuture = kv.get(groupKey, option);
        List<KeyValue> listInstance = new ArrayList<>();
        try {
            listInstance = getFuture.get(30, TimeUnit.SECONDS).getKvs();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return listInstance;
    }

    /**
     * 获取注册实例
     *
     * @param serverPath 服务记录key
     * @return kv
     */
    public KeyValue getInstance(String serverPath) {
        KV kv = JetcdOperationClient.getInstance().getKv();
        ByteSequence serverKey = ByteSequence.from(serverPath, StandardCharsets.UTF_8);
        CompletableFuture<GetResponse> getFuture = kv.get(serverKey);
        KeyValue keyValue = null;
        try {
            List<KeyValue> listInstance = getFuture.get(30, TimeUnit.SECONDS).getKvs();
            keyValue = listInstance.isEmpty() ? null : listInstance.get(0);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return keyValue;
    }

    /**
     * 注册监听
     *
     * @param watchPath 监听的路径
     * @param onNext    watcher
     * @return watcher对象
     */
    public Watch.Watcher watchInstance(String watchPath, Consumer<WatchResponse> onNext) {
        ByteSequence watchKey = ByteSequence.from(watchPath, StandardCharsets.UTF_8);
        Watch watch = JetcdOperationClient.getInstance().getWatch();
        WatchOption option = WatchOption.newBuilder().withPrefix(watchKey).build();
        return watch.watch(watchKey, option, onNext);
    }

    /**
     * 获取领导节点信息
     *
     * @param electionName 主节点名称
     * @return 获取领导节点信息
     */
    public KeyValue getLeader(String electionName) {
        ByteSequence electionKey = ByteSequence.from(electionName, StandardCharsets.UTF_8);
        Election election = JetcdOperationClient.getInstance().getElection();
        CompletableFuture<LeaderResponse> leadFuture = election.leader(electionKey);
        KeyValue keyValue = null;
        try {
            keyValue = leadFuture.get(30, TimeUnit.SECONDS).getKv();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return keyValue;
    }

}
