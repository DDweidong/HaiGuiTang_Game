import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 用户身份由登录体系管理（W2）: token 与用户信息见 utils/auth.js，
// 不再使用匿名 UUID（window.gameUserId 已移除）
const app = createApp(App)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
