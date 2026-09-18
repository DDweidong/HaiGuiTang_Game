<template>
  <div id="app">
    <Sidebar v-if="$route.name !== 'login'" />
    <div class="main-content" :class="{ full: $route.name === 'login' }">
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

.main-content.full {
  margin-left: 0;
  padding: 0;
}
</style>
