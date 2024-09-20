package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobLogScript;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * job log script
 *
 * @author honggang.liu
 */
@Mapper
public interface JobLogScriptDao {

    /**
     * 根据group和主键ID查询
     *
     * @param jobGroup 分表键
     * @param id       主键ID
     * @return 执行日志
     */
    JobLogScript load(@Param("jobGroup") int jobGroup, @Param("id") long id);

    /**
     * 保存
     *
     * @param jobLogScript 脚本信息
     * @return 影响行数
     */
    long save(JobLogScript jobLogScript);

    /**
     * 查找要进行重试的配置了脚本告警log
     *
     * @param jobGroup       执行器
     * @param maxTriggerTime 最大触发时间
     * @param pagesize       分页大小
     * @return 符合要去的数据
     */
    List<Long> findScriptRetryIds(@Param("jobGroup") Integer jobGroup, @Param("maxTriggerTime") long maxTriggerTime, @Param("pagesize") int pagesize);

    /**
     * 更新脚本重试次数
     *
     * @param id              脚本日志ID
     * @param jobGroup        执行器ID
     * @param fromRetryCount  从重试次数
     * @param toRetryCount    到重试次数
     * @param nextTriggerTime 下次执行时间
     * @return 返回影响行数，如果返回值大于0，则执行成功，否则执行失败
     */
    int updateScriptRetryCount(@Param("id") long id,
                               @Param("jobGroup") Integer jobGroup,
                               @Param("fromRetryCount") int fromRetryCount,
                               @Param("toRetryCount") int toRetryCount,
                               @Param("nextTriggerTime") long nextTriggerTime);

    int updateScriptTriggerTime(@Param("id") long id,
                                @Param("jobGroup") Integer jobGroup,
                                @Param("nextTriggerTime") long nextTriggerTime);

    /**
     * 查看要清理的日志
     *
     * @param jobGroup        执行器ID
     * @param jobId           任务ID
     * @param clearBeforeTime 清理多久前的数据
     * @param clearBeforeNum  清理条数
     * @param pagesize        分页大小
     * @return 要清理的日志ID
     */
    List<Long> findClearLogIds(@Param("jobGroup") int jobGroup,
                               @Param("jobId") int jobId,
                               @Param("clearBeforeTime") Date clearBeforeTime,
                               @Param("clearBeforeNum") int clearBeforeNum,
                               @Param("pagesize") int pagesize);

    /**
     * 日志清理
     *
     * @param logScriptIds 日志脚本清理
     * @return 清理行数
     */
    int clearLog(@Param("logScriptIds") List<Long> logScriptIds);
}
