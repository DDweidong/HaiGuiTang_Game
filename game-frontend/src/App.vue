<template>
  <div id="app">
    <Sidebar />
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import Sidebar from './components/Sidebar.vue'
</script>

<style scoped>
#app {
  display: flex;
}

.main-content {
  flex: 1;
  margin-left: 200px;
  padding: 20px;
}
</style>