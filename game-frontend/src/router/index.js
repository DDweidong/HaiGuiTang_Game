import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'
import AuthView from '../views/AuthView.vue'
import { getAccessToken } from '../utils/auth'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'home',
    component: HomeView
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView
  },
  {
    path: '/login',
    name: 'login',
    component: AuthView,
    meta: { public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫: 未登录访问受限页跳登录；已登录访问登录页跳回首页
router.beforeEach(to => {
  const hasToken = !!getAccessToken()
  if (to.meta.public) {
    return hasToken ? { name: 'home' } : true
  }
  if (!hasToken) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
