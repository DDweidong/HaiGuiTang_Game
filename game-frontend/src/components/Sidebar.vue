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
  </div>
</template>

<script setup>
import { ref } from 'vue'

const menuItems = ref([
  { name: '首页', route: 'home' },
  { name: '我的页面', route: 'profile' }
])
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
</style>