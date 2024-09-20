package com.tc.gschedulercore.core.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 系统配置信息
 *
 * @author honggang.liu
 */
public class SystemInfo implements Serializable {
    /**
     * 主页信息
     */
    private HomeInfo homeInfo;
    /**
     * logo信息
     */
    private LogoInfo logoInfo;

    /**
     * 菜单信息
     */
    private List<MenuInfo> menuInfo;

    /**
     * 默认homeinfo
     */
    private HomeInfo defaultHomeInfo = new HomeInfo("welcome", "首页");

    /**
     * 默认logoinfo
     */
    private LogoInfo defaultLogoInfo = new LogoInfo("", "go-scheduler", "static/img/logo.png");

    public SystemInfo(List<MenuInfo> menuInfo) {
        this.homeInfo = this.defaultHomeInfo;
        this.logoInfo = this.defaultLogoInfo;
        this.menuInfo = menuInfo;
    }

    public HomeInfo getHomeInfo() {
        return homeInfo;
    }

    public void setHomeInfo(HomeInfo homeInfo) {
        this.homeInfo = homeInfo;
    }

    public LogoInfo getLogoInfo() {
        return logoInfo;
    }

    public void setLogoInfo(LogoInfo logoInfo) {
        this.logoInfo = logoInfo;
    }

    public List<MenuInfo> getMenuInfo() {
        return menuInfo;
    }

    public void setMenuInfo(List<MenuInfo> menuInfo) {
        this.menuInfo = menuInfo;
    }
}
