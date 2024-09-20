package com.tc.gschedulercore.enums;

/**
 * @author honggang.liu
 * @date 17/5/9
 */
public enum ExecutorBlockStrategyEnum {

    /**
     * 单机串行
     */
    SERIAL_EXECUTION("Serial execution"),
    /**
     * 单机并行
     */
    CONCURRENT_EXECUTION("Concurrent Execution"),
    /**
     * 丢弃后续任务
     */
    DISCARD_LATER("Discard Later"),
    /**
     * 覆盖前一任务
     */
    COVER_EARLY("Cover Early");

    private String title;

    ExecutorBlockStrategyEnum(String title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static ExecutorBlockStrategyEnum match(String name, ExecutorBlockStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
