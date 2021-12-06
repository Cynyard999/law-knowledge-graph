import Vue from 'vue'
import Vuex from 'vuex'
import * as d3 from "d3";

Vue.use(Vuex);

export default new Vuex.Store({
  // data
  state: {
    showLinkName:true,
    nodesNameSize:8,
    currentGraph: {
      nodes: [],
      links: []
    },
    userInsert: {
      nodes: [],
      links: [],
    },
  },
  // methods
  mutations: {
    linkNameRerender (state) {
      d3.selectAll("text>textPath").each(function (n) {
        if(state.showLinkName){
          this.innerHTML=n.type
        }
        else
          this.innerHTML=""
      })
    },
  },
  // 异步方法
  actions:{

  },
  // 模块
  modules:{

  }
})
