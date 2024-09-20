package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户管理
 *
 * @author honggang.liu
 */
@Mapper
public interface JobUserDao {

    List<JobUser> pageList(@Param("offset") int offset,
                           @Param("pagesize") int pagesize,
                           @Param("username") String username,
                           @Param("roleName") String roleName);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("username") String username,
                      @Param("roleName") String roleName);

    JobUser loadByUserName(@Param("username") String username);

    JobUser load(@Param("id") int id);

    int save(JobUser jobUser);

    int update(JobUser jobUser);

    int delete(@Param("id") int id);

//    /**
//     * 根据平台名称加载用户
//     *
//     * @param platformName 平台名称
//     * @return 用户
//     */
//    List<XxlJobUser> loadByPlatform(@Param("offset") int offset,
//                                    @Param("pagesize") int pagesize,
//                                    @Param("platformName") String platformName);
//
//    /**
//     * 根据平台名称统计用户数量
//     *
//     * @param platformName 平台名称
//     * @return 用户数量
//     */
//    int loadByPlatformCount(@Param("platformName") String platformName);

    /**
     * 根据角色名称加载用户
     *
     * @param roleName 角色名称
     * @return 用户
     */
    List<JobUser> loadByRoleName(@Param("offset") int offset,
                                 @Param("pagesize") int pagesize,
                                 @Param("roleName") String roleName);

    /**
     * 根据角色名称统计用户数量
     *
     * @param roleName 角色名称
     * @return 用户数量
     */
    int loadByRoleNameCount(@Param("roleName") String roleName);

    /**
     * 根据用户组名加载用户
     *
     * @param ugroupName 用户组名
     * @return 用户
     */
    List<JobUser> loadByUGroupName(@Param("ugroupName") String ugroupName);

}
