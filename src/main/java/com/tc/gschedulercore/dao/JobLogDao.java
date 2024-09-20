package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * job log
 *
 * @author honggang.liu 2016-1-12 18:03:06
 */
@Mapper
public interface JobLogDao {

    List<JobLog> pageList(@Param("offset") int offset,
                          @Param("pagesize") int pagesize,
                          @Param("jobGroups") List<Integer> jobGroups,
                          @Param("jobGroup") Integer jobGroup,
                          @Param("jobId") int jobId,
                          @Param("triggerTimeStart") Date triggerTimeStart,
                          @Param("triggerTimeEnd") Date triggerTimeEnd,
                          @Param("logStatus") int logStatus,
                          @Param("logId") long logId,
                          @Param("parentLog") long parentLog,
                          @Param("excludeTimeStart") Date excludeTimeStart,
                          @Param("excludeTimeEnd") Date excludeTimeEnd,
                          @Param("executeTimeAsc") Boolean executeTimeAsc,
                          @Param("handleMsg") String handleMsg,
                          @Param("instanceId") String instanceId,
                          @Param("tagName") String tagName
    );

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("jobGroups") List<Integer> jobGroups,
                      @Param("jobGroup") Integer jobGroup,
                      @Param("jobId") int jobId,
                      @Param("triggerTimeStart") Date triggerTimeStart,
                      @Param("triggerTimeEnd") Date triggerTimeEnd,
                      @Param("logStatus") int logStatus,
                      @Param("logId") long logId,
                      @Param("parentLog") long parentLog,
                      @Param("excludeTimeStart") Date excludeTimeStart,
                      @Param("excludeTimeEnd") Date excludeTimeEnd,
                      @Param("executeTimeAsc") Boolean executeTimeAsc,
                      @Param("handleMsg") String handleMsg,
                      @Param("instanceId") String instanceId,
                      @Param("tagName") String tagName);

    /**
     * 根据group和主键ID查询
     *
     * @param jobGroup 分表键
     * @param id       主键ID
     * @return 执行日志
     */
    JobLog load(@Param("jobGroup") int jobGroup, @Param("id") long id);

    /**
     * 根据主键ID查询
     *
     * @param id 主键ID
     * @return 执行日志
     */
    JobLog loadBy(@Param("id") long id);

    /**
     * 加锁加载
     *
     * @param id 日志ID
     * @return 执行日志
     */
    JobLog loadForUpdate(@Param("id") long id);

    long save(JobLog xxlJobLog);

    /**
     * 判断是否存在符合条件的joblog
     *
     * @param jobGroup      执行器id
     * @param jobId         任务id
     * @param instanceId    实例id
     * @param parentLog     父任务id
     * @param executorParam 执行参数
     * @return 灿在返回true, 否则返回false
     */
    JobLog loadLog(@Param("jobGroup") int jobGroup, @Param("jobId") int jobId, @Param("instanceId") String instanceId, @Param("parentLog") long parentLog, @Param("executorParam") String executorParam);

    int updateTriggerInfo(JobLog xxlJobLog);

    int updateHandleInfo(JobLog xxlJobLog);

    //更新 log 表终止任务flag
    int updateTaskTerminationFlag(@Param("logId") long logId,
                                  @Param("taskTerminationFlag") int TaskTerminationFlag);


    /**
     * 更新是否有子任务
     *
     * @param xxlJobLog log
     * @return 返回影响行数
     */
    int updateHasSub(JobLog xxlJobLog);

    /**
     * 更新分发子任务状态
     *
     * @param instanceId     运行的实例ID
     * @param jobGroup       group
     * @param jobIdes        任务id集合
     * @param oldDispatchSub 旧状态
     * @param newDispatchSub 更新为的新状态
     * @return 返回影响行数
     */
    int updateDispatchSub(@Param("instanceId") String instanceId,
                          @Param("jobIdes") List<Integer> jobIdes,
                          @Param("jobGroup") int jobGroup,
                          @Param("oldDispatchSub") int oldDispatchSub,
                          @Param("newDispatchSub") int newDispatchSub);


    /**
     * 更新分发子任务状态
     *
     * @param logId          logId
     * @param jobGroup       group
     * @param jobId          任务id
     * @param oldDispatchSub 旧状态
     * @param newDispatchSub 更新为的新状态
     * @return 返回影响行数
     */
    int updateDispatchSubBy(@Param("logId") long logId,
                            @Param("jobId") int jobId,
                            @Param("jobGroup") int jobGroup,
                            @Param("oldDispatchSub") int oldDispatchSub,
                            @Param("newDispatchSub") int newDispatchSub);

    int delete(@Param("jobId") int jobId, @Param("jobGroup") int jobGroup);

    Map<String, Object> findLogReport(
            @Param("jobGroup") int jobGroup,
            @Param("jobId") int jobId,
            @Param("from") Date from,
            @Param("to") Date to);

    List<Long> findClearLogIds(@Param("jobGroup") int jobGroup,
                               @Param("jobId") int jobId,
                               @Param("clearBeforeTime") Date clearBeforeTime,
                               @Param("clearBeforeNum") int clearBeforeNum,
                               @Param("pagesize") int pagesize);

    int clearLog(@Param("jobGroup") int jobGroup, @Param("logIds") List<Long> logIds);

    int deleteLog(@Param("jobGroup") int jobGroup, @Param("jobId") int jobId,
                  @Param("clearBeforeTime") Date clearBeforeTime,
                  @Param("pagesize") int pagesize);

    /**
     * 查找任务下发到执行器但是执行器上报错误的joblog
     *
     * @param jobGroup
     * @param minExecutorFailTriggerTime
     * @param maxExecutorFailTriggerTime
     * @param pagesize
     * @return
     */
    List<Long> findFailJobLogIds(@Param("jobGroup") Integer jobGroup, @Param("minExecutorFailTriggerTime") long minExecutorFailTriggerTime, @Param("maxExecutorFailTriggerTime") long maxExecutorFailTriggerTime, @Param("pagesize") int pagesize);


    int updateAlarmStatus(@Param("logId") long logId,
                          @Param("jobGroup") Integer jobGroup,
                          @Param("oldAlarmStatus") int oldAlarmStatus,
                          @Param("newAlarmStatus") int newAlarmStatus);

    /**
     * 更新脚本重试次数
     *
     * @param logId           日志ID
     * @param jobGroup        执行器ID
     * @param fromRetryCount  从重试次数
     * @param toRetryCount    到重试次数
     * @param nextTriggerTime 下次执行时间
     * @return 返回影响行数，如果返回值大于0，则执行成功，否则执行失败
     */
    int updateScriptRetryCount(@Param("logId") long logId,
                               @Param("jobGroup") Integer jobGroup,
                               @Param("fromRetryCount") int fromRetryCount,
                               @Param("toRetryCount") int toRetryCount,
                               @Param("nextTriggerTime") long nextTriggerTime);

    /**
     * 更新处理状态为处理中
     *
     * @param logId                  日志ID
     * @param oldHandleStatus        旧处理状态
     * @param newHandleStatus        更新为处理状态
     * @param childrenExecutorParams 子任务执行参数
     * @return 返回影响行数
     */
    int update2Processing(@Param("logId") long logId,
                          @Param("jobGroup") int jobGroup,
                          @Param("oldHandleStatus") int oldHandleStatus,
                          @Param("newHandleStatus") int newHandleStatus,
                          @Param("childrenExecutorParams") String childrenExecutorParams,
                          @Param("handleTime") Date handleTime);


    List<Long> findLostJobIds(@Param("jobGroup") int jobGroup, @Param("losedTime") Date losedTime);

    /**
     * 查找超过阈值的 logId
     *
     * @param currentTime 当前时间
     * @param jobGroup    job group
     * @param pagesize    分页
     * @return logid集合
     */
    List<Long> findThresholdTimeoutJobLogIds(@Param("jobGroup") int jobGroup, @Param("currentTime") long currentTime, @Param("pagesize") int pagesize);

    int getTaskActualRunNumbers(@Param("jobGroupId") int jobGroupId, @Param("jobId") int jobId, @Param("currentExecTime") Date currentExecTime, @Param("yesterdayExecTime") Date yesterdayExecTime, @Param("pagesize") int pagesize);

    /**
     * 查询任务在起止时间内的运行次数
     *
     * @param jobGroupId 执行器ID
     * @param jobId      任务ID
     * @param fromTime   开始时间
     * @param toTime     截至时间
     * @return 运行次数
     */
    int queryJobExecuteNumber(@Param("jobGroupId") int jobGroupId, @Param("jobId") int jobId, @Param("triggerType") int triggerType, @Param("fromTime") Date fromTime, @Param("toTime") Date toTime);

    /**
     * 更新阈值告警状态
     *
     * @param logId          logid
     * @param oldAlarmStatus 旧告警状态
     * @param newAlarmStatus 新告警状态
     * @return 影响行数
     */
    int updateThresholdAlarmStatus(@Param("logId") long logId,
                                   @Param("oldAlarmStatus") int oldAlarmStatus,
                                   @Param("newAlarmStatus") int newAlarmStatus);


    /**
     * 根据instanceId,任务ID统计是某种执行状态的数量,此接口主要用于sharing类型任务
     *
     * @param instanceId  实例ID
     * @param jobGroup    执行器ID
     * @param jobId       任务ID
     * @param handleCodes 处理状态码集合
     * @return 统计数量
     */
    int countStatusShardingBy(@Param("instanceId") String instanceId,
                              @Param("jobGroup") int jobGroup,
                              @Param("jobId") int jobId,
                              @Param("handleCodes") List<Integer> handleCodes);


    /**
     * 根据instanceId,任务ID找到一个jobLog
     *
     * @param instanceId  实例ID
     * @param jobGroup    执行器ID
     * @param jobId       任务ID
     * @param handleCodes 处理状态码集合
     * @return 统计数量
     */
    JobLog findOneJobLog(@Param("instanceId") String instanceId,
                         @Param("jobGroup") int jobGroup,
                         @Param("jobId") int jobId,
                         @Param("handleCodes") List<Integer> handleCodes);

    /**
     * 根据instanceId,任务ID统计是某种执行状态的数量,此接口主要用于sharing类型任务
     *
     * @param instanceId  实例ID
     * @param jobGroup    执行器ID
     * @param jobId       任务ID
     * @param dispatchSub 下发状态
     * @return 统计数量
     */
    int countDispatchShardingBy(@Param("instanceId") String instanceId,
                                @Param("jobGroup") int jobGroup,
                                @Param("jobId") int jobId,
                                @Param("dispatchSub") int dispatchSub);

    /**
     * 查询此实例对应的全部jobLog
     *
     * @param instanceId  实例ID
     * @param jobGroup    执行器ID
     * @param jobId       任务ID
     * @param handleCodes 处理状态码集合
     * @return 查询此实例对应的全部jobLog
     */
    List<JobLog> queryList(@Param("instanceId") String instanceId,
                           @Param("jobGroup") int jobGroup,
                           @Param("jobId") int jobId,
                           @Param("handleCodes") List<Integer> handleCodes);

    /**
     * 查询此实例对应的全部jobLog id
     *
     * @param instanceId 实例ID
     * @param jobId      任务ID
     * @param jobGroup   执行器ID
     * @return 查询此实例对应的全部jobLog
     */
    List<Long> queryLogIdes(@Param("instanceId") String instanceId,
                            @Param("jobId") int jobId,
                            @Param("jobGroup") int jobGroup);

    /**
     * 查找处理中的任务
     *
     * @param maxHandleTime 最大的处理长时间
     * @param minHandleTime
     * @return 符合预期数据
     */
    List<JobLog> query10Process(@Param("minHandleTime") Date minHandleTime, @Param("maxHandleTime") Date maxHandleTime);

    /**
     * 查找10个待处理的延迟任务
     *
     * @param jobGroup    执行器ID
     * @param currentTime 当前时间
     * @return 符合预期数据
     */
    List<JobLog> query10DelayExecute(@Param("jobGroup") int jobGroup, @Param("currentTime") Long currentTime);

    /**
     * 查找启动此实例的第一个jobId
     *
     * @param instanceId 实例ID
     * @return 第一个jobId
     */
    int findFirstJobByInstance(@Param("instanceId") String instanceId);

    /**
     * 查询大于某个时间点的最近的一条log
     *
     * @param jobGroup        执行器
     * @param jobId           jobID
     * @param greaterThanTime 某个时间点
     * @return 最近一条日志
     */
    JobLog findLatestGreatThanLog(@Param("jobGroup") int jobGroup, @Param("jobId") int jobId, @Param("greaterThanTime") Date greaterThanTime);

}
