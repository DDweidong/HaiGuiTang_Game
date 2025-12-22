<template>
  <div class="profile">
    <h1 class="completed-title">已完成</h1>
    <div class="cards-container" v-if="cards.length > 0">
      <div class="card" v-for="(card, index) in cards" :key="index">
        <div class="card-title">{{ card.title }}</div>
        <el-popover
          placement="top-start"
          :width="300"
          trigger="hover"
        >
          <template #default>
            <div>{{ card.solution }}</div>
          </template>
          <template #reference>
            <div class="card-content">{{ card.solution }}</div>
          </template>
        </el-popover>
      </div>
    </div>
    <div class="no-data" v-else>
      <div class="no-data-text">暂无数据</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const cards = ref([])

onMounted(async () => {
  try {
    // 从全局属性获取用户ID
    const userId = window.gameUserId
    
    // 向指定地址发送GET请求
    const response = await fetch(`http://localhost:8080/turtle-soups/${userId}`)
    
    if (response.ok) {
      const data = await response.json()
      // 使用返回的数据更新卡片
      cards.value = data.map(item => ({
        title: item.title,
        solution: item.solution
      }))
    } else {
      // 如果请求不成功，则设置为空数组
      cards.value = []
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    // 出错时设置为空数组
    cards.value = []
  }
})
</script>

<style scoped>
.profile {
  padding: 20px;
}

.completed-title {
  text-align: left;
  margin-bottom: 20px;
  color: #333;
}

.cards-container {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.card {
  background-color: #f5f5f5;
  border-radius: 10px;
  padding: 15px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  transition: transform 0.3s ease;
  height: 180px;
  display: flex;
  flex-direction: column;
  min-width: 0; /* 确保卡片不会因内容而扩展 */
}

.card:hover {
  transform: translateY(-5px);
}

.card-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #42b983;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* 标题超出时显示省略号 */
}

.card-content {
  font-size: 15px;
  color: #666;
  line-height: 1.5;
  overflow: hidden; /* 强制隐藏溢出内容 */
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  line-clamp: 6;
  -webkit-line-clamp: 6;
  max-height: 9em; /* 1.5行高 × 6行 = 9em */
  flex-grow: 1;
  cursor: pointer;
}

.no-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.no-data-text {
  font-size: 36px;
  color: #999;
  font-weight: bold;
}
</style>