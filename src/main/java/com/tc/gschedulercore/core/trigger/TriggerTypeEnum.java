package com.tc.gschedulercore.core.trigger;


import com.tc.gschedulercore.core.util.I18nUtil;

/**
 * trigger type enum
 *
 * @author honggang.liu 2018-09-16 04:56:41
 */
public enum TriggerTypeEnum {

    /**
     * 手动触发
     */
    MANUAL(1, I18nUtil.getString("jobconf_trigger_type_manual")),
    /**
     * cron
     */
    CRON(2, I18nUtil.getString("jobconf_trigger_type_cron")),
    /**
     * 重试
     */
    RETRY(3, I18nUtil.getString("jobconf_trigger_type_retry")),
    /**
     * 父任务
     */
    PARENT(4, I18nUtil.getString("jobconf_trigger_type_parent")),
    /**
     * API
     */
    API(5, I18nUtil.getString("jobconf_trigger_type_api")),
    /**
     * 错失重试
     */
    MISFIRE(6, I18nUtil.getString("jobconf_trigger_type_misfire")),
    /**
     * 请求拒绝重试
     */
    REFUSED_CONNECT(7, I18nUtil.getString("jobconf_trigger_type_refused_connect")),
    /**
     * 手动重试
     */
    MANUAL_RETRY(8, I18nUtil.getString("manual_jobconf_trigger_type_retry")),
    /**
     * 系统补偿重试
     */
    SYSTEM_COMPENSATE_RETRY(9, I18nUtil.getString("system_compensate_jobconf_trigger_type_retry")),
    /**
     * 延迟任务
     */
    DelayExecute(10, "delayExecute");

    TriggerTypeEnum(int value, String title) {
        this.value = value;
        this.title = title;
    }

    /**
     * 标题
     */
    private String title;
    /**
     * 值
     */
    private int value;

    public String getTitle() {
        return title;
    }

    public int getValue() {
        return value;
    }
}
