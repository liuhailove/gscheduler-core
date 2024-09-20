package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色管理
 *
 * @author honggang.liu
 */
@Mapper
public interface JobMenuDao {
    /**
     * 全部访问链接
     *
     * @return 全部访问链接
     */
    List<JobMenu> findPage(@Param("offset") int offset,
                           @Param("pagesize") int pagesize);

    /**
     * 操作count
     *
     * @return 操作count
     */
    int findAllCount();

    /**
     * 操作加载
     *
     * @param id 操作ID
     * @return 操作加载
     */
    JobMenu load(@Param("id") Integer id);

    /**
     * 操作加载
     *
     * @param name 操作名称
     * @return 操作加载
     */
    JobMenu loadByName(@Param("name") String name);

    /**
     * 菜单加载
     *
     * @param names 菜单名称
     * @return 操作加载
     */
    List<JobMenu> loads(@Param("names") List<String> names);

    /**
     * 菜单加载
     *
     * @return 操作加载
     */
    List<JobMenu> loadsAll();

    /**
     * 操作加载
     *
     * @param names  菜单名称
     * @param parent 父亲
     * @return 操作加载
     */
    List<JobMenu> loadOps(@Param("names") List<String> names, @Param("parent") String parent);

    /**
     * 操作全量加载
     *
     * @return 操作加载
     */
    List<JobMenu> loadAllOps();

    /**
     * 操作保存
     *
     * @param jobMenu 对象
     * @return 保存
     */
    int save(JobMenu jobMenu);

    /**
     * 操作更新
     *
     * @param jobMenu 操作更新
     * @return xxlJobUrl
     */
    int update(JobMenu jobMenu);

    /**
     * 操作删除
     *
     * @param id 操作ID
     * @return 删除结果
     */
    int delete(@Param("id") int id);
}
