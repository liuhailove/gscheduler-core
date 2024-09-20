package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.AlarmScriptItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 告警脚本ItemDao
 *
 * @author honggang.liu
 */
@Mapper
public interface AlarmScriptItemDao {

    /**
     * 保存
     *
     * @param alarmScriptItem 告警脚本
     * @return 影响行数
     */
    int save(AlarmScriptItem alarmScriptItem);

    /**
     * 更新
     *
     * @param alarmScriptItem 告警脚本
     * @return 影响行数
     */
    int update(AlarmScriptItem alarmScriptItem);

    /**
     * 移除规则
     *
     * @param id 规则项ID
     * @return 影响行数
     */
    int remove(@Param("id") Long id);

    /**
     * 根据告警规则ID和jobId查告警规则脚本
     *
     * @param alarmRuleId 告警规则ID
     * @param jobId       job任务
     * @return 规则集合
     */
    List<AlarmScriptItem> queryByAlarmRuleAndJob(@Param("alarmRuleId") Long alarmRuleId, @Param("jobId") Integer jobId);

    /**
     * 根据告警规则ID和jobId查告警规则脚本
     *
     * @param alarmRuleId   告警规则ID
     * @param alarmScriptId 脚本ID
     * @param jobGroup      执行器ID
     * @param jobId         job任务
     * @return 规则集合
     */
    AlarmScriptItem queryBy(@Param("alarmRuleId") Long alarmRuleId, @Param("alarmScriptId") Long alarmScriptId, @Param("jobGroup") Integer jobGroup, @Param("jobId") Integer jobId);

    /**
     * 脚本ID
     *
     * @param id 主键ID
     * @return 脚本ID
     */
    AlarmScriptItem load(@Param("id") Long id);

    /**
     * 移除规则
     *
     * @param alarmRuleId 告警规则ID
     * @return 影响行数
     */
    int removeBy(@Param("alarmRuleId") Long alarmRuleId);

    /**
     * 根据Job删除规则
     *
     * @param alarmRuleId 告警规则
     * @param jobId       jobId
     * @return 影响行数
     */
    int removeByJob(@Param("alarmRuleId") Long alarmRuleId, @Param("jobId") Integer jobId);


    /**
     * 移除规则
     *
     * @param alarmScriptId 告警脚本ID
     * @return 影响行数
     */
    int removeByScript(@Param("alarmScriptId") Long alarmScriptId);

    /**
     * 根据告警规则ID查询全部的jobId
     *
     * @param alarmRuleId 告警规则ID
     * @return jobId
     */
    List<Integer> queryJobIdByAlarm(@Param("alarmRuleId") Long alarmRuleId);
}
