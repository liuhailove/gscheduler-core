package com.tc.gschedulercore.core.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 电话告警参数
 *
 * @author honggang.liu
 */
public class TelParam implements Serializable {

    @SerializedName("task_id")
    private Integer taskId;

    @SerializedName("voice_info")
    private VoiceInfo voiceInfo;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public VoiceInfo getVoiceInfo() {
        return voiceInfo;
    }

    public void setVoiceInfo(VoiceInfo voiceInfo) {
        this.voiceInfo = voiceInfo;
    }

    public static class VoiceInfo {

        @SerializedName("phone_number")
        private String phoneNumber;

        @SerializedName("language")
        private String language = "zh-Hans";

        @SerializedName("placeholders")
        private List<PlaceHolder> placeHolders;

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public List<PlaceHolder> getPlaceHolders() {
            return placeHolders;
        }

        public void setPlaceHolders(List<PlaceHolder> placeHolders) {
            this.placeHolders = placeHolders;
        }
    }

    public static class PlaceHolder {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setVale(String value) {
            this.value = value;
        }
    }

    public static TelParam buildTelParam(int taskid, String phoneNumber, String env, String jobName) {
        TelParam telParam = new TelParam();
        telParam.setTaskId(taskid);
        VoiceInfo voiceInfo = new VoiceInfo();
        voiceInfo.setPhoneNumber(phoneNumber);
        voiceInfo.setLanguage("zh-Hans");
        PlaceHolder placeHolderRegion = new PlaceHolder();
        placeHolderRegion.setKey("TaskRegion");
        placeHolderRegion.setVale(env);

        PlaceHolder placeHolderTask = new PlaceHolder();
        placeHolderTask.setKey("TaskName");
        placeHolderTask.setVale(jobName);
        voiceInfo.setPlaceHolders(new ArrayList<>());
        voiceInfo.getPlaceHolders().add(placeHolderRegion);
        voiceInfo.getPlaceHolders().add(placeHolderTask);
        telParam.setVoiceInfo(voiceInfo);
        return telParam;
    }
}



