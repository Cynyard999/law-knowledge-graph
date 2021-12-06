package com.graph.entity;

import lombok.Data;
import org.neo4j.ogm.annotation.*;

import java.util.Map;

/**
 * 关系po
 * @author zzy
 */
@Data
public class BaseRel {

    @Id
    @GeneratedValue
    private Long id;

    private String type;

    private Map<String, Object> properties;
}
