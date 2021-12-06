package com.graph.service.impl;

import com.graph.LawGraphApplication;
import com.graph.repository.NodeRepository;
import com.graph.service.intf.NodeService;
import com.graph.vo.NodeVO;
import com.graph.vo.ResponseVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.Assert.*;

@SpringBootTest(classes = LawGraphApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)

public class NodeServiceImplTest {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeRepository nodeRepository;

    private NodeVO nodeVO;

    @Before
    public void prepare() {
        Set<String> labels = new HashSet<>();
        labels.add("案件");
        Map<String, Object> properties = new HashMap<>();
        properties.put("名称", "test");
        properties.put("描述", "blabla");
        nodeVO = new NodeVO(labels, properties);
    }

    @Test
    public void findAll(){
        ResponseVO responseVO = nodeService.findAll(10);
        assertTrue(responseVO.getSuccess());
        List<NodeVO> res = (List<NodeVO>) responseVO.getContent();
        for (NodeVO node : res) {
            System.out.println(node.getId() + " " + node.getLabels() + " " + node.getProperties().get("名称"));
        }
    }

    @Test
    public void findAllByType() {
        ResponseVO responseVO = nodeService.findAllByType("案件", 10);
        assertTrue(responseVO.getSuccess());
        List<NodeVO> res = (List<NodeVO>) responseVO.getContent();
        for (NodeVO node : res) {
            System.out.println(node.getId() + " " + node.getLabels() + " " + node.getProperties().get("名称"));
        }
        ResponseVO responseVO1 = nodeService.findAllByType("罪名", 10);
        assertTrue(responseVO1.getSuccess());
        List<NodeVO> res1 = (List<NodeVO>) responseVO1.getContent();
        for (NodeVO node : res1) {
            System.out.println(node.getId() + " " + node.getLabels() + " " + node.getProperties().get("名称"));
        }
        ResponseVO responseVO2 = nodeService.findAllByType("罪类", 10);
        assertTrue(responseVO2.getSuccess());
        List<NodeVO> res2 = (List<NodeVO>) responseVO2.getContent();
        for (NodeVO node : res2) {
            System.out.println(node.getId() + " " + node.getLabels() + " " + node.getProperties().get("名称"));
        }
    }

    @Test
    public void findNodeById() {
        ResponseVO responseVO = nodeService.findNodeById(48056L);
        assertTrue(responseVO.getSuccess());
        NodeVO res = (NodeVO) responseVO.getContent();
        System.out.println(res.getId() + " " + res.getLabels() + " " + res.getProperties());
        System.out.println(res.getProperties().get("名称"));
    }

    @Test
    @Transactional // 回滚
    public void saveNode() {
        ResponseVO responseVO = nodeService.saveNode(nodeVO);
        assertTrue(responseVO.getSuccess());
        NodeVO res = (NodeVO) responseVO.getContent();
        System.out.println(res.toString());
        String name = (String) res.getProperties().get("名称");
        assertEquals("test", name);
        ResponseVO responseVO1 = nodeService.saveNode(nodeVO);
        assertFalse(responseVO1.getSuccess());
    }

    @Test
    @Transactional
    public void deleteNode() {
        ResponseVO responseVO = nodeService.deleteNode(0L);
        assertTrue(responseVO.getSuccess());
        ResponseVO responseVO1 = nodeService.deleteNode(0L);
        assertFalse(responseVO1.getSuccess());
    }

    @Test
    @Transactional
    public void updateNodeTest() {
        // 创建一个节点
        ResponseVO responseVO = nodeService.saveNode(nodeVO);
        assertTrue(responseVO.getSuccess());
        NodeVO res = (NodeVO) responseVO.getContent();
        System.out.println(res.toString());
        // 对属性进行更新
        NodeVO newNodeVO = new NodeVO();
        BeanUtils.copyProperties(res, newNodeVO);
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "test");
        properties.put("age", 25);
        properties.put("desc", "blabla");
        properties.put("job", "software engineer");
        newNodeVO.setProperties(properties);
        System.out.println(newNodeVO);
        ResponseVO responseVO1 = nodeService.updateNode(newNodeVO);
        assertTrue(responseVO1.getSuccess());
        NodeVO res1 = (NodeVO) responseVO1.getContent();
        long id = res1.getId();
        System.out.println("update:\n" + res1.toString());
        assertEquals(25L, res1.getProperties().get("age"));
        assertEquals("software engineer", res1.getProperties().get("job"));
        nodeService.deleteNode(id);
        ResponseVO responseVO2 = nodeService.updateNode(res1);
        assertFalse(responseVO2.getSuccess());
        assertEquals("节点不存在", responseVO2.getMessage());
    }

    @Test
    public void findLabelsTest() {
        ResponseVO responseVO = nodeService.findAllLabels();
        System.out.println(responseVO.getContent());
    }

    @Test
    public void findProsTest() {
        System.out.println(nodeService.findAllNodePros().getContent());
    }

    @Test
    public void findNodeByProAndVal(){
//        ResponseVO responseVO=nodeService.findNodeByProAndVal("名称","案件172",5);
//        System.out.println(responseVO.getContent());
    }

}
