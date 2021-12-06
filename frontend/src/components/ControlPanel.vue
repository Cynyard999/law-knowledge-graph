<template>
  <div class="controlPanelContainer" v-show="ifShow">
    <el-card class="boxCard">
      <el-form ref="nodeForm" label-width="80px" :rules="rules" :model="nodeForm" style="margin-top: 1rem">
        <el-form-item label="节点类型" prop="nodeType">
          <el-select v-model="nodeForm.nodeType"
                     placeholder="请输入节点类型"
                     filterable
                     remote
                     reserve-keyword
                     :remote-method="loadAllNodeType"
                     :loading="nodeTypeLoading">
            >
            <el-option style="display: flex;flex-direction:column"
                       v-for="item in nodeLabelOptions"
                       :key="item"
                       :label="item"
                       :value="item">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="节点数量" prop="nodeNum">
          <el-input v-model.number="nodeForm.nodeNum"></el-input>
        </el-form-item>
      </el-form>
      <div style="margin-top: 1rem">
        <i class="el-icon-s-home resetIcon" @click="backHome"></i>
        <el-button type="primary" @click="submitForm('nodeForm')">立即查看</el-button>
        <el-button @click="resetForm('nodeForm')">清空</el-button>
      </div>
      <div style="margin-top: 1rem">
        <div style="text-align: left;font-size: 13px;color: #5a5a5a">关系标签：</div>
      <el-radio-group v-model="$store.state.showLinkName" style="margin-top: 1rem" @change="stateChanged">
        <el-radio :label="true">是</el-radio>
        <el-radio :label="false">否</el-radio>
      </el-radio-group>
      </div>
      <div style="margin-top: 1rem">
        <div style="text-align: left;font-size: 13px;color: #5a5a5a">节点文字大小:</div>
        <el-slider v-model="$store.state.nodesNameSize" input-size="medium"
                   :min="5"
                   :max="15"
                   @input="changeNodesNameSize"
        ></el-slider>
      </div>
      <div class="iconGroup">
        <div>
          <el-button v-show="hasHidden" @click="showHidden" round>显示隐藏节点</el-button>
        </div>
        <div>
          <el-button type="primary" @click=showAddNodePanel>
            添加节点
          </el-button>
          <el-button type="primary" @click=showAddRelPanel>
            添加关系
          </el-button>
        </div>
        <div class="graphMeasure">
          <p>节点数量: {{ nodescount }}</p>
          <p>关系数量: {{ relscount }}</p>
        </div>
      </div>
    </el-card>
    <node-info-panel ref="nodeInfoPanel"></node-info-panel>
    <rel-form-panel ref="relFormPanel"></rel-form-panel>
  </div>
</template>

<script>

import axios from "axios";
import NodeInfoPanel from "./NodeInfoPanel";
import RelFormPanel from "./RelFormPanel";
import * as d3 from "d3";

export default {
  name: "ControlPanel",
  components: {NodeInfoPanel, RelFormPanel},
  props: ['nodescount', 'relscount'],
  data() {
    return {
      // showLinkName:true,
      ifShow:true,
      nodeTypeLoading: false,
      isEditing: false,
      hasHidden: false,
      nodeForm: {
        nodeNum: 5,
        nodeType: ''
      },
      nodeLabelOptions: [],
      nodeLabels: [],
      rules: {
        nodeType: [
          {required: true, message: '节点类型不能为空', trigger: 'change'}
        ],
        nodeNum: [
          {required: true, message: '数量不能为空', trigger: 'blur'},
          {type: "number", min: 1, message: '请输入大于0的数字', trigger: 'blur'}
        ],
      }
    }
  },
  mounted() {
    const _this = this;
    axios.get("api/node/findAllLabels")
      .then(function (response) {
        if (response.status === 200) {
          _this.nodeLabels = response.data.content;
        }
      })
      .catch(function (error) {
        console.log(error);
      })
  },
  methods: {
    stateChanged(){
      this.$store.commit("linkNameRerender")
    },
    changeNodesNameSize(){
      d3.selectAll("g>text.nodesName").style("font-size",this.$store.state.nodesNameSize+"px")
    },
    loadAllNodeType(query) {
      if (query !== '') {
        this.nodeTypeLoading = true;
        setTimeout(() => {
          this.nodeTypeLoading = false;
          this.nodeLabelOptions = this.nodeLabels.filter(item => {
            return item.toLowerCase()
              .indexOf(query.toLowerCase()) > -1;
          });
        }, 200);
      } else {
        this.nodeLabelOptions = [];
      }
    },
    showAddNodePanel() {
      this.$refs.nodeInfoPanel.initAddNodeForm();
    },
    showAddRelPanel() {
      this.$refs.relFormPanel.initAddRelForm();
    },
    // TODO fix: 添加节点后 再选择节点类型 将会刷新掉节点
    insertNode(node) {
      this.$parent.insertNode(newNode);
    },
    submitForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.$parent.resetAll();
          this.$parent.getNodes(this.nodeForm.nodeType, this.nodeForm.nodeNum);
        } else {
          console.log('error input');
          return false;
        }
      });
    },
    // TODO 重制后不断报错
    resetForm(formName) {
      this.hasHidden = false;
      this.$refs[formName].resetFields();
      this.$parent.resetAll();
    },
    showHidden() {
      d3.selectAll("[display='none']").attr("display", "inline");
      this.hasHidden = false;
    },
    backHome(){
      this.$parent.backHome()
    }
  },
}
</script>

<style scoped>
.controlPanelContainer {
  height: 100%;
}

.boxCard {
  /*margin-top: 5rem;*/
  /*margin-right: 5px;*/
  /*margin-left: 5px;*/
  height: 100%;
  width: 100%;
  position: relative;
}

.iconGroup{
  /*margin-top: 15rem;*/
  width: 85%;
  margin: 0 auto;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-content: flex-end;
  position:absolute;
  bottom: 1rem;
}

.iconGroup div{
  margin-bottom: 1rem;
}

.graphMeasure {
  /*margin-top: 100%;*/
  margin-left: 0;
  font-size: 13px;
  text-decoration: none;
  color: #5a5a5a;
  bottom: 1rem;
  display: flex;
  justify-content: space-evenly;
}
.resetIcon{
  padding-right: 1rem;
  font-size: 20px;
  cursor: pointer;
  color: #ccc;
}
.resetIcon:hover{
  color: #999;
}

</style>
