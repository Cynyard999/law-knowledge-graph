package com.graph.controller;

import com.graph.service.intf.NodeService;
import com.graph.vo.NodeVO;
import com.graph.vo.ResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 实体控制层
 *
 * @author zzy
 */
@RestController
@RequestMapping("/api/node")
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @GetMapping("/findAll/{num}")
    public ResponseVO findAll(@PathVariable int num) {
        return nodeService.findAll(num);
    }

    @GetMapping("/{nodeType}/{num}")
    public ResponseVO findAllByType(@PathVariable String nodeType, @PathVariable int num) {
        return nodeService.findAllByType(nodeType, num);
    }

    @GetMapping("{id}")
    public ResponseVO findNodeById(@PathVariable Long id) {
        return nodeService.findNodeById(id);
    }

    @GetMapping("/findAllLabels")
    public ResponseVO findAllLabels() {
        return nodeService.findAllLabels();
    }

    @GetMapping("/findAllNodePros")
    public ResponseVO findAllNodePros() {
        return nodeService.findAllNodePros();
    }

    @PostMapping("/saveNode")
    public ResponseVO saveNode(@RequestBody NodeVO nodeVO) {
        return nodeService.saveNode(nodeVO);
    }

    @PostMapping("/updateNode")
    public ResponseVO updateNode(@RequestBody NodeVO nodeVO) {
        return nodeService.updateNode(nodeVO);
    }

    @DeleteMapping("{id}/deleteNode")
    public ResponseVO deleteNode(@PathVariable long id) {
        return nodeService.deleteNode(id);
    }

    @PostMapping("/searchNode/{num}")
    public ResponseVO findNodeByProAndVal(@RequestBody NodeVO nodeVO, @PathVariable int num) {
        return nodeService.findNodeByProAndVal(nodeVO, num);
    }
}
