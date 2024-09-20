package com.tc.gschedulercore.core.dto;

import java.io.Serializable;

/**
 * logo信息
 *
 * @author honggang.liu
 */
public class LogoInfo implements Serializable {
    /**
     * 访问的URL
     */
    private String href;
    /**
     * 标题
     */
    private String title;
    /**
     * 图片
     */
    private String image;

    public LogoInfo(String href, String title, String image) {
        this.href = href;
        this.title = title;
        this.image = image;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
