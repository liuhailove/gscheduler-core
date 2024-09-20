package com.tc.gschedulercore.service;


import com.tc.gschedulercore.core.model.JobUser;

import java.util.List;

/**
 * 授权用户接口
 * @author honggang.liu
 */
public interface LoginService {

    List<JobUser> pageList(int offset,
                           int pagesize,
                           String username,
                           String roleName);

    int pageListCount(int offset,
                      int pagesize,
                      String username,
                      String roleName);

    JobUser loadByUserName(String username);

    JobUser load(int id);

    int save(JobUser authUser);

    int update(JobUser authUser);

    int delete(int id);

    /**
     * 根据角色名称加载用户
     *
     * @param roleName 角色名称
     * @return 用户
     */
    List<JobUser> loadByRoleName(int offset,
                                 int pagesize,
                                 String roleName);

    /**
     * 根据角色名称统计用户数量
     *
     * @param roleName 角色名称
     * @return 用户数量
     */
    int loadByRoleNameCount(String roleName);

    /**
     * 根据用户组名加载用户
     *
     * @param ugroupName 用户组名
     * @return 用户
     */
    List<JobUser> loadByUGroupName(String ugroupName);

}

