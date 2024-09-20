package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.ServerRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 自身服务注册DAO
 *
 * @author honggang.liu
 */
@Mapper
public interface ServerRegistryDao {

    List<Integer> findDead(@Param("deadTimeout") Date deadTimeout);

    int removeDead(@Param("ids") List<Integer> ids);

    List<ServerRegistry> findAll(@Param("deadTimeout") Date deadTimeout);

    /**
     * 注册更新
     *
     * @param serverAddress server地址
     * @param updateTime    更新时间
     * @return 影响行数
     */
    int registryUpdate(@Param("serverAddress") String serverAddress,
                       @Param("updateTime") Date updateTime);

    /**
     * 注册插入
     *
     * @param serverAddress server地址
     * @param updateTime    更新时间
     * @return 影响行数
     */
    int registrySave(@Param("serverAddress") String serverAddress,
                     @Param("updateTime") Date updateTime);

    /**
     * 服务节点删除
     *
     * @param serverAddress 服务地址
     * @return 移除结果
     */
    int registryDelete(@Param("serverAddress") String serverAddress);

    /**
     * 判断是否存在
     *
     * @param serverAddress 服务自身ip
     * @return 存在返回true, 否则返回false
     */
    int exist(@Param("serverAddress") String serverAddress);

}
