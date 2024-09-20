package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.AlarmItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 告警规则条目Dao
 *
 * @author honggang.liu
 */
@Mapper
public interface AlarmItemDao {
    /**
     * 规则保存
     *
     * @param alarmItem 告警规则条目
     * @return 影响行数
     */
    int save(AlarmItem alarmItem);

    /**
     * 规则更新
     *
     * @param alarmItem 告警规则条目
     * @return 影响行数
     */
    int update(AlarmItem alarmItem);

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
     * @param alarmRuleId 规则ID
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
    List<AlarmItem> pageList(@Param("offset") int offset,
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
     * 根据告警规则ID查告警规则条目
     *
     * @param alarmRuleId 告警规则ID
     * @return 规则集合
     */
    List<AlarmItem> queryByAlarmRule(@Param("alarmRuleId") Long alarmRuleId);

    /**
     * 规则条目ID
     *
     * @param id 主键ID
     * @return 告警规则条目
     */
    AlarmItem load(@Param("id") Long id);

    /**
     * 判断app对应的resource是否存在告警规则
     *
     * @param alarmRuleId  告警规则ID
     * @param resourceType 资源类型
     * @param alarmType    告警类型
     * @return 存在返回true，否则返回false或者NULL
     */
    Boolean existBy(@Param("alarmRuleId") Long alarmRuleId, @Param("resourceType") Integer resourceType, @Param("alarmType") Integer alarmType);

}
