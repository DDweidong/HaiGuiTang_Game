import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 生成UUID的函数
function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// 获取或创建用户ID
let userId = localStorage.getItem('gameUserId');
if (!userId) {
  userId = generateUUID();
  localStorage.setItem('gameUserId', userId);
}

// 将用户ID添加到全局属性中，方便在整个应用中使用
window.gameUserId = userId;

//console.log('User ID:', userId);

const app = createApp(App)
app.use(router)
app.use(ElementPlus)
app.mount('#app')