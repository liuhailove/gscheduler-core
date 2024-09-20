package com.tc.gschedulercore.dao;

import com.tc.gschedulercore.core.model.JobLogReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * job log
 *
 * @author honggang.liu 2019-11-22
 */
@Mapper
public interface JobLogReportDao {

    int save(JobLogReport jobLogReport);

    int update(JobLogReport jobLogReport);

    List<JobLogReport> queryLogReport(@Param("jobGroup") Integer jobGroup, @Param("triggerDayFrom") Date triggerDayFrom,
                                      @Param("triggerDayTo") Date triggerDayTo);

    JobLogReport queryLogReportTotal();

    /**
     *  根据groups统计
     * @param jobGroupList group列表
     * @return 统计
     */
    JobLogReport queryLogReportTotalByGroups(@Param("jobGroupList") List<Integer> jobGroupList);

}
