package com.tc.gschedulercore.core.dto.workflow;

/**
 * 图中的一个区域快
 *
 * @author honggang.liu
 */
public class Area {
    /**
     * 颜色
     */
    private String color;

    /**
     * 高
     */
    private int height;

    /**
     * 宽度
     */
    private int width;

    /**
     * 距左侧
     */
    private int left;

    /**
     * 距上测
     */
    private int top;

    /**
     * 名称
     */
    private String name;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
