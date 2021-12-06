package com.graph.service.impl;

import com.graph.entity.ResultNode;
import com.graph.entity.ResultRel;
import com.graph.repository.NodeRepository;
import com.graph.repository.RelationshipRepository;
import com.graph.service.intf.RelationshipService;
import com.graph.vo.NodeVO;
import com.graph.vo.RelVO;
import com.graph.vo.ResponseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

/**
 * 关系业务逻辑处理实现
 * @author zzy & ltj
 */
@Service
public class RelationshipServiceImpl implements RelationshipService {

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private NodeRepository nodeRepository;


    @Override
    public ResponseVO saveRel(RelVO relVO) {
        long startNodeId = relVO.getStartNode().getId();
        long endNodeId = relVO.getEndNode().getId();
        ResultRel res = relationshipRepository.saveRel(startNodeId, endNodeId, relVO.getType(), relVO.getProperties());
        return ResponseVO.buildSuccess(convertRel(res));
    }

    @Override
    public ResponseVO updateRel(RelVO relVO) {
        long relId = relVO.getId();
        if (relationshipRepository.findLinkByRelId(relId) == null) {
            System.out.println("The relationship does not exist.");
            return ResponseVO.buildFailure("关系不存在");
        }
        // 删除后新建关系
        relationshipRepository.deleteRelById(relId);
        return saveRel(relVO);
    }

    @Override
    public ResponseVO deleteRel(long relId) {
        relationshipRepository.deleteRelById(relId);
        System.out.println("delete a rel: " + relId);
        return ResponseVO.buildSuccess("删除成功");
    }

    @Override
    public ResponseVO findRelOfNode(long nodeId) {
        List<String> relations = relationshipRepository.findRelByNodeId(nodeId);
        return ResponseVO.buildSuccess(relations);
    }

    @Override
    public ResponseVO findLink(String relType, long nodeId) {
        List<ResultRel> res = relationshipRepository.findEndNode(relType, nodeId);
        return ResponseVO.buildSuccess(convertList(res));
    }

    @Override
    public ResponseVO findLinkByRelId(Long relId) {
        ResultRel res = relationshipRepository.findLinkByRelId(relId);
        return ResponseVO.buildSuccess(convertRel(res));
    }

    @Override
    public ResponseVO findRelById(long id) {
        String rel = relationshipRepository.findRelTypeById(id);
        return ResponseVO.buildSuccess(rel);
    }

    @Override
    public ResponseVO findAllTypes() {
        return ResponseVO.buildSuccess(relationshipRepository.findAllTypes());
    }

    @Override
    public ResponseVO findAllRelPros() {
        return ResponseVO.buildSuccess(relationshipRepository.findAllRelPros());
    }

    private List<RelVO> convertList(List<ResultRel> res) {
        List<RelVO> info = new LinkedList<>();
        for (ResultRel rel : res) {
            RelVO relVO = new RelVO();
            relVO = convertRel(rel);
            info.add(relVO);
        }
        return info;
    }

    @Override
    public ResponseVO findRelByProAndVal(String pro, String val, int num) {
        String sql = "MATCH ()-[n]-() WHERE n." + pro + " = \"" + val + "\" RETURN id(n) as id, type(n) as labels, properties(n) as properties LIMIT " + num;
        System.out.println(sql);
        List<ResultNode> nodes = relationshipRepository.findRelByProAndVal(sql);
        return ResponseVO.buildSuccess(nodes);
    }

    private RelVO convertRel(ResultRel res) {
        RelVO relVO = new RelVO();
        BeanUtils.copyProperties(res, relVO);
        relVO.setStartNode(convertNode(nodeRepository.findNodeById(res.getStartNodeId())));
        relVO.setEndNode(convertNode(nodeRepository.findNodeById(res.getEndNodeId())));
        return relVO;
    }

    private NodeVO convertNode(ResultNode node) {
        NodeVO nodeVO = new NodeVO();
        BeanUtils.copyProperties(node, nodeVO);
        return nodeVO;
    }
}
