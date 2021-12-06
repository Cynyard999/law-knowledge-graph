package com.graph.repository;

import com.graph.entity.BaseNode;
import com.graph.entity.ResultNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 节点业务逻辑处理
 *
 * @author zzy
 */
@Repository
public interface NodeRepository extends Neo4jRepository<BaseNode, Long> {

    /**
     * 保存节点
     *
     * @param labels
     * @param properties
     * @return
     */
    @Query("CALL apoc.create.node($labels, $properties) yield node " +
            "RETURN id(node) as id, labels(node) as labels, properties(node) as properties")
    ResultNode saveNode(@Param("labels") Set<String> labels, @Param("properties") Map<String, Object> properties);

    /**
     * 根据id删除节点
     *
     * @param id
     */
    @Query("MATCH (n) WHERE id(n) = $id DETACH DELETE n")
    void deleteNodeById(@Param("id") long id);

    /**
     * 更新节点的属性，使用id属性作为唯一标识
     *
     * @param labels
     * @param identPros
     * @param properties
     * @return
     */
    @Query("CALL apoc.merge.node($labels, $identPros, {}, $properties) yield node " +
            "RETURN id(node) as id, labels(node) as labels, properties(node) as properties")
    ResultNode updateNode(@Param("labels") Set<String> labels, @Param("identPros") Map<String, Object> identPros, @Param("properties") Map<String, Object> properties);

    /**
     * 返回一定数量的节点
     *
     * @param limit
     * @return
     */
    @Query("MATCH (n) RETURN id(n) as id, labels(n) as labels, properties(n) as properties LIMIT $limit")
    List<ResultNode> findAllByNum(@Param("limit") int limit);

    /**
     * 查询节点列表
     * 注意标签的传参方式
     * 由于spring data neo4j的特性，会将类名也作为label传入，所以查询时按照第一个label判断
     *
     * @param label
     * @param limit
     * @return 节点列表
     */
    @Query("MATCH (n) WHERE labels(n)[0] = $label " +
            "RETURN id(n) as id, labels(n) as labels, properties(n) as properties " +
            "LIMIT $limit")
    List<ResultNode> findAllByType(@Param("label") String label, @Param("limit") int limit);

    /**
     * annoying: 使用@Properties注解，保存节点后会有前缀
     *
     * @param name
     * @return
     */
    @Query("MATCH (n) WHERE n.name = $name or n.名称 = $name " +
            "RETURN id(n) as id, labels(n) as labels, properties(n) as properties")
    ResultNode findByName(@Param("name") String name);

    /**
     * 根据id查找节点
     *
     * @param id
     * @return
     */
    @Query("MATCH (n) WHERE id(n) = $id " +
            "RETURN id(n) as id, labels(n) as labels, properties(n) as properties")
    ResultNode findNodeById(@Param("id") long id);

    /**
     * 获取标签
     *
     * @return 所有节点的标签
     */
    @Query("call db.labels()")
    Set<String> findAllLabels();

    /**
     * 获取所有节点的属性名
     *
     * @return
     */
    @Query("MATCH (n) UNWIND keys(n) as key RETURN DISTINCT key")
    Set<String> findAllNodePros();

    /**
     * 根据传入的属性名和属性值获取节点
     *
     * @return
     */
    @Query("call apoc.cypher.run({cql}, null) yield value return value.id as id, value.labels as labels, value.properties as properties")
    List<ResultNode> findNodeByProAndVal(@Param("cql") String cql);


    /**
     * 传入拼接cql查询方法
     * doIt执行写操作 run执行读操作
     *
     * @param cql
     * @return
     */
    @Query("call apoc.cypher.doIt({cql}, null) yield value return value as value")
    long apocTest(@Param("cql") String cql);

    @Query("call apoc.cypher.run({cql}, null) yield value return value as value")
    Map apocTest1(@Param("cql") String cql);

}
