package com.tc.gschedulercore.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by honggang.liu on 16/9/30.
 */
public class JobRegistry implements Serializable {

    private int id;
    private String registryGroup;
    private String registryKey;
    private String registryValue;
    private Float cpuStat;
    private Long memoryStat;
    private Float loadStat;
    /**
     * 路由标签
     */
    private String routerFlag;
    private Date updateTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRegistryGroup() {
        return registryGroup;
    }

    public void setRegistryGroup(String registryGroup) {
        this.registryGroup = registryGroup;
    }

    public String getRegistryKey() {
        return registryKey;
    }

    public void setRegistryKey(String registryKey) {
        this.registryKey = registryKey;
    }

    public String getRegistryValue() {
        return registryValue;
    }

    public void setRegistryValue(String registryValue) {
        this.registryValue = registryValue;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Float getCpuStat() {
        return cpuStat;
    }

    public void setCpuStat(Float cpuStat) {
        this.cpuStat = cpuStat;
    }

    public Long getMemoryStat() {
        return memoryStat;
    }

    public void setMemoryStat(Long memoryStat) {
        this.memoryStat = memoryStat;
    }

    public Float getLoadStat() {
        return loadStat;
    }

    public void setLoadStat(Float loadStat) {
        this.loadStat = loadStat;
    }

    public String getRouterFlag() {
        return routerFlag;
    }

    public void setRouterFlag(String routerFlag) {
        this.routerFlag = routerFlag;
    }
}
