package com.tc.gschedulercore.core.dto;

import java.io.Serializable;

/**
 * 手动触发信息
 *
 * @author honggang.liu
 */
public class TriggerInfo implements Serializable {
    private int id;
    private String executorParam;
    private String addressList;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getExecutorParam() {
        return executorParam;
    }

    public void setExecutorParam(String executorParam) {
        this.executorParam = executorParam;
    }

    public String getAddressList() {
        return addressList;
    }

    public void setAddressList(String addressList) {
        this.addressList = addressList;
    }
}
