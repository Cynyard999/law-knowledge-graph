<template>
  <div>
    <el-drawer
      title="编辑节点"
      :size="450"
      :visible.sync="drawer">
      <el-form ref="nodeForm" style="text-align: left;" label-width="10px">
        <el-form-item
        >
          <el-input
            style="width: 120px"
            size="small"
            value="节点类型"
            disabled
          >
          </el-input>
          <el-autocomplete
            v-model="newNodeType"
            placeholder="查看推荐类型"
            style="width: auto;"
            :fetch-suggestions="labelSearch"
            size="small"
            @select="selectLabel"
          >
          </el-autocomplete>
        </el-form-item>
        <el-form-item>
          <el-input
            value="节点名称"
            style="width: 120px;"
            size="small"
            disabled
          >
          </el-input>
          <el-input
            placeholder="输入节点名"
            v-model="newNodeName"
            style="width: auto"
            size="small"
          >
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input
            value="节点ID"
            style="width: 120px;"
            size="small"
            disabled
          >
          </el-input>
          <el-input
            v-model="newNodeId"
            style="width: auto"
            size="small"
            disabled
          >
          </el-input>
        </el-form-item>

        <el-form-item
          v-for="(property, index) in nodeForm"
          :key="index"
        >
          <el-autocomplete
            v-model="property.key"
            placeholder="查看推荐属性"
            style="width: 120px;"
            :fetch-suggestions="propertySearch"
            :disabled=!property.keyAlterable
            size="small"
            @select="selectProperty"
          >
          </el-autocomplete>
          <el-input
            placeholder="请输入内容"
            v-model="property.value"
            style="width: auto"
            size="small"
          >
          </el-input>
          <el-button type="text" size="small" @click.prevent="removeProperty(property)">
            删除
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="addProperty">添加属性</el-button>
        </el-form-item>
        <el-form-item v-if="isEditing">
          <el-button type="primary" @click="submitEditing()">确定</el-button>
          <el-button @click="cancelEditing()">取消</el-button>
        </el-form-item>
        <el-form-item v-else>
          <el-button type="primary" @click="submitAdding()">确定</el-button>
          <el-button @click="cancelAdding()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script>
  import axios from "axios";
  // TODO 节点信息的新显示洁面
  export default {
    name: "NodeInfoPanel",
    data() {
      return {
        newNodeType: '',
        newNodeName: '',
        newNodeId: '系统自动生成',
        isEditing: false,
        drawer: false,
        nodeLabels: [],
        nodeProperties: [],
        nodeForm: [],
      }
    },
    mounted() {
      this.loadNodeLabels();
      this.loadNodeProperties();
    },
    methods: {
      loadNodeLabels() {
        let _this = this;
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
      loadNodeProperties() {
        let _this = this;
        axios.get("api/node/findAllNodePros")
          .then(function (response) {
            if (response.status === 200) {
              _this.nodeProperties = response.data.content;
            }
          })
          .catch(function (error) {
            console.log(error);
          })
      },
      propertySearch(queryString, cb) {
        // 先筛选去除id,name,nodeType
        let props = this.nodeProperties.filter(function (item, index) {
          return !(item === 'id' || item === '名称' || item === '节点类型');
        }).map(function (item, index) {
          let property = {};
          property["value"] = item;
          property["key"] = item;
          return property;
        });
        // 返回的数据需要是对象数组
        let results = queryString ? props.filter(this.createPropertyFilter(queryString)) : props;
        // 调用 callback 返回建议列表的数据
        cb(results);
      },
      createPropertyFilter(queryString) {
        return (property) => {
          return (property.value.toLowerCase().indexOf(queryString.toLowerCase()) === 0);
        };
      },
      labelSearch(queryString, cb) {
        let labels = this.nodeLabels.map(function (item, index) {
          let label = {};
          label["value"] = item;
          label["key"] = item;
          return label;
        });
        let results = queryString ? labels.filter(this.createLabelFilter(queryString)) : labels;
        cb(results);
      },
      createLabelFilter(queryString) {
        return (label) => {
          return (label.value.toLowerCase().indexOf(queryString.toLowerCase()) === 0);
        };
      },
      selectProperty(item) {

      },
      selectLabel(item) {

      },
      initEditNodeForm(node) {
        // 初始化已填字段
        this.nodeForm = [];
        let temp = JSON.parse(JSON.stringify(node));
        for (let propertyName in temp) {
          // 删除空属性
          if (temp.hasOwnProperty(propertyName) && temp[propertyName] === '') {
            delete temp[propertyName];
          } else if (temp.hasOwnProperty(propertyName) && propertyName === '节点类型') {
            this.newNodeType = temp[propertyName];
          } else if (temp.hasOwnProperty(propertyName) && propertyName === 'id') {
            this.newNodeId = temp[propertyName];
          } else if (temp.hasOwnProperty(propertyName) && propertyName === '名称') {
            this.newNodeName = temp[propertyName];
          } else {
            this.nodeForm.push({
              key: propertyName,
              value: temp[propertyName],
              keyAlterable: false,
            })
          }
        }
        this.isEditing = true;
        this.drawer = true;
      },
      initAddNodeForm() {
        this.isEditing = false;
        this.drawer = true;
      },
      addProperty() {
        this.nodeForm.push({
          key: '',
          value: '',
          keyAlterable: true,
        });
      },
      removeProperty(item) {
        let index = this.nodeForm.indexOf(item);
        if (index !== -1) {
          this.nodeForm.splice(index, 1)
        }
      },
      addNodeFrontEnd(newNode) {
        this.$parent.insertNode(newNode);
      },
      editNodeFrontEnd(newNode) {
        this.$parent.updateNode(newNode);
      },
      checkNodeFormValidate() {
        if (this.newNodeName === '') {
          this.$notify({
            title: '编辑失败',
            message: '请输入节点名称',
            type: 'warning'
          });
          return false;
        }
        if (this.newNodeType === '') {
          this.$notify({
            title: '编辑失败',
            message: '请输入节点类型',
            type: 'warning'
          });
          return false;
        }
        for (let i = 0, len = this.nodeForm.length; i < len; i++) {
          if (this.nodeForm[i].key === '') {
            this.$notify({
              title: '编辑失败',
              message: '不能有空的属性名',
              type: 'warning'
            });
            return false;
          }
          if (this.nodeForm[i].value === '') {
            this.$notify({
              title: '编辑失败',
              message: '不能有空的属性值',
              type: 'warning'
            });
            return false;
          }
          if (this.nodeForm[i].key.toLowerCase() === 'id' || this.nodeForm[i].key.toLowerCase() === '名称' || this.nodeForm[i].key.toLowerCase() === '节点类型') {
            this.$notify({
              title: '编辑失败',
              message: '请勿添加固有属性: ' + this.nodeForm[i].key,
              type: 'warning'
            });
            return false;
          }
        }
        for (let i = 0; i < this.nodeForm.length - 1; i++) {
          for (let j = i + 1; j < this.nodeForm.length; j++) {
            if (this.nodeForm[i].key.toLowerCase() === this.nodeForm[j].key.toLowerCase()) {
              this.$notify({
                title: '编辑失败',
                message: this.nodeForm[i].key + '属性重复',
                type: 'warning'
              });
              return false;
            }
          }
        }
        return true;
      },
      generateNodeFromNodeForm() {
        let returnNode = {};
        let properties = {};
        // 传入label属性
        let labels = [];
        labels.push(this.newNodeType);
        returnNode.labels = labels;
        // 传入id属性（仅在更新节点情况下）
        if (this.isEditing) {
          returnNode.id = this.newNodeId;
        }
        for (let i = 0, len = this.nodeForm.length; i < len; i++) {
          properties[this.nodeForm[i].key] = this.nodeForm[i].value;
        }
        // 传入name属性
        returnNode.properties = properties;
        returnNode.properties['名称'] = this.newNodeName;
        return returnNode;
      },
      submitEditing() {
        let valid = this.checkNodeFormValidate();
        if (valid) {
          // 从nodeForm数组转化为后端的node对象格式
          let newNode = this.generateNodeFromNodeForm();
          let _this = this;
          axios({
            method: 'post',
            url: "api/node/updateNode",
            data: newNode
          }).then(function (response) {
            if (response.status === 200) {
              if (response.data.success){
                // 转化为前端的node对象格式
                let temp = newNode;
                newNode = temp.properties;
                newNode.id = temp.id;
                newNode.nodeType = temp.labels[0];
                _this.editNodeFrontEnd(newNode);
                _this.cancelEditing();
                _this.$notify({
                  title: '更新成功',
                  message: "更新节点成功",
                  type: 'success'
                });
              }
              else {
                _this.$notify({
                  title: '更新失败',
                  message: response.data.message,
                  type: 'warning'
                });
              }
            } else {
              _this.$notify({
                title: '更新失败',
                message: "请稍后再试",
                type: 'warning'
              });
            }
          }).catch(function (error) {
            _this.$notify({
              title: '添加失败',
              message: error,
              type: 'warning'
            });
          });
        }
      },

      submitAdding() {
        let valid = this.checkNodeFormValidate();
        if (valid) {
          // 从nodeForm数组转化为后端的node对象格式
          let newNode = this.generateNodeFromNodeForm();
          let _this = this;
          axios({
            method: 'post',
            url: "api/node/saveNode",
            data: newNode})
            .then(function (response) {
            if (response.status === 200) {
              if (response.data.success){
                let temp = newNode;
                newNode = temp.properties;
                newNode.id = response.data.content.id;
                newNode.nodeType = temp.labels[0];
                // TODO 加上这一行会报错？
                _this.addNodeFrontEnd(newNode);
                _this.cancelAdding();
                _this.$notify({
                  title: '添加成功',
                  message: "添加节点成功",
                  type: 'success'
                });
              }
              else {
                _this.$notify({
                  title: '添加失败',
                  message: response.data.message,
                  type: 'warning'
                });
              }
            } else {
              _this.$notify({
                title: '添加失败',
                message: "请稍后再试",
                type: 'warning'
              });
            }
          })
            .catch(function (error) {
            _this.$notify({
              title: '添加失败',
              message: error,
              type: 'warning'
            });
          });
        }
      },
      cancelEditing() {
        this.nodeForm = [];
        this.newNodeType = '';
        this.newNodeName = '';
        this.newNodeId = '';
        this.drawer = false;
      }
      ,
      cancelAdding() {
        this.nodeForm = [];
        this.newNodeType = '';
        this.newNodeName = '';
        this.drawer = false;
      },
    }
  }
</script>

<style>
  .el-drawer__body {
    overflow: auto;
  }


</style>
