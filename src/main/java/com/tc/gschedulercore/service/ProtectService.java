package com.tc.gschedulercore.service;



import com.tc.gschedulercore.core.model.NotifyInfo;

import java.util.Date;
import java.util.List;

/**
 * 防护消息服务
 *
 * @author honggang.liu
 */
public interface ProtectService {

    /**
     * 移除规则
     *
     * @param id 规则ID
     * @return 影响行数
     */
    int remove(Long id);

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
    List<NotifyInfo> pageList(int offset,
                              int pageSize,
                              String app,
                              String alarmName,
                              Integer alarmType,
                              Integer alarmLevel,
                              Date alarmStartTime,
                              Date alarmEndTime,
                              List<String> permissionApps);

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
    int pageListCount(int offset,
                      int pageSize,
                      String app,
                      String alarmName,
                      Integer alarmType,
                      Integer alarmLevel,
                      Date alarmStartTime,
                      Date alarmEndTime,
                      List<String> permissionApps);

    /**
     * 规则ID
     *
     * @param id 主键ID
     * @return 流控规则
     */
    NotifyInfo load(Long id);
}
