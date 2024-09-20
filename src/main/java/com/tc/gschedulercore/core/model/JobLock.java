package com.tc.gschedulercore.core.model;

import java.io.Serializable;

/**
 * @author honggang.liu
 */
public class JobLock implements Serializable {

    private String lockName;

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }
}
