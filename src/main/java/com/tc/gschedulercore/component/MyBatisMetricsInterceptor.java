package com.tc.gschedulercore.component;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.util.IpUtil;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
        @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})
})
public class MyBatisMetricsInterceptor implements Interceptor {

    private Counter sqlCounter;

    private final CCJSqlParserManager parserManager = new CCJSqlParserManager();

    private static final String UNKNOWN_TABLE = "UNKNOWN_TABLE";

    @Resource
    private DataSource shardingSphereDataSource;


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String tableName = UNKNOWN_TABLE;
        Timer.Sample sample = Timer.start(JobAdminConfig.getAdminConfig().getRegistry());
        // 获取 SQL 语句
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        String sql = "";
        net.sf.jsqlparser.statement.Statement statement = null;
        try {
            sql = statementHandler.getBoundSql().getSql().replaceAll("\\s+", " ");
            // 替换掉"`"
            sql = sql.replaceAll("`", "");
            statement = parserManager.parse(new StringReader(sql));
            // Increment the SQL counter
            getSqlCounter(statement).increment();
            tableName = extractTableName(statement);
            // 记录当前数据源连接池的情况
            reportConnectionPoolStats();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            return invocation.proceed();
        } finally {
            sample.stop(Timer.builder("mybatisSqlExecution")
                    .publishPercentiles(0.95, 0.99)  // 计算 P95 和 P99
                    .tags("sql", sql)
                    .tags("database", "go-scheduler")
                    .tags("opt_type", getSqlType(statement))
                    .tags("table", tableName)
                    .tags("ip", IpUtil.getLocalHostLANAddress())
                    .register(JobAdminConfig.getAdminConfig().getRegistry()));
        }
    }

    private Counter getSqlCounter(net.sf.jsqlparser.statement.Statement statement) {
        if (sqlCounter == null) {
            sqlCounter = Counter.builder("mybatisSqlCount")
                    .tags("database", "go-scheduler")
                    .tags("opt_type", getSqlType(statement))
                    .tags("table", extractTableName(statement))
                    .tags("ip", IpUtil.getLocalHostLANAddress())
                    .register(JobAdminConfig.getAdminConfig().getRegistry());
        }
        return sqlCounter;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // No properties to set
    }


    public String extractTableName(net.sf.jsqlparser.statement.Statement statement) {
        try {
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(statement);
            if (!tableList.isEmpty()) {
                return tableList.get(0); // 如果语句涉及多张表，返回第一张表的名称
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown_table";
    }

    public String getSqlType(net.sf.jsqlparser.statement.Statement statement) {
        if (statement instanceof Select) {
            return "SELECT";
        } else if (statement instanceof Update) {
            return "UPDATE";
        } else if (statement instanceof Delete) {
            return "DELETE";
        } else if (statement instanceof Insert) {
            return "INSERT";
        } else {
            return "UNKNOWN";
        }
    }

    private void reportConnectionPoolStats() {
        if (shardingSphereDataSource instanceof ShardingSphereDataSource) {
            // 获取底层的数据源集合
            Map<String, ShardingSphereMetaData> shardingSphereMetaDataMap = ((ShardingSphereDataSource) shardingSphereDataSource).getContextManager().getMetaDataContexts().getMetaDataMap();
            ShardingSphereMetaData shardingSphereMetaData = shardingSphereMetaDataMap.get("logic_db");
            Map<String, DataSource> dataSourceMap = shardingSphereMetaData.getResource().getDataSources();
            // 遍历每个底层的数据源
            dataSourceMap.forEach((name, dataSource) -> {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                // 获取 HikariPoolMXBean
                HikariPoolMXBean poolProxy = hikariDataSource.getHikariPoolMXBean();

                // Active connections
                Gauge.builder("db_stats_in_use", poolProxy, HikariPoolMXBean::getActiveConnections)
                        .description("Active connections in HikariCP")
                        .tags("pool", hikariDataSource.getPoolName())  // 添加连接池名称作为标签
                        .tags("database", "go-scheduler")
                        .tags("ip", IpUtil.getLocalHostLANAddress())
                        .register(JobAdminConfig.getAdminConfig().getRegistry());

                // Idle connections
                Gauge.builder("db_stats_idle", poolProxy, HikariPoolMXBean::getIdleConnections)
                        .description("Idle connections in HikariCP")
                        .tags("pool", hikariDataSource.getPoolName())
                        .tags("database", "go-scheduler")
                        .tags("ip", IpUtil.getLocalHostLANAddress())
                        .register(JobAdminConfig.getAdminConfig().getRegistry());

                // Total connections
                Gauge.builder("db_stats_open", poolProxy, HikariPoolMXBean::getTotalConnections)
                        .description("Total connections in HikariCP")
                        .tags("pool", hikariDataSource.getPoolName())
                        .tags("database", "go-scheduler")
                        .tags("ip", IpUtil.getLocalHostLANAddress())
                        .register(JobAdminConfig.getAdminConfig().getRegistry());

                // Threads awaiting connection
                Gauge.builder("db_stats_pending", poolProxy, HikariPoolMXBean::getThreadsAwaitingConnection)
                        .description("Threads awaiting connection in HikariCP")
                        .tags("pool", hikariDataSource.getPoolName())
                        .tags("database", "go-scheduler")
                        .tags("ip", IpUtil.getLocalHostLANAddress())
                        .register(JobAdminConfig.getAdminConfig().getRegistry());
            });

        }
    }
}

