package com.graph.repository;

import com.graph.entity.BaseRel;
import com.graph.entity.ResultNode;
import com.graph.entity.ResultRel;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对关系的增删改查
 * @author ltj & zzy
 */
@Repository
public interface RelationshipRepository extends Neo4jRepository<BaseRel, Long> {

    /**
     * 保存关系
     * @param startNodeId
     * @param endNodeId
     * @param relType
     * @param properties
     * @return
     */
    @Query("WITH $type as relType\n" +
            "MATCH (b) WHERE id(b) = $bid\n" +
            "MATCH (e) WHERE id(e) = $eid\n" +
            "call apoc.create.relationship(b, relType, $pros, e) YIELD rel\n" +
            "RETURN id(rel) as id, type(rel) as type, properties(rel) as properties, id(b) as startNodeId, id(e) as endNodeId")
    ResultRel saveRel(@Param("bid") long startNodeId,
                      @Param("eid") long endNodeId,
                      @Param("type") String relType,
                      @Param("pros") Map<String, Object> properties);


    /**
     * 根据关系的id直接删除关系
     * @param id
     */
    @Query("MATCH (b) - [r] -> (e) WHERE id(r) = $id DELETE r")
    void deleteRelById(@Param("id") long id);

    /**
     * 更新关系
     * @param startNodeId
     * @param endNodeId
     * @param type
     * @param properties
     * @return
     */
    @Query("MATCH (b) WHERE id(b) = $startNodeId\n" +
            "MATCH (e) WHERE id(e) = $endNodeId\n" +
            "CALL apoc.merge.relationship(b, $type, {}, {}, e, $onMatchPros)\n" +
            "YIELD rel\n" +
            "RETURN id(rel) as id, type(rel) as type, properties(rel) as properties, id(b) as startNodeId, id(e) as endNodeId")
    ResultRel updateRel(@Param("startNodeId") long startNodeId,
                        @Param("endNodeId") long endNodeId,
                        @Param("type") String type,
                        @Param("onMatchPros") Map<String, Object> properties);

    /**
     * 根据起始节点id获取它的关系
     * @param id 节点id
     * @return 关系类型字符串列表 如["Conquer", "BornIn"]
     */
    @Query("MATCH (b) - [r] - (e)\n" +
            "WHERE id(b) = $id \n" +
            "WITH DISTINCT type(r) AS r1\n" +
            "RETURN r1")
    List<String> findRelByNodeId(@Param("id") long id);

    /**
     * 根据关系和节点id查找相关节点
     * 可以用@QueryResult注解类实现接受多类型返回值
     * @param relType
     * @param nodeId
     * @return
     */
    @Query("MATCH (b) - [r] - (e)\n" +
            "WHERE id(b) = $id and type(r) = $type\n" +
            "RETURN id(r) as id, type(r) as type, properties(r) as properties, id(b) as startNodeId, id(e) as endNodeId limit 20")
    List<ResultRel> findEndNode(@Param("type") String relType, @Param("id") long nodeId);

    /**
     * 根据关系id查找实体-关系-实体(返回唯一)
     * @param relId
     * @return
     */
    @Query("MATCH (b) - [r] -> (e) \n" +
            "WHERE id(r) = $id\n" +
            "RETURN id(r) as id, type(r) as type, properties(r) as properties, id(b) as startNodeId, id(e) as endNodeId")
    ResultRel findLinkByRelId(@Param("id") Long relId);

    /**
     * 根据id返回关系类型
     * @param relId 关系id
     * @return 关系类型
     */
    @Query("MATCH ()-[r]-()\n" +
            "WHERE id(r) = $id\n" +
            "RETURN DISTINCT type(r)")
    String findRelTypeById(@Param("id") long relId);

    /**
     * 返回所有的关系类型
     * @return
     */
    @Query("call db.relationshipTypes()")
    Set<String> findAllTypes();

    /**
     * 获取所有关系的属性名
     * @return
     */
    @Query("MATCH (b) - [r] - (e) UNWIND keys(r) as key RETURN DISTINCT key")
    Set<String> findAllRelPros();

    /**
     * 根据传入的属性名和属性值获取关系
     *
     * @return
     */
    @Query("call apoc.cypher.run({cql}, null) yield value return value.id as id, value.labels as labels, value.properties as properties")
    List<ResultNode> findRelByProAndVal(@Param("cql")String cql);

}
