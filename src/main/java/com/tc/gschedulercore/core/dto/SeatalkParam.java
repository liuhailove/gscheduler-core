package com.tc.gschedulercore.core.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * seatalk请求通知参数
 *
 * @author honggang.liu
 */
public class SeatalkParam implements Serializable {
    /**
     * 固定
     */
    private String tag = "text";
    /**
     * 发送text
     */
    private Text text;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public static class Text {
        private String content;
        @SerializedName("mentioned_list")
        private List<String> mentiondList;
        @SerializedName("mentioned_email_list")
        private List<String> mentionedEmailList;
        @SerializedName("at_all")
        private boolean atAll;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isAtAll() {
            return atAll;
        }

        public void setAtAll(boolean atAll) {
            this.atAll = atAll;
        }

        public List<String> getMentiondList() {
            return mentiondList;
        }

        public void setMentiondList(List<String> mentiondList) {
            this.mentiondList = mentiondList;
        }

        public List<String> getMentionedEmailList() {
            return mentionedEmailList;
        }

        public void setMentionedEmailList(List<String> mentionedEmailList) {
            this.mentionedEmailList = mentionedEmailList;
        }
    }
}


