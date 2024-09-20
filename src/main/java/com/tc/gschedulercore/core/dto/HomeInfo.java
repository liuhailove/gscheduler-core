package com.tc.gschedulercore.core.dto;

import java.io.Serializable;

/**
 * home信息
 *
 * @author honggang.liu
 */
public class HomeInfo implements Serializable {
    /**
     * 访问的URL
     */
    private String href;
    /**
     * 标题
     */
    private String title;

    public HomeInfo(String href, String title) {
        this.href = href;
        this.title = title;
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
}
