package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobPlatform;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 平台管理
 *
 * @author honggang.liu
 */
@Mapper
public interface JobPlatformDao {

    List<JobPlatform> pageList(@Param("offset") int offset,
                               @Param("pagesize") int pagesize,
                               @Param("platformName") String platformName,
                               @Param("env") String env,
                               @Param("region") String region,
                               @Param("platformNames") List<String> platformNames);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("platformName") String platformName,
                      @Param("env") String env,
                      @Param("region") String region,
                      @Param("platformNames") List<String> platformNames);

    JobPlatform load(@Param("id") int id);

    /**
     * 根据平台名称加载平台
     *
     * @param platformName 平台名称
     * @return 平台
     */
    JobPlatform loadByName(@Param("platformName") String platformName);

    int save(JobPlatform jobPlatform);

    int update(JobPlatform jobPlatform);

    int delete(@Param("id") int id);

}
