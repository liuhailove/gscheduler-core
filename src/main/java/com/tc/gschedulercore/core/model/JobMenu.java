package com.tc.gschedulercore.core.model;

import java.io.Serializable;

/**
 * url model
 *
 * @author honggang.liu
 */
public class JobMenu implements Serializable {
    /**
     * 主键ID
     */
    private int id;
    /**
     * 角色名称，只允许英文
     */
    private String menuName;
    /**
     * 访问的URL
     */
    private String href;
    /**
     * 标题
     */
    private String title;
    /**
     * 图标
     */
    private String icon;

    /**
     * 图片
     */
    private String image;

    /**
     * target
     */
    private String target;

    /**
     * 父亲name
     */
    private String parent;

    /**
     * 是否为操作
     */
    private boolean operator;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public boolean isOperator() {
        return operator;
    }

    public void setOperator(boolean operator) {
        this.operator = operator;
    }

    public JobMenu() {
    }

    public JobMenu(String title, String href) {
        this.href = href;
        this.title = title;
    }

    public JobMenu(int id, String name, String href, String title, String icon, String image, String target, String parent, boolean operator) {
        this.id = id;
        this.menuName = name;
        this.href = href;
        this.title = title;
        this.icon = icon;
        this.image = image;
        this.target = target;
        this.parent = parent;
        this.operator = operator;
    }
}
