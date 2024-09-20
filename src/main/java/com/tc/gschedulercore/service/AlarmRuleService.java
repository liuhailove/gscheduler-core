package com.tc.gschedulercore.service;


import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.AlarmItem;
import com.tc.gschedulercore.core.model.AlarmRule;
import com.tc.gschedulercore.core.model.AlarmScript;
import com.tc.gschedulercore.core.model.AlarmScriptItem;
import org.apache.ibatis.annotations.Param;

import java.text.ParseException;
import java.util.List;

/**
 * 告警规则Service
 *
 * @author honggang.liu
 */
public interface AlarmRuleService {

    /**
     * 规则保存
     *
     * @param alarmRule 告警规则
     * @return 影响行数
     */
    ReturnT<Integer> add(AlarmRule alarmRule);

    /**
     * 规则更新
     *
     * @param alarmRule 告警规则
     * @return 影响行数
     */
    ReturnT<Integer> update(AlarmRule alarmRule);

    /**
     * 移除规则
     *
     * @param id 规则ID
     * @return 影响行数
     */
    ReturnT<Integer> remove(Long id);

    /**
     * 分页查找
     *
     * @param offset       偏移量
     * @param pageSize     分页大小
     * @param resourceType 资源类别
     * @param alarmName    告警名称
     * @param alarmLevel   告警级别
     * @param jobGroups    授权应用
     * @return 记录数
     */
    List<AlarmRule> pageList(int offset,
                             int pageSize,
                             Integer resourceType,
                             Integer alarmLevel,
                             String alarmName,
                             List<Integer> jobGroups);

    /**
     * 分页查找
     *
     * @param offset       偏移量
     * @param pageSize     分页大小
     * @param resourceType 资源类型
     * @param alarmName    告警名称
     * @param alarmLevel   告警级别
     * @param jobGroups    授权应用
     * @return 记录数
     */
    int pageListCount(int offset,
                      int pageSize,
                      Integer resourceType,
                      Integer alarmLevel,
                      String alarmName,
                      List<Integer> jobGroups);

    /**
     * 根据应用名称查询告警规则
     *
     * @param app 应用名称
     * @return 规则集合
     */
    List<AlarmRule> queryByApp(String app);

    /**
     * 查询规则集合
     *
     * @param jobGroupId 执行器ID
     * @param jobId      任务ID
     * @return 规则集合
     */
    List<AlarmRule> findByJobGroupAndJobId(@Param("jobGroupId") Integer jobGroupId, @Param("jobId") Integer jobId);

    /**
     * 判断app是否存在告警规则
     *
     * @param app 应用名称
     * @return 存在返回true，否则返回false或者NULL
     */
    Boolean exist(String app);

    /**
     * 规则ID
     *
     * @param id 主键ID
     * @return 告警规则
     */
    AlarmRule load(Long id);

    /**
     * 规则保存
     *
     * @param alarmItem 告警规则条目
     * @return 影响行数
     */
    ReturnT<Integer> addItem(AlarmItem alarmItem);

    /**
     * 流控规则更新
     *
     * @param alarmItem 告警规则条目
     * @return 影响行数
     */
    ReturnT<Integer> updateItem(AlarmItem alarmItem);

    /**
     * 移除规则
     *
     * @param ruleItemId 规则项ID
     * @return 影响行数
     */
    ReturnT<Integer> removeItem(Long ruleItemId);

    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    List<AlarmItem> itemsPageList(int offset,
                                  int pageSize,
                                  Long alarmRuleId);

    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    int itemsPageListCount(int offset,
                           int pageSize,
                           Long alarmRuleId);

    /**
     * 规则条目ID
     *
     * @param ruleItemId 主键ID
     * @return 告警规则条目
     */
    AlarmItem loadItem(Long ruleItemId);

    /**
     * 关联资源集合
     *
     * @param ruleId       规则ID
     * @param resourceIdes 资源集合
     * @return 操作结果
     */
    ReturnT<Integer> createRelation(Long ruleId, Integer[] resourceIdes) throws ParseException;

    /**
     * 根据告警规则ID查告警规则条目
     *
     * @param alarmRuleId 告警规则ID
     * @return 规则集合
     */
    List<AlarmItem> queryByAlarmRule(Long alarmRuleId);

    /**
     * 加载脚本
     *
     * @param alarmScriptId 脚本ID
     * @return 告警脚本
     */
    AlarmScript loadScript(Long alarmScriptId);

    /**
     * 增加告警监控脚本
     *
     * @param alarmScript 告警监控脚本
     * @return 处理结果
     */
    ReturnT<Integer> addScript(AlarmScript alarmScript) throws ParseException;

    /**
     * 更新告警监控脚本
     *
     * @param alarmScript 告警监控脚本
     * @return 处理结果
     */
    ReturnT<Integer> updateScript(AlarmScript alarmScript) throws ParseException;

    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    List<AlarmScript> scriptsPageList(int offset,
                                      int pageSize,
                                      Long alarmRuleId);

    /**
     * 分页查找
     *
     * @param offset      偏移量
     * @param pageSize    分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    int scriptsPageListCount(int offset,
                             int pageSize,
                             Long alarmRuleId);

    /**
     * 移除脚本
     *
     * @param id 规则ID
     * @return 影响行数
     */
    ReturnT<Integer> removeScript(Long id);

    /**
     * 查询script item
     *
     * @param alarmRuleId 规则ID
     * @param jobId       任务ID
     * @return 列表信息
     */
    List<AlarmScriptItem> queryItemsByAlarmRuleAndJob(Long alarmRuleId, Integer jobId);
}
