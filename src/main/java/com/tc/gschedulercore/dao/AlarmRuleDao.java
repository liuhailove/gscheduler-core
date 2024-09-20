package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.AlarmRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.cache.annotation.Cacheable;

import java.util.Date;
import java.util.List;

/**
 * 告警规则Dao
 *
 * @author honggang.liu
 */
@Mapper
public interface AlarmRuleDao {
    /**
     * 规则保存
     *
     * @param alarmRule 告警规则
     * @return 影响行数
     */
    int save(AlarmRule alarmRule);

    /**
     * 规则更新
     *
     * @param alarmRule 告警规则
     * @return 影响行数
     */
    int update(AlarmRule alarmRule);

    /**
     * 移除规则
     *
     * @param id 规则ID
     * @return 影响行数
     */
    int remove(@Param("id") Long id);

    /**
     * 分页查找
     *
     * @param offset         偏移量
     * @param pageSize       分页大小
     * @param resourceType   资源类别
     * @param alarmName      告警名称
     * @param alarmLevel     告警级别
     * @param permissionApps 授权应用
     * @return 记录数
     */
    List<AlarmRule> pageList(@Param("offset") int offset,
                             @Param("pageSize") int pageSize,
                             @Param("resourceType") Integer resourceType,
                             @Param("alarmLevel") Integer alarmLevel,
                             @Param("alarmName") String alarmName,
                             @Param("permissionApps") List<Integer> permissionApps);

    /**
     * 分页查找
     *
     * @param offset         偏移量
     * @param pageSize       分页大小
     * @param resourceType   资源类别
     * @param alarmName      告警名称
     * @param alarmLevel     告警级别
     * @param permissionApps 授权应用
     * @return 记录数
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pageSize") int pageSize,
                      @Param("resourceType") Integer resourceType,
                      @Param("alarmLevel") Integer alarmLevel,
                      @Param("alarmName") String alarmName,
                      @Param("permissionApps") List<Integer> permissionApps);

    /**
     * 根据应用名称查询告警规则
     *
     * @param app 应用名称
     * @return 规则集合
     */
    List<AlarmRule> queryByApp(@Param("app") String app);

    /**
     * 查找符合资源类型的告警规则
     *
     * @param resourceType 资源类型
     * @return 告警规则
     */
    List<AlarmRule> findAllByResourceType(@Param("resourceType") Integer resourceType);


    /**
     * 查找打开开关的全部规则
     *
     * @return 告警规则
     */
    @Cacheable(value = "alarmRuleCache", key = "'findAllOpenAndHasJob'")
    List<AlarmRule> findAllOpenAndHasJob();


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
    Boolean exist(@Param("app") String app);


    /**
     * 判断app对应的resource是否存在告警规则
     *
     * @param jobGroupId 执行器ID
     * @param alarmName  告警名称
     * @return 存在返回true，否则返回false或者NULL
     */
    Boolean existBy(@Param("jobGroupId") Integer jobGroupId, @Param("alarmName") String alarmName);

    /**
     * 规则ID
     *
     * @param id 主键ID
     * @return 告警规则
     */
    AlarmRule load(@Param("id") Long id);

    /**
     * 创建关联
     *
     * @param ruleId      规则ID
     * @param jobIdes     job集合
     * @param gmtModified 更新时间
     * @return 影响行数
     */
    Integer createRelation(@Param("ruleId") Long ruleId, @Param("jobIdes") String jobIdes, @Param("gmtModified") Date gmtModified);
}
