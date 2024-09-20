package com.tc.gschedulercore.component;


import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.util.IpUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.shardingsphere.infra.database.metadata.DataSourceMetaData;
import org.apache.shardingsphere.infra.executor.sql.hook.SQLExecutionHook;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class ShardingPrometheusSQLExecutionHook implements SQLExecutionHook {
    private final MeterRegistry meterRegistry;
    private Timer.Sample timerSample;
    /**
     * 用于保存物理 SQL
     */
    private String sql;
    private Counter sqlCounter;
    private final CCJSqlParserManager parserManager = new CCJSqlParserManager();

    public ShardingPrometheusSQLExecutionHook() {
        this.meterRegistry = JobAdminConfig.getAdminConfig().getRegistry();
    }

    @Override
    public void start(String dataSourceName, String sql, List<Object> list, DataSourceMetaData dataSourceMetaData, boolean b, Map<String, Object> map) {
        this.sql = sql;
        // 开始计时，记录执行的物理 SQL
        timerSample = Timer.start(meterRegistry);
    }

    @Override
    public void finishSuccess() {
        net.sf.jsqlparser.statement.Statement statement = null;
        String tableName = "";
        try {
            sql = sql.replaceAll("\\s+", " ").trim();
            // 替换掉"`"
            sql = sql.replaceAll("`", "").replaceAll("\n", "");
            statement = parserManager.parse(new StringReader(sql));
            // Increment the SQL counter
            getSqlCounter(statement).increment();
            tableName = extractTableName(statement);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 记录成功执行的物理 SQL 及其耗时
        timerSample.stop(Timer.builder("sql_execution")
                .description("ShardingJDBC SQL execution time")
                .publishPercentiles(0.95, 0.99)  // 计算 P95 和 P99
                .tags("sql", sql)
                .tags("database", "go-scheduler")
                .tags("opt_type", getSqlType(statement))
                .tags("table", tableName)
                .tags("ip", IpUtil.getLocalHostLANAddress())
                .register(meterRegistry));
    }

    @Override
    public void finishFailure(Exception cause) {
        // 记录失败执行的物理 SQL 及其耗时
        timerSample.stop(Timer.builder("sql_execution")
                .description("ShardingJDBC SQL execution time")
                .tags("sql", sql)
                .tag("status", "failure")
                .register(meterRegistry));
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

    private Counter getSqlCounter(net.sf.jsqlparser.statement.Statement statement) {
        if (sqlCounter == null) {
            synchronized (this) {
                if (sqlCounter == null) {
                    sqlCounter = Counter.builder("sql_seconds_count")
                            .tags("database", "go-scheduler")
                            .tags("opt_type", getSqlType(statement))
                            .tags("table", extractTableName(statement))
                            .tags("ip", IpUtil.getLocalHostLANAddress())
                            .register(JobAdminConfig.getAdminConfig().getRegistry());
                }
            }
        }
        return sqlCounter;
    }
}
