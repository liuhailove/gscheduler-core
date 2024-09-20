package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.DelayLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 延迟日志DAO
 *
 * @author honggang.liu
 */
@Mapper
public interface DelayLogDao {

    /**
     * 分页查询
     *
     * @param offset           偏移
     * @param pagesize         分页大小
     * @param jobGroups        执行器集合
     * @param jobGroup         执行器
     * @param jobId            jobId
     * @param triggerTimeStart 日志触发时间范围-开始时间
     * @param triggerTimeEnd   日志触发时间范围-截止时间
     * @param logId            日志ID
     * @return 分页数据
     */
    List<DelayLog> pageList(@Param("offset") int offset,
                            @Param("pagesize") int pagesize,
                            @Param("jobGroups") List<Integer> jobGroups,
                            @Param("jobGroup") Integer jobGroup,
                            @Param("jobId") Integer jobId,
                            @Param("triggerTimeStart") Date triggerTimeStart,
                            @Param("triggerTimeEnd") Date triggerTimeEnd,
                            @Param("logId") Long logId);

    /**
     * 统计
     *
     * @param offset           偏移
     * @param pagesize         分页大小
     * @param jobGroups        执行器集合
     * @param jobGroup         执行器
     * @param jobId            jobId
     * @param triggerTimeStart 日志触发时间范围-开始时间
     * @param triggerTimeEnd   日志触发时间范围-截止时间
     * @param logId            日志ID
     * @return 统计数据
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("jobGroups") List<Integer> jobGroups,
                      @Param("jobGroup") Integer jobGroup,
                      @Param("jobId") Integer jobId,
                      @Param("triggerTimeStart") Date triggerTimeStart,
                      @Param("triggerTimeEnd") Date triggerTimeEnd,
                      @Param("logId") Long logId);

    /**
     * 根据主键ID查询
     *
     * @param id 主键ID
     * @return 执行日志
     */
    DelayLog load(@Param("id") long id);

    /**
     * 延迟消息保存
     *
     * @param xxlDelayLog 延迟日志
     * @return 影响行数
     */
    long save(DelayLog xxlDelayLog);

    /**
     * 告警状态更新
     *
     * @param id             主键ID
     * @param jobGroup       执行器ID
     * @param oldAlarmStatus 旧告警状态
     * @param newAlarmStatus 新告警状态
     * @return 影响行数
     */
    int updateAlarmStatus(@Param("id") long id,
                          @Param("jobGroup") Integer jobGroup,
                          @Param("oldAlarmStatus") int oldAlarmStatus,
                          @Param("newAlarmStatus") int newAlarmStatus);
}
