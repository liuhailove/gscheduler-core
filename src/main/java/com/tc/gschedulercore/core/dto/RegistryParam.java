package com.tc.gschedulercore.core.dto;

import java.io.Serializable;

/**
 * 业务服务注册参数
 *
 * @author honggang.liu
 */
public class RegistryParam implements Serializable {
    private static final long serialVersionUID = 42L;

    private String registryGroup;
    private String registryKey;
    private String registryValue;

    /**
     * 内存使用率
     */
    private Long memoryStat;
    /**
     * cpu使用率
     */
    private Float cpuStat;

    /**
     * load1负载
     */
    private Float loadStat;

    /**
     * 路由标识
     */
    private String routerFlag;

    public RegistryParam() {
    }

    public RegistryParam(String registryGroup, String registryKey, String registryValue) {
        this.registryGroup = registryGroup;
        this.registryKey = registryKey;
        this.registryValue = registryValue;
    }

    public RegistryParam(String registryGroup, String registryKey, String registryValue, Float cpuStat, Long memoryStat, Float loadStat) {
        this.registryGroup = registryGroup;
        this.registryKey = registryKey;
        this.registryValue = registryValue;
        this.cpuStat = cpuStat;
        this.memoryStat = memoryStat;
        this.loadStat = loadStat;
    }

    public RegistryParam(String registryGroup, String registryKey, String registryValue, Long memoryStat, Float cpuStat, Float loadStat, String routerFlag) {
        this.registryGroup = registryGroup;
        this.registryKey = registryKey;
        this.registryValue = registryValue;
        this.memoryStat = memoryStat;
        this.cpuStat = cpuStat;
        this.loadStat = loadStat;
        this.routerFlag = routerFlag;
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

    public Float getCpuStat() {
        return cpuStat;
    }

    public void setCpuStat(Float cpuStat) {
        this.cpuStat = cpuStat;
    }

    public Float getLoadStat() {
        return loadStat;
    }

    public void setLoadStat(Float loadStat) {
        this.loadStat = loadStat;
    }
    public Long getMemoryStat() {
        return memoryStat;
    }

    public void setMemoryStat(Long memoryStat) {
        this.memoryStat = memoryStat;
    }

    public String getRouterFlag() {
        return routerFlag;
    }

    public void setRouterFlag(String routerFlag) {
        this.routerFlag = routerFlag;
    }

    @Override
    public String toString() {
        return "RegistryParam{" +
                "registryGroup='" + registryGroup + '\'' +
                ", registryKey='" + registryKey + '\'' +
                ", registryValue='" + registryValue + '\'' +
                ", memoryStat=" + memoryStat +
                ", cpuStat=" + cpuStat +
                ", loadStat=" + loadStat +
                ", routerFlag='" + routerFlag + '\'' +
                '}';
    }
}
