<template>
  <div>
    <el-menu mode="horizontal" class="exportBar">
      <el-submenu index="2" style="width: 150px">
        <template slot="title">图谱导出</template>
        <div style="display: flex;flex-direction: column">
          <el-menu-item index="2-1" @click="exportPic" >导出图片</el-menu-item>
          <el-menu-item index="2-2" @click="exportJson" >导出JSON</el-menu-item>
        </div>

      </el-submenu>
    </el-menu>
  </div>
</template>

<script>
export default {
  name: "TopNav",
  data() {
    return {}
  },
  methods: {
    exportJson() {
      let _this = this;
      _this.$parent.exportJSON();
    },
    exportPic() {
      let _this = this;
      _this.$parent.exportImg();
    },
  }

}
</script>

<style scoped>
.exportBar {
  float: right;
}

</style>
