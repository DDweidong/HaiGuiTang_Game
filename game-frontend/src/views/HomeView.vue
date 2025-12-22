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
import { useRouter } from 'vue-router'

const gameStarted = ref(false)
const gameEnded = ref(false)
const messages = ref([])
const userInput = ref('')
const messagesContainer = ref(null)
const memoryId = ref('')
const messageCount = ref(0)
const router = useRouter()

const startGame = async () => {
  gameStarted.value = true
  gameEnded.value = false
  messageCount.value = 0
  
  // 生成memoryId: userId/时间戳
  const userId = window.gameUserId
  const timestamp = Date.now()
  memoryId.value = `${userId}/${timestamp}`
  
  // 清空消息
  messages.value = []
  
  // 发送初始请求
  try {
    const response = await fetch('http://localhost:8080/chat', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        memoryId: memoryId.value,
        message: "开始游戏"
      })
    })
    
    if (response.ok) {
      const text = await response.text()
      messages.value.push({ sender: 'ai', text: text })
      
      // 检查是否包含"游戏结束"
      if (text.includes("游戏结束")) {
        gameEnded.value = true
      }
      
      scrollToBottom()
    } else {
      messages.value.push({ sender: 'ai', text: "抱歉，初始化游戏时出现错误。" })
      scrollToBottom()
    }
  } catch (error) {
    console.error('Error:', error)
    messages.value.push({ sender: 'ai', text: "网络错误，请检查连接后重试。" })
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
      // 自动发送"猜不出来，公布答案"
      try {
        const response = await fetch('http://localhost:8080/chat', {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            memoryId: memoryId.value,
            message: "猜不出来，公布答案"
          })
        })
        
        if (response.ok) {
          const text = await response.text()
          messages.value.push({ sender: 'ai', text: text })
          
          // 检查是否包含"游戏结束"
          if (text.includes("游戏结束")) {
            gameEnded.value = true
          }
          
          scrollToBottom()
        }
      } catch (error) {
        console.error('Error:', error)
      }
      return
    }
    
    // 发送用户消息到后端
    try {
      const response = await fetch('http://localhost:8080/chat', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          memoryId: memoryId.value,
          message: userMessage
        })
      })
      
      if (response.ok) {
        const text = await response.text()
        messages.value.push({ sender: 'ai', text: text })
        
        // 检查是否包含"游戏结束"
        if (text.includes("游戏结束")) {
          gameEnded.value = true
        }
        
        scrollToBottom()
      } else {
        messages.value.push({ sender: 'ai', text: "抱歉，发送消息时出现错误。" })
        scrollToBottom()
      }
    } catch (error) {
      console.error('Error:', error)
      messages.value.push({ sender: 'ai', text: "网络错误，请检查连接后重试。" })
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