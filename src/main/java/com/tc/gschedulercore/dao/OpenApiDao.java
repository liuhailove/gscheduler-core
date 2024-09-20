package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.OpenApi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 开放API管理
 *
 * @author honggang.liu
 */
@Mapper
public interface OpenApiDao {
    /**
     * 分页加载
     *
     * @param offset   偏移
     * @param pagesize 分页大小
     * @return 开放API列表
     */
    List<OpenApi> pageList(@Param("offset") int offset,
                           @Param("pagesize") int pagesize);


    /**
     * 分页count
     *
     * @param offset   偏移
     * @param pagesize 分页大小
     * @return 开放API count
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize);

    /**
     * 开放API加载
     *
     * @param id 主键ID
     * @return 开放API加载
     */
    OpenApi load(@Param("id") Integer id);

    List<OpenApi> loadByIdes(@Param("ides") List<Integer> ides);


    /**
     * 开放API加载
     *
     * @param accessToken 访问token
     * @return 开放API加载
     */
    OpenApi loadByAccessToken(@Param("accessToken") String accessToken);

    /**
     * 开放API加载
     *
     * @param accessToken 访问token
     * @return 是返回true, 否则返回false
     */
    boolean existByAccessToken(@Param("accessToken") String accessToken);

    /**
     * 角色保存
     *
     * @param xxlJobRole 角色对象
     * @return 角色保存
     */
    int save(OpenApi openApi);

    /**
     * 更新
     *
     * @param openApi 更新
     * @return 更新
     */
    int update(OpenApi openApi);

    /**
     * 删除
     *
     * @param id 角色ID
     * @return 角色删除结果
     */
    int delete(@Param("id") int id);
}
