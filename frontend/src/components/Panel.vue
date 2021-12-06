<template>
  <div class="myContainer">
    <header class="myHeader">
      <top-nav></top-nav>
      <search></search>
    </header>
    <section class="mySection">
      <ul class="mainBody">
        <li class="bar">
          <control-panel :nodescount='currentNodeCount' :relscount="currentRelCount" ref="controlPanel"></control-panel>
        </li>
        <li class="graph">
          <el-tabs stretch v-model="activeTab" @tab-click="tabSwitch">
            <el-tab-pane label="力导图模式" name="first" key="first">
              <div class="mainGraph"></div>
            </el-tab-pane>
            <el-tab-pane label="排版模式" name="second" key="second">
              <div class="treeGraph"></div>
            </el-tab-pane>
          </el-tabs>
        </li>
        <li class="bar">
          <detail-panel ref="detailPanel"></detail-panel>
        </li>
        <li>
          <chat-robot></chat-robot>
        </li>
      </ul>
    </section>
  </div>
</template>

<script>
  import * as d3 from 'd3'
  import axios from 'axios'
  import ControlPanel from "./ControlPanel";
  import DetailPanel from "./DetailPanel";
  import TopNav from "./TopNav";
  import Search from "./Search";
  import ChatRobot from "./ChatRobot";

  export default {
    name: 'Panel',
    components: {Search, TopNav, DetailPanel, ControlPanel, ChatRobot},
    data() {
      return {
        activeTab: "first",
        lastActiveTab: "first",
        newAddedLink: {},
        links: [],
        nodes: [],
        nodesName: [],
        linksName: [],
        nodeRadius: 10,
        selectedNodeHtml: null,
        simulation: null,
        editedNode: {},
        initDone: false,
        width: 600,
        height: 400,
        colorList: [],
        relQueryRes: '',
        currentTreeGraph: {
          name: "",
          id: "",
          children: []
        },

        nodeRadiusMap: new Map(),
      }
    },
    computed: {
      currentNodeCount: function () {
        if (this.$store.state.currentGraph) {
          return this.$store.state.currentGraph.nodes.length + this.$store.state.userInsert.nodes.length;
        }
      },
      currentRelCount: function () {
        if (this.$store.state.currentGraph) {
          return this.$store.state.currentGraph.links.length + this.$store.state.userInsert.links.length
        }
      },
    },
    mounted() {

    },

    methods: {
      init() {
        this.$store.state.currentGraph.links.push.apply(this.$store.state.currentGraph.links, this.$store.state.userInsert.links);
        this.$store.state.currentGraph.nodes.push.apply(this.$store.state.currentGraph.nodes, this.$store.state.userInsert.nodes);
        this.initGraph(this.$store.state.currentGraph);
        this.initDone = true;
      },

      update() {
        this.$store.state.currentGraph.links.push.apply(this.$store.state.currentGraph.links, this.$store.state.userInsert.links);
        this.$store.state.currentGraph.nodes.push.apply(this.$store.state.currentGraph.nodes, this.$store.state.userInsert.nodes);
        this.updateGraph(this.$store.state.currentGraph);
      },

      initGraph(data) {
        const _this = this;
        const links = data.links;
        const nodes = data.nodes;
        //力的仿真
        _this.simulation = d3.forceSimulation(nodes)
          .force("link", d3.forceLink(links).id(d => d.id).distance(80)) // 上面的links，获取links的id,distance设置了连线的长度
          .force("collide", d3.forceCollide().radius(() => 10)) //碰撞半径
          .force("charge", d3.forceManyBody().strength(-10)) // 电荷,斥力强度
          .force("center", d3.forceCenter(_this.width / 2, _this.height / 2)); // 力的中心

        // 容器
        let svg = d3.select(".mainGraph")
          .append("svg")
          .attr("viewBox", [0, 0, _this.width, _this.height]);
        svg.call(d3.zoom().on("zoom", function () {
          g.attr("transform", d3.event.transform) //滚动放缩时将g进行缩放，使用svg会造成偏移
        }));

        // 取消svg和圆圈的双击放大事件
        svg.on("dblclick.zoom", null);

        // svg.append("marker")
        //   .attr("class", "arrow")
        //   .attr("id", "direction")
        //   .attr("orient", "auto") //自动调整方向
        //   .attr("stroke-width", 2) //箭头粗细
        //   .attr("markerUnits", "strokeWidth")
        //   .attr("markerUnits", "userSpaceOnUse")//匹配粗细
        //   .attr("viewBox", "0 -5 10 15")//箭头所在的可见范围
        //   .attr("refX", 22)
        //   .attr("refY", 0)
        //   .attr("markerWidth", 12)
        //   .attr("markerHeight", 12)
        //   .append("path")
        //   .attr("d", "M 0 -5 L 10 0 L 0 5")
        //   .attr('fill', '#C6E2FF')
        //   .attr("stroke-opacity", 0.6);//形状属性

        const g = svg.append("g").attr("id", "outerG");//g标签包裹所有nodes links

        // 连线属性
        _this.links = g.append("g")
          .selectAll("path")
          .data(links, function (d) {
            return "link" + d.id;
          })
          .join("path")
          .attr("stroke", "#BFEFFF") //颜色
          .attr("stroke-opacity", 0.6)
          .attr("marker-end", "url(#direction)") //在末尾处进行标记，通过URL定位marker内容
          .attr("stroke-width", d => Math.sqrt(d.width)) //宽度,d就是.data(links)中的links
          .attr("class", "link")
          .attr("id", function (d) {
            return "link" + d.id;
          })
          .on("click", _this.selectRel)
        ;
        _this.linksName = g.append("g")
          .selectAll("text")
          .data(links, function (d) {
            if (typeof (d.source) === 'object') {
              return d.id + "_" + d.source.id + "_" + d.type + "_" + d.target.id
            } else {
              return d.id + "_" + d.source + "_" + d.type + "_" + d.target
            }
            // return "link"+d.id;
          })
          .join("text")
          .attr("class", "linksName")
          .attr("id", d => "linkName" + d.id);

        _this.linksName
          .append('textPath')
          .attr('xlink:href', d => "#link" + d.id)
          .attr('id', d => "linkName" + d.id)
          .attr('startOffset', '50%')
          .text(d => d.type);

        // 结点属性
        _this.nodes = g.append("g")
          .selectAll("rect") // svg的类型
          .data(nodes)
          .join("rect")
          .attr("width", function (d) {
            if (_this.nodeRadiusMap.get(d.id.toString())) {
              return 2 * _this.nodeRadiusMap.get(d.id.toString());
            } else {
              _this.nodeRadiusMap.set(d.id.toString(), _this.nodeRadius);
              return 2 * _this.nodeRadius;
            }
          })
          .attr("height", function (d) {
            if (_this.nodeRadiusMap.get(d.id.toString())) {
              return 2 * _this.nodeRadiusMap.get(d.id.toString());
            } else {
              _this.nodeRadiusMap.set(d.id.toString(), _this.nodeRadius);
              return 2 * _this.nodeRadius;
            }
          })
          .attr("rx", function (d) {
            if (_this.nodeRadiusMap.get(d.id.toString())) {
              return _this.nodeRadiusMap.get(d.id.toString());
            } else {
              _this.nodeRadiusMap.set(d.id.toString(), _this.nodeRadius);
              return _this.nodeRadius;
            }
          })
          .attr("fill", _this.color)
          .on("click", function (d) {
            _this.selectNode(d, this)
          })
          .call(_this.drag(_this.simulation)) // 拖动时调用drag函数
          .attr("class", "node")
          .attr("id", function (d) {
            return "node" + d.id;
          });

        _this.nodesName = g.append("g")
          .selectAll("text")
          .data(nodes)
          .join("text")
          .text(function (d) {
            return d['名称'];
          })
          .attr("dx", function (d) {
            return 0 - 4 * d['名称'].length
          })
          .attr("dy", function (d) {
            return 20 + _this.nodeRadiusMap.get(d.id.toString()) - _this.nodeRadius;
          })
          .attr("class", "nodesName");

        _this.simulation.on("tick", () => { //动态控制,仿真开始时会被调用
          _this.links
            .attr("d", d => "M " + d.source.x + " " + d.source.y + " L " + d.target.x + " " + d.target.y);

          _this.nodes
            .attr("x", function (d) {
              return d.x - _this.nodeRadiusMap.get(d.id.toString());
            })
            .attr("y", function (d) {
              return d.y - _this.nodeRadiusMap.get(d.id.toString());
            });

          _this.nodesName
            .attr("x", function (d) {
              return d.x;
            })
            .attr("y", function (d) {
              return d.y;
            })
          ;
        });
      },

      updateGraph(data) {
        let _this = this;
        const links = data.links;
        const nodes = data.nodes;
        _this.nodes = _this.nodes
          .data(nodes)
          .join("rect")
          .attr("width", function (d) {
            if (_this.nodeRadiusMap.get(d.id.toString())) {
              return 2 * _this.nodeRadiusMap.get(d.id.toString());
            } else {
              _this.nodeRadiusMap.set(d.id.toString(), _this.nodeRadius);
              return 2 * _this.nodeRadius;
            }
          })
          .attr("height", function (d) {
            if (_this.nodeRadiusMap.get(d.id.toString())) {
              return 2 * _this.nodeRadiusMap.get(d.id.toString());
            } else {
              _this.nodeRadiusMap.set(d.id.toString(), _this.nodeRadius);
              return 2 * _this.nodeRadius;
            }
          })
          .attr("rx", function (d) {
            if (_this.nodeRadiusMap.get(d.id.toString())) {
              return _this.nodeRadiusMap.get(d.id.toString());
            } else {
              _this.nodeRadiusMap.set(d.id.toString(), _this.nodeRadius);
              return _this.nodeRadius;
            }
          })
          .attr("class", "node")
          .attr("fill", _this.color)
          .attr("id", function (d) {
            return "node" + d.id;
          })
          .on("click", function (d) {
            _this.selectNode(d, this)
          })
          .call(_this.drag(_this.simulation))
          .merge(_this.nodes);

        _this.nodesName = _this.nodesName
          .data(nodes)
          .join("text")
          .merge(_this.nodesName)
          .text(function (d) {
            return d['名称'];
          })
          .attr("dx", function (d) {
            return 0 - 4 * d['名称'].length
          })
          .attr("dy", function (d) {
            return 20 + _this.nodeRadiusMap.get(d.id.toString()) - _this.nodeRadius;
          })
          .attr('class', 'nodesName');

        _this.links = _this.links
          .data(links)
          .join("path")
          .attr("stroke", "#BFEFFF")
          .attr("stroke-opacity", 0.6)
          .attr("stroke-width", d => Math.sqrt(d.width))
          .attr("marker-end", "url(#direction)")
          .merge(_this.links)
          .attr("class", "link")
          .attr("id", function (d) {
            return "link" + d.id;
          })
          .on("click", _this.selectRel);

        _this.linksName = _this.linksName
          .data(links, function (d) {
            return "link" + d.id;
          })
          .join('text')
          .attr("class", "linksName");

        _this.linksName
          .append('textPath')
          .attr(
            // 绑定了位置，对应的是link的id
            'xlink:href', function (d) {
              return "#link" + d.id;
            }
          )
          .attr("id", d => "linkName" + d.id)
          .attr('startOffset', '50%')
          .merge(_this.linksName)
          // 呈现的字体
          .text(d => d.type);

        _this.simulation.nodes(nodes);
        _this.simulation.force("link").links(links);
        _this.simulation.alpha(1).restart();
      },


      // TODO 把里面的方法单独提取出来 更新 初始化
      initTree(treeData) {
        d3.select(".treeGraph").select("svg").remove();
        // 设置图表的宽高和Margin
        let margin = {top: 0, right: 0, bottom: 0, left: 0};
        let width = this.width;
        let height = this.height;

        var svg = d3
          .select(".treeGraph")
          // 在页面的body里添加svg对象
          .append("svg")
          .attr("viewBox", [0, 0, width, height]);

        // 子group元素存为view变量
        var view = svg
          // 在svg里添加group元素
          .append("g")
          // 将group放置在左上方
          .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var zoom = d3
          .zoom()
          // 设置缩放区域为0.1-100倍
          .scaleExtent([0.1, 10])
          .on("zoom", () => {
            view.attr(
              "transform",
              "translate(" +
              (d3.event.transform.x + margin.left) +
              "," +
              (d3.event.transform.y + margin.top) +
              ") scale(" +
              d3.event.transform.k +
              ")"
            );
          });
        // svg层绑定zoom事件，同时释放zoom双击事件
        svg.call(zoom).on("dblclick.zoom", () => {
        });

        var i = 0,
          duration = 750,
          root;

        root = d3.hierarchy(treeData, function (d) {
          return d.children;
        });
        // 第一层元素初始位置
        root.x0 = height / 2;
        root.y0 = 0;
        // 第二层以上元素收起
        root.children.forEach(collapse);
        // 更新节点状态
        update(root);

        // collapse方法，用于切换子节点的展开和收起状态
        function collapse(d) {
          if (d.children) {
            d._children = d.children;
            d._children.forEach(collapse);
            d.children = null;
          }
        }

        function update(source) {
          // 大致计算需要放大的倍数
          var scale =
            (getDepth(root) / 8 || 0.5) +
            (getMax(root) / 12 || 0.5);

          var treemap = d3.tree().size([height * scale, width]);
          // 设置节点的x、y位置信息
          var treeData = treemap(root);

          // 计算新的Tree层级
          var nodes = treeData.descendants(),
            links = treeData.descendants().slice(1);

          // 设置每个同级节点间的y间距
          nodes.forEach(function (d) {
            d.y = d.depth * 90;
          });

          // ****************** 节点部分操作 ***************************

          // 给节点添加id，用于选择集索引
          var node = view.selectAll("g.treeNode").data(nodes, function (d) {
            return d.id || (d.id = ++i);
          });

          // 添加enter操作，添加类名为node的group元素
          var nodeEnter = node
            .enter()
            .append("g")
            .attr("class", "treeNode")
            // 默认位置为当前父节点的位置
            .attr("transform", function (d) {
              return "translate(" + source.y0 + "," + source.x0 + ")";
            })
            // 给每个新加的节点绑定click事件
            .on("click", click);

          // 给每个新加的group元素添加cycle元素
          nodeEnter
            .append("circle")
            .attr("class", "treeNode")
            .attr("r", 5)
            // 如果元素有子节点，且为收起状态，则填充浅蓝色
            .style("fill", function (d) {
              return d._children ? "#555" : "#999";
            });

          // 给每个新加的group元素添加文字说明
          nodeEnter
            .append("text")
            .attr("class", "treeText")
            .attr("dy", ".35em")
            .attr("x", function (d) {
              return d.children || d._children ? -5 : 5;
            })
            .attr("text-anchor", function (d) {
              return d.children || d._children ? "end" : "start";
            })
            .text(function (d) {
              return d.data['名称'];
            });

          // 获取update集
          var nodeUpdate = nodeEnter.merge(node);

          // 设置节点的位置变化，添加过渡动画效果
          nodeUpdate
            .transition()
            .duration(duration)
            .attr("transform", function (d) {
              return "translate(" + d.y + "," + d.x + ")";
            });

          // 更新节点的属性和样式
          nodeUpdate
            .select("circle.treeNode")
            .attr("r", 5)
            .style("fill", function (d) {
              return d._children ? "#555" : "#999";
            })
            .attr("cursor", "pointer");

          // 获取exit操作
          var nodeExit = node
            .exit()
            // 添加过渡动画
            .transition()
            .duration(duration)
            .attr("transform", function (d) {
              return "translate(" + source.y + "," + source.x + ")";
            })
            // 移除元素
            .remove();

          // exit集中节点的cycle元素尺寸变为0
          nodeExit.select("circle").attr("r", 1e-6);

          // exit集中节点的text元素可见度降为0
          nodeExit.select("text").style("fill-opacity", 1e-6);

          // ****************** 连接线部分操作 ***************************

          // 添加贝塞尔曲线的path，衔接与父节点和子节点间
          let diagonal = d3.linkHorizontal().x(d => d.y).y(d => d.x);

          // 更新数据
          var link = view.selectAll("path.treeLink").data(links, function (d) {
            return d.id;
          });

          // 添加enter操作，添加类名为link的path元素
          var linkEnter = link
            .enter()
            .insert("path", "g")
            .attr("class", "treeLink")
            // 默认位置为当前父节点的位置
            .attr("d", function (d) {
              var o = {x: source.x0, y: source.y0};
              return diagonal({source: o, target: o});
            });

          // 获取update集
          var linkUpdate = linkEnter.merge(link);

          // 更新添加过渡动画
          linkUpdate
            .transition()
            .duration(duration)
            .attr("d", function (d) {
              return diagonal({source: d, target: d.parent});
            });

          // 获取exit集
          var linkExit = link
            .exit()
            // 设置过渡动画
            .transition()
            .duration(duration)
            .attr("d", function (d) {
              var o = {x: source.x, y: source.y};
              return diagonal({source: o, target: o});
            })
            // 移除link
            .remove();

          // 为动画过渡保存旧的位置
          nodes.forEach(function (d) {
            d.x0 = d.x;
            d.y0 = d.y;
          });

          // 当点击时，切换children，同时用_children来保存原子节点信息
          function click(d) {
            if (d.children) {
              d._children = d.children;
              d.children = null;
            } else {
              d.children = d._children;
              d._children = null;
            }
            update(d);
          }

          function getMax(obj) {
            let max = 0;
            if (obj.children) {
              max = obj.children.length;
              obj.children.forEach(d => {
                const tmpMax = getMax(d);
                if (tmpMax > max) {
                  max = tmpMax;
                }
              });
            }
            return max;
          }

          // 获取最深层级数
          function getDepth(obj) {
            var depth = 0;
            if (obj.children) {
              obj.children.forEach(d => {
                var tmpDepth = getDepth(d);
                if (tmpDepth > depth) {
                  depth = tmpDepth;
                }
              });
            }
            return 1 + depth;
          }
        }

      },

      deepSearchTree(tree, targetID) {
        if (tree.id.toString() === targetID.toString()) {
          return tree;
        }
        let children = tree.children;
        for (var i = 0; i < children.length; i++) {
          let res = this.deepSearchTree(children[i], targetID);
          if (res) {
            return res;
          }
        }
        return null;
      },

      getSpecificNodes(property, value, number) {
        let node = {};
        let properties = {};
        properties[property] = value;
        node.labels = [];
        node.properties = properties;
        node.id = -1;
        const _this = this;
        axios({
          method: 'post',
          url: "api/node/searchNode/" + number,
          data: node
        })
          .then(function (response) {
            if (response.status === 200) {
              if (response.status === 200 && response.data.success) {
                // 初始化力导图
                _this.$store.state.currentGraph["nodes"] = response.data.content.map(function (item, index) {
                  let node = JSON.parse(JSON.stringify(item.properties));
                  node['节点类型'] = item.labels[0];
                  node.id = item.id;
                  // 为每一个node节点映射一个半径
                  _this.nodeRadiusMap.set(node.id.toString(), _this.nodeRadius);
                  return node;
                });
                if (_this.initDone) {
                  _this.update();
                } else {
                  _this.init();
                }
                // 初始化树
                _this.currentTreeGraph["children"] = response.data.content.map(function (item, index) {
                  let node = JSON.parse(JSON.stringify(item.properties));
                  node.nodeType = item.labels[0];
                  node.id = item.id;
                  node.children = [];
                  return node;
                });
              } else {
                this.$notify({
                  title: '获取节点失败',
                  message: '请稍后再试',
                  type: 'warning'
                });
              }
            }
          })
          .catch(function (error) {
            _this.$notify({
              title: '搜索失败',
              message: error,
              type: 'warning'
            });
          });
      },

      getNodes(type, number) {
        const _this = this;
        // 获取需要的nodes
        axios.get("api/node/" + type + "/" + number)
          .then(function (response) {
            if (response.status === 200 && response.data.success) {
              // 初始化力导图
              _this.$store.state.currentGraph["nodes"] = response.data.content.map(function (item, index) {
                let node = JSON.parse(JSON.stringify(item.properties));
                node['节点类型'] = item.labels[0];
                node.id = item.id;
                // 为每一个node节点映射一个半径
                _this.nodeRadiusMap.set(node.id.toString(), _this.nodeRadius);
                return node;
              });
              if (_this.initDone) {
                _this.update();
              } else {
                _this.init();
              }
              // 初始化树
              _this.currentTreeGraph["children"] = response.data.content.map(function (item, index) {
                let node = JSON.parse(JSON.stringify(item.properties));
                node.nodeType = item.labels[0];
                node.id = item.id;
                node.children = [];
                return node;
              });
            } else {
              this.$notify({
                title: '获取节点失败',
                message: '请稍后再试',
                type: 'warning'
              });
            }
          })
          .catch(function (error) {
            console.log(error);
          })
      },

      highlightNodes(nodeIdList) {
        for (let i = 0; i < nodeIdList.length; i++) {
          d3.select("#node" + nodeIdList[i]).attr("fill", "#FFF8DC");
        }
      },

      checkNodeExist(id) {
        if (d3.select("#node" + id)._groups[0][0]) {
          return true
        }
        return false;
      },

      checkRelExist(id) {
        for (let index in this.$store.state.currentGraph.links) {
          if (this.$store.state.currentGraph.links[index].id === id) {
            return true;
          }
        }
        return false;
      },

      insertNode(node) {
        this.$store.state.userInsert.nodes.push(node);
        this.nodeRadiusMap.set(node.id.toString(), this.nodeRadius);
        if (this.initDone) {
          this.update();
        } else {
          this.init();
        }
      },

      addRel(relForm) {
        let _this = this;
        let relExist = false;
        let newLink = {
          "source": {},
          "target": {},
          "width": 5,
          "type": relForm.name,
          "id": -1,
        };
        for (let index in this.$store.state.currentGraph.nodes) {
          if (this.$store.state.currentGraph.nodes[index].id === relForm.startNodeId) {
            newLink.source = this.$store.state.currentGraph.nodes[index];
          } else if (this.$store.state.currentGraph.nodes[index].id === relForm.endNodeId) {
            newLink.target = this.$store.state.currentGraph.nodes[index];
          }
        }
        if (JSON.stringify(newLink.source) === '{}') {
          alert("起始节点不在当前图谱中")
        } else if (JSON.stringify(newLink.target) === '{}') {
          alert("终止节点不在当前图谱中")
        } else {
          d3.selectAll("g>path").each(function (e) {
            if (e.source.id === newLink.source.id && e.target.id === newLink.target.id) {
              d3.selectAll("text>textPath").each(function (n) {
                if (e.id === n.id && n.type === newLink.type) {
                  alert("关系已存在")
                  relExist = true;
                }
              })
            }
          });
          if (!relExist) {
            _this.newAddedLink = newLink;
            _this.$refs.controlPanel.$refs.relFormPanel.getInfoFromBackend(newLink);
          }
        }
      },

      addRelFrontEnd(autoId) {
        this.newAddedLink.id = autoId;
        this.$store.state.currentGraph.links.push(this.newAddedLink);
        this.updateGraph(this.$store.state.currentGraph);
      },

      updateNode(node) {
        // 通过选择器更新节点显示的名字
        d3.selectAll("g>text").each(function (t) {
          if (t.id === node.id) {
            this.innerHTML = node["名称"];
          }
        });
      },

      color(d) {
        let colorList = ['#F16767', '#579480', '#8DCC93', '#604A0E', '#58C7E3', '#D9C9AE', '#C98FC0'];
        return colorList[0]
      },

      drag(simulation) {
        function dragstarted(subject) {
          if (!d3.event.active) simulation.alphaTarget(0.3).restart();
          subject.fx = subject.x;
          subject.fy = subject.y;
        }

        function dragged(subject) {
          subject.fx = d3.event.x;
          subject.fy = d3.event.y;
        }

        function dragended(subject) {
          if (!subject.active) simulation.alphaTarget(0);
          subject.fx = null;
          subject.fy = null;
        }

        return d3.drag()
          .on("start", dragstarted)
          .on("drag", dragged)
          .on("end", dragended);
      },

      selectNode(d, _that) {
        this.$refs.detailPanel.showDetail(d, d3.select(_that).attr("width") / 2);
      },

      changeNodeRadius(id, radius) {
        let pRadius = this.nodeRadiusMap.get(id.toString());
        this.nodeRadiusMap.set(id.toString(), radius);
        let _this = this;
        let node = d3.select("#node" + id);
        if(node.empty()){
          return
        }
        // 使得文字位置同步移动
        node.attr("width", 2 * radius);
        node.attr("height", 2 * radius);
        node.attr("x",node.attr("x")-(radius-pRadius));
        node.attr("y",node.attr("y")-(radius-pRadius));
        if (node.attr("rx") * 1 !== 0) {
          node.attr("rx", radius);
        }
        d3.selectAll("g>text").each(function (t) {
          if (t.id === id) {
            d3.select(this).attr("dy", 20 + radius - _this.nodeRadius);
          }
        });
      },


      selectRel(d) {
        this.$refs.detailPanel.showRelInfo(d);
      },

      // TODO 判断currentNode是关系中的起始节点还是终止节点
      renderLinks(response, currentNode, currentType) {
        let relations = response.data.content;
        let currentTreeNode = this.deepSearchTree(this.currentTreeGraph, currentNode.id);
        if (currentTreeNode) {
          relations.map(function (item, index) {
            let newNode = JSON.parse(JSON.stringify(item.endNode.properties));
            newNode.nodeType = item.endNode.labels[0];
            newNode.id = item.endNode.id;
            newNode.children = [];
            currentTreeNode.children.push(newNode);
          });
        } else {
          console.log("not found tree node")
        }
        for (let i = 0; i < relations.length; i++) {
          let relation = relations[i];
          let newNode = JSON.parse(JSON.stringify(relation.endNode.properties));
          newNode.id = relation.endNode.id;
          newNode["节点类型"] = relation.endNode.labels[0];
          if (this.checkNodeExist(newNode.id)) {
          } else {
            this.nodeRadiusMap.set(newNode.id.toString(), this.nodeRadius);
            this.$store.state.currentGraph.nodes.push(newNode);
          }
          let newLink = {
            "source": currentNode.id,
            "target": newNode.id,
            "width": 5,
            "type": relation.type,
            "id": relation.id
          };
          if (this.checkRelExist(newLink.id)) {
          } else {
            this.$store.state.currentGraph.links.push(newLink);
          }
        }

        // for (let i = this.$store.state.currentGraph.links.length - 1; i >= 0; i--) {
        //   //遍历所有已加入的关系.给旧数组节点在新关系中找相同节点。有的话旧节点就不用删除了
        //   if (this.$store.state.currentGraph.links[i].source.id === currentNode.id && this.$store.state.currentGraph.links[i].type !== currentType) {
        //     //起始节点相同，关系类型不同，则关系可以移除了
        //     let ifRemove = true;
        //     //假设该要移除的关系的endNode也要被移除
        //     for (let k = 0; k < response.data.length; k++) { //遍历请求获取到的新关系的对象数组
        //       // 三元组对象的endnode
        //       if (response.data.content[k].endNode.id === this.$store.state.currentGraph.links[i].target.id) { //新数组中的endnode的id和该即将被去除的关系的endnode的id相同，
        //         // 则该endnode不需要从$store.state.currentGraph的nodes数组中删除
        //         ifRemove = false;
        //         // 当前旧关系对应的旧节点在新关系中找到了相同节点，那么这个节点不用删除，开始遍历下一个旧关系。
        //         break;
        //       }
        //     }
        //     if (ifRemove) {//需要移除当前旧节点
        //       for (let j = this.$store.state.currentGraph.nodes.length - 1; j >= 0; j--) {
        //         if (this.$store.state.currentGraph.nodes[j].id === this.$store.state.currentGraph.links[i].target.id) {
        //           console.log("移除");
        //           this.$store.state.currentGraph.nodes.splice(j, 1);
        //         }
        //       }
        //     }
        //     this.$store.state.currentGraph.links.splice(i, 1)
        //   }
        // }
        this.updateGraph(this.$store.state.currentGraph);
      },

      hideNode(currentNode) {
        d3.select("#node" + currentNode.id).attr("display", "none");
        this.$refs.controlPanel.hasHidden = true;
        let clickNodeName = currentNode['名称'];
        d3.selectAll("text").each(function (d) {
          if (d['名称'] === clickNodeName) {
            d3.select(this).attr("display", "none");
          }
        });
        d3.selectAll("g>path").each(function (e) {
          if (e.source.id === d.id || e.target.id === d.id) {
            d3.select(this).attr("display", "none");
            d3.selectAll("text>textPath").each(function (n) {
              if (e.id === n.id) {
                d3.select(this).attr("display", "none");
              }
            })
          }
        })
      },

      delNodeInNodeArray(id) {
        // for (let index in this.$store.state.currentGraph.nodes) {
        //   if (this.$store.state.currentGraph.nodes[index].id === id) {
        //     console.log(this.$store.state.currentGraph.nodes[index])
        //     delete this.$store.state.currentGraph.nodes[index];
        //   }
        // }
      },

      delRelInRelArray(id) {
        // for (let index in this.$store.state.currentGraph.links) {
        //   if (this.$store.state.currentGraph.links[index].id === id) {
        //     delete this.$store.state.currentGraph.links[index];
        //   }
        // }
      },

      delNode(currentNode) {
        // TODO fix: 从$store.state.currentGraph中的node和link中也要删除 当前的delXXXFrontEnd方法会报错
        var group = d3.select("svg").selectAll(".node")._groups[0];
        for (let i = 0; i < group.length; i++) {
          if (group[i].id === currentNode.id.toString()) {
            var _that = group[i];
            d3.select(_that).remove();
          }
        }

        var clickNodeName = currentNode['名称'];
        d3.selectAll("text").each(function (d) {
          var temp = d['名称'];
          if (temp === clickNodeName) {
            d3.select(this).remove();
          }
        });

        d3.selectAll("g>path").each(function (e) {
          if (e.source.id === currentNode.id || e.target.id === currentNode.id) {
            d3.select(this).remove();
            d3.selectAll("text>textPath").each(function (n) {
              if (e.id === n.id) {
                d3.select(this).remove();
              }
            })
          }
        });
        // 这个方法前端调试好后再用
        //axios.delete("api/node/" + currentNode.id + "/deleteNode")
      },

      delRel(currentRel) {
        d3.selectAll("g>path").each(function (e) {
          // e.id是int
          // currentRel.relId也是int
          if (e.id === currentRel.relId) {
            d3.select(this).remove();
            d3.selectAll("text>textPath").each(function (n) {
              if (e.id === n.id) {
                d3.select(this).remove();
              }
            })
          }
        });
        // 这个方法前端调试好后再用
        //axios.delete("api/relationship/" + currentRel.relId + "/deleteRel")
      },

      editRelFrontEnd(newRelName, oldId, newId) {
        // for (let index in this.$store.state.currentGraph.links) {
        //   if (this.$store.state.currentGraph.links[index].id === oldId) {
        //     this.$store.state.currentGraph.links[index].id = newId;
        //     this.$store.state.currentGraph.links[index].type = newRelName;
        //     //前端更新
        //     this.updateGraph(this.$store.state.currentGraph);
        //   }
        // }
        // 后端方法需要修改
      },

      backHome() {
        document.getElementById('outerG').removeAttribute("transform");
      },

      resetAll() {
        if (this.initDone) {
          d3.select(".mainGraph").select("svg").remove();
          d3.select(".treeGraph").select("svg").remove();
          this.simulation = null;
          this.nodes = [];
          this.links = [];
          this.$store.state.currentGraph.nodes = [];
          this.$store.state.currentGraph.links = [];
          this.initDone = false;
          this.$refs.detailPanel.resetAll();
          this.nodeRadiusMap = new Map();
          this.activeTab = "first";
        }
      },

      exportJSON() {
        let _this = this;
        let blob = new Blob([JSON.stringify(_this.$store.state.currentGraph)]);
        let downloadAnchorNode = document.createElement('a');
        downloadAnchorNode.download = 'data.json';
        downloadAnchorNode.style.display = 'none';
        downloadAnchorNode.href = URL.createObjectURL(blob);
        document.body.appendChild(downloadAnchorNode);
        downloadAnchorNode.click();
        document.body.removeChild(downloadAnchorNode)
      },

      exportImg() {
        let _this = this;
        if (d3.select('svg').node() === null) {
          alert("当前图谱为空!")
        } else {
          let serializer = new XMLSerializer();
          let source = '<?xml version="1.0" standalone="no"?>\r\n' + serializer.serializeToString(d3.select('svg').node());
          let image = new Image;
          image.src = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(source);
          let canvas = document.createElement("canvas");
          canvas.width = 1000;
          canvas.height = 800;
          let context = canvas.getContext("2d");
          context.fillStyle = "#4c7a8e";
          context.fillRect(0, 0, 10000, 10000);
          image.onload = function () {
            context.drawImage(image, 0, 0);
            let a = document.createElement("a");
            a.download = "graph.png";
            a.href = canvas.toDataURL("image/png");
            a.click();
            a.remove();
          }
        }
      },

      tabSwitch(tab) {
        if (tab.name === 'second') {
          this.$refs.controlPanel.ifShow = false;
          this.$refs.detailPanel.ifShow = false;
        }
        if (tab.name === 'second' && this.initDone && this.lastActiveTab === "first") {
          this.initTree(this.currentTreeGraph);
        }
        if (tab.name === 'first') {
          this.$refs.controlPanel.ifShow = true;
          this.$refs.detailPanel.ifShow = true;
        }
        this.lastActiveTab = this.activeTab;
      }
    }
  }
</script>

<style>
  @import '../css/panel.css';
</style>
