package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.AlarmScript;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 告警脚本Dao
 *
 * @author honggang.liu
 */
@Mapper
public interface AlarmScriptDao {

    /**
     * 保存
     *
     * @param alarmScript 告警脚本
     * @return 影响行数
     */
    int save(AlarmScript alarmScript);

    /**
     * 更新
     *
     * @param alarmScript 告警脚本
     * @return 影响行数
     */
    int update(AlarmScript alarmScript);

    int updateTriggerTimeById(@Param("id") Long id, @Param("triggerLastTime") Long lastTime, @Param("triggerNextTime") Long nextTime);

    int updateTriggerNextTime(AlarmScript alarmScript);

    /**
     * 移除规则
     *
     * @param id 规则项ID
     * @return 影响行数
     */
    int remove(@Param("id") Long id);


    /**
     * 移除规则
     *
     * @param alarmRuleId 告警规则ID
     * @return 影响行数
     */
    int removeBy(@Param("alarmRuleId") Long alarmRuleId);


    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    List<AlarmScript> pageList(@Param("offset") int offset,
                               @Param("pageSize") int pageSize,
                               @Param("alarmRuleId") Long alarmRuleId);

    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pageSize") int pageSize,
                      @Param("alarmRuleId") Long alarmRuleId);

    /**
     * 脚本ID
     *
     * @param id 主键ID
     * @return 脚本ID
     */
    AlarmScript load(@Param("id") Long id);


    /**
     * 判断脚本告警规则是否已经存在
     *
     * @param alarmRuleId 告警规则ID
     * @param scriptName  脚本名称
     * @return 存在返回true，否则返回false或者NULL
     */
    Boolean existBy(@Param("alarmRuleId") Long alarmRuleId, @Param("scriptName") String scriptName);

    /**
     * 根据规则ID删除
     *
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    List<AlarmScript> queryBy(@Param("alarmRuleId") Long alarmRuleId);

    List<AlarmScript> queryByTime(@Param("maxNextTime") Long maxNextTime, @Param("pagesize") int pageSize);
}
