package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 触发告警条目
 *
 * @author honggang.liu
 */
public class AlarmItem implements Serializable {

    /**
     * 执行日志
     */
    public static final int LOG_EXECUTE_ELAPSE = 1;
    public static final int LOG_EXPECTED_NOT_MATCH_ACTUAL = 2;

    /**
     * 过程日志
     */
    public static final int LOG_PROCESS_MSG = 3;
    public static final int LOG_PROCESS_KEY1 = 4;
    public static final int LOG_PROCESS_VAL1 = 5;
    public static final int LOG_PROCESS_KEY2 = 6;
    public static final int LOG_PROCESS_VAL2 = 7;
    public static final int LOG_PROCESS_KEY3 = 8;
    public static final int LOG_PROCESS_VAL3 = 9;
    public static final int LOG_PROCESS_SCRIPT = 10;


    /**
     * 匹配模式
     */
    public static final int EXACTLY_MATCH_TYPE = 1;
    public static final int PREFIX_MATCH_TYPE = 2;
    public static final int POSTFIX_MATCH_TYPE = 3;
    public static final int CONTAIN_MATCH_TYPE = 4;
    public static final int REGEXP_MATCH_TYPE = 5;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 条目对应的规则ID
     */
    private Long alarmRuleId;

    /**
     * 资源类别(1：执行日志，2：过程日志)
     */
    private Integer resourceType;

    /**
     * 指标对应的具体值
     * <p>
     * 执行日志：
     * log.execute.elapse 日志执行耗时
     * log.expected.not.match.actual 预期与实际执行不匹配
     * <p>
     * 过程日志：
     * log.process.msg	   过程消息
     * log.process.key1	   处理消息key1
     * log.process.value1  处理消息val1
     * log.process.key2	   处理消息key2
     * log.process.value2  处理消息value2
     * log.process.key3	   处理消息key3
     * log.process.value3  处理消息value3
     * <p>
     */
    private Integer alarmType;

    /**
     * 检测周期
     * <p>
     * 对设置的触发条件进行检测的周期。单位为分钟，取值范围为1 min～60 min。
     * 假设检测周期设置为统计粒度5分钟，则意味系统会每5分钟进行一次检查，判断是否满足当前设置的告警触发条件。
     */
    private Integer checkPeriodInMin;

    /**
     * 生效类型
     * <p>
     * 所选资源类型和设定的告警阈值之间的关系
     * （1：>,2:>= 3:< 4:<=,5:==,6:!=）
     * (1:精确匹配,2:前缀匹配,3:后缀匹配，4：包含匹配，5：正则匹配）
     */
    private Integer effectType;

    /**
     * 观测值
     */
    private String observationVal;

    /**
     * 创建时间
     */
    private Date gmtCreate;
    /**
     * 更新时间
     */
    private Date gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAlarmRuleId() {
        return alarmRuleId;
    }

    public void setAlarmRuleId(Long alarmRuleId) {
        this.alarmRuleId = alarmRuleId;
    }

    public Integer getResourceType() {
        return resourceType;
    }

    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(Integer alarmType) {
        this.alarmType = alarmType;
    }

    public Integer getCheckPeriodInMin() {
        return checkPeriodInMin;
    }

    public void setCheckPeriodInMin(Integer checkPeriodInMin) {
        this.checkPeriodInMin = checkPeriodInMin;
    }

    public Integer getEffectType() {
        return effectType;
    }

    public void setEffectType(Integer effectType) {
        this.effectType = effectType;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getObservationVal() {
        return observationVal;
    }

    public void setObservationVal(String observationVal) {
        this.observationVal = observationVal;
    }

    public static String queryEffectTypeDesc(Integer effectType) {
        if (effectType == null) {
            return "KNOWN";
        }
        if (effectType == EXACTLY_MATCH_TYPE) {
            return "精确匹配";
        }
        if (effectType == PREFIX_MATCH_TYPE) {
            return "前缀匹配";
        }
        if (effectType == POSTFIX_MATCH_TYPE) {
            return "后缀匹配";
        }
        if (effectType == CONTAIN_MATCH_TYPE) {
            return "包含匹配";
        }
        if (effectType == REGEXP_MATCH_TYPE) {
            return "正则匹配";
        }
        return "KNOWN";
    }

    public static String queryAlarmTypeDesc(Integer alarmType) {
        if (alarmType == null) {
            return "KNOWN";
        }
        if (alarmType == LOG_EXECUTE_ELAPSE) {
            return "执行耗时";
        }
        if (alarmType == LOG_EXPECTED_NOT_MATCH_ACTUAL) {
            return "预期与实际执行不符";
        }
        if (alarmType == LOG_PROCESS_MSG) {
            return "过程消息";
        }
        if (alarmType == LOG_PROCESS_KEY1) {
            return "处理消息Key1";
        }
        if (alarmType == LOG_PROCESS_VAL1) {
            return "处理消息Val1";
        }
        if (alarmType == LOG_PROCESS_KEY2) {
            return "处理消息Key2";
        }
        if (alarmType == LOG_PROCESS_VAL2) {
            return "处理消息Val2";
        }
        if (alarmType == LOG_PROCESS_KEY3) {
            return "处理消息Key3";
        }
        if (alarmType == LOG_PROCESS_VAL3) {
            return "处理消息Val3";
        }
        if (alarmType == LOG_PROCESS_SCRIPT) {
            return "脚本告警";
        }
        return "KNOWN";
    }
}
