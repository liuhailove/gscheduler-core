package com.tc.gschedulercore.core.dto.workflow;

/**
 * 流程图中的线
 *
 * @author honggang.liu
 */
public class Line {
    /**
     * 来源节点
     */
    private String from;

    /**
     * 连接到的节点
     */
    private String to;

    /**
     * 是否标记
     */
    private boolean marked;

    /**
     * 名称
     */
    private String name;

    /**
     * type是单元类型（"node"结点,"line"转换线）,
     */
    private String type;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

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
}
