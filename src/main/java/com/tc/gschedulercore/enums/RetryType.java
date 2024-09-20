package com.tc.gschedulercore.enums;

/**
 * 重试蕾西
 *
 * @author honggang.liu
 */
public class RetryType {

    /**
     * 历史类型
     */
    public static final int HISTORY_TYPE = 0;

    /**
     * 固定频率模式
     */
    public static final int FIX_RATE_TYPE = 1;

    /**
     * 用户自定义模式
     */
    public static final int CUSTOMER_TYPE = 2;

    /**
     * 指数退避模式
     */
    public static final int EXPONENTIAL_BACK_OFF_TYPE = 3;

    public static final int CRON_TYPE = 4;


}
