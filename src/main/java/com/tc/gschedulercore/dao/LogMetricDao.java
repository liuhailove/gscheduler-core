package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.LogMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 调度log metric dao
 *
 * @author honggang.liu
 */
@Mapper
public interface LogMetricDao {

    /**
     * log metric写入
     *
     * @param logMetric metric
     * @return 影响行数
     */
    int save(LogMetric logMetric);

    /**
     * 按照ID删除
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int delete(@Param("id") Long id);

    /**
     * 日志ID
     *
     * @param jobLogId 执行日志ID
     * @return 影响行数
     */
    int deleteBy(@Param("jobLogId") Long jobLogId);

    /**
     * metric查询
     *
     * @param jobLogId 执行日志ID
     * @return metric集合
     */
    List<LogMetric> findBy(@Param("jobLogId") Long jobLogId, @Param("jobId") Integer jobId);

    /**
     * metric查询最近的metric
     *
     * @param jobLogId 执行日志ID
     * @return metric集合
     */
    LogMetric findLatestBy(@Param("jobLogId") Long jobLogId, @Param("jobId") Integer jobId);

    /**
     * 查询loges对应的metric
     *
     * @param jobLogIdes loges集合
     * @return 业务上报数据
     */
//    List<LogMetric> findByLoges(@Param("jobLogIdes") List<Long> jobLogIdes);
    List<LogMetric> findByLoges(@Param("jobLogIdes") List<Long> jobLogIdes, @Param("offset") int offset, @Param("limit") int limit);


    //根据时间获取log metrics Id便于删除
    List<Long> findClearLogMetricsIds(@Param("jobId") Integer jobId, @Param("clearBeforeTime") Date clearBeforeTime, @Param("pagesize") int pagesize);

    int clearLog(@Param("jobId") Integer jobId, @Param("logMetricsIds") List<Long> logMetricsIds);

}
