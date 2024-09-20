package com.tc.gschedulercore.service;


import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobGroup;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobInfoHistory;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * core job action for go-scheduler
 *
 * @author honggang.liu 2016-5-28 15:30:33
 */
public interface JobService {

    /**
     * page list
     *
     * @param start
     * @param length
     * @param jobGroups
     * @param jobDesc
     * @param executorHandler
     * @param author
     * @return
     */
    Map<String, Object> pageList(int start, int length, List<Integer> jobGroups, int triggerStatus, String jobDesc, String jobName, String executorHandler, String author);

    /**
     * page list
     *
     * @param start     开始下标
     * @param length    长度
     * @param jobGroups 执行器ID
     * @return 待延迟列表
     */
    Map<String, Object> triggerDelayList(int start, int length, List<Integer> jobGroups);

    /**
     * add job
     *
     * @param jobInfo
     * @return
     */
    ReturnT<String> add(JobInfo jobInfo);

    /**
     * update job
     *
     * @param jobInfo
     * @return
     */
    ReturnT<String> update(JobInfo jobInfo);

    /**
     * remove job
     * *
     *
     * @param id
     * @return
     */
    ReturnT<String> remove(int id);

    /**
     * start job
     *
     * @param id
     * @return
     */
    ReturnT<String> start(int id, String updateBy);

    /**
     * 查询Job信息，job按照jobName和appName唯一
     *
     * @param jobName job名称
     * @param appName 执行器名称
     * @return 存在返回job，不存在返回null
     */
    ReturnT<JobInfo> queryBy(String jobName, String appName);

    /**
     * stop job
     *
     * @param id
     * @return
     */
    ReturnT<String> stop(int id, String updateBy);

    /**
     * dashboard info
     *
     * @return
     */
    Map<String, Object> dashboardInfo(List<Integer> jobGroups);

    /**
     * chart info
     *
     * @param startDate
     * @param endDate
     * @return
     */
    ReturnT<List<Map<String, Object>>> chartInfo(List<Integer> jobGroups, Date startDate, Date endDate);

    /**
     * 加载Job
     *
     * @param id 主键ID
     * @return Job
     */
    ReturnT<JobInfo> loadById(int id);

    /**
     * 加载Job
     *
     * @param ides 主键ID
     * @return Job
     */
    ReturnT<List<JobInfo>> loadByIdes(List<Integer> ides);

    /**
     * 加载Job列表
     *
     * @param jobGroup 执行器
     * @return Job列表
     */
    ReturnT<List<JobInfo>> loadByJobGroup(int jobGroup);


    /**
     * 加载Job变更历史
     *
     * @param id 主键ID
     * @return Job History
     */
    ReturnT<List<JobInfoHistory>> loadHistoryById(int id);

    /**
     * 待执行任务报表
     *
     * @param jobGroups 执行器集合
     * @return 待执行任务报表
     */
    ReturnT<List<Map<String, Object>>> nextTriggerTimeReport(List<Integer> jobGroups);

    /**
     * 此accessToken能否处理对应的任务
     *
     * @param accessToken   访问token
     * @param jobId         任务id
     * @param permissionUrl 被允许的url
     * @return 可以返回true, 否则返回false
     */
    boolean canHandleJob(String accessToken, int jobId, String permissionUrl);

    /**
     * 此accessToken能否处理对应的任务
     *
     * @param accessToken   访问token
     * @param jobGroup      任务组
     * @param jobName       job名称
     * @param permissionUrl 被允许的URL
     * @return 可以返回true, 否则返回false
     */
    boolean canHandleJobBy(String accessToken, String permissionUrl, String jobGroup, String jobName);

    /**
     * 此accessToken能否处理对应的任务
     *
     * @param accessToken 访问token
     * @param jobGroup    组ID
     * @return 可以返回true, 否则返回false
     */
    boolean canHandleJobByGroup(String accessToken, int jobGroup);

    /**
     * 设置延迟
     *
     * @param jobIdes         jobId集合
     * @param delayTimeLength 延迟时长
     * @return 操作结果
     */
    ReturnT<String> setTriggerDelay(@RequestParam(required = false) List<Integer> jobIdes, @RequestParam Long delayTimeLength);

    /**
     * 更新路由标签
     *
     * @param jobInfo 任务
     * @return 更新成功返回1
     */
    int updateRouterFlag(JobInfo jobInfo);

    /**
     * 根据JobId获取group信息
     *
     * @param jobId 任务ID
     * @return group ID
     */
    Integer loadGroupBy(Long jobId);

    /**
     * 加载被缓存的job信息
     *
     * @param id job id
     * @return job明细
     */
    JobInfo loadByIdCached(Integer id);

    /**
     * 获取缓存后的group
     * @param groupId groupId
     * @return 缓存后的group
     */
    JobGroup loadGroupCached(@Param("id") Integer groupId);

    List<Integer> findAllGroupIdCached();
}
