package com.tc.gschedulercore.service.impl;


import com.tc.gschedulercore.core.model.JobUser;
import com.tc.gschedulercore.dao.JobUserDao;
import com.tc.gschedulercore.service.LoginService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户管理
 *
 * @author honggang.liu
 */
@Service
public class LoginServiceImpl implements LoginService {

    /**
     * 用户服务
     */
    @Resource
    private JobUserDao xxlJobUserDao;

    /**
     * 缓存管理器
     */
    @Resource
    private CacheManager cacheManager;

    @Override
    public List<JobUser> pageList(int offset, int pagesize, String username, String roleName) {
        return xxlJobUserDao.pageList(offset, pagesize, username, roleName);
    }

    @Override
    public int pageListCount(int offset, int pagesize, String username, String roleName) {
        return xxlJobUserDao.pageListCount(offset, pagesize, username, roleName);
    }

    @Override
    public JobUser loadByUserName(String username) {
        return xxlJobUserDao.loadByUserName(username);
    }

    /**
     * 加载auth User
     *
     * @param id 用户ID
     * @return 返回授权用户
     */
//    @Cacheable(value = "xxlJobUser", key = "'xxlJobUser-'+#id")
    @Override
    public JobUser load(int id) {
        return xxlJobUserDao.load(id);
    }

    @Override
    public int save(JobUser jobUser) {
        return xxlJobUserDao.save(jobUser);
    }

    @Override
    public int update(JobUser jobUser) {
        // 使缓存失效
        Cache xxlJobUserCache = cacheManager.getCache("xxlJobUser");
        if (xxlJobUserCache != null) {
            xxlJobUserCache.evictIfPresent("xxlJobUser-" + jobUser.getId());
        }
        return xxlJobUserDao.update(jobUser);
    }

    @Override
    public int delete(int id) {
        // 使缓存失效
        Cache xxlJobUserCache = cacheManager.getCache("xxlJobUser");
        if (xxlJobUserCache != null) {
            xxlJobUserCache.evictIfPresent("xxlJobUser-" + id);
        }
        return xxlJobUserDao.delete(id);
    }

    /**
     * 根据角色名称加载用户
     *
     * @param offset   偏移量
     * @param pagesize 分页大小
     * @param roleName 角色名称
     * @return 用户
     */
    @Override
    public List<JobUser> loadByRoleName(int offset, int pagesize, String roleName) {
        return xxlJobUserDao.loadByRoleName(offset, pagesize, roleName);
    }

    /**
     * 根据角色名称统计用户数量
     *
     * @param roleName 角色名称
     * @return 用户数量
     */
    @Override
    public int loadByRoleNameCount(String roleName) {
        return xxlJobUserDao.loadByRoleNameCount(roleName);
    }

    /**
     * 根据用户组名加载用户
     *
     * @param ugroupName 用户组名
     * @return 用户
     */
    @Override
    public List<JobUser> loadByUGroupName(String ugroupName) {
        return xxlJobUserDao.loadByUGroupName(ugroupName);
    }

}
