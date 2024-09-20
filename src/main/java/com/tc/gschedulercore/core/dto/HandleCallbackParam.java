package com.tc.gschedulercore.core.dto;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 回调参数
 * Created by honggang.liu
 */
public class HandleCallbackParam implements Serializable {
    private static final long serialVersionUID = 42L;

    private long logId;
    private Long jobId;
    private long logDateTim;

    /**
     * 客户端版本
     */
    private String clientVersion;
    /**
     * 处理结束时间
     */
    private long handleFinishDateTime;

    private int handleCode;
    private String[] handleMsg;

    public HandleCallbackParam() {
    }

    public HandleCallbackParam(long logId, Long jobId, long logDateTim, long handleFinishDateTime, int handleCode, String[] handleMsg) {
        this.logId = logId;
        this.jobId = jobId;
        this.logDateTim = logDateTim;
        this.handleFinishDateTime = handleFinishDateTime;
        this.handleCode = handleCode;
        this.handleMsg = handleMsg;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public long getLogDateTim() {
        return logDateTim;
    }

    public void setLogDateTim(long logDateTim) {
        this.logDateTim = logDateTim;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public String[] getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String[] handleMsg) {
        this.handleMsg = handleMsg;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public long getHandleFinishDateTime() {
        return handleFinishDateTime;
    }

    public void setHandleFinishDateTime(long handleFinishDateTime) {
        this.handleFinishDateTime = handleFinishDateTime;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    @Override
    public String toString() {
        return "HandleCallbackParam{" +
                "logId=" + logId +
                ", jobId=" + jobId +
                ", logDateTim=" + logDateTim +
                ", clientVersion='" + clientVersion + '\'' +
                ", handleFinishDateTime=" + handleFinishDateTime +
                ", handleCode=" + handleCode +
                ", handleMsg=" + Arrays.toString(handleMsg) +
                '}';
    }
}
