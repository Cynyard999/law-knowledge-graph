package com.graph.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.annotation.QueryResult;

import java.util.Map;
import java.util.Set;

/**
 * 自定义节点查询结果
 * tip：返回值的名字需要与类中的成员变量名相同
 * @author zzy
 */
@QueryResult
@Getter
@Setter
public class ResultNode {

    private Long id;

    private Set<String> labels;

    private Map<String, Object> properties;

    @Override
    public String toString() {
        return "ResultNode{" +
                "id=" + id +
                ", labels=" + labels +
                ", properties=" + properties +
                '}';
    }
}
