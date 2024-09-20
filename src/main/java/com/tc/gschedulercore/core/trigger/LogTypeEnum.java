package com.tc.gschedulercore.core.trigger;

import com.tc.gschedulercore.core.util.I18nUtil;

/**
 * logger type enum
 *
 * @author honggang.liu
 */
public enum LogTypeEnum {

    /**
     * 主日志
     */
    MAIN_LOG(0, I18nUtil.getString("jobconf_log_type_main_log")),
    /**
     * 子日志
     */
    SUB_LOG(1, I18nUtil.getString("jobconf_log_type_sub_log"));

    LogTypeEnum(int value, String title) {
        this.value = value;
        this.title = title;
    }

    /**
     * 标识
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
