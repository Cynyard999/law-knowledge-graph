package com.graph.entity;

import lombok.Data;
import org.neo4j.ogm.annotation.*;
import java.util.Map;
import java.util.Set;

/**
 * 实体po
 * @author zzy
 */
@Data
public class BaseNode {

    @Id
    @GeneratedValue
    private Long id;

    @Labels
    private Set<String> labels;

    @Properties(prefix = "property", allowCast = true)
    private Map<String, Object> properties;

}
