package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobLogTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色管理
 *
 * @author honggang.liu
 */
@Mapper
public interface JobLogTagDao {
    /**
     * 分页加载
     *
     * @param offset   偏移
     * @param pagesize 分页大小
     * @return 角色列表
     */
    List<JobLogTag> pageList(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("appName") String appName,
                             @Param("tagName") String tagName,
                             @Param("appNames") List<String> appNames);


    /**
     * 分页count
     *
     * @param offset   偏移
     * @param pagesize 分页大小
     * @return 角色count
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("appName") String appName,
                      @Param("tagName") String tagName,
                      @Param("appNames") List<String> appNames);

    /**
     * 日志标签加载
     *
     * @param id 标签ID
     * @return 标签加载
     */
    JobLogTag load(@Param("id") Integer id);

    /**
     * 根据执行器名称加载全部
     *
     * @param appName 执行器名称
     * @return tag
     */
    List<JobLogTag> loadAll(@Param("appName") String appName);

    /**
     * 判断标签是否存在
     *
     * @param appName 应用名称
     * @param tagName 标签名称
     * @return 存在返回true，否则返回false
     */
    Boolean exist(@Param("appName") String appName, @Param("tagName") String tagName);

    /**
     * 标签保存
     *
     * @param jobLogTag 标签对象
     * @return 标签保存
     */
    int save(JobLogTag jobLogTag);

    /**
     * 标签更新
     *
     * @param jobLogTag 标签对象
     * @return 标签更新
     */
    int update(JobLogTag jobLogTag);

    /**
     * 标签删除
     *
     * @param id 标签ID
     * @return 删除结果
     */
    int delete(@Param("id") int id);
}
