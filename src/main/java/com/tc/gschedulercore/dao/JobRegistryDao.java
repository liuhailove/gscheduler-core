package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Created by honggang.liu on 16/9/30.
 */
@Mapper
public interface JobRegistryDao {

    List<Integer> findDead(@Param("deadTimeout") Date deadTimeout);

    int removeDead(@Param("ids") List<Integer> ids);

    List<JobRegistry> findAll(@Param("deadTimeout") Date deadTimeout);

    /**
     * 根据执行器查询CPU使用率最低ip
     *
     * @param registryKey 执行器ID
     * @return CPU使用率最低ip
     */
    JobRegistry findMinCpuAddress(@Param("registryKey") String registryKey);

    /**
     * 根据执行器查询内存使用率最低ip
     *
     * @param registryKey 执行器ID
     * @return CPU使用率最低ip
     */
    JobRegistry findMinMemoryAddress(@Param("registryKey") String registryKey);

    /**
     * 根据执行器查询负载使用率最低ip
     *
     * @param registryKey 执行器ID
     * @return CPU使用率最低ip
     */
    JobRegistry findMinLoadAddress(@Param("registryKey") String registryKey);


    /**
     * 注册更新
     *
     * @param registryGroup group名称
     * @param registryKey   注册key
     * @param registryValue 注册value
     * @param cpuStat       cpu使用率
     * @param memoryStat    内存使用率
     * @param loadStat      load1使用率
     * @param routerFlag    路由标识
     * @param updateTime    更新时间
     * @return 影响行数
     */
    int registryUpdate(@Param("registryGroup") String registryGroup,
                       @Param("registryKey") String registryKey,
                       @Param("registryValue") String registryValue,
                       @Param("cpuStat") Float cpuStat,
                       @Param("memoryStat") Long memoryStat,
                       @Param("loadStat") Float loadStat,
                       @Param("routerFlag") String routerFlag,
                       @Param("updateTime") Date updateTime);

    /**
     * 注册插入
     *
     * @param registryGroup group名称
     * @param registryKey   注册key
     * @param registryValue 注册value
     * @param cpuStat       cpu使用率
     * @param memoryStat    内存使用率
     * @param loadStat      load1使用率
     * @param routerFlag    路由标识
     * @param updateTime    更新时间
     * @return 影响行数
     */
    int registrySave(@Param("registryGroup") String registryGroup,
                     @Param("registryKey") String registryKey,
                     @Param("registryValue") String registryValue,
                     @Param("cpuStat") Float cpuStat,
                     @Param("memoryStat") Long memoryStat,
                     @Param("loadStat") Float loadStat,
                     @Param("routerFlag") String routerFlag,
                     @Param("updateTime") Date updateTime);

    int registryDelete(@Param("registryGroup") String registryGroup,
                       @Param("registryKey") String registryKey,
                       @Param("registryValue") String registryValue);

    /**
     * 判断是否存在
     *
     * @param registryGroup     固定 EXECUTOR
     * @param registryValueList 执行器值
     * @return 存在返回true, 否则返回false
     */
    int exist(@Param("registryGroup") String registryGroup, @Param("registryValueList") List<String> registryValueList);

    /**
     * 加载路由标签
     *
     * @param registryKey   key
     * @return 标签集合
     */
    List<String> loadRouterFlags(@Param("registryKey") String registryKey);

}
