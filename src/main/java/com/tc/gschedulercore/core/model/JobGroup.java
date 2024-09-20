package com.tc.gschedulercore.core.model;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by honggang.liu on 16/9/30.
 */
public class JobGroup implements Serializable {

    /**
     * 分割符
     */
    public static final String SPLIT_FLAG = "##__##";

    public static final String SCHEDULE_BASE_PFB = "schedule_base_pfb";
    private int id;
    private String appname;
    private String title;
    /**
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    private int addressType;
    private int alertNotificationWay;


    public int getDodTeamId() {
        return dodTeamId;
    }

    public void setDodTeamId(int dodTeamId) {
        this.dodTeamId = dodTeamId;
    }

    private int dodTeamId;

    /**
     * 执行器地址列表，多地址逗号分隔(手动录入)
     */
    private String addressList;
    private Date updateTime;
    /**
     * alarm告警通知
     */
    private String alarmSeatalk;
    private String blackAddressList;
    /**
     * true:online,false:offline
     */
    private boolean onlineStatus;
    /**
     * 报告接收人
     */
    private String reportReceiver;
    /**
     * 调度配置
     */
    private String scheduleConf;
    /**
     * 调度配置激活
     */
    private boolean triggerStatus;
    /**
     * 上次调度时间
     */
    private long triggerLastTime;
    /**
     * 下次调度时间
     */
    private long triggerNextTime;
    /**
     * 上次访问token
     */
    private String lastAccessToken;
    /**
     * 当前访问Token
     */
    private String currentAccessToken;
    /**
     * token生效日期
     */
    private Date tokenEffectiveDate;
    /**
     * 当前生效token (定时更新到生效token)
     */
    private String tokenEffective;
    /**
     * 所属用户分组
     */
    private String ugroups;
    /**
     * 路由标签，标签可以控制数据路由的IP地址，如果任何IP对应的标签都和设置的标签不匹配，则退化为无标签版本
     * routerFlag不可以包含逗号，api上报中的逗号(,)也会被自动替换为下划线(_)
     */
    private String routerFlag;
    /**
     * 执行器地址列表，多地址逗号分隔(手动录入)，包含路由标签，单个地址格式为ip:port##$$##routerFlag,ip:port##$$##routerFlag
     */
    private String addressListWithFlag;
    /**
     * 执行器地址列表(系统注册)
     */
    private List<String> registryList;

    public int getAlertNotificationWay() {
        return alertNotificationWay;
    }

    public void setAlertNotificationWay(int alertNotificationWay) {
        this.alertNotificationWay = alertNotificationWay;
    }

    public String getBlackAddressList() {
        return blackAddressList;
    }

    public void setBlackAddressList(String blackAddressList) {
        this.blackAddressList = blackAddressList;
    }

    public List<String> getRegistryList() {
        if (addressList != null && !addressList.trim().isEmpty()) {
            registryList = new ArrayList<>(Arrays.asList(addressList.split(",")));
        } else {
            registryList = new ArrayList<>(0);
        }
        // 遍历黑名单ip
        if (blackAddressList != null && !blackAddressList.trim().isEmpty()) {
            blackAddressList = blackAddressList.trim();
            registryList.removeAll(new ArrayList<>(Arrays.asList(blackAddressList.split(","))));
        }
        return registryList;
    }

    public void setRegistryList(List<String> registryList) {
        this.registryList = registryList;
    }

    /**
     * getRegistryWithFlagByJobFlag 获取带有标签的注册地址，
     * 如果group配置有标签，则先匹配group纬度标签，
     * 如果job纬度也有标签，则需要进一步过滤
     *
     * @param jobRouterFlag job纬度的路由标签
     * @return 匹配的路由地址
     */
    public List<String> getRegistryWithFlagByJobFlag(String jobRouterFlag) {
        //如果没有任务维度的路由标签，看group维度
        if (!StringUtils.hasLength(jobRouterFlag)) {
            return getRegistryWithFlagNewList(jobRouterFlag);
        }
        jobRouterFlag = jobRouterFlag.trim();
        // group纬度的标签列表
        List<String> registryWithGroupFlagList = getRegistryWithFlagNewList(jobRouterFlag);
        // 全部带有标签的列表
        List<String> registryWithFlagList = getRegistryWithFlagList();
        List<String> registriesFinal = new ArrayList<>();
        for (String address : registryWithGroupFlagList) {
            if (registryWithFlagList.contains(address + SPLIT_FLAG + jobRouterFlag)) {
                registriesFinal.add(address);
            }
        }
        // 如果所有IP都没有这个标签，则回退到最初的匹配规则
        if (registriesFinal.isEmpty()) {
            return registryWithGroupFlagList;
        }
        return registriesFinal;
    }

    public List<String> getRegistryWithFlagNewList(String jobRouterFlag) {
        // 标签功能值
        //insure live 配置文件有center 关键字，所以排除掉insure
        if (StringUtils.hasLength(JobAdminConfig.getAdminConfig().getEnv()) &&
                JobAdminConfig.getAdminConfig().getEnv().contains("live") &&
                !JobAdminConfig.getAdminConfig().getServerName().contains("Center")) {
            return getRegistryList();
        }

        List<String> registries = getRegistryList();
        //如果group上路由标签为空，并且任务维度或者openapi 也没有路由标签，就返回全量地址。基准也是一种pfb，不需要业务上报，调度server 自己修改的
        if (!StringUtils.hasLength(routerFlag) && !StringUtils.hasLength(jobRouterFlag)) {
            return registries;
        }
        //如果group 上的路由标签是空，但是任务维度或者openapi 有路由标签，那以后者为准。标签优先级：group>job>openapi
        if (!StringUtils.isEmpty(routerFlag)) {
            jobRouterFlag = routerFlag;
        }
        List<String> registriesNew = new ArrayList<>();
        List<String> registryWithFlagList2 = getRegistryWithFlagList();
        for (String address : registries) {
            if (registryWithFlagList2.contains(address + SPLIT_FLAG + jobRouterFlag)) {
                registriesNew.add(address);
            }
        }
        // 如果所有IP都没有这个标签，则回退到最初的匹配规则
        if (registriesNew.isEmpty()) {
            return registries;
        }
        return registriesNew;
    }

    /**
     * 获取具有标签的IP列表
     *
     * @return IP列表
     */
    public List<String> getRegistryWithFlagList() {
        // 路由标签逻辑
        // 带有标签的地址列表
        List<String> registryWithFlagList;
        if (addressListWithFlag != null && addressListWithFlag.trim().length() > 0) {
            registryWithFlagList = new ArrayList<>(Arrays.asList(addressListWithFlag.split(",")));
        } else {
            registryWithFlagList = new ArrayList<>(0);
        }
        return registryWithFlagList;
    }

    public List<String> getUgroupList() {
        if (ugroups != null && ugroups.trim().length() > 0) {
            return new ArrayList<>(Arrays.asList(ugroups.split(",")));
        }
        return new ArrayList<>(0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getAddressType() {
        return addressType;
    }

    public void setAddressType(int addressType) {
        this.addressType = addressType;
    }

    public String getAddressList() {
        return addressList;
    }

    public void setAddressList(String addressList) {
        this.addressList = addressList;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getAlarmSeatalk() {
        return alarmSeatalk;
    }

    public void setAlarmSeatalk(String alarmSeatalk) {
        this.alarmSeatalk = alarmSeatalk;
    }

    public boolean isOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(boolean onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getReportReceiver() {
        return reportReceiver;
    }

    public void setReportReceiver(String reportReceiver) {
        this.reportReceiver = reportReceiver;
    }

    public String getScheduleConf() {
        return scheduleConf;
    }

    public void setScheduleConf(String scheduleConf) {
        this.scheduleConf = scheduleConf;
    }

    public long getTriggerLastTime() {
        return triggerLastTime;
    }

    public void setTriggerLastTime(long triggerLastTime) {
        this.triggerLastTime = triggerLastTime;
    }

    public long getTriggerNextTime() {
        return triggerNextTime;
    }

    public void setTriggerNextTime(long triggerNextTime) {
        this.triggerNextTime = triggerNextTime;
    }

    public Boolean isTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(boolean triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    public String getLastAccessToken() {
        return lastAccessToken;
    }

    public void setLastAccessToken(String lastAccessToken) {
        this.lastAccessToken = lastAccessToken;
    }

    public String getCurrentAccessToken() {
        return currentAccessToken;
    }

    public void setCurrentAccessToken(String currentAccessToken) {
        this.currentAccessToken = currentAccessToken;
    }

    public Date getTokenEffectiveDate() {
        return tokenEffectiveDate;
    }

    public void setTokenEffectiveDate(Date tokenEffectiveDate) {
        this.tokenEffectiveDate = tokenEffectiveDate;
    }

    public String getTokenEffective() {
        return tokenEffective;
    }

    public void setTokenEffective(String tokenEffective) {
        this.tokenEffective = tokenEffective;
    }

    public String getUgroups() {
        return ugroups;
    }

    public void setUgroups(String ugroups) {
        this.ugroups = ugroups;
    }

    public String getRouterFlag() {
        return routerFlag;
    }

    public void setRouterFlag(String routerFlag) {
        this.routerFlag = routerFlag;
    }

    public String getAddressListWithFlag() {
        return addressListWithFlag;
    }

    public void setAddressListWithFlag(String addressListWithFlag) {
        this.addressListWithFlag = addressListWithFlag;
    }

    @Override
    public String toString() {
        return "XxlJobGroup{" +
                "id=" + id +
                ", appname='" + appname + '\'' +
                ", title='" + title + '\'' +
                ", addressType=" + addressType +
                ", addressList='" + addressList + '\'' +
                ", updateTime=" + updateTime +
                ", alarmSeatalk='" + alarmSeatalk + '\'' +
                ", blackAddressList='" + blackAddressList + '\'' +
                ", onlineStatus=" + onlineStatus +
                ", reportReceiver='" + reportReceiver + '\'' +
                ", scheduleConf='" + scheduleConf + '\'' +
                ", triggerStatus=" + triggerStatus +
                ", triggerLastTime=" + triggerLastTime +
                ", triggerNextTime=" + triggerNextTime +
                ", lastAccessToken='" + lastAccessToken + '\'' +
                ", currentAccessToken='" + currentAccessToken + '\'' +
                ", tokenEffectiveDate=" + tokenEffectiveDate +
                ", tokenEffective='" + tokenEffective + '\'' +
                ", ugroups='" + ugroups + '\'' +
                ", routerFlag='" + routerFlag + '\'' +
                ", addressListWithFlag='" + addressListWithFlag + '\'' +
                ", registryList=" + registryList +
                '}';
    }
}
