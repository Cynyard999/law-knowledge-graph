package com.graph.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/**
 * 节点vo
 * @author zzy
 */
@Getter
@Setter
public class NodeVO {

    private Long id;

    private Set<String> labels;

    private Map<String, Object> properties;

    /**
     * cannot deserialize from Object value (no delegate- or property-based Creator
     * 反序列化报错
     * Json反序列化需要一个无参构造器，正常情况下JVM会默认给类加个无参构造器
     * 但是如果手动添加了有参数的构造器，那么JVM就不会给类添加无参构造器，造成报错，需要手动添加一个无参构造器
     */
    public NodeVO() {
    }

    public NodeVO(long id, Set<String> labels, Map<String, Object> properties) {
        this.id = id;
        this.labels = labels;
        this.properties = properties;
    }

    public NodeVO(Set<String> labels, Map<String, Object> properties) {
        this.labels = labels;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "NodeVO{" +
                "id=" + id +
                ", labels=" + labels +
                ", properties=" + properties +
                '}';
    }
}
