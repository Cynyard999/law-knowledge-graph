package com.graph.service.impl;

import com.graph.LawGraphApplication;
import com.graph.entity.ResultNode;
import com.graph.entity.ResultRel;
import com.graph.repository.NodeRepository;
import com.graph.repository.RelationshipRepository;
import com.graph.service.intf.RelationshipService;
import com.graph.vo.NodeVO;
import com.graph.vo.RelVO;
import com.graph.vo.ResponseVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@SpringBootTest(classes = LawGraphApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RelationshipServiceImplTest {

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private NodeRepository nodeRepository;

    private RelVO relVO;

    @Before
    public void prepare() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("key", "value");
        ResultNode startNode = nodeRepository.findNodeById(90582L);
        ResultNode endNode = nodeRepository.findNodeById(38230L);
        NodeVO startNodeVO = new NodeVO();
        NodeVO endNodeVO = new NodeVO();
        BeanUtils.copyProperties(startNode, startNodeVO);
        BeanUtils.copyProperties(endNode, endNodeVO);
        relVO = new RelVO("判为", properties, startNodeVO, endNodeVO);
        System.out.println("start test");
    }

    @Test
    @Transactional
    public void saveRel() {
        ResponseVO responseVO = relationshipService.saveRel(relVO);
        assertTrue(responseVO.getSuccess());
        RelVO res = (RelVO) responseVO.getContent();
        assertEquals("判为", res.getType());
        System.out.println(res.toString());
    }

    @Test
    @Transactional
    public void updateRel() {
        ResponseVO responseVO = relationshipService.findLinkByRelId(90436L);
        RelVO resVO = (RelVO) responseVO.getContent();
        RelVO updateRelVO = new RelVO();
        BeanUtils.copyProperties(resVO, updateRelVO);
        Map<String, Object> pros = new HashMap<>();
        pros.put("key", "value");
        pros.put("test", "test");
        updateRelVO.setProperties(pros);
        ResponseVO responseVO1 = relationshipService.updateRel(updateRelVO);
        assertTrue(responseVO1.getSuccess());
        RelVO res = (RelVO) responseVO1.getContent();
        System.out.println(res.toString());
        assertEquals("value", res.getProperties().get("key"));
        assertEquals("test", res.getProperties().get("test"));
    }

    @Test
    @Transactional
    public void deleteRel() {
        long relId = 90436L;
        ResponseVO responseVO = relationshipService.deleteRel(relId);
        assertTrue(responseVO.getSuccess());
        ResultRel res = relationshipRepository.findLinkByRelId(relId);
        assertNull(res);
    }

    @Test
    public void findRelOfNode() {
        ResponseVO responseVO = relationshipService.findRelOfNode(49731L);
        assertTrue(responseVO.getSuccess());
        List<String> relations = (List<String>) responseVO.getContent();
        System.out.println(relations);
    }

    @Test
    public void findLink() {
        ResponseVO responseVO = relationshipService.findLink("分类为", 49731L);
        assertTrue(responseVO.getSuccess());
        List<RelVO> res = (List<RelVO>) responseVO.getContent();
        for (RelVO r : res) {
            System.out.println(r.toString());
        }
        System.out.println(res.size());
    }

    @Test
    public void findLinkByRelId() {
        ResponseVO responseVO = relationshipService.findLinkByRelId(49770L);
        assertTrue(responseVO.getSuccess());
        RelVO relVO = (RelVO) responseVO.getContent();
        assertEquals("分类为", relVO.getType());
        System.out.println(relVO.toString());
    }

    @Test
    public void findRelById() {
        ResponseVO responseVO = relationshipService.findRelById(49770L);
        assertTrue(responseVO.getSuccess());
        String relType = (String) responseVO.getContent();
        assertEquals("分类为", relType);
    }

    @Test
    public void findAllTypesTest() {
        ResponseVO responseVO = relationshipService.findAllTypes();
        System.out.println(responseVO.getContent());
    }

    @Test
    public void findProsTest() {
        System.out.println(relationshipService.findAllRelPros().getContent());
    }

    @Test
    public void findNodeByProAndVal(){
        ResponseVO responseVO=relationshipService.findRelByProAndVal("名称","案件172",5);
        System.out.println(responseVO.getContent());
    }
}