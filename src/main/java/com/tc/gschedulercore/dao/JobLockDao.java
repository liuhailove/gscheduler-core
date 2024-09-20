package com.tc.gschedulercore.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author honggang.liu
 */
@Mapper
public interface JobLockDao {

    /**
     * 加锁加载
     *
     * @param lockName 锁名称
     * @return 锁名称
     */
    String loadForUpdate(@Param("lockName") String lockName);

    /**
     * 增加锁记录
     *
     * @param lockName 锁名称
     * @return 增加是否成功
     */
    int lockNameSave(@Param("lockName") String lockName);

}
