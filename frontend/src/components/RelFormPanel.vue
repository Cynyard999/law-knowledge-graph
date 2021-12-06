<template>
  <div>
    <el-drawer
      title="添加关系"
      :size="450"
      :visible.sync="drawer"
    >
      <el-form ref="ruleForm" label-width="100px" :rules="rules" :model="ruleForm">
        <el-form-item label="关系名称" style="width: 300px;" prop="name">
          <el-input v-model="ruleForm.name"></el-input>
        </el-form-item>
        <el-form-item label="起始节点Id" style="width: 300px;" prop="startNodeId">
          <el-input v-model.number="ruleForm.startNodeId"></el-input>
        </el-form-item>
        <el-form-item label="终止节点Id" style="width: 300px;" prop="endNodeId">
          <el-input v-model.number="ruleForm.endNodeId"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button @click="submitForm('ruleForm')" type="primary">新增关系</el-button>
        </el-form-item>
      </el-form>

    </el-drawer>
  </div>

</template>

<script>
  import axios from "axios";

  export default {
    name: "relFormPanel",
    data() {
      return {
        drawer: false,
        // 向后端发送post请求传递的关系对象
        returnRel: {},
        ruleForm: {
          name: '',
          startNodeId: '',
          endNodeId: '',
        },
        rules: {
          name: [
            {required: true, message: '请输入关系名称', trigger: 'blur'},
          ],
          startNodeId: [
            {required: true, message: '请输入起始节点id', trigger: 'blur'},
            {type: 'number', message: 'id必须为数字值'},
          ],
          endNodeId: [
            {required: true, message: '请输入终止节点id', trigger: 'blur'},
            {type: 'number', message: 'id必须为数字值'},
          ],
        },
      }
    },
    methods: {
      initAddRelForm() {
        this.drawer = true;
      },
      async generateRelFromNodeForm(link) {
        let type = link.type;
        let properties = {};
        let startNode = {
          "id": link.source.id,
          "labels": [
            link.source.nodeType,
          ],
          "properties": {}
        };
        let endNode = {
          "id": link.target.id,
          "labels": [
            link.target.nodeType,
          ],
          "properties": {}
        };
        this.returnRel.type = type;
        this.returnRel.properties = properties;
        this.returnRel.startNode = startNode;
        this.returnRel.endNode = endNode;
      },
      getInfoFromBackend(link) {
        this.generateRelFromNodeForm(link);
        let _this = this;
        axios({
          method: 'post',
          url: "api/relationship/saveRel",
          data: this.returnRel
        }).then(function (response) {
          if (response.status === 200) {
            if (response.data.success) {
              _this.$parent.$parent.addRelFrontEnd(response.data.content.id);
            }
          } else {
            _this.$notify({
              title: '更新失败',
              message: response.data.message,
              type: 'warning'
            });
          }
        }).catch(function (error) {
          console.log(error)
          _this.$notify({
            title: '添加失败',
            message: error,
            type: 'warning'
          });
        })
      },
      submitForm(formName) {
        this.$refs[formName].validate((valid) => {
          if (valid) {
            this.$parent.$parent.addRel(this.ruleForm);
          } else {
            console.log('error submit!!');
            return false;
          }
        });
      }
    }

  }
</script>

<style scoped>

</style>
