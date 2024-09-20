package com.tc.gschedulercore.core.dto.workflow;

/**
 * 工作流中的节点
 *
 * @author honggang.liu
 */
public class Node {
    /**
     * 名称
     */
    private String name;

    /**
     * 类型，
     * "start",
     * "end",
     * "fork",
     * "join",
     * "complex",
     * "node",
     * "task",
     * "chat",
     * "state",
     * "plug"
     */
    private String type;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
