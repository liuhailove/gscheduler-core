package com.tc.gschedulercore.core.dto;

import java.io.Serializable;

/**
 * 延迟执行时长上报参数
 *
 * @author honggang.liu
 */
public class ReportDelayParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 日志ID
     */
    private long logId;
    /**
     * 任务ID
     */
    private long jobId;
    /**
     * 日志生成时间
     */
    private long logDateTim;

    /**
     * sdk发送报告时对应的时间
     */
    private long currentDateTim;

    /**
     * 业务对应的地址
     */
    private String address;

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public long getLogDateTim() {
        return logDateTim;
    }

    public void setLogDateTim(long logDateTim) {
        this.logDateTim = logDateTim;
    }

    public long getCurrentDateTim() {
        return currentDateTim;
    }

    public void setCurrentDateTim(long currentDateTim) {
        this.currentDateTim = currentDateTim;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "ReportDelayParam{" +
                "logId=" + logId +
                ", jobId=" + jobId +
                ", logDateTim=" + logDateTim +
                ", currentDateTim=" + currentDateTim +
                ", address='" + address + '\'' +
                '}';
    }
}
