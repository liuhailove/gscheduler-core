package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务检查dao
 *
 * @author honggang.liu
 */
@Mapper
public interface JobCheckDao {

    /**
     * 保存
     *
     * @param jobCheck check配置
     * @return 影响行数
     */
    int save(JobCheck jobCheck);

    /**
     * 加载jobCheck配置
     *
     * @param id 主键ID
     * @return check任务
     */
    JobCheck load(@Param("id") int id);

    /**
     * 更新jobCheck
     *
     * @param jobCheck job检查配置
     * @return 影响行数
     */
    int update(JobCheck jobCheck);

    /**
     * 删除check任务
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int delete(@Param("id") long id);

    /**
     * 删除check任务
     *
     * @param jobId 任务ID
     * @return 影响行数
     */
    int deleteBy(@Param("jobId") Integer jobId);

    /**
     * 删除check任务
     *
     * @param alarmRuleId 规则ID
     * @return 影响行数
     */
    int deleteByAlarm(@Param("alarmRuleId") Long alarmRuleId);


    /**
     * 根据规则ID加载jobCheck列表
     *
     * @param alarmRuleId 规则ID
     * @return check列表
     */
    List<JobCheck> queryByAlarm(@Param("alarmRuleId") Long alarmRuleId);

    /**
     * 根据任务ID加载jobCheck
     *
     * @param jobGroup 执行器
     * @param jobId    任务ID
     * @return check列表
     */
    JobCheck queryByJob(@Param("jobGroup") Integer jobGroup, @Param("jobId") Integer jobId);


    /**
     * 判断jobCheck是否存在，jobId唯一
     *
     * @param jobGroupId 执行器ID
     * @param jobId      任务ID
     * @return 存在返回1否则返回0
     */
    Boolean exist(@Param("jobGroupId") Integer jobGroupId, @Param("jobId") Integer jobId);

    /**
     * 根据alarmRuleId查询jobId
     *
     * @param alarmRuleId 规则ID
     * @return 关联的jobId
     */
    List<Integer> queryJobIdByAlarm(@Param("alarmRuleId") Long alarmRuleId);
}
