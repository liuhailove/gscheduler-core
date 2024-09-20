package com.tc.gschedulercore.service.impl;

import com.tc.gschedulercore.core.model.NotifyInfo;
import com.tc.gschedulercore.dao.NotifyInfoDao;
import com.tc.gschedulercore.service.ProtectService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 防护事件实现
 *
 * @author honggang.liu
 */
@Service
public class ProtectServiceImpl implements ProtectService {

    /**
     * 通知消息DAO
     */
    @Resource
    private NotifyInfoDao notifyInfoDao;

    /**
     * 移除规则
     *
     * @param id 规则ID
     * @return 影响行数
     */
    @Override
    public int remove(Long id) {
        return notifyInfoDao.remove(id);
    }

    /**
     * 分页查找
     *
     * @param offset         偏移量
     * @param pageSize       分页大小
     * @param app            应用名称
     * @param alarmName      告警名称
     * @param alarmType      告警类型
     * @param alarmLevel     告警级别
     * @param alarmStartTime 告警触发时间段-开始时间
     * @param alarmEndTime   告警触发时间段-截止时间
     * @param permissionApps 授权应用
     * @return 记录数
     */
    @Override
    public List<NotifyInfo> pageList(int offset, int pageSize, String app, String alarmName, Integer alarmType, Integer alarmLevel, Date alarmStartTime, Date alarmEndTime, List<String> permissionApps) {
        return notifyInfoDao.pageList(offset, pageSize, app, alarmName, alarmLevel, alarmType, alarmStartTime, alarmEndTime, permissionApps);
    }

    /**
     * 分页查找
     *
     * @param offset         偏移量
     * @param pageSize       分页大小
     * @param app            应用名称
     * @param alarmName      告警名称
     * @param alarmType      告警类型
     * @param alarmLevel     告警级别
     * @param alarmStartTime 告警触发时间段-开始时间
     * @param alarmEndTime   告警触发时间段-截止时间
     * @param permissionApps 授权应用
     * @return 记录数
     */
    @Override
    public int pageListCount(int offset, int pageSize, String app, String alarmName, Integer alarmType, Integer alarmLevel, Date alarmStartTime, Date alarmEndTime, List<String> permissionApps) {
        return notifyInfoDao.pageListCount(offset, pageSize, app, alarmName, alarmLevel, alarmType, alarmStartTime, alarmEndTime, permissionApps);
    }

    /**
     * 规则ID
     *
     * @param id 主键ID
     * @return 流控规则
     */
    @Override
    public NotifyInfo load(Long id) {
        return notifyInfoDao.load(id);
    }
}
