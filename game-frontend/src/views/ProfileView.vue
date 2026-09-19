<template>
  <div class="profile">
    <h1 class="section-title">用量统计</h1>
    <div class="stats-cards" v-if="summary.totalCalls > 0">
      <div class="stat-card">
        <div class="stat-label">累计对话次数</div>
        <div class="stat-value">{{ summary.totalCalls }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计输入 Token</div>
        <div class="stat-value">{{ formatNumber(summary.totalInputTokens) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计输出 Token</div>
        <div class="stat-value">{{ formatNumber(summary.totalOutputTokens) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">估算费用</div>
        <div class="stat-value cost">¥{{ summary.estimatedCost }}</div>
      </div>
    </div>
    <div class="no-usage" v-else>暂无用量数据，去玩一局吧</div>

    <h1 class="section-title">已完成</h1>
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

    <div class="records-section" v-if="records.length > 0">
      <h2 class="section-title">最近调用明细</h2>
      <el-table :data="records" stripe style="width: 100%">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="model" label="模型" width="140" />
        <el-table-column prop="inputTokens" label="输入 Token" />
        <el-table-column prop="outputTokens" label="输出 Token" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'

const cards = ref([])
const summary = ref({ totalCalls: 0, totalInputTokens: 0, totalOutputTokens: 0, estimatedCost: 0 })
const records = ref([])

const formatNumber = (num) => (num ?? 0).toLocaleString()

onMounted(async () => {
  // 后端从登录态取 userId，只返回本人数据（http 拦截器已解包 Result 为 data）
  try {
    const usageData = await http.get('/token-usage/summary')
    summary.value = usageData
    const recordsData = await http.get('/token-usage/records', { params: { pageNum: 1, pageSize: 10 } })
    records.value = recordsData.records || []
  } catch (error) {
    console.error('获取用量数据失败:', error)
  }

  try {
    const data = await http.get('/turtle-soups', { params: { pageNum: 1, pageSize: 100 } })
    cards.value = (data.records || []).map(item => ({
      title: item.title,
      solution: item.solution
    }))
  } catch (error) {
    console.error('获取数据失败:', error)
    cards.value = []
  }
})
</script>

<style scoped>
.profile {
  padding: 20px;
}

.section-title {
  text-align: left;
  margin-bottom: 20px;
  color: #333;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background-color: #f5f5f5;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #888;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 26px;
  font-weight: bold;
  color: #333;
}

.stat-value.cost {
  color: #42b983;
}

.no-usage {
  padding: 20px 0 30px;
  color: #999;
}

.records-section {
  margin-bottom: 40px;
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