package com.tc.gschedulercore.core.dto.workflow;

import java.util.List;

/**
 * 工作流
 *
 * @author honggang.liu
 */
public class Flow {
    /**
     * 流程图中的区域
     */
    private List<Area> areas;

    /**
     * 流程图中的线
     */
    private List<Line> lines;

    /**
     * 流程图中的节点
     */
    private List<Node> nodes;

    public List<Area> getAreas() {
        return areas;
    }

    public void setAreas(List<Area> areas) {
        this.areas = areas;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }
}
