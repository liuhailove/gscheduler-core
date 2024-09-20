package com.tc.gschedulercore.core.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * 电话告警参数
 *
 * @author honggang.liu
 */
public class DoDParam implements Serializable {

    @SerializedName("dod_type")
    private Integer dodType;
    @SerializedName("team_id")
    private Integer teamId;
    @SerializedName("time")
    private Long time;

    public static DoDParam buildDoDParam(int dodType, int teamId, Long time) {
        DoDParam telParam = new DoDParam();
        telParam.setDodType(dodType);
        telParam.setTeamId(teamId);
        telParam.setTime(time);
        return telParam;
    }

    public Integer getDodType() {
        return dodType;
    }

    public void setDodType(Integer dodType) {
        this.dodType = dodType;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}



