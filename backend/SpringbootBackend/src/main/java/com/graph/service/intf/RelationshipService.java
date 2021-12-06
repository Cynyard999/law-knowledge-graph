package com.graph.service.intf;

import com.graph.vo.RelVO;
import com.graph.vo.ResponseVO;

/**
 * 对关系的增删改查
 * @author zzy
 */
public interface RelationshipService {

    /**
     * 在两个节点中创建关系
     * @param relVO
     * @return 提示信息：创建是否成功
     */
    ResponseVO saveRel(RelVO relVO);

    /**
     * 更新关系指的是更新关系的type或者方向
     * @param relVO
     * @return
     */
    ResponseVO updateRel(RelVO relVO);

    /**
     * 删除关系
     * @param relId
     * @return 提示信息：删除是否成
     */
    ResponseVO deleteRel(long relId);

    /**
     * 根据节点id查找它拥有的关系 返回关系字符串列表
     * @param nodeId
     * @return List<String>
     */
    ResponseVO findRelOfNode(long nodeId);

    /**
     * 根据节点id + 关系种类查询 实体-关系-实体的3元组
     * @param nodeId
     * @param relType
     * @return List<RelVO>
     */
    ResponseVO findLink(String relType, long nodeId);

    /**
     * 根据关系id查找模式
     * @param relId
     * @return relVO
     */
    ResponseVO findLinkByRelId(Long relId);

    /**
     * 根据id查看关系信息
     * @param id
     * @return
     */
    ResponseVO findRelById(long id);

    /**
     * 返回所有的关系类型
     * @return
     */
    ResponseVO findAllTypes();

    /**
     * 获取所有的关系属性名
     * @return
     */
    ResponseVO findAllRelPros();

    /**
     * 根据属性名和属性值获取关系
     *
     * @return
     */
    ResponseVO findRelByProAndVal(String pro, String val, int num);


}
