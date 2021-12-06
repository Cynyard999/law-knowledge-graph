package com.graph.controller;

import com.graph.service.intf.RelationshipService;
import com.graph.vo.RelVO;
import com.graph.vo.ResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 关系控制层
 * @author zzy
 */
@RestController
@RequestMapping("/api/relationship")
public class RelationshipController {

    @Autowired
    private RelationshipService relationshipService;

    @GetMapping("/{nodeId}")
    public ResponseVO findRelOfNode(@PathVariable long nodeId) {
        return relationshipService.findRelOfNode(nodeId);
    }

    @GetMapping("/{relType}/{nodeId}/findLink")
    public ResponseVO findLink(@PathVariable String relType, @PathVariable long nodeId) {
        return relationshipService.findLink(relType, nodeId);
    }

    @GetMapping("/{relId}/findRelInfo")
    public ResponseVO findRelInfo(@PathVariable long relId) {
        return relationshipService.findLinkByRelId(relId);
    }

    @GetMapping("/{relId}/findRel")
    public ResponseVO findRelById(@PathVariable long relId) {
        return relationshipService.findRelById(relId);
    }

    @GetMapping("/findAllTypes")
    public ResponseVO findAllTypes() {
        return relationshipService.findAllTypes();
    }

    @GetMapping("/findAllRelPros")
    public ResponseVO findAllRelPros() {
        return relationshipService.findAllRelPros();
    }

    @PostMapping("/saveRel")
    public ResponseVO saveRel(@RequestBody RelVO relVO) {
        return relationshipService.saveRel(relVO);
    }

    @PostMapping("/updateRel")
    public ResponseVO updateRel(@RequestBody RelVO relVO) {
        return relationshipService.updateRel(relVO);
    }

    @DeleteMapping("/{relId}/deleteRel")
    public ResponseVO deleteRel(@PathVariable long relId) {
        return relationshipService.deleteRel(relId);
    }

    @GetMapping("/{pro}/{value}/{num}")
    public ResponseVO findRelByProAndVal(@PathVariable String pro, @PathVariable String value, @PathVariable int num) {
        return relationshipService.findRelByProAndVal(pro, value, num);
    }

}
