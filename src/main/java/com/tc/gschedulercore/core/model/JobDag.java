package com.tc.gschedulercore.core.model;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调度DAG信息
 *
 * @author honggang.liu
 */
public class JobDag implements Serializable {
    /**
     * 主键ID
     */
    private int id;

    /**
     * 图描述
     */
    private String dagDesc;

    /**
     * 包含的JOB列表
     */
    private String jobs;
    /**
     * 负责人
     */
    private String author;

    /**
     * 变更人
     */
    private String updateBy;

    /**
     * 添加时间
     */
    private Date addTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDagDesc() {
        return dagDesc;
    }

    public void setDagDesc(String dagDesc) {
        this.dagDesc = dagDesc;
    }

    public String getJobs() {
        return jobs;
    }

    public void setJobs(String jobs) {
        this.jobs = jobs;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取job列表
     *
     * @return job列表
     */
    public List<Integer> getJobList() {
        if (this.jobs == null) {
            return new ArrayList<>(0);
        }
        return Arrays.stream(StringUtils.split(this.jobs, ",")).map(Integer::parseInt).collect(Collectors.toList());
    }
}
