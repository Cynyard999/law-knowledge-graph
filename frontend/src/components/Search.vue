<template>
  <div class="searchContainer">
    <el-row>
      <el-col :span="4" class="choice0">
        <el-switch
          v-model="searchMode"
          active-text="图内搜索"
          inactive-text="全局搜索"
          active-value="1"
          inactive-value="0"
          @change="chooseMode"
        >
        </el-switch>
      </el-col>
      <el-col :span="3" class="choice1">
        <el-select v-model="nodeOrRel"
                   placeholder="请选择"
                   class="selectStyle"
                   :disabled="searchMode === '0'"
                   @change="setChoices2">
          <el-option v-for="item in choice1" :key="item" :value="item"
                     style="display: flex;flex-direction: column"></el-option>
        </el-select>
      </el-col>
      <el-col :span="3" class="choice2">
        <el-select v-model="property"
                   placeholder="请选择"
                   class="selectStyle">
          <el-option v-for="item in choice2" :key="item" :value="item"
                     style="display: flex;flex-direction: column"></el-option>
        </el-select>

      </el-col>
      <el-col :span="8" class="center">
        <el-input
          v-model="search"
          @input="showHistory"
          @keyup.enter.native="searchHandler"
          :placeholder="hint"
          class="inputStyle"
        >
          <el-button slot="append" icon="el-icon-search" id="local_search" @click="searchHandler"></el-button>
          <el-button slot="append" icon="el-icon-refresh-left" id="local_restore" @click="resetSearch"></el-button>
        </el-input>
        <el-card
          @mouseenter="enterSearchBoxHanlder"
          v-if="isSearch"
          id="search-box">
          <dl>
            <dt class="search-title">历史搜索</dt>
            <dt class="remove-history" @click="removeAllHistory">
              <i class="el-icon-delete"></i>清空历史记录
            </dt>
            <el-tag
              v-for="search in historySearchList"
              :key="search.id"
              closable
              :type="search.type"
              @close="closeHandler(search)"
              style="margin-right:5px; margin-bottom:5px;"
              @click="clickHistory(search.name)">
              {{search.name}}
            </el-tag>
          </dl>
        </el-card>
      </el-col>
      <el-col :span="3" v-show="searchMode==='0'" class="choice2">
        <el-input-number v-model="searchNodeNum" :min="1" :max="10" label="节点数量"></el-input-number>
      </el-col>
    </el-row>
  </div>
</template>

<script>
  import {Store} from "../utils/store";
  import axios from "axios";

  export default {
    data() {
      return {
        search: "", // 当前输入框的值
        isSearch: false, // 是否正在输入
        historySearchList: [], //历史搜索数据
        searchList: [], //搜索返回数据
        types: ["", "success", "info", "warning", "danger"], //搜索历史tag式样
        searchMode: "",
        searchNodeNum: 5,
        nodeOrRel: "节点",
        property: "",
        choice1: ["节点", "关系"],
        pros: [],
        choice2: [],
        relTypes: [],
        currentNodeData: [],
        currentLinkData: [],
        hint: '请输入搜索内容',
      };
    },
    // 绑定本地存储中的历史数据
    mounted() {
      let localSearchHistory = localStorage.getItem("searchHistory");
      if (localSearchHistory) {
        this.historySearchList = JSON.parse(localSearchHistory);
      }
      this.getPros();
      this.getTypes();
    },
    methods: {
      chooseMode() {
        if (this.searchMode === "1") {
          this.nodeOrRel = '';
          this.choice2 = [];
        } else {
          this.nodeOrRel = '节点';
          this.hint = '请输入搜索内容';
          this.choice2 = this.pros
        }
      },
      // 获取所有的属性
      getPros() {
        let _this = this;
        axios.get("api/node/findAllNodePros")
          .then(function (response) {
            if (response.status === 200) {
              _this.pros = response.data.content;
              _this.choice2 = _this.pros
            }
          })
          .catch(function (error) {
            console.log(error);
          })
      },
      // 获取所有类型
      getTypes() {
        let _this = this;
        axios.get("api/relationship/findAllTypes")
          .then(function (response) {
            if (response.status === 200) {
              _this.relTypes = response.data.content;
            }
          })
          .catch(function (error) {
            console.log(error);
          })
      },
      setChoices2() {
        if (this.nodeOrRel === '节点') {
          // 改变类型 清空属性
          this.choice2 = this.pros;
          this.property = '';
          this.hint = '请输入搜索内容';
        } else {
          this.choice2 = this.relTypes;
          this.property = '';
          this.hint = '请输入起始节点id或name';
        }
      },
      // 是否显示历史记录
      showHistory() {
        // 有搜索内容，显示历史记录
        this.isSearch = this.search.length > 0;
      },
      enterSearchBoxHanlder() {
        clearTimeout(this.searchBoxTimeout);
      },
      // 点击历史记录直接搜索
      clickHistory(item) {
        this.search = item;
        this.searchHandler();
      },

      searchHandler() {
        this.isSearch = false;
        const inputValue = this.search;
        const inputProperty = this.property;
        const nodeOrRel = this.nodeOrRel;
        const searchMode = this.searchMode;
        const number = this.searchNodeNum;
        if (nodeOrRel === '') {
          this.$notify({
            title: '警告',
            message: '请输入搜索类型',
            type: 'warning'
          });
          return;
        }
        if (inputProperty === '') {
          this.$notify({
            title: '警告',
            message: '请输入搜索属性值',
            type: 'warning'
          });
          return;
        }
        if (inputValue === '') {
          this.$notify({
            title: '警告',
            message: '请输入搜索属性内容',
            type: 'warning'
          });
          return;
        }

        let searchNodeResult = [];
        // 获取当前页面知识图谱信息
        // 全局搜索
        if (searchMode === '0') {
          this.$parent.resetAll();
          this.$parent.getSpecificNodes(inputProperty, inputValue, number);
          return;
        }
        if (nodeOrRel === '节点') {
          this.currentNodeData = this.$store.state.currentGraph.nodes;
          // 找到应该高亮的ID
          // 筛选后得到对应id的数组 includes
          searchNodeResult = this.currentNodeData.filter(function (node, index) {
            return (node.hasOwnProperty(inputProperty) && node[inputProperty].includes(inputValue))
          }).map(function (node) {
            return node.id;
          });
        }
        // 关系搜索
        else {
          this.currentLinkData = this.$store.state.currentGraph.links;
          searchNodeResult = this.currentLinkData.filter(function (link, index) {
            return (link.type === inputProperty && link.source.id === parseInt(inputValue)
              || link.source.name === inputValue);
          }).map(function (link) {
            return link.target.id;
          });
        }
        this.$parent.highlightNodes(searchNodeResult);
        let n = Math.random() * 5;
        let exist =
          this.historySearchList.filter(value => {
            return value.name === this.search;
          }).length !== 0;
        if (!exist) {
          this.historySearchList.unshift({name: this.search, type: this.types[n]});
        } else {
          let i = this.historySearchList.findIndex(element => element["name"] = inputValue);
          this.historySearchList.splice(i, 1);
          this.historySearchList.unshift({name: this.search, type: this.types[n]});
        }
        if (this.historySearchList.length > 10) {
          this.historySearchList = this.historySearchList.slice(0, 10);
        }
        Store.saveHistory(this.historySearchList);
      },
      closeHandler(search) {
        this.historySearchList.splice(this.historySearchList.indexOf(search), 1);
        Store.saveHistory(this.historySearchList);
        if (this.historySearchList.length === 0) {
          this.history = false;
        }
      },
      removeAllHistory() {
        this.historySearchList = [];
        Store.removeAllHistory();
      },
      resetSearch() {
        // 清空搜索结果
        this.$parent.update();
      },
    },
  };

</script>

<style>
  .searchContainer {
    width: 1200px;
    height: 100%;
    margin: 0 auto;
  }

  .choice0 {
    margin-top: 25px;
  }

  .choice1 {
    margin-top: 15px;
    margin-left: 100px;
  }

  .choice2 {
    margin-top: 15px;
  }


  .center {
    margin-top: 15px;
  }

  .selectStyle {
    margin-right: 20px;
  }

  .inputStyle {
    width: 358px;
  }

  #search {
    background-color: white;
    border-radius: 0;
  }

  #restore {
    background-color: white;
    border-radius: 0;
  }

  #search-box {
    width: 255px;
    height: 300px;
    margin-top: 0;
    margin-left: 20px;
    padding-bottom: 20px;
    position: absolute;
    z-index: 4; /* 设置层级 否则会遮挡搜索记录面板 */
  }

  .search-title {
    margin-right: 150px;
    color: #bdbaba;
    font-size: 15px;
    margin-bottom: 5px;
    margin-top: 0;
  }


  .remove-history {
    color: #bdbaba;
    font-size: 15px;
    float: right;
    margin-top: -24px;
  }
</style>












