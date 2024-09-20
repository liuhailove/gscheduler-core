package com.tc.gschedulercore.core.dto;

import java.io.Serializable;

/**
 * @author honggang.liu 2020-04-11 22:27
 */
public class IdleBeatParam implements Serializable {
    private static final long serialVersionUID = 42L;

    public IdleBeatParam() {
    }
    public IdleBeatParam(int jobId) {
        this.jobId = jobId;
    }

    private int jobId;


    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

}