package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

public class JobLogReport implements Serializable {

    private int id;

    /**
     * jobGroup id
     */
    private Integer jobGroup;

    /**
     * 对应的应用名称
     */
    private String appname;

    /**
     * 对应的环境
     */
    private String env;

    /**
     * job任务ID
     */
    private Integer jobId;

    /**
     * job描述
     */
    private String jobDesc;

    private Date triggerDay;

    private int runningCount;
    private int sucCount;
    private int failCount;

    private int expectedCount;

    public int getExpectedCount() {
        return expectedCount;
    }

    public void setExpectedCount(int expectedCount) {
        this.expectedCount = expectedCount;
    }

    public int getActualCount() {
        return actualCount;
    }

    public void setActualCount(int actualCount) {
        this.actualCount = actualCount;
    }

    private int actualCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getTriggerDay() {
        return triggerDay;
    }

    public void setTriggerDay(Date triggerDay) {
        this.triggerDay = triggerDay;
    }

    public int getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(int runningCount) {
        this.runningCount = runningCount;
    }

    public int getSucCount() {
        return sucCount;
    }

    public void setSucCount(int sucCount) {
        this.sucCount = sucCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public Integer getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(Integer jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

}
