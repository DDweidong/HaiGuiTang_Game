<template>
  <div class="home">
    <div class="ellipse-container" :class="{ expanded: gameStarted }">
      <h1 class="title">海龟汤游戏</h1>
      
      <div class="content" v-if="!gameStarted">
        <div class="rules">
          <h2>游戏规则</h2>
          <p>1. 玩家提出问题来猜测故事的完整情节</p>
          <p>2. 问题必须是能用"是"、"否"或"无关"来回答的问题</p>
          <p>3. 根据回答逐步推理出完整的故事</p>
        </div>
        
        <button class="start-button" @click="startGame">开始游戏</button>
      </div>
      
      <div class="game-area" v-else>
        <div class="messages" ref="messagesContainer">
          <!-- 消息将会在这里显示 -->
          <div 
            class="message" 
            v-for="(message, index) in messages" 
            :key="index"
            :class="{ 'my-message': message.sender === 'me', 'ai-message': message.sender === 'ai' }"
          >
            <div class="message-content">
              {{ message.text }}
            </div>
          </div>
        </div>
        <div class="input-area">
          <input 
            type="text" 
            v-model="userInput" 
            placeholder="请输入你的问题..."
            @keyup.enter="sendMessage"
            :disabled="gameEnded"
          >
          <button @click="handleButtonAction" :class="{ 'back-button': gameEnded }">
            {{ gameEnded ? '返回首页' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import http from '../api/http'
import { getUser } from '../utils/auth'

const gameStarted = ref(false)
const gameEnded = ref(false)
const messages = ref([])
const userInput = ref('')
const messagesContainer = ref(null)
const memoryId = ref('')
const messageCount = ref(0)

const startGame = async () => {
  gameStarted.value = true
  gameEnded.value = false
  messageCount.value = 0

  // 生成memoryId: 登录用户ID/时间戳（后端会校验前缀与登录态一致）
  memoryId.value = `${getUser().userId}/${Date.now()}`

  // 清空消息
  messages.value = []

  try {
    // http 拦截器已解包 Result，直接拿到 AI 回复文本
    const reply = await http.put('/chat', {
      memoryId: memoryId.value,
      message: '开始游戏'
    })
    handleChatReply(reply)
  } catch (error) {
    messages.value.push({ sender: 'ai', text: '抱歉，初始化游戏时出现错误。' })
    scrollToBottom()
  }
}

const sendMessage = async () => {
  if (userInput.value.trim() !== '' && !gameEnded.value) {
    // 添加用户消息
    messages.value.push({ sender: 'me', text: userInput.value })
    const userMessage = userInput.value
    userInput.value = ''
    messageCount.value++

    // 滚动到底部
    scrollToBottom()

    // 检查是否达到30条消息限制
    if (messageCount.value >= 30) {
      // 自动发送"猜不出来，公布答案"；前端主动发起的揭晓，回复到达即视为游戏结束
      // （不依赖 AI 回复里的"游戏结束"标记——qwen-max 指令遵循不稳定，见后端 prompt 铁律）
      try {
        const reply = await http.put('/chat', {
          memoryId: memoryId.value,
          message: '猜不出来，公布答案'
        })
        handleChatReply(reply, true)
      } catch (error) {
        console.error('Error:', error)
      }
      return
    }

    // 发送用户消息到后端
    try {
      const reply = await http.put('/chat', {
        memoryId: memoryId.value,
        message: userMessage
      })
      // 用户主动要求揭晓/结束的消息同样强制结束游戏
      handleChatReply(reply, REVEAL_PATTERN.test(userMessage))
    } catch (error) {
      messages.value.push({ sender: 'ai', text: '网络错误，请检查连接后重试。' })
      scrollToBottom()
    }
  }
}

const handleButtonAction = () => {
  if (gameEnded.value) {
    // 返回首页
    resetGame()
  } else {
    // 发送消息
    sendMessage()
  }
}

const resetGame = () => {
  gameStarted.value = false
  gameEnded.value = false
  messages.value = []
  userInput.value = ''
  messageCount.value = 0
  memoryId.value = ''
}

// 揭晓/结束类请求: 与后端 prompt 铁律的触发词保持一致
const REVEAL_PATTERN = /公布答案|揭晓答案|结束游戏|猜不出来|放弃|不玩了/

// 把 AI 回复加入消息列表；约定: 游戏结束时 AI 回复包含"游戏结束"，
// forceEnd 用于前端主动发起揭晓的场景（不依赖 AI 是否输出该标记）
const handleChatReply = (reply, forceEnd = false) => {
  messages.value.push({ sender: 'ai', text: reply })
  if (forceEnd || reply.includes('游戏结束')) {
    gameEnded.value = true
  }
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.home {
  display: flex;
  justify-content: center;
  padding-top: 50px;
}

.ellipse-container {
  width: 100%;
  max-width: 990px;
  background-color: #f5f5f5;
  border-radius: 50px;
  padding: 0 36px 36px 36px;
  text-align: center;
  transition: all 0.5s ease;
  max-height: 500px;
  overflow: hidden;
  position: relative;
  box-shadow: 2px 0 5px rgba(0,0,0,0.1);
}

.ellipse-container.expanded {
  border-radius: 30px;
  max-height: 80vh; /* 修改：增加到80%视口高度 */
  overflow: hidden;
}

.title {
  font-size: 2.5rem;
  margin-bottom: 30px;
  color: #333;
}

.rules {
  background: rgba(255, 255, 255, 0.8);
  padding: 20px;
  border-radius: 15px;
  margin-bottom: 30px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.rules h2 {
  margin-top: 0;
  color: #333;
}

.rules p {
  margin: 14px 0;
  text-align: left;
  padding-left: 20px;
  color: #666;
}

.start-button {
  background: #42b983;
  color: white;
  border: none;
  padding: 15px 40px;
  font-size: 1.2rem;
  border-radius: 50px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: bold;
}

.start-button:hover {
  background: #359c6d;
  transform: scale(1.05);
}

.game-area {
  height: 590px; 
  display: flex;
  flex-direction: column;
}

.messages {
  flex: 1;
  overflow-y: auto;
  max-height: 60vh;
  padding: 10px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
}

.message {
  display: flex;
  margin-bottom: 15px;
}

.ai-message {
  justify-content: flex-start;
}

.my-message {
  justify-content: flex-end;
}

.message-content {
  max-width: 80%;
  padding: 12px 15px;
  border-radius: 18px;
  word-wrap: break-word;
}

.ai-message .message-content {
  background: rgba(66, 185, 131, 0.2);
  border-bottom-left-radius: 5px;
}

.my-message .message-content {
  background: #42b983;
  color: white;
  border-bottom-right-radius: 5px;
}

.input-area {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.input-area input {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 25px;
  outline: none;
  font-size: 18px;
}

.input-area input:disabled {
  background-color: #f0f0f0;
  cursor: not-allowed;
}

.input-area button {
  font-size: 18px;
  background: #42b983;
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 25px;
  cursor: pointer;
}

.input-area .back-button {
  background: #359c6d;
}
</style>