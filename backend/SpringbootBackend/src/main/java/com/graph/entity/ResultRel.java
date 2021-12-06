package com.graph.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.annotation.QueryResult;

import java.util.Map;

/**
 * 自定义关系查询结果
 * @author zzy
 */
@QueryResult
@Getter
@Setter
public class ResultRel {

    private Long id;

    private String type;

    private Map<String, Object> properties;

    private long startNodeId;

    private long endNodeId;
}
