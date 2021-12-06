package com.graph.service.impl;

import com.graph.entity.ResultNode;
import com.graph.repository.NodeRepository;
import com.graph.service.intf.NodeService;
import com.graph.vo.NodeVO;
import com.graph.vo.ResponseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 节点的业务逻辑处理实现
 *
 * @author zzy
 */
@Service
public class NodeServiceImpl implements NodeService {

    @Autowired
    private NodeRepository nodeRepository;

    private static final String DUPLICATE_NODE = "节点重复";

    private static final String NODE_NOT_EXIST = "节点不存在";

    private static final String SUCCESS_SAVE = "创建成功";

    private static final String SUCCESS_UPDATE = "更新成功";

    private static final String SUCCESS_DELETE = "删除成功";

    @Override
    public ResponseVO findAll(int num) {
        List<ResultNode> res = nodeRepository.findAllByNum(num);
        System.out.println("findAll");
        List<NodeVO> info = convertList(res);
        return ResponseVO.buildSuccess(info);
    }

    @Override
    public ResponseVO findAllByType(String nodeType, int num) {
        List<ResultNode> res = nodeRepository.findAllByType(nodeType, num);
        System.out.println("findAllByType");
        List<NodeVO> info = convertList(res);
        return ResponseVO.buildSuccess(info);
    }

    @Override
    public ResponseVO findNodeById(Long id) {
        ResultNode res = nodeRepository.findNodeById(id);
        System.out.println("findNodeById");
        NodeVO info = new NodeVO();
        BeanUtils.copyProperties(res, info);
        return ResponseVO.buildSuccess(info);
    }

    @Override
    public ResponseVO saveNode(NodeVO nodeVO) {
        Map<String, Object> pros = nodeVO.getProperties();
        String name = pros.get("名称").toString();
        // 不能够创建重名的节点
        if (nodeRepository.findByName(name) != null) {
            System.out.println("duplicate");
            return ResponseVO.buildFailure(DUPLICATE_NODE);
        }
        System.out.println("create a node: " + name);
        ResultNode res = nodeRepository.saveNode(nodeVO.getLabels(), nodeVO.getProperties());
        return ResponseVO.buildSuccess(convertNode(res));
    }

    @Override
    public ResponseVO updateNode(NodeVO nodeVO) {
        try {
            if (nodeRepository.findNodeById(nodeVO.getId()) == null) {
                System.out.println("The node does not exist");
                return ResponseVO.buildFailure(NODE_NOT_EXIST);
            } else {
                // 更新前需要判断节点名字是否重复
                String name = (String) nodeVO.getProperties().get("名称");
                System.out.println("update a node: " + name);
                ResultNode check = nodeRepository.findByName(name);
                if (check != null && !check.getId().equals(nodeVO.getId())) {
                    System.out.println("duplicate");
                    return ResponseVO.buildFailure("节点名重复");
                }
                // 根据节点id，获取节点在数据库中的名字，作为唯一标识对节点进行更新
                long id = nodeVO.getId();
                ResultNode preNode = nodeRepository.findNodeById(id);
                String identity = (String) preNode.getProperties().get("名称");
                Map<String, Object> identPros = new HashMap<>();
                identPros.put("名称", identity);
                ResultNode res = nodeRepository.updateNode(nodeVO.getLabels(), identPros, nodeVO.getProperties());
                return ResponseVO.buildSuccess(convertNode(res));
            }
        } catch (Exception e) {
            System.out.println("update error: " + e.getMessage());
            return ResponseVO.buildFailure("更新失败: " + e.getMessage());
        }
    }

    @Override
    public ResponseVO deleteNode(long id) {
        if (nodeRepository.findNodeById(id) == null) {
            System.out.println("The node does not exist");
            return ResponseVO.buildFailure(NODE_NOT_EXIST);
        } else {
            System.out.println("Delete a node. id: " + id);
            nodeRepository.deleteNodeById(id);
            return ResponseVO.buildSuccess(SUCCESS_DELETE);
        }
    }

    @Override
    public ResponseVO findAllLabels() {
        Set<String> labels = nodeRepository.findAllLabels();
        return ResponseVO.buildSuccess(labels);
    }

    @Override
    public ResponseVO findAllNodePros() {
        return ResponseVO.buildSuccess(nodeRepository.findAllNodePros());
    }


    @Override
    public ResponseVO findNodeByProAndVal(NodeVO nodeVO, int num) {
        List<ResultNode> nodes = new ArrayList<>();
        for (String key : nodeVO.getProperties().keySet()) {
            Object value = nodeVO.getProperties().get(key);
            System.out.println(key + " = " + value);
            String sql = "MATCH (n) WHERE n." + key + " = \"" + value
                    + "\" RETURN id(n) as id, labels(n) as labels, properties(n) as properties "
                    + "LIMIT " + num;
            nodes = nodeRepository.findNodeByProAndVal(sql);
        }
        return ResponseVO.buildSuccess(nodes);
    }

    /**
     * po转vo
     *
     * @param res
     * @return vo
     */
    private List<NodeVO> convertList(List<ResultNode> res) {
        List<NodeVO> info = new LinkedList<>();
        for (ResultNode node : res) {
            NodeVO nodeVO = new NodeVO();
            BeanUtils.copyProperties(node, nodeVO);
            info.add(nodeVO);
        }
        return info;
    }

    private NodeVO convertNode(ResultNode res) {
        NodeVO nodeVO = new NodeVO();
        BeanUtils.copyProperties(res, nodeVO);
        return nodeVO;
    }
}
