<template xmlns="http://www.w3.org/1999/html">
  <div style="position:relative;">
    <div class="button-box" v-drag>
      <div id="robotBtn" class="btn-bg-img" @click="openChat"></div>
    </div>

    <div class="chat-wrapper">
      <div class="chat-main" v-drag>
        <div class="chat-header">
          <span id="btn_close" class="chat-close" @click="closeChat">×</span>
          <div class="chat-service-info">
            <a class="chat-service-img"></a>
            <div class="chat-service-title">
              <p class="chat-service-intro" style="font-weight: bold;">大智</p>
              <p class="chat-service-intro" style="font-size: 12px">你好！我是法务问答机器人！</p>
            </div>
          </div>
        </div>
        <div id="chat_contain" class="chat-contain">
        </div>
        <div>
          <el-row class="qa-type-group">
            <el-button plain @click="chooseCategory(1)" class="qa-type">罪名预测</el-button>
            <el-button plain @click="chooseCategory(2)" class="qa-type">问题分类</el-button>
            <el-button plain @click="chooseCategory(3)" class="qa-type">罪名查询</el-button>
          </el-row>
        </div>
        <div class="chat-submit"
             v-loading="loading"
             element-loading-text="查找答案中"
             element-loading-spinner="el-icon-loading"
             element-loading-background="rgba(0, 0, 0, 0.8)"
        >
          <label for="chat_input"></label><textarea autofocus id="chat_input" class="chat-input-text"
                                                    @keydown="isEnter(chatInput, 'user',$event)"
        ></textarea>
          <div class="chat-input-tools" style="background: #F8F8FF;padding-bottom: 10px">
            <button class="chat-button" style="margin-left: 450px" @click="btnSend">发送</button>
            <button class="chat-button" style="margin-left: 25px; margin-right: 26px" @click="closeChat">关闭</button>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>
<script>
  import $ from 'jquery'
  import axios from "axios";

  export default {
    name: "ChatRobot",
    data() {
      return {
        questionCategory: 0,
        loading: false,
        isOpen: false,
        d: {},
        chatMain: {},
        chatHint: {},
        chat_main: {},
        chatInput: {},
        chatContain: {},
        btnClose: {},
        timer: {},
        timerId: 0,
        flagInput: false,
        shiftDown: false,
        isfirstOpen: true,
      }
    },
    mounted() {
      this.d = document;
      this.chatMain = $('.chat-main');
      this.chatHint = $('.chat-hint');
      // var chat_Hint = $('#chat_hint');
      // var headerInfoDiv = $('.header-info-div');
      this.chat_main = this.d.querySelector('.chat-main');
      this.chatInput = this.d.querySelector('#chat_input');
      this.chatContain = this.d.querySelector('#chat_contain');
      // var btnOpen = d.querySelector('#btn_open');
      this.btnClose = this.d.querySelector('#btn_close');
    },
    methods: {
      openChat() {
        let inDrag = document.getElementById('robotBtn').getAttribute('data-flag');
        if (inDrag === 'true') {
          return
        }
        $('.chat-main').css({
          'display': 'inline-block',
          'height': '570px'
        });
        if (this.isfirstOpen) {
          this.createInfo("service", "您好！请选择查询的类型");
          this.isfirstOpen = false;
        } else {
          this.createInfo("service", "欢迎回来~");
        }
      },
      closeChat() {
        $('.chat-main').animate({
          'height': '0'
        });
        $('.chat-main').css({
          'display': 'none'
        });
      },
      createInfo(name, value) {
        value = value.replace(/(((ht|f)tps?):\/\/)?([A-Za-z0-9]+\.[A-Za-z0-9]+[\/=\?%\-&_~`@[\]\':+!]*([^<>\"\"])*)/g, function (content) {
          return "<a href='http://" + content + "' class='chat-address' target='view_window' style='color:#1A1A1A '>" + content + '</a>';
        });
        var chatTime = new Date();
        chatTime = chatTime.toLocaleTimeString();
        var nodeP = this.d.createElement('p'),
          nodeSpan = this.d.createElement('span');
        var nodeTime = this.d.createElement('p');
        if(name==='service'){
          nodeP.style="margin-bottom:10px; text-align: left;"
          nodeSpan.style="margin-left: 40px;" +
            "display: inline-block;" +
            "position: relative;" +
            "padding: 10px;" +
            "max-width: 200px;" +
            "color: white;" +
            "white-space: pre-wrap;" +
            "border: 1px solid #58ACFA;" +
            "border-radius: 4px;" +
            "background-color: #58ACFA;" +
            "box-sizing: border-box;";
          nodeTime.style="margin: 0 0 0 37px;color: black;font-size: 12px;";
        }
        else{
          nodeP.style="margin-bottom: 10px; text-align: right;"
          nodeSpan.style="display: inline-block;" +
            "position: relative;" +
            "padding: 10px;" +
            "max-width: 200px;" +
            "color: white;" +
            "white-space: pre-wrap;" +
            "border: 1px solid #FA58AC;" +
            "border-radius: 4px;" +
            "background-color: #FA58AC;" +
            "box-sizing: border-box;" +
            "margin-right: 20px;" +
            "text-align: left;";
          nodeTime.style="margin: 0 20px 0 0; color: black; font-size: 12px;";
        }
        // nodeP.classList.add('chat-'+name+'-contain');
        // nodeSpan.classList.add('chat-'+name+'-text','chat-text');
        // nodeTime.classList.add('chat-time');

        nodeSpan.classList.add(name)
        nodeSpan.innerHTML = value;
        nodeTime.innerHTML = chatTime;
        nodeP.appendChild(nodeTime);
        nodeP.appendChild(nodeSpan);
        this.chatContain.appendChild(nodeP);
        this.chatContain.scrollTop = this.chatContain.scrollHeight;
      },
      // 监控是否按下enter
      isEnter(input, type, e) {
        if (e.keyCode === 16) { //按住shift键
          this.shiftDown = true;
        }
        if (e.keyCode === 13) { // enter
          if (this.shiftDown === true) {
            this.shiftDown = false;
            return true;
          } else if (
            this.shiftDown === false && input.value === '') {
            e.preventDefault();
            return true;
          } else {
            e.preventDefault();
            // 添加发送信息输入框
            this.createInfo(type, input.value);
            this.handleInput(input.value);
            input.value = null;
            input.focus();
          }
        }
      },
      handleInput(text) {
        if (this.questionCategory === 0){
          this.createInfo('service', "请先选择要查询的问题")
          return
        }
        this.loading = true;
        let _this = this;
        // axios({
        //   method: 'post',
        //   url: "https://zzy.claws.top/robot",
        //   data: {"flag": _this.questionCategory, "sent": text}
        // })
        axios.get("https://zzy.claws.top/robot/" + _this.questionCategory + '/' + text)
          .then(function (response) {
            _this.loading = false;
            if (response.status === 200) {
              _this.createInfo('service', response.data)
            }
          })
          .catch(function (error) {
            _this.loading = false;
            console.log(error)
          });
      },
      // 按发送按钮发送
      btnSend() {
        if (this.chatInput.value !== '') {
          this.createInfo('user', this.chatInput.value);
          this.handleInput(this.chatInput.value);
          this.chatInput.value = null;
          this.chatInput.focus();
        } else {
        }
      },
      chooseCategory(e) {
        this.questionCategory = e;
        if (e === 1) {
          this.createInfo('service', "请输入案件信息")
        }
        if (e === 2) {
          this.createInfo('service', "请输入咨询问题")
        }
        if (e === 3) {
          this.createInfo('service', "请输入想查询的罪名信息")
        }
      }
    },
    directives: {
      drag(el) {
        let oDiv = el;
        let downTime = '';
        let upTime = '';
        document.onselectstart = function () {
          return false
        };
        oDiv.onmousedown = (e) => {
          document.getElementById('robotBtn').setAttribute('data-flag', "true");
          downTime = new Date().getTime();
          //  算出鼠标相对元素的位置
          let disX = e.clientX - oDiv.offsetLeft;
          let disY = e.clientY - oDiv.offsetTop;
          document.onmousemove = (e) => {
            // 通过事件委托，计算移动的距离
            let l = e.clientX - disX;
            let t = e.clientY - disY;
            if (t > 0 && t < document.body.clientHeight - 48) {
              oDiv.style.top = t + 'px';
            }
            if (l > 0 && l < document.body.clientWidth) {
              oDiv.style.left = l + 'px';
            }
          };
          document.onmouseup = (e) => {
            document.onmousemove = null;
            document.onmouseup = null;
            upTime = new Date().getTime();
            if ((upTime - downTime) < 200) {
              document.getElementById('robotBtn').setAttribute('data-flag', "false")
            }
          }
        }
      },
    }
  }

</script>

<style scoped>
  @import '../css/chatRobot.css';
  @import '../css/reset.css';
</style>
