package com.graph.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 关系vo
 * @author zzy
 */
@Getter
@Setter
public class RelVO {

    private Long id;

    private String type;

    private Map<String, Object> properties;

    private NodeVO startNode;

    private NodeVO endNode;

    public RelVO() {
    }

    public RelVO(Long id, String type, Map<String, Object> properties, NodeVO startNode, NodeVO endNode) {
        this.id = id;
        this.type = type;
        this.properties = properties;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public RelVO(String type, Map<String, Object> properties, NodeVO startNode, NodeVO endNode) {
        this.type = type;
        this.properties = properties;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    @Override
    public String toString() {
        return "RelVO{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", properties=" + properties +
                ", startNode=" + startNode.toString() +
                ", endNode=" + endNode.toString() +
                '}';
    }
}
