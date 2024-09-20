package com.tc.gschedulercore.core.etcd;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import io.etcd.jetcd.*;
import io.etcd.jetcd.common.exception.ClosedClientException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Etcd操作类
 *
 * @author honggang.liu
 */
public class JetcdOperationClient {
    private static Logger logger = LoggerFactory.getLogger(JetcdOperationClient.class.getSimpleName());
    /**
     * etcd client
     */
    private Client client;

    /**
     * etcd kv
     */
    private KV kv;

    private Lock lock;

    /**
     * etcd 租约
     */
    private Lease lease;

    /**
     * etcd watch
     */
    private Watch watch;

    /**
     * 选主
     */
    private Election election;

    /**
     * 逗号分隔符
     */
    private static final String COMMA = ",";

    private static JetcdOperationClient instance = new JetcdOperationClient();

    /**
     * 单例
     *
     * @return 返回etcd操作对象
     */
    public static JetcdOperationClient getInstance() {
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        logger.info(" JetcdOperationClient init,endpoints={},userName={},password={}", JobAdminConfig.getAdminConfig().getEndpoints(), JobAdminConfig.getAdminConfig().getEndpointsUsername(), JobAdminConfig.getAdminConfig().getEndpointsPassword());
        if (!Boolean.TRUE.equals(JobAdminConfig.getAdminConfig().getUseEtcd())) {
            return;
        }
        ClientBuilder clientBuilder = Client.builder();
        clientBuilder.endpoints(JobAdminConfig.getAdminConfig().getEndpoints().split(COMMA)).connectTimeout(Duration.ofSeconds(60));
        if (JobAdminConfig.getAdminConfig().getEndpointsUsername() != null && !StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getEndpointsUsername().trim())) {
            clientBuilder.user(ByteSequence.from(JobAdminConfig.getAdminConfig().getEndpointsUsername(), StandardCharsets.UTF_8));
        }
        if (JobAdminConfig.getAdminConfig().getEndpointsPassword() != null && !StringUtils.isEmpty(JobAdminConfig.getAdminConfig().getEndpointsPassword().trim())) {
            clientBuilder.password(ByteSequence.from(JobAdminConfig.getAdminConfig().getEndpointsPassword(), StandardCharsets.UTF_8));
        }
        client = clientBuilder.build();
        kv = client.getKVClient();
        lock = client.getLockClient();
        lease = client.getLeaseClient();
        watch = client.getWatchClient();
        election = client.getElectionClient();
    }

    public void destroy() {
        logger.info(" JetcdOperationClient init");
        if (!Boolean.TRUE.equals(JobAdminConfig.getAdminConfig().getUseEtcd())) {
            return;
        }
        try {
            election.close();
        } catch (ClosedClientException e) {
            logger.warn("election has been closed");
        }
        try {
            watch.close();
        } catch (ClosedClientException e) {
            logger.warn("watch has been closed");
        }
//        try {
//            lease.close();
//        } catch (ClosedClientException e) {
//            logger.warn("lease has been closed");
//        }
        try {
            lock.close();
        } catch (ClosedClientException e) {
            logger.warn("lock has been closed");
        }
        try {
            kv.close();
        } catch (ClosedClientException e) {
            logger.warn("kv has been closed");
        }
//        try {
//            client.close();
//        } catch (ClosedClientException e) {
//            logger.warn("client has been closed");
//        }
    }

    public KV getKv() {
        return kv;
    }

    public Lock getLock() {
        return lock;
    }

    public Lease getLease() {
        return lease;
    }

    public Watch getWatch() {
        return watch;
    }

    public Election getElection() {
        return election;
    }

    public String getCOMMA() {
        return COMMA;
    }
}
