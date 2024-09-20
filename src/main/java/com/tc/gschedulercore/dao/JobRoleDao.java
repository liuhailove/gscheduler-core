package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色管理
 *
 * @author honggang.liu
 */
@Mapper
public interface JobRoleDao {
    /**
     * 分页加载
     *
     * @param offset   偏移
     * @param pagesize 分页大小
     * @return 角色列表
     */
    List<JobRole> pageList(@Param("offset") int offset,
                           @Param("pagesize") int pagesize);


    /**
     * 分页count
     *
     * @param offset   偏移
     * @param pagesize 分页大小
     * @return 角色count
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize);

    /**
     * 角色加载
     *
     * @param id 角色ID
     * @return 角色加载
     */
    JobRole load(@Param("id") Integer id);

    /**
     * 角色加载
     *
     * @param roleName 角色名称
     * @return 角色加载
     */
    JobRole loadByName(@Param("roleName") String roleName);

    /**
     * 角色加载
     *
     * @param menuName 菜单名称
     * @return 角色加载
     */
    List<JobRole> loadByMenu(@Param("menuName") String menuName);

    /**
     * 角色中是否包含此菜单
     *
     * @param menuName 菜单名称
     * @return 是返回true,否则返回false
     */
    boolean existByMenu(@Param("menuName") String menuName);

    /**
     * 角色保存
     *
     * @param jobRole 角色对象
     * @return 角色保存
     */
    int save(JobRole jobRole);

    /**
     * 角色更新
     *
     * @param jobRole 角色更新
     * @return 角色更新
     */
    int update(JobRole jobRole);

    /**
     * 角色删除
     *
     * @param id 角色ID
     * @return 角色删除结果
     */
    int delete(@Param("id") int id);
}
