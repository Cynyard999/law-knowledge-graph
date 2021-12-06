<template>
  <div class="detailPanelContainer" v-show="ifShow">
    <el-card class="boxCard" v-show="ifRelShow">
      <div slot="header" style="text-align: center">
        <span style="width: auto">关系信息</span>
        <el-button style="float:right; padding: 5px 5px; margin: auto 0" size="small" type="danger" round
                   @click="deleteRel">删除
        </el-button>
        <el-button style="float:right; padding: 5px 5px; margin: auto 2px" size="small" type="primary" round
                   @click="editRel">编辑
        </el-button>
      </div>
      <div class="text item">
        <table>
          <tbody>
          <tr v-for="(v,k) in currentRel">
            <td>{{k}}</td>
            <td>
              <div class="info-line" :title="v">{{v}}</div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </el-card>
    <el-card class="boxCard" v-show="ifNodeShow">
      <div slot="header" style="text-align: center">
        <span style="width: auto">节点信息</span>
        <el-button style="float:right; padding: 5px 5px; margin: auto 0" size="small" type="danger" round
                   @click="deleteNode">删除
        </el-button>
        <el-button style="float:right; padding: 5px 5px; margin: auto 2px" size="small" type="info" round
                   @click="hideNode">隐藏
        </el-button>
        <el-button style="float:right; padding: 5px 5px; margin: auto 2px" size="small" type="primary" round
                   @click="editNode">编辑
        </el-button>
      </div>
      <div class="text item">
        <table>
          <tbody>
          <tr v-for="(v,k) in currentNode">
            <td>{{k}}</td>
            <td>
              <div class="info-line" :title="v">{{v}}</div>
            </td>
          </tr>
          </tbody>
        </table>
        <el-form ref="form" v-show="ifNodeShow">
          <el-form-item>
            <el-select v-model="currentType" placeholder="请选择查询关系" class="relation-select" no-data-text="没有以该节点为起点的关系">
              <el-option v-for="(type,i) in relationshipTypes" :label="type" :value="relationshipTypes[i]"
                         :key="type" style="display: flex;flex-direction: column">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="findRels">查询关系</el-button>
          </el-form-item>
        </el-form>
        <div>
          <span>节点形状：</span>
          <div class="shapeGroup">
            <div id="changeToRect" @click="changeToRect"></div>
            <div id="changeToCircle" @click="changeToCircle"></div>
          </div>
        </div>
        <div>
          <span style="text-align: left">节点大小：</span>
          <el-slider
            v-model="currentNodeRadius"
            :min="1"
            :max="20"
            :step="1"
            @input="changeNodeRadius"
          >
          </el-slider>
        </div>
        <div>
          <span>节点颜色：</span>
          <div class="colorfulCircleGroup">
            <div id="colorfulCircle1" @click="changeNodeColor(0)"></div>
            <div id="colorfulCircle2" @click="changeNodeColor(1)"></div>
            <div id="colorfulCircle3" @click="changeNodeColor(2)"></div>
            <div id="colorfulCircle4" @click="changeNodeColor(3)"></div>
            <div id="colorfulCircle5" @click="changeNodeColor(4)"></div>
          </div>
        </div>

      </div>
    </el-card>
    <el-drawer
      title="编辑关系信息"
      :size="450"
      :visible.sync="ifEditRelShow"
      direction="rtl">
      <el-form label-width="100px">
        <el-form-item label="关系id" style="width: 300px;margin-left: 2rem">
          <el-input v-model="relId" :disabled=true>
          </el-input>
        </el-form-item>
        <el-form-item label="关系名称" style="width: 300px;margin-left: 2rem">
          <el-input v-model="newRelName" placeholder="请输入关系名称"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updateRelBackend" style="margin-left: 0">确定</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
    <NodeInfoPanel ref="nodeInfoPanel"></NodeInfoPanel>

  </div>
</template>

<script>
  import * as d3 from 'd3'
  import axios from "axios";
  import NodeInfoPanel from "./NodeInfoPanel";

  export default {
    name: "DetailPanel",
    components: {NodeInfoPanel},
    data() {
      return {
        rules: {
          named: [{required: true, message: '请输入'}],
        },
        currentNodeId: '',
        relId: '',
        nodeValue: '',
        edit: false,
        ifShow: true,
        ifNodeShow: false,
        ifRelShow: false,
        ifEditNodeShow: false,
        ifEditRelShow: false,
        newRelName: '',
        currentNode: {},
        currentRel: {},
        returnRel: {},
        currentType: '',
        relationshipTypes: [],
        drawer: true,
        currentNodeRadius: 10,
      }
    },
    methods: {
      changeNodeRadius() {
        this.$parent.changeNodeRadius(this.currentNodeId, this.currentNodeRadius);
      },
      resetAll() {
        this.ifNodeShow = false;
        this.ifRelShow = false;
        this.ifEditNodeShow = false;
        this.ifEditRelShow = false;
        this.currentNode = {};
        this.currentRel = {};
        this.relationshipTypes = [];
        this.currentNodeId = '';
      },
      showDetail(d, radius) {
        // 点击相同节点的时候不用再请求
        if (d.id !== this.currentNodeId) {
          this.currentNodeRadius = radius * 1;
          this.currentNodeId = d.id;
          this.currentNode = JSON.parse(JSON.stringify(d));
          // 去掉stimulation添加的位置属性
          delete this.currentNode.index;
          delete this.currentNode.x;
          delete this.currentNode.y;
          delete this.currentNode.vx;
          delete this.currentNode.vy;
          delete this.currentNode.fx;
          delete this.currentNode.fy;
          this.ifNodeShow = true;
          this.ifRelShow = false;
          this.currentType = '';
          let _this = this;
          axios.get("api/relationship/" + d.id)
            .then(function (response) {
              _this.relationshipTypes = response.data.content;
            })
            .catch(function (error) {
              console.log(error)
            });
        }
      },
      showRelInfo(relation) {
        this.currentType = {};
        this.ifNodeShow = false;
        this.ifRelShow = true;
        this.relId = relation.id;
        this.currentRel.relId = relation.id;
        this.currentRel.relType = relation.type;
        this.currentRel.startNode = relation.source.name;
        this.currentRel.startNodeId = relation.source.id;
        this.currentRel.endNode = relation.target.name;
        this.currentRel.endNodeId = relation.target.id;
      },
      findRels() {
        var _this = this;
        if (_this.currentType === "") {
          _this.$message.error('未选择任何类型');
        } else {
          axios.get("api/relationship/" + _this.currentType + "/" + _this.currentNode.id + "/findLink")
            .then(function (response) {
              if (response.status === 200) {
                _this.$parent.renderLinks(response, _this.currentNode, _this.currentType)
              }
            })
            .catch(function (error) {
              console.log(error)
            })
        }
      },
      hideNode() {
        this.$parent.hideNode(this.currentNode);
        this.ifNodeShow = false;
        this.currentNodeId = '';
        this.currentNode = {};
      },
      deleteNode() {
        this.$parent.delNode(this.currentNode);
        this.ifNodeShow = false;
        this.currentNode = {};
      },
      deleteRel() {
        this.$parent.delRel(this.currentRel);
        this.ifRelShow = false;
        this.currentType = {};
      },
      editNode() {
        this.$refs.nodeInfoPanel.initEditNodeForm(this.currentNode);
      },
      updateNode(node) {
        // 同步修改到当前面板
        this.currentNode = JSON.parse(JSON.stringify(node));
        this.$parent.updateNode(node);
      },
      editRel() {
        this.ifEditRelShow = true;
      },
      generateRelForm(link) {
        let id = link.relId;
        let type = this.newRelName;
        let properties = {};
        let startNode = {
          "id": link.startNodeId,
          "labels": [],
          "properties": {}
        };
        let endNode = {
          "id": link.endNodeId,
          "labels": [],
          "properties": {}
        };
        this.returnRel.id = id;
        this.returnRel.type = type;
        this.returnRel.properties = properties;
        this.returnRel.startNode = startNode;
        this.returnRel.endNode = endNode;
      },
      updateRelBackend() {
        this.ifEditRelShow = false;
        this.generateRelForm(this.currentRel);
        let _this = this;
        axios({
          method: 'post',
          url: "api/relationship/updateRel",
          data: _this.returnRel
        }).then(function (response) {
          if (response.status === 200) {
            if (response.data.success) {
              _this.$parent.editRelFrontEnd(_this.newRelName, _this.currentRel.relId, response.data.content.id)
              alert("更新成功")
            }
          } else {
            _this.$notify({
              title: '更新失败',
              message: response.data.message,
              type: 'warning'
            });
          }
        }).catch(function (error) {
          _this.$notify({
            title: '更新失败',
            message: error,
            type: 'warning'
          });
        })
      },
      changeNodeColor(n) {
        let colorList = ["#EAEAEA", "#ccccff", "#EEC591", "#CAE1FF", "#9BCD9B"];
        d3.select("#node" + this.currentNodeId).attr("fill", colorList[n])
      },
      changeToRect() {
        let node = document.getElementById("node" + this.currentNodeId);
        if (d3.select("#node" + this.currentNodeId).attr("rx") * 1 > 0) {
          node.setAttribute("rx", 0);
        }
      },
      changeToCircle() {
        let node = document.getElementById("node" + this.currentNodeId);
        if (d3.select("#node" + this.currentNodeId).attr("rx") * 1 === 0) {
          node.setAttribute("rx", (node.getAttribute("width") * 1) / 2)
        }
      }
    }
  }
</script>

<style scoped>
  @import '../css/detailPanel.css';
</style>
