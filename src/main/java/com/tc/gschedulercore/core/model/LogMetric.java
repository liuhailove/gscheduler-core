package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 执行日志metric
 *
 * @author honggang.liu
 */
public class LogMetric implements Serializable {

    public static final String MetricPartSeparator = "BCC28710C96347CDA63B01E75762BA5B";

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 执行日志ID
     */
    private Long jobLogId;

    /**
     * jobId
     */
    private Integer jobId;

    /**
     * 上报时间戳
     */
    private Long ts;

    /**
     * 写入的消息
     */
    private String msg;

    /**
     * 关注的key1
     */
    private String key1;

    /**
     * 关注的value1
     */
    private String value1;

    /**
     * 关注的key2
     */
    private String key2;

    /**
     * 关注的value2
     */
    private String value2;

    /**
     * 关注的key3
     */
    private String key3;

    /**
     * 关注的value3
     */
    private String value3;

    /**
     * 添加时间
     */
    private Date addTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobLogId() {
        return jobLogId;
    }

    public void setJobLogId(Long jobLogId) {
        this.jobLogId = jobLogId;
    }

    public Long getTs() {
        return ts;
    }

    public void setTs(Long ts) {
        this.ts = ts;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getKey1() {
        return key1;
    }

    public void setKey1(String key1) {
        this.key1 = key1;
    }

    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public String getKey2() {
        return key2;
    }

    public void setKey2(String key2) {
        this.key2 = key2;
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    public String getKey3() {
        return key3;
    }

    public void setKey3(String key3) {
        this.key3 = key3;
    }

    public String getValue3() {
        return value3;
    }

    public void setValue3(String value3) {
        this.value3 = value3;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    /**
     * Parse {@link LogMetric} from thin string,
     *
     * @param line metric行信息
     * @return metric对象
     */
    public static LogMetric fromThinString(String line) {
        LogMetric metric = new LogMetric();
        String[] strs = line.split(MetricPartSeparator);
        metric.setTs(Long.parseLong(strs[0]));
        metric.setJobId(Integer.parseInt(strs[1]));
        metric.setJobLogId(Long.parseLong(strs[2]));
        metric.setMsg(strs[3]);
        if (strs.length >= 5) {
            metric.setKey1(strs[4]);
        }
        if (strs.length >= 6) {
            metric.setValue1(strs[5]);
        }
        if (strs.length >= 7) {
            metric.setKey2(strs[6]);
        }
        if (strs.length >= 8) {
            metric.setValue2(strs[7]);
        }
        if (strs.length >= 9) {
            metric.setKey3(strs[8]);
        }
        if (strs.length >= 10) {
            metric.setValue3(strs[9]);
        }
        return metric;
    }

    @Override
    public String toString() {
        return "LogMetric{" +
                "id=" + id +
                ", jobLogId=" + jobLogId +
                ", jobId=" + jobId +
                ", ts=" + ts +
                ", msg='" + msg + '\'' +
                ", key1='" + key1 + '\'' +
                ", value1='" + value1 + '\'' +
                ", key2='" + key2 + '\'' +
                ", value2='" + value2 + '\'' +
                ", key3='" + key3 + '\'' +
                ", value3='" + value3 + '\'' +
                ", addTime=" + addTime +
                '}';
    }
}
