package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobUserGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户组管理
 *
 * @author honggang.liu
 */
@Mapper
public interface JobUserGroupDao {

    List<JobUserGroup> pageList(@Param("offset") int offset,
                                @Param("pagesize") int pagesize,
                                @Param("groupName") String groupName,
                                @Param("groupNames") List<String> groupNames);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("groupName") String groupName,
                      @Param("groupNames") List<String> groupNames);

    JobUserGroup load(@Param("id") int id);

    /**
     * 根据名称，查看分组信息
     *
     * @param groupName 分组名称
     * @return 分组信息
     */
    JobUserGroup loadByUGroupName(@Param("groupName") String groupName);

    /**
     * 根据执行器名称查看分组信息
     *
     * @param appName 执行器名称
     * @return 分组信息
     */
    List<JobUserGroup> loadByAppName(@Param("appName") String appName);

    /**
     * 根据分组名称加载分组信息
     *
     * @param groupNames 分组名称
     * @return 分组信息
     */
    List<JobUserGroup> loadByNames(@Param("groupNames") List<String> groupNames);


    /**
     * 根据分组名称加载执行器
     *
     * @param groupNames 分组名称
     * @return 执行器
     */
    List<String> loadPermissionJobGroupsByNames(@Param("groupNames") List<String> groupNames);

    /**
     * 根据分组名称加载访问平台
     *
     * @param groupNames 分组名称
     * @return 访问平台
     */
    List<String> loadPermissionPlatformsByNames(@Param("groupNames") List<String> groupNames);

    /**
     * 根据平台名称加载分组
     *
     * @param platformName 平台名称
     * @return 分组
     */
    List<JobUserGroup> loadByPlatform(@Param("platformName") String platformName);

    /**
     * 判断分组中是否包含platformName
     *
     * @param platformName 平台名称
     * @return 分组
     */
    boolean existByPlatform(@Param("platformName") String platformName);

    int save(JobUserGroup jobUserGroup);

    int update(JobUserGroup jobUserGroup);

    int delete(@Param("id") int id);

}
