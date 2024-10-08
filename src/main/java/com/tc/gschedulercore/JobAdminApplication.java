package com.tc.gschedulercore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * 启动类
 *
 * @author honggang.liu
 */
@SpringBootApplication
@EnableSwagger2
@EnableCaching
public class JobAdminApplication {
    private static Logger logger = LoggerFactory.getLogger(JobAdminApplication.class.getSimpleName());

    /*
     * 动态指定sharding jdbc 的雪花算法中的属性work.id属性
     * 通过调用System.setProperty()的方式实现,可用容器的 id 或者机器标识位
     * workId最大值 1L << 100，就是1024，即 0<= workId < 1024
     * {@link SnowflakeShardingKeyGenerator#getWorkerId()}
     *
     */
    static {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostAddressIp = inetAddress.getHostAddress();
            if (hostAddressIp.hashCode() != 0) {
                String workId = Math.abs(hostAddressIp.hashCode()) % 1024 + "";
                System.setProperty("workId", workId);
                logger.info("workId:{}", workId);
            }
            logger.info("hashcode=0,workId:{}", 0);
        } catch (UnknownHostException e) {
            String workId = Math.abs(UUID.randomUUID().hashCode()) % 1024 + "";
            System.setProperty("workId", workId);
            logger.info("workId:{}", workId);
            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        SpringApplication.run(JobAdminApplication.class, args);
    }

}