package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/**
 * job info
 *
 * @author honggang.liu 2016-1-12 18:03:45
 */
@Mapper
public interface JobInfoDao {

    List<JobInfo> pageList(@Param("offset") int offset,
                           @Param("pagesize") int pagesize,
                           @Param("jobGroups") List<Integer> jobGroups,
                           @Param("triggerStatus") int triggerStatus,
                           @Param("jobDesc") String jobDesc,
                           @Param("jobName") String jobName,
                           @Param("executorHandler") String executorHandler,
                           @Param("author") String author);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("jobGroups") List<Integer> jobGroups,
                      @Param("triggerStatus") int triggerStatus,
                      @Param("jobDesc") String jobDesc,
                      @Param("jobName") String jobName,
                      @Param("executorHandler") String executorHandler,
                      @Param("author") String author);

    /**
     * 可以触发延迟的列表
     *
     * @param offset    偏移量
     * @param pagesize  分页大小
     * @param jobGroups 执行器
     * @return 可以触发延迟的列表
     */
    List<JobInfo> triggerDelayList(@Param("offset") int offset,
                                   @Param("pagesize") int pagesize,
                                   @Param("jobGroups") List<Integer> jobGroups);

    /**
     * 可以触发延迟的列表统计
     *
     * @param offset    偏移量
     * @param pagesize  分页大小
     * @param jobGroups 执行器
     * @return 可以触发延迟的列表
     */
    int triggerDelayListCount(@Param("offset") int offset,
                              @Param("pagesize") int pagesize,
                              @Param("jobGroups") List<Integer> jobGroups);

    int save(JobInfo info);

    JobInfo loadById(@Param("id") int id);

    /**
     * 加载Job集合
     *
     * @param ides id集合
     * @return 加载Job集合
     */
    List<JobInfo> loadByIdes(@Param("ides") List<Integer> ides);

    int update(JobInfo jobInfo);

    int delete(@Param("id") long id);

    List<JobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

    int findAllCount();

    /**
     * 根据group id统计
     *
     * @param jobGroups jobGroups
     * @return 任务数
     */
    int findAllCountByGroups(@Param("jobGroups") List<Integer> jobGroups);

    List<JobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize);

    int scheduleUpdate(JobInfo jobInfo);

    /**
     * 更新jobInfo中jobGroupId为给定的alarmSeatalk
     *
     * @param jobGroupId   执行器ID
     * @param alarmSeatalk 告警seatalk
     * @return 影响行数
     */
    int updateSeatalk(@Param("jobGroupId") int jobGroupId, @Param("alarmSeatalk") String alarmSeatalk);

    /**
     * 更新jobInfo中告警静默
     *
     * @param jobId          任务ID
     * @param alarmSilenceTo 告警静默到
     * @return 影响行数
     */
    int updateAlarmSilenceTo(@Param("jobId") int jobId, @Param("alarmSilenceTo") Long alarmSilenceTo);

    /**
     * 判断job是否存在，job按照jobName和appName唯一
     *
     * @param jobName job名称
     * @param appName 执行器名称
     * @return 存在返回1否则返回0
     */
    int exist(@Param("jobName") String jobName, @Param("appName") String appName);

    /**
     * 查询Job信息，job按照jobName和appName唯一
     *
     * @param jobName job名称
     * @param appName 执行器名称
     * @return 存在返回job，不存在返回null
     */
    JobInfo queryBy(@Param("jobName") String jobName, @Param("appName") String appName);

    /**
     * 统计jobGroup对应的trigger状态数据
     *
     * @param jobGroup jobGroupId
     * @return 统计jobGroup对应的trigger状态数据
     */
    Map<String, Integer> triggerStatusCount(@Param("jobGroup") int jobGroup);

    /**
     * 统计jobGroup对应的trigger状态数据
     *
     * @param jobGroups jobGroupId
     * @return 统计jobGroup对应的trigger状态数据
     */
    Map<String, Integer> triggerStatusCountByGroups(@Param("jobGroups") List<Integer> jobGroups);

    /**
     * 判断job集合中，是否存在参数来自父任务的配置
     *
     * @param jobIds job集合
     * @return 存在返回1否则返回0
     */
    boolean existParamFromParent(@Param("jobIds") List<Integer> jobIds);

    /**
     * 获取父任务中包含parentJob的任务
     *
     * @param parentJob 父任务ID
     * @return 获取任务包含parentJob的任务
     */
    List<JobInfo> getJobsByParent(@Param("parentJob") int parentJob);

    /**
     * 获取子任务中包含childJob的任务
     *
     * @param childJob 父任务ID
     * @return 获取任务包含childJob的任务
     */
    List<JobInfo> getJobsByChild(@Param("childJob") int childJob);

    /**
     * 获取日志保存时间大于0的job
     *
     * @return 日志保存时间大于0的job
     */
    List<JobInfo> findAllRetentionGreatThanZero();

    /**
     * 获取日志保存时间等于0的job
     *
     * @return 日志保存时间等于0的job
     */
    List<JobInfo> findAllRetentionEqualZero();

    /**
     * 查询在父任务完成后开始的任务
     *
     * @return 任务列表
     */
    List<JobInfo> getJobsBeginAfterParent();

    /**
     * 查询GroupId
     *
     * @param jobId jobid
     * @return GroupId
     */
    Integer loadGroupBy(@Param("jobId") Long jobId);

    /**
     * 设置下次触发时间
     *
     * @param id              任务ID
     * @param triggerNextTime 下次触发时间
     * @return 影响行数
     */
    int triggerNextTime(@Param("id") Integer id, @Param("triggerNextTime") Long triggerNextTime);

    /**
     * 更新路由标签
     *
     * @param jobInfo 任务
     * @return 更新成功返回1
     */
    int updateRouterFlag(JobInfo jobInfo);
}
