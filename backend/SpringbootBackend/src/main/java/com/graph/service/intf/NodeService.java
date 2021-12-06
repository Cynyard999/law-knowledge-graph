package com.graph.service.intf;

import com.graph.vo.NodeVO;
import com.graph.vo.ResponseVO;


/**
 * 实现实体的增删改查逻辑
 *
 * @author zzy
 */
public interface NodeService {

    /**
     * 返回一定数量的实体信息
     *
     * @param num
     * @return
     */
    ResponseVO findAll(int num);

    /**
     * 根据实体类型和数量限制返回
     *
     * @param nodeType
     * @param num      数量限制
     * @return
     */
    ResponseVO findAllByType(String nodeType, int num);

    /**
     * 根据id查询实体
     *
     * @param id
     * @return
     */
    ResponseVO findNodeById(Long id);

    /**
     * 创建实体，根据name属性判断是否重复
     *
     * @param nodeVO
     * @return
     */
    ResponseVO saveNode(NodeVO nodeVO);

    /**
     * 更新节点
     *
     * @param nodeVO
     * @return
     */
    ResponseVO updateNode(NodeVO nodeVO);


    /**
     * 删除节点
     *
     * @param id
     * @return
     */
    ResponseVO deleteNode(long id);

    /**
     * 获取所有节点的标签
     *
     * @return
     */
    ResponseVO findAllLabels();

    /**
     * 获取所有节点的属性名
     *
     * @return
     */
    ResponseVO findAllNodePros();

    /**
     * 根据属性名和属性值获取节点
     *
     * @return
     */
    ResponseVO findNodeByProAndVal(NodeVO nodeVO, int num);

}
