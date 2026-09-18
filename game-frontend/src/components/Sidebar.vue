<template>
  <div class="sidebar">
    <div class="logo-container">
      <img src="/logo.png" alt="海龟汤Logo" class="logo">
    </div>
    <div class="intro">
      <p>海龟汤是一款逻辑推理游戏，通过提问找出事件背后的真相。</p>
    </div>
    <ul>
      <li
        v-for="item in menuItems"
        :key="item.name"
        :class="{ active: $route.name === item.route }"
        @click="$router.push({ name: item.route })"
      >
        {{ item.name }}
      </li>
    </ul>
    <div class="user-area">
      <div class="username">{{ user.username }}</div>
      <button class="logout-btn" @click="logout">退出登录</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUser, clearLogin } from '../utils/auth'

const router = useRouter()
const user = ref(getUser() || { username: '' })

const menuItems = ref([
  { name: '首页', route: 'home' },
  { name: '我的页面', route: 'profile' }
])

function logout() {
  clearLogin()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.sidebar {
  width: 200px;
  height: 100vh;
  background-color: #f5f5f5;
  position: fixed;
  left: 0;
  top: 0;
  padding-top: 20px;
  box-shadow: 2px 0 5px rgba(0,0,0,0.1);
}

.logo-container {
  text-align: center;
  margin-bottom: 20px;
}

.logo {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
}

.intro {
  padding: 0 15px 20px 15px;
  text-align: center;
}

.intro p {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.sidebar ul {
  list-style-type: none;
  padding: 0;
  margin: 0;
}

.sidebar li {
  font-size: 20px;
  padding: 18px 26px;
  cursor: pointer;
  transition: background-color 0.3s;
  border-bottom: 1px solid #ddd;
}

.sidebar li:hover {
  background-color: #e0e0e0;
}

.sidebar li.active {
  background-color: #42b983;
  color: white;
}

.user-area {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #ddd;
}

.username {
  font-size: 16px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  font-size: 14px;
  color: #666;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.logout-btn:hover {
  background-color: #e0e0e0;
}
</style>