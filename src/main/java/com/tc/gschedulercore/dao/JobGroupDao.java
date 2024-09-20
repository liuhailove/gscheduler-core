package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Created by honggang.liu on 16/9/30.
 */
@Mapper
public interface JobGroupDao {

    List<JobGroup> findAll();

    /**
     * 查找ID集合
     *
     * @return ID集合
     */
    List<Integer> findAllId();

    /**
     * 查找下线的执行器
     *
     * @param startTime 开始时间
     * @param endTime   截止时间
     * @return 查找下线的执行器
     */
    List<JobGroup> findOffline(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<JobGroup> findByAddressType(@Param("addressType") int addressType);

    int save(JobGroup jobGroup);

    int update(JobGroup jobGroup);

    /**
     * 更新在线状态
     *
     * @param jobGroup jobgroup
     * @return 返回影响行数
     */
    int updateOnlineStatus(JobGroup jobGroup);

    int remove(@Param("id") int id);

    JobGroup load(@Param("id") int id);

    /**
     * 按照名称查找执行器
     *
     * @param appName 执行器名称
     * @return 执行器
     */
    JobGroup loadByName(@Param("appName") String appName);

    /**
     * 按照名称查找执行器ID
     *
     * @param appNames 执行器名称列表
     * @return 执行器ID
     */
    List<Integer> loadPkByNames(@Param("appNames") String[] appNames);

    /**
     * 按照名称查找执行器
     *
     * @param appNames 执行器名称列表
     * @return 执行器
     */
    List<JobGroup> loadFullByAppNames(@Param("offset") int offset, @Param("pagesize") int pagesize, @Param("appNames") String[] appNames);

    int loadFullByAppNamesCount(@Param("offset") int offset, @Param("pagesize") int pagesize, @Param("appNames") String[] appNames);


    List<JobGroup> loadByIds(@Param("ids") List<Integer> ids);

    List<JobGroup> pageList(@Param("offset") int offset,
                            @Param("pagesize") int pagesize,
                            @Param("appname") String appname,
                            @Param("title") String title,
                            @Param("ids") List<Integer> ids);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("appname") String appname,
                      @Param("title") String title,
                      @Param("ids") List<Integer> ids);

    /**
     * 更新下次的调度时间
     *
     * @param jobGroup 任务组
     * @return 更新成功返回1
     */
    int scheduleUpdate(JobGroup jobGroup);

    /**
     * 查找需要更新accessToken的执行器
     *
     * @param nowDate 当前时间
     * @return 执行期列表
     */
    List<JobGroup> findNeedUpdateAccessToken(@Param("nowDate") Date nowDate);

    /**
     * 执行器中是否包含此用户分组
     *
     * @param ugroup 用户分组
     * @return 存在返回true，否则返回false
     */
    Boolean existUGroup(@Param("ugroup") String ugroup);

    /**
     * 更新路由标签
     *
     * @param jobGroup 任务组
     * @return 更新成功返回1
     */
    int updateRouterFlag(JobGroup jobGroup);
}
