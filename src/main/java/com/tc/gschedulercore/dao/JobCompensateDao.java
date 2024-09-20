package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobCompensate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 任务补偿DAO
 *
 * @author honggang.liu
 */
@Mapper
public interface JobCompensateDao {

    /**
     * 判断补偿队列中是否已经存在
     *
     * @param jobLogId logId
     * @param jobGroup 执行器ID
     * @param jobId    任务ID
     * @return 存在返回true, 否则返回false
     */
    Boolean exist(@Param("jobLogId") long jobLogId, @Param("jobGroup") int jobGroup, @Param("jobId") int jobId);

    /**
     * 保存补偿任务
     *
     * @param xxlJobCompensate 补偿实例
     * @return 主键ID
     */
    long save(JobCompensate xxlJobCompensate);
}
