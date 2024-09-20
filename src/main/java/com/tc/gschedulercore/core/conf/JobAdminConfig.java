package com.tc.gschedulercore.core.conf;

import com.tc.gschedulercore.core.alarm.JobAlarmer;
import com.tc.gschedulercore.core.scheduler.JobScheduler;
import com.tc.gschedulercore.dao.*;
import com.tc.gschedulercore.service.AlarmRuleService;
import com.tc.gschedulercore.service.JobService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * go-scheduler config
 *
 * @author honggang.liu 2017-04-28
 */

@Component
public class JobAdminConfig implements InitializingBean, DisposableBean {

    private static JobAdminConfig adminConfig = null;

    public static JobAdminConfig getAdminConfig() {
        return adminConfig;
    }


    // ---------------------- XxlJobScheduler ----------------------

    private JobScheduler xxlJobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;

        xxlJobScheduler = new JobScheduler();
        xxlJobScheduler.init();
    }

    @Override
    public void destroy() throws Exception {
        xxlJobScheduler.destroy();
    }


    // ---------------------- XxlJobScheduler ----------------------

    // conf
    @Value("${xxl.job.i18n}")
    private String i18n;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${spring.mail.from}")
    private String emailFrom;

    @Value("${xxl.job.triggerpool.fast.max}")
    private int triggerPoolFastMax;

    @Value("${xxl.job.triggerpool.slow.max}")
    private int triggerPoolSlowMax;

    @Value("${xxl.job.logretentiondays}")
    private int logretentiondays;

    /**
     * 环境信息
     */
    @Value("${spring.profiles.active}")
    private String env;

    @Value("${xxl.job.proxyAddr}")
    private String proxyAddr;

    /**
     * etcd地址
     */
    @Value("${server.registry.address:http://localhost:2379}")
    private String endpoints;
    /**
     * etcd用户名
     */
    @Value("${server.registry.username}")
    private String endpointsUsername;

    /**
     * etcd密码
     */
    @Value("${server.registry.password}")
    private String endpointsPassword;

    public String getCugESDomain() {
        return cugESDomain;
    }

    @Value("${spring.profiles.cug.es}")
    private String cugESDomain;

    /**
     * 服务名称
     */
    @Value("${server.name}")
    private String serverName;

    /**
     * 服务端口号
     */
    @Value("${server.port}")
    private Integer port;
    // dao, service

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${go.scheduler.system.alarm.seatalk}")
    private String systemAlarm;

    /**
     * 是否使用etcd
     */
    @Value("${go.scheduler.use.etcd}")
    private Boolean useEtcd;

    @Resource
    private JobLogDao jobLogDao;
    @Resource
    private JobInfoDao jobInfoDao;

    @Resource
    private JobRegistryDao xxlJobRegistryDao;
    @Resource
    private JobGroupDao jobGroupDao;

    /**
     * 自身服务注册DAO
     */
    @Resource
    private ServerRegistryDao serverRegistryDao;

    /**
     * job服务
     */
    @Resource
    private JobService jobService;

    public MeterRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(MeterRegistry registry) {
        this.registry = registry;
    }

    @Resource
    private MeterRegistry registry;
    @Resource
    private JobLogReportDao jobLogReportDao;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private DataSource dataSource;
    @Resource
    private JobAlarmer jobAlarmer;
    /**
     * 获取事务API
     */
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * job info变更历史dao
     */
    @Resource
    private JobInfoHistoryDao jobInfoHistoryDao;

    /**
     * 缓存管理器
     */
    @Resource
    private CacheManager cacheManager;

    /**
     * 锁操作
     */
    @Resource
    private JobLockDao xxlJobLockDao;

    @Resource
    private JobUserDao xxlJobUserDao;

    /**
     * 日志标签
     */
    @Resource
    private JobLogTagDao jobLogTagDao;

    /**
     * 补偿dao
     */
    @Resource
    private JobCompensateDao jobCompensateDao;

    /**
     * 延迟日志dao
     */
    @Resource
    private DelayLogDao delayLogDao;

    @Resource
    private LogMetricDao logMetricDao;

    /**
     * 规则DAO
     */
    @Resource
    private AlarmRuleDao alarmRuleDao;

    /**
     * 规则明细dao
     */
    @Resource
    private AlarmItemDao alarmItemDao;

    /**
     * 告警脚本item Dao
     */
    @Resource
    private AlarmScriptItemDao alarmScriptItemDao;

    public AlarmScriptDao getAlarmScriptDao() {
        return alarmScriptDao;
    }

    @Resource
    private AlarmScriptDao alarmScriptDao;

    /**
     * 日志脚本DAO
     */
    @Resource
    private JobLogScriptDao jobLogScriptDao;

    /**
     * 通知DAO
     */
    @Resource
    private NotifyInfoDao notifyInfoDao;

    /**
     * 规则服务
     */
    @Resource
    private AlarmRuleService alarmRuleService;

    /**
     * 任务检查服务
     */
    @Resource
    private JobCheckDao jobCheckDao;

    public String getI18n() {
        if (!Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n)) {
            return "zh_CN";
        }
        return i18n;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax < 200) {
            return 200;
        }
        return triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    public int getLogretentiondays() {
        if (logretentiondays < 7) {
            // Limit greater than or equal to 7, otherwise close
            return -1;
        }
        return logretentiondays;
    }

    public JobLogDao getJobLogDao() {
        return jobLogDao;
    }

    public JobInfoDao getJobInfoDao() {
        return jobInfoDao;
    }

    public JobRegistryDao getJobRegistryDao() {
        return xxlJobRegistryDao;
    }

    public JobGroupDao getJobGroupDao() {
        return jobGroupDao;
    }

    public JobLogReportDao getJobLogReportDao() {
        return jobLogReportDao;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public JobAlarmer getJobAlarmer() {
        return jobAlarmer;
    }

    public String getEnv() {
        return env;
    }

    public String getProxyAddr() {
        return proxyAddr;
    }


    public String getEndpoints() {
        return endpoints;
    }

    public String getServerName() {
        return serverName;
    }

    public Integer getPort() {
        return port;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getSystemAlarm() {
        return systemAlarm;
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public JobInfoHistoryDao getJobInfoHistoryDao() {
        return jobInfoHistoryDao;
    }

    public JobLockDao getJobLockDao() {
        return xxlJobLockDao;
    }

    public JobUserDao getXxlJobUserDao() {
        return xxlJobUserDao;
    }

    public Boolean getUseEtcd() {
        return useEtcd;
    }

    public JobLogTagDao getXxlJobLogTagDao() {
        return jobLogTagDao;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public JobCompensateDao getJobCompensateDao() {
        return jobCompensateDao;
    }

    public String getEndpointsUsername() {
        return endpointsUsername;
    }

    public String getEndpointsPassword() {
        return endpointsPassword;
    }

    public DelayLogDao getXxlDelayLogDao() {
        return delayLogDao;
    }

    public LogMetricDao getLogMetricDao() {
        return logMetricDao;
    }

    public AlarmRuleDao getAlarmRuleDao() {
        return alarmRuleDao;
    }

    public AlarmItemDao getAlarmItemDao() {
        return alarmItemDao;
    }

    public NotifyInfoDao getNotifyInfoDao() {
        return notifyInfoDao;
    }

    public AlarmRuleService getAlarmRuleService() {
        return alarmRuleService;
    }

    public JobCheckDao getJobCheckDao() {
        return jobCheckDao;
    }

    public AlarmScriptItemDao getAlarmScriptItemDao() {
        return alarmScriptItemDao;
    }

    public JobLogScriptDao getJobLogScriptDao() {
        return jobLogScriptDao;
    }

    public JobService getJobService() {
        return jobService;
    }

    public ServerRegistryDao getServerRegistryDao() {
        return serverRegistryDao;
    }
}
