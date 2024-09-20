package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.NotifyInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 通知信息Dao
 *
 * @author honggang.liu
 */
@Mapper
public interface NotifyInfoDao {
    /**
     * 保存
     *
     * @param notifyInfo 通知信息
     * @return 影响行数
     */
    int save(NotifyInfo notifyInfo);

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
     * @param app            应用名称
     * @param alarmName      告警名称
     * @param alarmLevel     告警级别
     * @param alarmType      告警类型
     * @param alarmStartTime 告警触发时间段-开始时间
     * @param alarmEndTime   告警触发时间段-截止时间
     * @param permissionApps 授权应用
     * @return 记录数
     */
    List<NotifyInfo> pageList(@Param("offset") int offset,
                              @Param("pageSize") int pageSize,
                              @Param("app") String app,
                              @Param("alarmName") String alarmName,
                              @Param("alarmLevel") Integer alarmLevel,
                              @Param("alarmType") Integer alarmType,
                              @Param("alarmStartTime") Date alarmStartTime,
                              @Param("alarmEndTime") Date alarmEndTime,
                              @Param("permissionApps") List<String> permissionApps);

    /**
     * 分页查找
     *
     * @param offset         偏移量
     * @param pageSize       分页大小
     * @param app            应用名称
     * @param alarmName      告警名称
     * @param alarmLevel     告警级别
     * @param alarmType      告警类型
     * @param alarmStartTime 告警触发时间段-开始时间
     * @param alarmEndTime   告警触发时间段-截止时间
     * @param permissionApps 授权应用
     * @return 记录数
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pageSize") int pageSize,
                      @Param("app") String app,
                      @Param("alarmName") String alarmName,
                      @Param("alarmLevel") Integer alarmLevel,
                      @Param("alarmType") Integer alarmType,
                      @Param("alarmStartTime") Date alarmStartTime,
                      @Param("alarmEndTime") Date alarmEndTime,
                      @Param("permissionApps") List<String> permissionApps);

    /**
     * 规则ID
     *
     * @param id 主键ID
     * @return 流控规则
     */
    NotifyInfo load(@Param("id") Long id);

}
