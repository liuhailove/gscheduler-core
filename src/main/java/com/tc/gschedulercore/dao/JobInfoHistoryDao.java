package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobInfoHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * job info 变更版本记录
 *
 * @author honggang.liu
 */
@Mapper
public interface JobInfoHistoryDao {

    /**
     * 存储变更历史
     * @param xxlJobInfoHistory 变更历史
     * @return 成功返回>0的值
     */
    int save(JobInfoHistory xxlJobInfoHistory);

    /**
     *  根据jobId查询变更历史
     * @param jobId job id
     * @return 变更历史
     */
    List<JobInfoHistory> findByJobId(@Param("jobId") int jobId);

    /**
     * 删除旧版本的变更
     * @param jobId job id
     * @param limit limit
     * @return 成功返回>0的值
     */
    int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

    /**
     *  删除job下的全部变更
     * @param jobId job id
     * @return 成功返回>0的值
     */
    int deleteByJobId(@Param("jobId") int jobId);
}
